package cafe.plastic.android.gpstimeline.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;
import java.util.UUID;

@Dao
public interface GPSRecordDao {
    @Insert
    void insert(GPSRecord record);

    @Insert
    void insertAll(List<GPSRecord> records);

    @Update
    void update(GPSRecord record);

    @Update
    void update(List<GPSRecord> records);

    @Delete
    void delete(GPSRecord record);

    @Query("SELECT * FROM records")
    List<GPSRecord> getAll();

    @Query("SELECT * FROM records WHERE uuid=:id")
    GPSRecord get(UUID id);


}
