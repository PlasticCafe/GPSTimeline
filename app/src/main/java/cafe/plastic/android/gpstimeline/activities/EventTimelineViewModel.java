package cafe.plastic.android.gpstimeline.activities;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;

import java.util.ArrayList;

import cafe.plastic.android.gpstimeline.data.GPSRecord;

public class EventTimelineViewModel extends AndroidViewModel {
    private ArrayList<GPSRecord> mGPSRecords;
    public EventTimelineViewModel(@NonNull Application application) {
        super(application);
        mGPSRecords = new ArrayList<>();
        for(int i=0; i<100; i++) {
            mGPSRecords.add(new GPSRecord());
        }
    }

    public ArrayList<GPSRecord> getGPSRecords() {
        return mGPSRecords;
    }
}
