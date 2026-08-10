package hoppie;

import org.junit.Test;
import static org.junit.Assert.*;

/** Tests parsing, message numbering, response type classification, and slash handling in CpdlcMessage. */
public class CpdlcMessageTest {

    /** Verifies parsing of standard CPDLC raw network strings. */
    @Test
    public void testParseCpdlcContentStandard() {
        String rawContent = "/data2/3//WU/CLEARED DIRECT NAVIX";
        CpdlcMessage msg = new CpdlcMessage("EHAM_TWR", "cpdlc", "KLM123", rawContent);

        assertEquals(3, msg.getMsgNumber());
        assertEquals("WU", msg.getResponseType());
        assertEquals("CLEARED DIRECT NAVIX", msg.getMessage());
        assertEquals("EHAM_TWR", msg.getFrom());
        assertEquals("KLM123", msg.getTo());
    }

    /** Verifies parsing of CPDLC messages referencing a replied message number. */
    @Test
    public void testParseCpdlcContentWithRepliedMsgNumber() {
        String rawContent = "/data2/5/3/NE/ROGER";
        CpdlcMessage msg = new CpdlcMessage("EDDF_APP", "cpdlc", "DLH456", rawContent);

        assertEquals(5, msg.getMsgNumber());
        assertEquals("NE", msg.getResponseType());
        assertEquals("ROGER", msg.getMessage());
    }

    /** Verifies explicit parameter constructor initialization. */
    @Test
    public void testFullConstructor() {
        CpdlcMessage msg = new CpdlcMessage("EHAM_TWR", "cpdlc", "KLM123", "WILCO", 2, 1, "NE");

        assertEquals(2, msg.getMsgNumber());
        assertEquals("NE", msg.getResponseType());
        assertEquals("WILCO", msg.getMessage());
    }

    /** Verifies tracking of sent responses for CPDLC messages. */
    @Test
    public void testRepliedState() {
        CpdlcMessage msg = new CpdlcMessage("EHAM_TWR", "cpdlc", "KLM123", "/data2/3//WU/DESCEND FL100");
        assertFalse(msg.hasBeenReplied());
        assertNull(msg.getSentResponse());

        msg.setSentResponse("WILCO");
        assertTrue(msg.hasBeenReplied());
        assertEquals("WILCO", msg.getSentResponse());
    }

    /** Verifies mapping of raw response requirement codes to CpdlcResponseType enum values. */
    @Test
    public void testGetParsedResponseType() {
        CpdlcMessage wuMsg = new CpdlcMessage("EHAM_TWR", "cpdlc", "KLM123", "/data2/1//WU/DESCEND FL100");
        assertEquals(CpdlcResponseType.WILCO_UNABLE, wuMsg.getParsedResponseType());

        CpdlcMessage anMsg = new CpdlcMessage("EHAM_TWR", "cpdlc", "KLM123", "/data2/2//AN/REPORT LEVEL");
        assertEquals(CpdlcResponseType.AFFIRM_NEGATIVE, anMsg.getParsedResponseType());

        CpdlcMessage rMsg = new CpdlcMessage("EHAM_TWR", "cpdlc", "KLM123", "/data2/3//R/CONTACT EDGG 123.450");
        assertEquals(CpdlcResponseType.ROGER, rMsg.getParsedResponseType());

        CpdlcMessage neMsg = new CpdlcMessage("EHAM_TWR", "cpdlc", "KLM123", "/data2/4//NE/ROGER");
        assertEquals(CpdlcResponseType.NONE, neMsg.getParsedResponseType());
    }

    /** Verifies that slashes contained within the message body do not truncate content during parsing. */
    @Test
    public void testParseCpdlcContentWithSlashesInBody() {
        String rawContent = "/data2/10/5/WU/CLEARED DIRECT VIA SPY / RUNWAY 24";
        CpdlcMessage msg = new CpdlcMessage("EHAM_TWR", "cpdlc", "KLM123", rawContent);

        assertEquals(10, msg.getMsgNumber());
        assertEquals("WU", msg.getResponseType());
        assertEquals("CLEARED DIRECT VIA SPY / RUNWAY 24", msg.getMessage());
    }
}
