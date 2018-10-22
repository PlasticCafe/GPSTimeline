package cafe.plastic.android.gpstimeline.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import cafe.plastic.android.gpstimeline.services.LocationLoggerService;

public class Bootup extends BroadcastReceiver {
    public static final String TAG = Bootup.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Bootup broadcast received");
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "testificate", Toast.LENGTH_LONG).show();
                }
            });
            Intent loggerService = new Intent(context, LocationLoggerService.class);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(loggerService);
            } else {
                context.startService(loggerService);
            }
    }
}
