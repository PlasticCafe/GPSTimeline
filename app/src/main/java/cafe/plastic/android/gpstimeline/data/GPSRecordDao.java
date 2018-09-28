package cafe.plastic.android.gpstimeline.data;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;
import java.util.UUID;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface GPSRecordDao {
    @Insert(onConflict = REPLACE)
    void insert(GPSRecord record);

    @Insert(onConflict = REPLACE)
    void insertAll(List<GPSRecord> records);

    @Update(onConflict = REPLACE)
    void update(GPSRecord record);

    @Update(onConflict = REPLACE)
    void update(List<GPSRecord> records);

    @Delete
    void delete(GPSRecord record);

    @Query("SELECT * FROM records")
    LiveData<List<GPSRecord>> getAll();

    @Query("SELECT * FROM records WHERE uuid=:id")
    LiveData<GPSRecord> get(UUID id);


}
