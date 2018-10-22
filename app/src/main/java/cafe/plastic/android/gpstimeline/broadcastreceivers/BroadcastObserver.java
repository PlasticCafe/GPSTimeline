package cafe.plastic.android.gpstimeline.broadcastreceivers;

import io.reactivex.subjects.Subject;

public interface BroadcastObserver<T> {
    Subject<T> getSubject();
}
