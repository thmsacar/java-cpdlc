package hoppie;

import org.junit.Test;
import static org.junit.Assert.*;

public class CpdlcMessageTest {

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

    @Test
    public void testParseCpdlcContentWithRepliedMsgNumber() {
        String rawContent = "/data2/5/3/NE/ROGER";
        CpdlcMessage msg = new CpdlcMessage("EDDF_APP", "cpdlc", "DLH456", rawContent);

        assertEquals(5, msg.getMsgNumber());
        assertEquals("NE", msg.getResponseType());
        assertEquals("ROGER", msg.getMessage());
    }

    @Test
    public void testFullConstructor() {
        CpdlcMessage msg = new CpdlcMessage("EHAM_TWR", "cpdlc", "KLM123", "WILCO", 2, 1, "NE");

        assertEquals(2, msg.getMsgNumber());
        assertEquals("NE", msg.getResponseType());
        assertEquals("WILCO", msg.getMessage());
    }
}
