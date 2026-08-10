package service;

import hoppie.AcarsMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Tests automatic ATC logon, handover, and logoff transitions in CpdlcService. */
public class CpdlcServiceAutoHandoverTest {

    private CpdlcService service;

    @Before
    public void setUp() {
        service = new CpdlcService("THY100", "TESTHOPPIEID");
    }

    /** Verifies automatic logoff trigger on incoming LOGOFF or SERVICE TERMINATED messages. */
    @Test
    public void testAutoLogoffTriggeredOnLogoffMessage() {
        AtomicBoolean autoLogoffFired = new AtomicBoolean(false);
        AtomicReference<String> logoffStation = new AtomicReference<>("");

        service.addListener(new CpdlcListener() {
            @Override
            public void onMessageReceived(AcarsMessage message) {}
            @Override
            public void onMessagesUpdated(java.util.List<AcarsMessage> messages) {}
            @Override
            public void onConnectionStatusChanged(boolean isConnected) {}
            @Override
            public void onAtsUnitChanged(String atsUnit) {}
            @Override
            public void onError(String message) {}
            @Override
            public void onAutoLogoff(String station) {
                autoLogoffFired.set(true);
                logoffStation.set(station);
            }
        });

        // Simulate logon to EBBU by setting currentATS
        setField("currentATS", "EBBU");
        setField("isLoggedOn", true);

        assertTrue(service.isLoggedOn());
        assertEquals("EBBU", service.getCurrentATS());

        // Simulate incoming LOGOFF message from EBBU
        AcarsMessage logoffMsg = new AcarsMessage("EBBU", "THY100", "CPDLC", "SERVICE TERMINATED LOGOFF");
        injectMessage(logoffMsg);

        assertTrue("onAutoLogoff listener callback should have fired", autoLogoffFired.get());
        assertEquals("EBBU", logoffStation.get());
    }

    /** Verifies automatic handover trigger and station transition when receiving a HANDOVER message. */
    @Test
    public void testAutoHandoverTriggeredOnHandoverMessage() {
        AtomicBoolean autoHandoverFired = new AtomicBoolean(false);
        AtomicReference<String> handoverStation = new AtomicReference<>("");

        service.addListener(new CpdlcListener() {
            @Override
            public void onMessageReceived(AcarsMessage message) {}
            @Override
            public void onMessagesUpdated(java.util.List<AcarsMessage> messages) {}
            @Override
            public void onConnectionStatusChanged(boolean isConnected) {}
            @Override
            public void onAtsUnitChanged(String atsUnit) {}
            @Override
            public void onError(String message) {}
            @Override
            public void onAutoHandover(String nextStation) {
                autoHandoverFired.set(true);
                handoverStation.set(nextStation);
            }
        });

        // Log on to EBBU
        setField("currentATS", "EBBU");
        setField("isLoggedOn", true);
        assertEquals("EBBU", service.getCurrentATS());

        // Incoming HANDOVER message from EBBU
        AcarsMessage handoverMsg = new AcarsMessage("EBBU", "THY100", "CPDLC", "CONTACT EDGG 123.45 HANDOVER EDGG");
        injectMessage(handoverMsg);

        assertTrue("onAutoHandover listener callback should have fired", autoHandoverFired.get());
        assertEquals("EDGG", handoverStation.get());
        assertEquals("EDGG", service.getNextATS());

        // Simulate logon acceptance from next station EDGG
        setField("pendingLogonStation", "EDGG");
        AcarsMessage edggLogonMsg = new AcarsMessage("EDGG", "THY100", "CPDLC", "LOGON ACCEPTED");
        injectMessage(edggLogonMsg);

        assertEquals("EDGG", service.getCurrentATS());
        assertEquals("", service.getNextATS());
    }

    /** Verifies that nextATS field is cleared once pending station becomes the active currentATS. */
    @Test
    public void testNextATSClearedWhenStationBecomesCurrentATS() {
        service.setNextATS("EDGG");
        assertEquals("EDGG", service.getNextATS());

        // Setting pending logon and accepting logon from EDGG
        setField("pendingLogonStation", "EDGG");
        AcarsMessage edggLogonMsg = new AcarsMessage("EDGG", "THY100", "CPDLC", "LOGON ACCEPTED");
        injectMessage(edggLogonMsg);

        assertEquals("EDGG", service.getCurrentATS());
        assertEquals("", service.getNextATS());
    }

    private void setField(String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = CpdlcService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(service, value);
        } catch (Exception e) {
            fail("Failed to set field " + fieldName + ": " + e.getMessage());
        }
    }

    private void injectMessage(AcarsMessage msg) {
        try {
            java.lang.reflect.Method addMsgMethod = CpdlcService.class.getDeclaredMethod("addMessage", AcarsMessage.class);
            addMsgMethod.setAccessible(true);
            addMsgMethod.invoke(service, msg);
        } catch (Exception e) {
            fail("Failed to inject message into CpdlcService: " + e.getMessage());
        }
    }
}
