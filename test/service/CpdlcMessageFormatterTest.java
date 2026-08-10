package service;

import org.junit.Test;
import static org.junit.Assert.*;

/** Tests string formatting helper methods in CpdlcMessageFormatter. */
public class CpdlcMessageFormatterTest {

    /** Verifies formatting of "DUE TO" remark suffixes. */
    @Test
    public void testFormatDueTo() {
        assertEquals("DUE TO PERFORMANCE", CpdlcMessageFormatter.formatDueTo("PERFORMANCE", ""));
        assertEquals("DUE TO WEATHER", CpdlcMessageFormatter.formatDueTo("WEATHER", null));
        assertEquals("DUE TO HEAVY TRAFFIC", CpdlcMessageFormatter.formatDueTo("FREE TEXT", "HEAVY TRAFFIC"));
        assertEquals("", CpdlcMessageFormatter.formatDueTo("FREE TEXT", ""));
        assertEquals("", CpdlcMessageFormatter.formatDueTo(null, ""));
    }

    /** Verifies formatting of direct waypoint request strings. */
    @Test
    public void testFormatDirectRequest() {
        assertEquals("REQUEST DIRECT TO NAVIX", CpdlcMessageFormatter.formatDirectRequest("NAVIX", null));
        assertEquals("REQUEST DIRECT TO NAVIX DUE TO WEATHER", CpdlcMessageFormatter.formatDirectRequest("NAVIX", "DUE TO WEATHER"));
        assertEquals("", CpdlcMessageFormatter.formatDirectRequest("", "DUE TO WEATHER"));
        assertEquals("", CpdlcMessageFormatter.formatDirectRequest(null, null));
    }

    /** Verifies formatting of flight level request strings. */
    @Test
    public void testFormatLevelRequest() {
        assertEquals("REQUEST LEVEL 350", CpdlcMessageFormatter.formatLevelRequest("350", ""));
        assertEquals("REQUEST LEVEL 350 DUE TO PERFORMANCE", CpdlcMessageFormatter.formatLevelRequest("350", "DUE TO PERFORMANCE"));
        assertEquals("", CpdlcMessageFormatter.formatLevelRequest("", "DUE TO PERFORMANCE"));
    }

    /** Verifies formatting of airspeed and Mach number request strings. */
    @Test
    public void testFormatSpeedRequest() {
        assertEquals("REQUEST SPEED IAS 280", CpdlcMessageFormatter.formatSpeedRequest("ias", "280", ""));
        assertEquals("REQUEST SPEED M.80 DUE TO WEATHER", CpdlcMessageFormatter.formatSpeedRequest("mach", "80", "DUE TO WEATHER"));
        assertEquals("", CpdlcMessageFormatter.formatSpeedRequest("ias", "", ""));
    }

    /** Verifies formatting of "WHEN CAN WE EXPECT" clearance query strings. */
    @Test
    public void testFormatWhenCanWeExpectRequest() {
        assertEquals("WHEN CAN WE EXPECT LEVEL 370", CpdlcMessageFormatter.formatWhenCanWeExpectRequest("LEVEL", "370", ""));
        assertEquals("WHEN CAN WE EXPECT DIRECT TO NAVIX DUE TO WEATHER", CpdlcMessageFormatter.formatWhenCanWeExpectRequest("DIRECT TO", "NAVIX", "DUE TO WEATHER"));
        assertEquals("", CpdlcMessageFormatter.formatWhenCanWeExpectRequest("LEVEL", "", ""));
    }

    /** Verifies formatting of level maintaining/climbing/descending reports. */
    @Test
    public void testFormatLevelReport() {
        assertEquals("MAINTAINING LEVEL 350", CpdlcMessageFormatter.formatLevelReport("MAINTAINING", "350"));
        assertEquals("REACHING LEVEL 370", CpdlcMessageFormatter.formatLevelReport("REACHING", "370"));
        assertEquals("LEAVING LEVEL 330", CpdlcMessageFormatter.formatLevelReport("LEAVING", "330"));
        assertNull(CpdlcMessageFormatter.formatLevelReport("MAINTAINING", ""));
    }

    /** Verifies formatting of IAS and Mach speed reports. */
    @Test
    public void testFormatSpeedReport() {
        assertEquals("PRESENT SPEED IAS 280", CpdlcMessageFormatter.formatSpeedReport(false, "280"));
        assertEquals("PRESENT SPEED M.82", CpdlcMessageFormatter.formatSpeedReport(true, "82"));
        assertNull(CpdlcMessageFormatter.formatSpeedReport(false, ""));
    }

    /** Verifies formatting of position report payload strings. */
    @Test
    public void testFormatPositionReport() {
        assertEquals("POSITION ABEAM AT 1234Z LEVEL 350", 
            CpdlcMessageFormatter.formatPositionReport("ABEAM", "1234", "350", "", "", ""));
        
        assertEquals("POSITION ABEAM AT 1234Z LEVEL 350@ESTIMATING NAVIX AT 1300@THEREAFTER DIRECT", 
            CpdlcMessageFormatter.formatPositionReport("ABEAM", "1234", "350", "DIRECT", "NAVIX", "1300"));
        
        assertNull(CpdlcMessageFormatter.formatPositionReport("", "1234", "350", "", "", ""));
    }
}
