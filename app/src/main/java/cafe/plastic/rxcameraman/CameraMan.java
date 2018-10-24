package cafe.plastic.rxcameraman;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;


import java.util.Arrays;
import java.util.List;

import cafe.plastic.rxcameraman.Callbacks.Device;
import cafe.plastic.rxcameraman.Callbacks.Request;
import cafe.plastic.rxcameraman.Callbacks.RxImageReader;
import cafe.plastic.rxcameraman.Callbacks.Session;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;


public class CameraMan {
    private final static String TAG = CameraMan.class.getSimpleName();
    private CameraManager mCameraManager;
    private String mCameraId;
    private Size[] mImageSizes;
    private Integer mImageFormat;
    private SurfaceTexture mPreviewTexture;
    private Surface mPreviewSurface;
    private List<Surface> mSurfaces;
    private ImageReader mCaptureReader;
    private HandlerThread thread = new HandlerThread("Camera thread");
    private Observable<Device.State> mCameraObserver;
    private Observable<Session.State> mSessionObserver;
    private Request mRequestManager;

    private static class CaptureState {
        static final int NO_LOCKS = 1;
        static final int FOCUS_LK = NO_LOCKS * 2;
        static final int EXPSR_LK = FOCUS_LK * 2;
        static final int AWB_LK = EXPSR_LK * 2;
        static final int ALL_LK = FOCUS_LK | EXPSR_LK | AWB_LK;

        public static boolean hasFocus(Integer state) {
            return (state & FOCUS_LK) == FOCUS_LK;
        }

        public static boolean hasExposure(Integer state) {
            return (state & EXPSR_LK) == EXPSR_LK;
        }

        public static boolean hasWhiteBalance(Integer state) {
            return (state & AWB_LK) == AWB_LK;
        }

        public static boolean hasTotalLock(Integer state) {
            return (state & ALL_LK) == ALL_LK;
        }

    }

    public CameraMan(CameraManager cameraManager, String camera, Size[] imageSizes, Integer imageFormat) {
        mCameraManager = cameraManager;
        mCameraId = camera;
        mImageSizes = imageSizes;
        mImageFormat = imageFormat;
        mPreviewTexture = new SurfaceTexture(10);
        mPreviewTexture.setDefaultBufferSize(mImageSizes[mImageSizes.length - 2].getWidth(), mImageSizes[mImageSizes.length - 2].getHeight());
        mPreviewSurface = new Surface(mPreviewTexture);
        mCaptureReader = ImageReader.newInstance(mImageSizes[0].getWidth(), mImageSizes[0].getHeight(), mImageFormat, 2);
        mSurfaces = Arrays.asList(mPreviewSurface, mCaptureReader.getSurface());
        mCameraObserver = new Device(mCameraManager, mCameraId).openCamera();
        mSessionObserver = new Session(mCameraObserver, mSurfaces).getSessionObserver();
        mRequestManager = new Request();
        thread.start();
    }

