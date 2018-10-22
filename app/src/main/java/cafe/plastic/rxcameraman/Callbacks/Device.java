package cafe.plastic.rxcameraman.Callbacks;

import android.annotation.SuppressLint;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.functions.Action;

public class Device {
    private static final String TAG = Device.class.getSimpleName();
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice = null;
    private AtomicBoolean isCameraOpen = new AtomicBoolean(false);
    private String mCameraId;

    public static class State {
        public CameraDevice mDevice;
        public Event mEvent;

        public enum Event {
            OPENED,
            CLOSED,
            DISCONNECTED,
            ERROR
        }

        ;

        public State(CameraDevice device, Event event) {
            mDevice = device;
            mEvent = event;
        }

        public Event getEvent() {
            return mEvent;
        }

        public CameraDevice getDevice() {
            return mDevice;
        }
    }

    public Device(CameraManager cameraManager, String cameraId) {
        mCameraManager = cameraManager;
        mCameraId = cameraId;
    }

    @SuppressLint("MissingPermission")
    public Observable<State> openCamera() {
        Observable<State> camera = Observable.create(new ObservableOnSubscribe<State>() {
            @Override
            public void subscribe(ObservableEmitter<State> emitter) throws Exception {

                mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice cameraDevice) {
                        Log.d(TAG, "Camera opened");
                        synchronized (isCameraOpen) {
                            mCameraDevice = cameraDevice;
                            isCameraOpen.set(true);
                        }
                        emitter.onNext(new State(cameraDevice, State.Event.OPENED));
                    }

                    @Override
                    public void onClosed(@NonNull CameraDevice cameraDevice) {
                        Log.d(TAG, "Camera closed");
                        super.onClosed(cameraDevice);
                        isCameraOpen.set(false);
                        emitter.onNext(new State(cameraDevice, State.Event.CLOSED));
                        emitter.onComplete();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                        Log.d(TAG, "Camera disconnected");
                        closeCamera();
                        emitter.onNext(new State(cameraDevice, State.Event.DISCONNECTED));
                        emitter.onComplete();
                    }

                    @Override
                    public void onError(@NonNull CameraDevice cameraDevice, int i) {
                        Log.d(TAG, "Camera errored");
                        closeCamera();
                        emitter.onNext(new State(cameraDevice, State.Event.ERROR));
                        emitter.onComplete();
                    }
                }, null);
            }
        }).doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                Log.d(TAG, "Disposing camera");
                closeCamera();
            }
        }).share();
        return camera;
    }

    public static Single<CaptureRequest.Builder> captureRequest(CameraDevice cameraDevice, int template) {
        return Single.create(s -> {
            Log.d(TAG, "Building capture request");
            try {
                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(template);
                s.onSuccess(builder);
            } catch (CameraAccessException e) {
                s.onError(e);
            }
        });
    }

    private void closeCamera() {
        synchronized (isCameraOpen) {
            if (isCameraOpen.get()) {
                Log.d(TAG, "Closed camera manually");
                mCameraDevice.close();
                isCameraOpen.set(false);
            }
        }
    }
}
