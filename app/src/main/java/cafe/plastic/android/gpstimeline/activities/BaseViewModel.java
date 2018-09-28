package cafe.plastic.android.gpstimeline.activities;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Room;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cafe.plastic.android.gpstimeline.data.GPSRecord;
import cafe.plastic.android.gpstimeline.data.GPSRecordDatabase;

public class BaseViewModel extends AndroidViewModel {
    public ObservableList<GPSRecord> mRecords = new ObservableArrayList<>();

    private Executor diskThread = Executors.newSingleThreadExecutor();
    private GPSRecordDatabase mDb;
    private DatabaseTask mDatabaseTask;

    public BaseViewModel(@NonNull Application application) {
        super(application);
        mDb = Room.databaseBuilder(application, GPSRecordDatabase.class, "gpsrecord-database").build();
    }

    public LiveData<List<GPSRecord>> getGPSRecords() {
        /*if(mDatabaseTask == null || mDatabaseTask.getStatus() == AsyncTask.Status.FINISHED) {
            mDatabaseTask = new DatabaseTask();
            mDatabaseTask.execute();
        }*/
        return mDb.gpsRecordDao().getAll();
    }

    public LiveData<GPSRecord> getGPSRecord(UUID id) {
        return mDb.gpsRecordDao().get(id);
    }

    public void addRecord(final GPSRecord record) {
        Runnable add = new Runnable() {
            @Override
            public void run() {
                mDb.gpsRecordDao().insert(record);
            }
        };
        diskThread.execute(add);
    }

    public void updateRecord(final GPSRecord record) {
        Runnable update = new Runnable() {
            @Override
            public void run() {
                mDb.gpsRecordDao().update(record);
            }
        };
        diskThread.execute(update);
    }

    public void deleteRecord(final GPSRecord record) {
        Runnable delete = new Runnable() {
            @Override
            public void run() {
                mDb.gpsRecordDao().delete(record);
            }
        };
        diskThread.execute(delete);
    }

    private class DatabaseTask extends AsyncTask<GPSRecord, Void, Void> {

        @Override
        protected Void doInBackground(GPSRecord... record) {
            mDb.gpsRecordDao().insert(record[0]);
            return null;
        }
    }
}
