package hoppie;

import org.junit.Test;
import static org.junit.Assert.*;

public class HoppieUrlEncodingTest {

    @Test
    public void testEncodeParam() {
        // Spaces should be encoded as '+'
        assertEquals("HELLO+WORLD", HoppieAPI.encodeParam("HELLO WORLD"));

        // Special query characters (&, #, +, =) should be safely percentage-encoded
        assertEquals("ATIS+%26+METAR%231", HoppieAPI.encodeParam("ATIS & METAR#1"));
        assertEquals("%2B1000FT", HoppieAPI.encodeParam("+1000FT"));
        assertEquals("PARAM%3DVALUE", HoppieAPI.encodeParam("PARAM=VALUE"));

        // Forward slashes should be preserved for CPDLC message headers
        assertEquals("/data2/1//WU/REQUEST+DIRECT", HoppieAPI.encodeParam("/data2/1//WU/REQUEST DIRECT"));

        // Null and empty strings
        assertEquals("", HoppieAPI.encodeParam(null));
        assertEquals("", HoppieAPI.encodeParam(""));
    }

    @Test
    public void testCreateFullUrl() {
        HoppieAPI api = new HoppieAPI("TESTLOGON123");
        String url = api.createFullUrl("KLM123", "EDGG_CTR", "cpdlc", "/data2/1//WU/CLIMB TO FL350 & MAINTAIN");

        assertTrue(url.startsWith("http://www.hoppie.nl/acars/system/connect.html/connect.html?"));
        assertTrue(url.contains("logon=TESTLOGON123"));
        assertTrue(url.contains("from=KLM123"));
        assertTrue(url.contains("to=EDGG_CTR"));
        assertTrue(url.contains("type=cpdlc"));
        assertTrue(url.contains("packet=/data2/1//WU/CLIMB+TO+FL350+%26+MAINTAIN"));
    }

    @Test
    public void testSafeUserText() {
        assertEquals("CLIMB 5000∕FL100", HoppieAPI.safeUserText("CLIMB 5000/FL100"));
        assertEquals("EDGG∕CTR", HoppieAPI.safeUserText("EDGG/CTR"));
        assertEquals("", HoppieAPI.safeUserText(null));
    }
}
