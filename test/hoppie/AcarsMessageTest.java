package hoppie;

import org.junit.Test;
import java.util.HashMap;

import static org.junit.Assert.*;

public class AcarsMessageTest {

    @Test
    public void testDecodeUnicode() {
        assertEquals("⬈", AcarsMessage.decodeUnicode("\\u2B08"));
        assertEquals("⬊", AcarsMessage.decodeUnicode("\\u2B0A"));
        assertEquals("Plain Text", AcarsMessage.decodeUnicode("Plain Text"));
        assertNull(AcarsMessage.decodeUnicode(null));
    }

    @Test
    public void testGetListFormatOutgoing() {
        AcarsMessage msg = new AcarsMessage("KLM123", "cpdlc", "EHAM_TWR", "REQUEST LEVEL 350");
        HashMap<String, String> format = msg.getListFormat("KLM123");

        assertNotNull(format);
        assertEquals("(EHAM_TWR)<", format.get("header"));
        assertEquals("REQUEST LEVEL 350", format.get("preview"));
        assertEquals("⬈", format.get("symbol"));
        assertNotNull(format.get("time"));
    }

    @Test
    public void testGetListFormatIncoming() {
        AcarsMessage msg = new AcarsMessage("EHAM_TWR", "cpdlc", "KLM123", "CLEARED DIRECT NAVIX");
        HashMap<String, String> format = msg.getListFormat("KLM123");

        assertNotNull(format);
        assertEquals("(EHAM_TWR)>", format.get("header"));
        assertEquals("CLEARED DIRECT NAVIX", format.get("preview"));
        assertEquals("⬊", format.get("symbol"));
        assertNotNull(format.get("time"));
    }

    @Test
    public void testGetDetailFormatCpdlcNewlineReplacement() {
        AcarsMessage msg = new AcarsMessage("EHAM_TWR", "cpdlc", "KLM123", "LINE1@LINE2@LINE3");
        String detail = msg.getDetailFormat("KLM123");

        assertTrue(detail.contains("CPDLC FROM EHAM_TWR: \nLINE1\nLINE2\nLINE3"));
    }

    @Test
    public void testGetDetailFormatSystemMessage() {
        AcarsMessage msg = new AcarsMessage("system", "Connected as KLM123");
        String detail = msg.getDetailFormat("KLM123");

        assertTrue(detail.contains("SYSTEM:\nConnected as KLM123"));
    }

    @Test
    public void testIsOutgoingFlag() {
        AcarsMessage outgoingMsg = new AcarsMessage("KLM123", "cpdlc", "EHAM_TWR", "REQUEST LEVEL 350", true);
        assertTrue(outgoingMsg.isOutgoing());
        assertEquals("(EHAM_TWR)<", outgoingMsg.getListFormat().get("header"));

        AcarsMessage incomingMsg = new AcarsMessage("EHAM_TWR", "cpdlc", "KLM123", "CLEARED DIRECT NAVIX", false);
        assertFalse(incomingMsg.isOutgoing());
        assertEquals("(EHAM_TWR)>", incomingMsg.getListFormat().get("header"));
    }
}