    @SuppressLint("CheckResult")
    public Flowable<byte[]> getPicture_ng() throws CameraAccessException {
        Observable<CaptureRequest.Builder> previewRequest =
                mCameraObserver.filter(cameraEvent -> cameraEvent.getEvent() == Device.State.Event.OPENED)
                        .flatMapSingle(openEvent -> Device.captureRequest(openEvent.getDevice(), CameraDevice.TEMPLATE_PREVIEW))
                        .map(builder -> {
                            builder.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_AUTO);
                            builder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                    CaptureRequest.CONTROL_AF_TRIGGER_START);
                            builder.set(CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_ON);
                            builder.set(CaptureRequest.CONTROL_AWB_MODE,
                                    CaptureRequest.CONTROL_AWB_MODE_AUTO);
                            builder.addTarget(mPreviewSurface);
                            return builder;
                        });

        Observable<CaptureRequest.Builder> captureRequest =
                mCameraObserver.filter(cameraEvent -> cameraEvent.getEvent() == Device.State.Event.OPENED)
                        .flatMapSingle(openEvent -> Device.captureRequest(openEvent.getDevice(), CameraDevice.TEMPLATE_STILL_CAPTURE))
                        .map(builder -> {
                            builder.addTarget(mCaptureReader.getSurface());
                            builder.set(CaptureRequest.JPEG_ORIENTATION,
                                    90);
                            return builder;
                        });

        Flowable<CaptureResult> previewCapture = mRequestManager.setRepeatingRequest(previewRequest, mSessionObserver)
                .map(capture -> {
                    processCaptureResult(capture);
                    return capture;
                }).share();

        Flowable<CaptureResult> mainCapture = mRequestManager
                .capture(previewCapture.toObservable()
                        .filter(previewState -> CaptureState.hasTotalLock(processCaptureResult(previewState)))
                        .zipWith(captureRequest, (previewState, request) -> request), mSessionObserver)
                .map(capture -> {
                    processCaptureResult(capture);
                    return capture;
                }).share();

        Flowable<byte[]> finalImage = RxImageReader.getImage(mCaptureReader)
                .map(image -> {
                    Log.d(TAG, "Got image");
                    return image;
                });
        return mainCapture.flatMap(capture-> finalImage).subscribeOn(AndroidSchedulers.from(thread.getLooper()));
    }

    private Integer processCaptureResult(CaptureResult result) {
        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
        Integer wbState = result.get(CaptureResult.CONTROL_AWB_STATE);
        Integer captureState = 0;
        String focusLog = "";
        if (afState != null && afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) {
            captureState |= CaptureState.FOCUS_LK;
            focusLog += " Focus Locked";
        }

        if (aeState != null && (aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED ||
                aeState == CaptureResult.CONTROL_AE_STATE_LOCKED ||
                aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED)) {
            captureState |= CaptureState.EXPSR_LK;
            focusLog += " Exposure Locked";
        }

        if (wbState != null && (wbState == CaptureResult.CONTROL_AWB_STATE_CONVERGED ||
                wbState == CaptureResult.CONTROL_AWB_STATE_LOCKED)) {
            captureState |= CaptureState.AWB_LK;
            focusLog += " White balance locked";
        }
        if (focusLog.isEmpty()) focusLog = "No locks";
        return captureState;
    }


    public static class Builder {
        private Integer mImageFormat = null;
        private Size[] mImageSizes = null;
        private String mCamera;
        private CameraManager mCameraManager;

        public Builder(Context context) {
            mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        }

        public Builder camera(String camera) {
            mCamera = camera;
            return this;
        }

        public Builder lens(Integer lens) throws CameraAccessException {
            mCamera = findCameraForLens(lens);
            return this;
        }

        public Builder format(Integer format) {
            mImageFormat = format;
            return this;
        }

        //Decision flow: Pick Camera -> pick image format -> pick image resolution
        private void configure() throws CameraAccessException {
            if (mCamera == null) {
                mCamera = getDefaultCamera();
            }
            if (mImageFormat == null) {
                mImageFormat = ImageFormat.JPEG;
            }
            mImageSizes = obtainOutputSizes();
        }

        public CameraMan build() throws CameraAccessException {
            configure();
            return new CameraMan(this.mCameraManager, this.mCamera, this.mImageSizes, this.mImageFormat);
        }

        private String getDefaultCamera() throws CameraAccessException {
            return mCameraManager.getCameraIdList()[0];
        }

        private Size[] obtainOutputSizes() throws CameraAccessException {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCamera);
            return characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(mImageFormat);
        }

        private String findCameraForLens(Integer lens) throws CameraAccessException {
            for (String camera : mCameraManager.getCameraIdList()) {
                CameraCharacteristics chars = mCameraManager.getCameraCharacteristics(camera);
                if (lens == chars.get(CameraCharacteristics.LENS_FACING)) {
                    return camera;
                }
            }
            return null;
        }

        private Size getLargestSize(Size[] sizes) {
            return sizes[sizes.length - 1];
        /*long largestPixelcount = 0;
        Size largestSize = null;
        for (Size size : sizes) {
            long pixelCount = size.getHeight() * size.getWidth();
            if (largestPixelcount < pixelCount) {
                largestPixelcount = size.getHeight() * size.getWidth();
                largestSize = size;
            }
        }
        return largestSize;*/
        }
    }
}
