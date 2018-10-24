package cafe.plastic.android.gpstimeline.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

@Database(entities = {GPSRecord.class}, version = 2)
@TypeConverters({GPSRecordConverters.class})
public abstract class GPSRecordDatabase extends RoomDatabase {
    private static GPSRecordDatabase instance;
    public abstract GPSRecordDao gpsRecordDao();
    public synchronized static GPSRecordDatabase getGPSRecordDatabase(Context context) {
        if(instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), GPSRecordDatabase.class, "gpsrecord-database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
