package cafe.plastic.android.gpstimeline.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;
@Entity(tableName = "records")
public class GPSRecord {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "uuid")
    private UUID mId;
    @ColumnInfo(name = "latitude")
    private double mLat;
    @ColumnInfo(name = "longitude")
    private double mLon;
    @ColumnInfo(name = "time")
    private long mTimestamp;
    @ColumnInfo(name = "picture")
    private boolean mPicture;

    public GPSRecord() {
        mLat = 0;
        mLon = 0;
        mId = UUID.randomUUID();
        mTimestamp = new Date().getTime();
        mPicture = false;
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

    public boolean getPicture() {
        return mPicture;
    }

    public void setPicture(boolean picture) {
        mPicture = picture;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GPSRecord gpsRecord = (GPSRecord) o;
        return Double.compare(gpsRecord.mLat, mLat) == 0 &&
                Double.compare(gpsRecord.mLon, mLon) == 0 &&
                mTimestamp == gpsRecord.mTimestamp &&
                mPicture == gpsRecord.mPicture &&
                Objects.equals(mId, gpsRecord.mId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(mId, mLat, mLon, mTimestamp, mPicture);
    }
}
