package cafe.plastic.rxcameraman.Callbacks;


import android.graphics.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Action;

public class Session {
    private static final String TAG = Session.class.getSimpleName();
    private List<Surface> mSurfaces;
    private Observable<State> mNewSessions;
    private CameraCaptureSession mSession;
    private AtomicBoolean isSessionOpen = new AtomicBoolean(false);

    public static class State {
        private CameraCaptureSession session;
        private Event event;

        public enum Event {
            ACTIVE,
            READY,
            CLOSED,
            CONFIGURED,
            FAILED
        }

        public State(CameraCaptureSession session, Event event) {
            this.session = session;
            this.event = event;
        }

        public CameraCaptureSession getSession() {
            return session;
        }

        public Event getEvent() {
            return event;
        }
    }

    public Session(Observable<Device.State> cameraObserver, List<Surface> surfaces) {
        mSurfaces = surfaces;
        mNewSessions = cameraObserver.filter(cameraEvent -> cameraEvent.getEvent() == Device.State.Event.OPENED)
                .flatMap(cameraEvent -> captureSession(cameraEvent.getDevice())
                        .filter(sessionEvent -> sessionEvent.getEvent() == Session.State.Event.CONFIGURED)).share();
    }

    public void setSurfaces(List<Surface> surfaces) {
        mSurfaces = surfaces;
    }

    public Observable<State> getSessionObserver() {
        return mNewSessions;
    }

    private Observable<State> captureSession(CameraDevice device) {
        return Observable.create(new ObservableOnSubscribe<State>() {
            @Override
            public void subscribe(ObservableEmitter<State> s) throws CameraAccessException {
                device.createCaptureSession(mSurfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onActive(@NonNull CameraCaptureSession session) {
                        super.onActive(session);
                        Log.d(TAG + " onActive", "Active");
                        openSession(session);
                        s.onNext(new State(session, State.Event.ACTIVE));
                    }


                    @Override
                    public void onReady(@NonNull CameraCaptureSession session) {
                        super.onReady(session);
                        Log.d(TAG + " onReady", "Ready");
                        openSession(session);
                        s.onNext(new State(session, State.Event.READY));
                    }

                    @Override
                    public void onClosed(@NonNull CameraCaptureSession session) {
                        super.onClosed(session);
                        Log.d(TAG + " onClosed", "Closed");
                        s.onNext(new State(session, State.Event.CLOSED));
                        s.onComplete();
                    }

                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        Log.d(TAG + " onConfigured", "Configured");
                        openSession(session);
                        s.onNext(new State(session, State.Event.CONFIGURED));
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.d(TAG + " onConfigureFailed", "Failed");
                        s.onNext(new State(session, State.Event.FAILED));
                        s.onComplete();
                    }
                }, null);
            }
        }).doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                synchronized (isSessionOpen) {
                    if(isSessionOpen.get()) {
                        Log.d(TAG, "Camera session is closing");
                        mSession.close();
                        isSessionOpen.set(false);
                    }
                }
            }
        });
    }

    private void openSession(CameraCaptureSession session) {
        synchronized (isSessionOpen) {
            if (!isSessionOpen.get()) {
                Log.d(TAG, "Camera Session is open");
                mSession = session;
                isSessionOpen.set(true);
            }
        }
    }
}
