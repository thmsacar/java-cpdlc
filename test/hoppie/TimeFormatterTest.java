package hoppie;

import org.junit.Test;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

/** Tests Zulu (UTC) time formatting utility in TimeFormatter. */
public class TimeFormatterTest {

    /** Verifies formatting of Date objects to HH:mmZ string representation. */
    @Test
    public void testZuluTimeFormatting() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2026, Calendar.JANUARY, 15, 14, 30, 0);
        Date time = cal.getTime();

        String zulu = TimeFormatter.zuluTime(time);
        assertEquals("14:30Z", zulu);
    }
}
