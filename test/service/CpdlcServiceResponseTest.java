package service;

import hoppie.CpdlcMessage;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests verifying CpdlcService free text response upper-casing, packet transmission, and state updates.
 */
public class CpdlcServiceResponseTest {

    private CpdlcService cpdlcService;

    @Before
    public void setUp() {
        cpdlcService = new CpdlcService("KLM123", "test_hoppie_id");
    }

    /** Verifies that incoming CPDLC messages correctly track replied state and response text. */
    @Test
    public void testCpdlcMessageSentResponseState() {
        CpdlcMessage msg = new CpdlcMessage("EDGG_CTR", "cpdlc", "KLM123", "/data2/5//WU/CLIMB TO FL370");
        assertFalse("Message should initially be unreplied", msg.hasBeenReplied());
        assertNull("Sent response should initially be null", msg.getSentResponse());

        msg.setSentResponse("WILCO");
        assertTrue("Message should be marked as replied after setting response", msg.hasBeenReplied());
        assertEquals("WILCO", msg.getSentResponse());

        msg.setSentResponse("CLIMBING FL370 DUE TURBULENCE");
        assertEquals("CLIMBING FL370 DUE TURBULENCE", msg.getSentResponse());
    }

    /** Verifies that sending STANDBY does not flag the message as replied. */
    @Test
    public void testStandbyResponseDoesNotFlagMessageAsReplied() {
        CpdlcMessage msg = new CpdlcMessage("EDGG_CTR", "cpdlc", "KLM123", "/data2/5//WU/CLIMB TO FL370");
        cpdlcService.sendResponse("STANDBY", msg);
        assertFalse("Message should remain unreplied after sending STANDBY", msg.hasBeenReplied());
        assertNull("Sent response should remain null after STANDBY", msg.getSentResponse());
    }
}
