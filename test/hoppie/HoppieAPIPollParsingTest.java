package hoppie;

import org.junit.Test;
import java.util.List;

import static org.junit.Assert.*;

/** Tests regex parsing of poll response bodies from the Hoppie network in HoppieAPI. */
public class HoppieAPIPollParsingTest {

    /** Verifies parsing of poll response bodies containing multiple CPDLC and telex messages. */
    @Test
    public void testParsePollResponseBodyMultipleMessages() {
        String body = "ok {EHAM_TWR cpdlc {/data2/1//WU/LOGON ACCEPTED}} {EDDF_APP telex {WEATHER ADVISORY 1200Z}}";
        List<AcarsMessage> messages = HoppieAPI.parsePollResponseBody(body, "KLM123");

        assertEquals(2, messages.size());

        // First message (CPDLC)
        assertTrue(messages.get(0) instanceof CpdlcMessage);
        CpdlcMessage cpdlcMsg = (CpdlcMessage) messages.get(0);
        assertEquals("EHAM_TWR", cpdlcMsg.getFrom());
        assertEquals("cpdlc", cpdlcMsg.getType());
        assertEquals("KLM123", cpdlcMsg.getTo());
        assertEquals(1, cpdlcMsg.getMsgNumber());
        assertEquals("WU", cpdlcMsg.getResponseType());
        assertEquals("LOGON ACCEPTED", cpdlcMsg.getMessage());

        // Second message (Telex)
        assertFalse(messages.get(1) instanceof CpdlcMessage);
        AcarsMessage telexMsg = messages.get(1);
        assertEquals("EDDF_APP", telexMsg.getFrom());
        assertEquals("telex", telexMsg.getType());
        assertEquals("KLM123", telexMsg.getTo());
        assertEquals("WEATHER ADVISORY 1200Z", telexMsg.getMessage());
    }

    /** Verifies parsing when poll response body contains no new messages ("ok {}"). */
    @Test
    public void testParsePollResponseBodyEmpty() {
        String body = "ok {}";
        List<AcarsMessage> messages = HoppieAPI.parsePollResponseBody(body, "KLM123");

        assertTrue(messages.isEmpty());
    }
}
