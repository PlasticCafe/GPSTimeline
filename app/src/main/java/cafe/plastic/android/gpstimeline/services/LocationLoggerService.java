package cafe.plastic.android.gpstimeline.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.location.Location;
import android.media.Image;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import cafe.plastic.android.gpstimeline.R;
import cafe.plastic.android.gpstimeline.broadcastreceivers.ScreenOn;
import cafe.plastic.android.gpstimeline.data.GPSRecord;
import cafe.plastic.android.gpstimeline.data.GPSRecordDatabase;
import cafe.plastic.rxcameraman.CameraMan;
import cafe.plastic.android.gpstimeline.tools.LocationObserver;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class LocationLoggerService extends Service {
    private static final String TAG = LocationLoggerService.class.getSimpleName();
    private static final String CHANNEL_ID = "LocationLoggerService";
    private GPSRecordDatabase mDb;
    private ScreenOn mScreenReceiver = null;
    private LocationObserver mLocationObserver = new LocationObserver();
    private GoogleApiClient mClient;
    private CameraMan mCameraMan;
    private Observable<Pair<Location, byte[]>> mRecordWriter;
    private Disposable mRecordWriterSubscription;
    private PublishSubject<Boolean> onStartCommandObserver = PublishSubject.create();
    private boolean mInitialized = false;

    public static Intent newIntent(Context context) {
        return new Intent(context, LocationLoggerService.class);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDb = GPSRecordDatabase.getGPSRecordDatabase(this);
        mScreenReceiver = new ScreenOn(this);
        mClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
        mClient.connect();

        try {
            mCameraMan = new CameraMan.Builder(this)
                    .build();
            buildRecordWriter();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        initForeground();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mInitialized == false) {
            initForeground();
            return START_STICKY;
        }
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (action.equals(getString(R.string.service_add_log_now))) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "No permissions", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    onStartCommandObserver.onNext(true);
                }
            } else if (action.equals(getString(R.string.service_cancel_add_log))) {
                onStartCommandObserver.onNext(false);
            }
        } else {
            return START_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScreenReceiver != null) {
            unregisterReceiver(mScreenReceiver);
        }
        mScreenReceiver = null;
        mClient.disconnect();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initForeground() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.getApplicationContext())
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Active")
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.mapbox_marker_icon_default);
        buildActions(builder);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("GPSTimeline Location Logger");
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.enableLights(false);
            NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
            builder.setChannelId(CHANNEL_ID);
        }
        Notification notification = builder.build();
        startForeground(1, notification);
        mInitialized = true;
    }

    private void buildRecordWriter() throws CameraAccessException {
        mRecordWriter = mLocationObserver.getSubject()
                .map(loc -> {
                    Log.d(TAG, "Got location");
                    return loc;
                })
                .toFlowable(BackpressureStrategy.MISSING)
                .zipWith(mCameraMan.getPicture_ng(), (location, image) -> new Pair<Location, byte[]>(location, image))
                .toObservable()
                .share();
        onStartCommandObserver
                .sample(10, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((screen) -> {
                    if (screen) {
                        Log.d(TAG, "Screen on event, starting record capture");
                        if (mRecordWriterSubscription != null && mRecordWriterSubscription.isDisposed() != true) {
                            Log.d(TAG, "Disposing of old subscription");
                            mRecordWriterSubscription.dispose();
                        }
                        mRecordWriterSubscription = mRecordWriter.take(1)
                                .timeout(5, TimeUnit.SECONDS)
                                .observeOn(Schedulers.io())
                                .subscribe((info) -> {
                                    Location location = info.first;
                                    byte[] jpegBuffer = info.second;
                                    Log.d(TAG, "Got location and image");
                                    GPSRecord record = new GPSRecord();
                                    record.setLat(location.getLatitude());
                                    record.setLon(location.getLongitude());
                                    mDb.gpsRecordDao().insert(record);

                                    try {
                                        FileOutputStream fos = getApplicationContext().openFileOutput(record.getId().toString() + ".jpg", Context.MODE_PRIVATE);
                                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                                        bos.write(jpegBuffer);
                                        bos.flush();
                                        bos.close();
                                        record.setPicture(true);
                                        mDb.gpsRecordDao().update(record);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    Log.d(TAG, "Record inserted and image taken");
                                    mRecordWriterSubscription.dispose();
                                }, (err) -> {
                                    Log.d(TAG, err.toString())
                                    ;
                                });
                        getLocation();
                    } else {
                        if (mRecordWriterSubscription != null && mRecordWriterSubscription.isDisposed() != true) {
                            Log.d(TAG, "Disposing of old subscription");
                            mRecordWriterSubscription.dispose();
                        }
                    }
                });
    }

    private void buildActions(NotificationCompat.Builder builder) {
        Intent addIntent = new Intent(this, LocationLoggerService.class);
        addIntent.setAction(getString(R.string.service_add_log_now));
        builder.addAction(android.R.drawable.ic_menu_add, "Add new action", PendingIntent.getService(this, 0, addIntent, 0));
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        if (mClient.isConnected()) {
            LocationRequest request = LocationRequest.create();
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            request.setNumUpdates(1);
            request.setExpirationDuration(5000);
            request.setInterval(0);
            request.setSmallestDisplacement(20);
            LocationServices.FusedLocationApi
                    .requestLocationUpdates(mClient, request, mLocationObserver);
        }
    }

    private interface RxOnStartCommandCallback {
        void start();
    }
}
