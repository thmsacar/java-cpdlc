package flight;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeFormatter {

    private static final SimpleDateFormat sdfLocal = new SimpleDateFormat("HH:mm z");
    private static final SimpleDateFormat sdfZulu = new SimpleDateFormat("HH:mm'Z'");

    public TimeFormatter() {
    }

    public static String zuluTime(Date time) {
        sdfZulu.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdfZulu.format(time);
    }

    public static String localTime(Date time) {
        return sdfLocal.format(time);
    }


}
