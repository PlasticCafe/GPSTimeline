package cafe.plastic.android.gpstimeline.activities;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.persistence.room.Room;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import cafe.plastic.android.gpstimeline.data.GPSRecord;
import cafe.plastic.android.gpstimeline.data.GPSRecordDatabase;

public class EventTimelineViewModel extends AndroidViewModel {
    private GPSRecordDatabase db;

    public EventTimelineViewModel(@NonNull Application application) {
        super(application);
        db = Room.databaseBuilder(application, GPSRecordDatabase.class, "gpsrecord-database").build();
    }

    public List<GPSRecord> getGPSRecords() {
        return db.gpsRecordDao().getAll();
    }
}
