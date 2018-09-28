package cafe.plastic.android.gpstimeline.tools;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatTools {
    public static String format(String format, long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(new Date(timestamp));
    }
}
