package cafe.plastic.android.gpstimeline.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;
import java.util.UUID;
@Entity
public class GPSRecord {
    @PrimaryKey
    private UUID mId;
    @ColumnInfo(name = "latitude")
    private double mLat;
    @ColumnInfo(name = "longitude")
    private double mLon;
    @ColumnInfo(name = "time")
    private long mTimestamp;

    public GPSRecord() {
        mLat = 0;
        mLon = 0;
        mId = UUID.randomUUID();
        mTimestamp = new Date().getTime();
    }

    public GPSRecord(double lat, double lon, UUID id, long timestamp){
        mLat = lat;
        mLon = lon;
        mId = id;
        mTimestamp = timestamp;
    }

    public double getLat() {
        return mLat;
    }

    public void setLat(double lat) {
        mLat = lat;
    }

    public double getLon() {
        return mLon;
    }

    public void setLon(double lon) {
        mLon = lon;
    }

    public UUID getId() {
        return mId;
    }

    public void setId(UUID id) {
        mId = id;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }
}
