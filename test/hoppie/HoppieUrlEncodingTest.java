package hoppie;

import org.junit.Test;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/** Tests URL parameter encoding, full URL construction, and slash sanitization in HoppieAPI. */
public class HoppieUrlEncodingTest {

    private String invokeEncodeParam(String param) throws Exception {
        Method method = HoppieAPI.class.getDeclaredMethod("encodeParam", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, param);
    }

    private String invokeCreateFullUrl(HoppieAPI api, String from, String to, String type, String packet) throws Exception {
        Method method = HoppieAPI.class.getDeclaredMethod("createFullUrl", String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(api, from, to, type, packet);
    }

    /** Verifies URL parameter encoding while preserving CPDLC header slashes. */
    @Test
    public void testEncodeParam() throws Exception {
        // Spaces should be encoded as '+'
        assertEquals("HELLO+WORLD", invokeEncodeParam("HELLO WORLD"));

        // Special query characters (&, #, +, =) should be safely percentage-encoded
        assertEquals("ATIS+%26+METAR%231", invokeEncodeParam("ATIS & METAR#1"));
        assertEquals("%2B1000FT", invokeEncodeParam("+1000FT"));
        assertEquals("PARAM%3DVALUE", invokeEncodeParam("PARAM=VALUE"));

        // Forward slashes should be preserved for CPDLC message headers
        assertEquals("/data2/1//WU/REQUEST+DIRECT", invokeEncodeParam("/data2/1//WU/REQUEST DIRECT"));

        // Null and empty strings
        assertEquals("", invokeEncodeParam(null));
        assertEquals("", invokeEncodeParam(""));
    }

    /** Verifies full URL query parameter construction for Hoppie network requests. */
    @Test
    public void testCreateFullUrl() throws Exception {
        HoppieAPI api = new HoppieAPI("TESTLOGON123");
        String url = invokeCreateFullUrl(api, "KLM123", "EDGG_CTR", "cpdlc", "/data2/1//WU/CLIMB TO FL350 & MAINTAIN");

        assertTrue(url.startsWith("https://www.hoppie.nl/acars/system/connect.html/connect.html?"));
        assertTrue(url.contains("logon=TESTLOGON123"));
        assertTrue(url.contains("from=KLM123"));
        assertTrue(url.contains("to=EDGG_CTR"));
        assertTrue(url.contains("type=cpdlc"));
        assertTrue(url.contains("packet=/data2/1//WU/CLIMB+TO+FL350+%26+MAINTAIN"));
    }

    /** Verifies user text sanitization replacing ASCII slashes with Unicode division slashes and stripping newlines. */
    @Test
    public void testSafeUserText() {
        assertEquals("CLIMB 5000∕FL100", HoppieAPI.safeUserText("CLIMB 5000/FL100"));
        assertEquals("EDGG∕CTR", HoppieAPI.safeUserText("EDGG/CTR"));
        assertEquals("REMARK LINE 1 REMARK LINE 2", HoppieAPI.safeUserText("REMARK LINE 1\n REMARK LINE 2"));
        assertEquals("REMARK LINE 1REMARK LINE 2", HoppieAPI.safeUserText("REMARK LINE 1\r\nREMARK LINE 2"));
        assertEquals("", HoppieAPI.safeUserText(null));
    }

    /** Verifies parsing of Hoppie server error payloads and HTTP error status codes. */
    @Test
    public void testParseErrorMessage() {
        HoppieAPI.HoppieResponse hoppieErr = new HoppieAPI.HoppieResponse(200, "error {invalid logon key}");
        assertEquals("ERROR: invalid logon key", HoppieAPI.parseErrorMessage(hoppieErr));

        HoppieAPI.HoppieResponse http500Err = new HoppieAPI.HoppieResponse(500, "Internal Server Error");
        assertEquals("ERROR: HTTP 500 (Internal Server Error)", HoppieAPI.parseErrorMessage(http500Err));

        HoppieAPI.HoppieResponse http503Err = new HoppieAPI.HoppieResponse(503, "");
        assertEquals("ERROR: HTTP 503", HoppieAPI.parseErrorMessage(http503Err));
    }
}
