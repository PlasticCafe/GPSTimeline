package cafe.plastic.rxcameraman.Callbacks;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;

public class Request {
    private static final String TAG = Request.class.getSimpleName();
    private CaptureSessionCallback captureCallback;
    private CaptureSessionCallback repeatingCallback;

    public Flowable<CaptureResult> capture(Observable<CaptureRequest.Builder> builder, Observable<Session.State> session) {
        if (captureCallback != null) captureCallback.closeCapture();
        return Observable.zip(builder, session, new BiFunction<CaptureRequest.Builder, Session.State, Flowable<CaptureResult>>() {
                    @Override
                    public Flowable<CaptureResult> apply(CaptureRequest.Builder builder, Session.State session) {
                        return Flowable.create(s -> {
                            Log.d(TAG, "Opening capture request");
                            captureCallback = new CaptureSessionCallback(s);
                            session.getSession().capture(builder.build(), captureCallback, null);
                        }, BackpressureStrategy.BUFFER);
                    }
                }
        ).toFlowable(BackpressureStrategy.DROP)
                .flatMap(f -> f).doOnCancel(() -> {
                    Log.d(TAG, "Capture request disposed");
                    if (captureCallback != null)
                        captureCallback.closeCapture();
                });
    }


    public Flowable<CaptureResult> setRepeatingRequest(Observable<CaptureRequest.Builder> builder, Observable<Session.State> session) {
        if (repeatingCallback != null) repeatingCallback.closeCapture();
        return Observable.zip(builder, session, new BiFunction<CaptureRequest.Builder, Session.State, Flowable<CaptureResult>>() {
                    @Override
                    public Flowable<CaptureResult> apply(CaptureRequest.Builder builder, Session.State session) {
                        return Flowable.create(s -> {
                            Log.d(TAG, "Opening repeating request");
                            repeatingCallback = new CaptureSessionCallback(s);
                            session.getSession().setRepeatingRequest(builder.build(), repeatingCallback, null);
                        }, BackpressureStrategy.BUFFER);
                    }
                }
        ).toFlowable(BackpressureStrategy.DROP)
                .flatMap(f -> f).doOnCancel(() -> {
                    Log.d(TAG, "Repeating request disposed");
                    if (repeatingCallback != null) repeatingCallback.closeCapture();
                });
    }

    private static class CaptureSessionCallback extends CameraCaptureSession.CaptureCallback {
        private FlowableEmitter<CaptureResult> s;
        private CameraCaptureSession mCapture;
        private final AtomicBoolean isCaptureOpen = new AtomicBoolean(false);

        CaptureSessionCallback(FlowableEmitter<CaptureResult> _s) {
            s = _s;
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session,@NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.d(TAG, "Capture failed");
            s.onComplete();
        }

        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            openSession(session);
            s.onNext(partialResult);
        }


        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            openSession(session);
            s.onNext(result);
        }

        void closeCapture() {
            synchronized (isCaptureOpen) {
                if (isCaptureOpen.get()) {
                    Log.d(TAG, "Closing capture requests");
                    try {
                        mCapture.stopRepeating();
                        mCapture.abortCaptures();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    } finally {
                        isCaptureOpen.set(false);
                    }
                    s.onComplete();
                }
            }
        }

        private void openSession(@NonNull CameraCaptureSession session) {
            synchronized (isCaptureOpen) {
                if (!isCaptureOpen.get()) {
                    Log.d(TAG, "Capture request open");
                    mCapture = session;
                    isCaptureOpen.set(true);
                }
            }
        }
    }

}
