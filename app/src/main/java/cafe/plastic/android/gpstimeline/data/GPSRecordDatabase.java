package cafe.plastic.android.gpstimeline.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

@Database(entities = {GPSRecord.class}, version = 1)
@TypeConverters({GPSRecordConverters.class})
public abstract class GPSRecordDatabase extends RoomDatabase {
    public abstract GPSRecordDao gpsRecordDao();
}
