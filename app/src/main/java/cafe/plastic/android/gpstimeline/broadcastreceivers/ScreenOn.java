package cafe.plastic.android.gpstimeline.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import cafe.plastic.android.gpstimeline.R;
import cafe.plastic.android.gpstimeline.services.LocationLoggerService;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class ScreenOn extends BroadcastReceiver implements BroadcastObserver<Boolean> {
    public static final String TAG = ScreenOn.class.getSimpleName();
    private final PublishSubject<Boolean> screenTriggered = PublishSubject.create();

    public ScreenOn() {
        super();
    }

    public ScreenOn(Context context) {
        super();
        context.registerReceiver(this, getIntentFilter());
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            Log.d(TAG, "Got screen on action");
            Intent loggerService = LocationLoggerService.newIntent(context);
            loggerService.setAction(context.getString(R.string.service_add_log_now));
            context.startService(loggerService);
        } else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
        }
    }

    @Override
    public Subject<Boolean> getSubject() {
        return screenTriggered;
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        return filter;
    }
}
