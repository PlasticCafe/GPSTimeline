package cafe.plastic.android.gpstimeline.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface GPSRecordDao {
    @Query("SELECT * FROM records")
    List<GPSRecord> getAll();
}
