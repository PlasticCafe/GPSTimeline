package cafe.plastic.android.gpstimeline.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;

public class CameraCapturer {
    private static final String TAG = CameraCapturer.class.getSimpleName();
    private static final int LENS = CameraCharacteristics.LENS_FACING_BACK;
    private String mCameraId = null;
    private Size mImageSize = null;
    private OnCameraFinished mFinishedCallback = null;
    private Context mContext = null;
    private CameraManager mCameraManager = null;
    public interface OnCameraFinished {
        void receiveImage(boolean success, byte[] image);
    }

    CameraCapturer(Context context, OnCameraFinished finishedCallback) {
        mContext = context;
        mFinishedCallback = finishedCallback;
    }

    @SuppressLint("MissingPermission")
    public void takePicture() {
        mCameraManager = (CameraManager)mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            getCameraId();
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to call getCameraId()");
            e.printStackTrace();
            mFinishedCallback.receiveImage(false, new byte[]{});
            return;
        }
        ImageReader imageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 2);

        try {
            mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {

                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {

                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {

                }
            }, null);
        } catch (CameraAccessException e) {

        }



    }

    private void getCameraId() throws CameraAccessException {
        try {
            String[] cameraIds = mCameraManager.getCameraIdList();
            String targetCameraId;
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == LENS) {
                    StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    mCameraId = cameraId;
                    mImageSize = configurationMap.getOutputSizes(ImageFormat.JPEG)[0];
                    return;
                }
            }
            if (mCameraId == null) {
                throw new CameraAccessException(CameraAccessException.CAMERA_ERROR);
            }
        } catch (CameraAccessException e) {
            if(mCameraId == null)
                Log.e(TAG, "Error getting a camera ID");
            if(mImageSize == null)
                Log.e(TAG, "Error getting image sizes");
            throw e;
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera(ImageReader imageReader) {

        try {
            mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {

                    openSessionForFocus(cameraDevice, imageReader);

                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {

                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openSessionForFocus(CameraDevice cameraDevice, ImageReader imageReader) {
        List<Surface> surfaces = new ArrayList<>();
        surfaces.add(imageReader.getSurface());
        try {
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    
                    try {
                        cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                CaptureRequest.Builder requestBuilder = null;
                                try {
                                    requestBuilder = cameraDevice.createCaptureRequest((CameraDevice.TEMPLATE_PREVIEW));
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                                requestBuilder.addTarget(surfaces.get(0));
                                configureRequestBuilder(requestBuilder);
                                try {
                                    cameraCaptureSession.setRepeatingRequest(requestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                                        public void progress(CaptureResult result) throws CameraAccessException {
                                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                                            if(afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                                                    && (aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED)) {
                                                cameraCaptureSession.abortCaptures();
                                                cameraCaptureSession.stopRepeating();
                                                cameraCaptureSession.close();
                                                openSessionForCapture(cameraDevice, imageReader);
                                            }
                                        }
                                        @Override
                                        public void onCaptureProgressed( @NonNull CameraCaptureSession session,  @NonNull CaptureRequest request,  @NonNull CaptureResult partialResult) {
                                            try {
                                                progress(partialResult);
                                            } catch (CameraAccessException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onCaptureCompleted( @NonNull CameraCaptureSession session,  @NonNull CaptureRequest request,  @NonNull TotalCaptureResult result) {
                                            try {
                                                progress(result);
                                            } catch (CameraAccessException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onCaptureFailed( @NonNull CameraCaptureSession session,  @NonNull CaptureRequest request,  @NonNull CaptureFailure failure) {
                                        }
                                    }, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openSessionForCapture(CameraDevice cameraDevice, ImageReader imageReader) {

    }

    private void configureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.JPEG_ORIENTATION, 90);
                    builder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                            CaptureRequest.CONTROL_AF_TRIGGER_START);
                    builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                    builder.set(CaptureRequest.CONTROL_AWB_MODE,
                            CaptureRequest.CONTROL_AWB_MODE_AUTO);
    }
}
