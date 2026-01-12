package flight;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeFormatter {

    private static final SimpleDateFormat sdfLocal = new SimpleDateFormat("HH:mm z");
    private static final SimpleDateFormat sdfZulu = new SimpleDateFormat("HH:mm'Z'");

    public TimeFormatter() {
    }

    public String zuluTime(Date time) {
        sdfZulu.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdfZulu.format(time);
    }

    public String localTime(Date time) {
        return sdfLocal.format(time);
    }


}
