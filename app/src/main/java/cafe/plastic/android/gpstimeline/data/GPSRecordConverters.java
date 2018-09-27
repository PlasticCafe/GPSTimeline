package cafe.plastic.android.gpstimeline.data;

import android.arch.persistence.room.TypeConverter;

import java.util.UUID;

public class GPSRecordConverters {
    @TypeConverter
    public static UUID fromString(String uuid) {
        return UUID.fromString(uuid);
    }

    @TypeConverter
    public static String fromUUID(UUID uuid) {
        return uuid.toString();
    }
}

