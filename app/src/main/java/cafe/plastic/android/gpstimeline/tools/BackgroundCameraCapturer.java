package cafe.plastic.android.gpstimeline.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

public class BackgroundCameraCapturer {
    private static final String TAG = BackgroundCameraCapturer.class.getSimpleName();
    private String mOutputPath;
    private Context mContext;
    private Handler mHandler;
    private String mCameraId;
    private Size[] mJpegSizes;
    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private boolean mSessionStatus = false;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraCaptureSessionCallbacks mCaptureSessionCallbacks;
    private CameraStates mState = CameraStates.INACTIVE;

    enum CameraStates
    {
        INACTIVE,
        IDLE,
        WAITING_DEVICE,
        DEVICE_READY,
        WAITING_LOCK,
        WAITING_PRECAPTURE,
        WAITING_DONE,
        TAKEN
    }
    public BackgroundCameraCapturer(Context context) {
        mContext = context;
        HandlerThread handlerThread = new HandlerThread("Background Camera Thread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mCaptureSessionCallbacks = new CameraCaptureSessionCallbacks();
        mState = CameraStates.INACTIVE;
        CameraManager cameraManager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId: cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = cameraId;
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    mJpegSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
                    break;
                }
            }
            if(mCameraId == null) {
                throw new CameraAccessException(CameraAccessException.CAMERA_DISABLED);
            }
            if(mJpegSizes.length <= 0) {
                throw new CameraAccessException(CameraAccessException.CAMERA_ERROR);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            releaseCamera();
        }

        mState = CameraStates.IDLE;
    }

    @SuppressLint("MissingPermission")
    public void capture(String outputPath) {
        mSessionStatus = true;
        mOutputPath = outputPath;
        if(mState.ordinal() >= CameraStates.WAITING_LOCK.ordinal()) {
            releaseCamera();
        }
        Size jpegSize = mJpegSizes[0];
        mImageReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(), ImageFormat.JPEG, 2);
        CameraManager cameraManager = (CameraManager)mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraManager.openCamera(mCameraId, new DeviceStateCallbacks(), mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            releaseCamera();
        }
        mState = CameraStates.WAITING_DEVICE;
    }

    public synchronized boolean isSessionActive() {
        return mSessionStatus;
    }
    private class DeviceStateCallbacks extends CameraDevice.StateCallback {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Camera Opened");
            if(mState.ordinal() != CameraStates.WAITING_DEVICE.ordinal()) return;
            mCameraDevice = cameraDevice;
            mImageReader.setOnImageAvailableListener(new ImageListener(), mHandler);
            ArrayList<Surface> surfaces = new ArrayList<>();
            surfaces.add(mImageReader.getSurface());
            try {
                mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
                mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CaptureRequest.CONTROL_AF_TRIGGER_START);
                mCameraDevice.createCaptureSession(surfaces, new SessionStateCallbacks(), mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                releaseCamera();
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
                releaseCamera();
            }
            mState = CameraStates.DEVICE_READY;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Camera disconnected");
            mCameraDevice = null;
            releaseCamera();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Log.d(TAG, "Camera Error");
            releaseCamera();
        }
    }

    private class SessionStateCallbacks extends CameraCaptureSession.StateCallback {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.d(TAG, "Session configurded");
            if(mState.ordinal() != CameraStates.DEVICE_READY.ordinal()) return;
            try {
                mCameraCaptureSession = cameraCaptureSession;
                mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), mCaptureSessionCallbacks, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                releaseCamera();
            }
            mState = CameraStates.WAITING_LOCK;
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.d(TAG, "Session configure failed");
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            mCameraCaptureSession = null;
        }
    }

    private class CameraCaptureSessionCallbacks extends CameraCaptureSession.CaptureCallback {
        private void progress(CaptureResult partialResult) {
            switch(mState) {
                case WAITING_LOCK: {
                    Log.d(TAG, "WAITING_LOCK");
                    Integer afState = partialResult.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null || afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) {
                        Log.d(TAG, "Focused");
                        mCaptureRequestBuilder = null;
                        try {
                            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
                            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
                            mState = CameraStates.WAITING_PRECAPTURE;
                            mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), mCaptureSessionCallbacks, mHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                            releaseCamera();
                        }
                    } else {
                        break;
                    }
                }
                case WAITING_PRECAPTURE: {
                    Log.d(TAG, "WAITING_PRECAPTURE");
                    Integer aeState = partialResult.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = CameraStates.WAITING_DONE;
                    } else {
                        break;
                    }
                }
                case WAITING_DONE: {
                    Log.d(TAG, "WAITING_DONE");
                    Integer aeState = partialResult.get(CaptureResult.CONTROL_AE_STATE);
                    if(aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        try {
                            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
                            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
                            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);
                            CameraCaptureSession.CaptureCallback captureCallback =
                                    new CameraCaptureSession.CaptureCallback() {
                                        @Override
                                        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                            Log.d(TAG, "Image written");
                                        }
                                    };
                            mCameraCaptureSession.stopRepeating();
                            mCameraCaptureSession.abortCaptures();
                            mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), captureCallback, mHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                            releaseCamera();
                        }
                        mState = CameraStates.TAKEN;
                    }
                    break;
                }
                case TAKEN:
                    Log.d(TAG, "TAKEN");
                    break;
            }
        }
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            progress(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            Log.d(TAG, "Capture completed");
            progress(result);
        }

    }
    private class ImageListener implements ImageReader.OnImageAvailableListener {

        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Log.d(TAG, "Picture taken.");
            if(imageReader.getMaxImages() == 0) return;
            Image image = imageReader.acquireLatestImage();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            final byte[] data = new byte[buffer.capacity()];
            buffer.get(data);
            try {
                FileOutputStream fos = getApplicationContext().openFileOutput(mOutputPath + ".jpg", Context.MODE_PRIVATE);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                bos.write(data);
                bos.flush();
                bos.close();
                image.close();
                imageReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
            releaseCamera();
            mState = CameraStates.IDLE;
        }
    }

    synchronized private void releaseCamera() {
        Log.d(TAG, "Camera released");
        if(mCameraDevice != null) {
            try {
                mCameraDevice.close();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            mCameraDevice = null;
        }
        if(mCameraCaptureSession != null){
            try {
                mCameraCaptureSession.abortCaptures();
                mCameraCaptureSession.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            mCameraCaptureSession.close();
        }
        mCameraCaptureSession = null;
        mCameraDevice = null;
        if(mImageReader != null) mImageReader.close();
        mState = CameraStates.IDLE;
        mSessionStatus = false;
    }
}
