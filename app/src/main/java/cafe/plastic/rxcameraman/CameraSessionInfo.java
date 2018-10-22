package cafe.plastic.rxcameraman;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;

public class CameraSessionInfo {
    public CameraDevice mDevice;
    public CameraCaptureSession mSession;
    public CaptureRequest.Builder mBuilder;
    public Surface mSurface;

    public CameraSessionInfo(CameraDevice device, CameraCaptureSession session, CaptureRequest.Builder builder) {
        mDevice = device;
        mSession = session;
        mBuilder = builder;
    }

    public CameraDevice getDevice() {
        return mDevice;
    }

    public void setDevice(CameraDevice device) {
        mDevice = device;
    }

    public CameraCaptureSession getSession() {
        return mSession;
    }

    public void setSession(CameraCaptureSession session) {
        mSession = session;
    }

    public CaptureRequest.Builder getBuilder() {
        return mBuilder;
    }

    public void setBuilder(CaptureRequest.Builder builder) {
        mBuilder = builder;
    }

    public Surface getSurface() {
        return mSurface;
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }
}
