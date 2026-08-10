package service;

import gui2.components.CduDisplay;
import gui2.controller.CduController;
import hoppie.AcarsMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Tests connection state notifications, recovery, and CDU controller failure LED resetting.
 */
public class CpdlcServiceConnectionTest {

    private CpdlcService service;
    private CduController controller;

    @Before
    public void setUp() {
        service = new CpdlcService("KLM123", "TESTHOPPIEID");
        CduDisplay display = new CduDisplay();
        controller = new CduController(display);
        controller.setService(service);
    }

    /** Verifies setConnectionState(CONNECTED) always notifies status listeners. */
    @Test
    public void testSetConnectionStateAlwaysNotifiesStatusListeners() {
        AtomicInteger statusCount = new AtomicInteger(0);
        AtomicBoolean lastStatus = new AtomicBoolean(false);

        service.addListener(new CpdlcListener() {
            @Override
            public void onMessageReceived(AcarsMessage message) {}
            @Override
            public void onMessagesUpdated(List<AcarsMessage> messages) {}
            @Override
            public void onConnectionStatusChanged(boolean isConnected) {
                statusCount.incrementAndGet();
                lastStatus.set(isConnected);
            }
            @Override
            public void onAtsUnitChanged(String atsUnit) {}
            @Override
            public void onError(String message) {}
        });

        service.setConnectionState(ConnectionState.CONNECTED);
        assertEquals(1, statusCount.get());
        assertTrue(lastStatus.get());

        // Call again with same state CONNECTED
        service.setConnectionState(ConnectionState.CONNECTED);
        assertEquals(2, statusCount.get());
        assertTrue(lastStatus.get());
    }

    /** Verifies that receiving onConnectionStatusChanged(true) resets error state in CduController. */
    @Test
    public void testCduControllerClearsErrorOnSuccessfulFetch() {
        AtomicBoolean failLedState = new AtomicBoolean(false);
        controller.setFailLedConsumer(failLedState::set);

        // Simulate an error occurring
        controller.onError("Fetch error: Connection timed out");
        assertTrue(failLedState.get());
        assertEquals("ERROR: Fetch error: Connection timed out", controller.getStatusMessage());

        // Simulate a successful regular fetch cycle notifying status
        controller.onConnectionStatusChanged(true);

        assertFalse("failLed should be turned off when connection status is true", failLedState.get());
        assertEquals("CONNECTED TO HOPPIE", controller.getStatusMessage());
    }

    /** Verifies that SimBrief fetch failures trigger onFailure without calling global onError or failLed. */
    @Test
    public void testFetchSimbriefDataSilentFailure() throws Exception {
        AtomicBoolean errorCalled = new AtomicBoolean(false);
        AtomicBoolean failLedState = new AtomicBoolean(false);
        controller.setFailLedConsumer(failLedState::set);

        service.addListener(new CpdlcListener() {
            @Override public void onMessageReceived(AcarsMessage message) {}
            @Override public void onMessagesUpdated(List<AcarsMessage> messages) {}
            @Override public void onConnectionStatusChanged(boolean isConnected) {}
            @Override public void onAtsUnitChanged(String atsUnit) {}
            @Override public void onError(String message) { errorCalled.set(true); }
        });

        AtomicBoolean failureCallbackCalled = new AtomicBoolean(false);
        service.fetchSimbriefData("INVALID_USER_ID_9999", new CpdlcService.SimbriefCallback() {
            @Override
            public void onSuccess(flight.Flight flight) {}
            @Override
            public void onFailure(Exception e) {
                failureCallbackCalled.set(true);
            }
        });

        // Wait up to 3 seconds for async thread
        long deadline = System.currentTimeMillis() + 3000;
        while (!failureCallbackCalled.get() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        assertTrue("onFailure callback should be invoked", failureCallbackCalled.get());
        assertFalse("Global onError should NOT be called for SimBrief failure", errorCalled.get());
        assertFalse("failLed should NOT be illuminated for SimBrief failure", failLedState.get());
    }
}
