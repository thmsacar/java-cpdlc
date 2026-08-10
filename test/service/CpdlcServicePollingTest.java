package service;

import hoppie.AcarsMessage;
import hoppie.CpdlcMessage;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class CpdlcServicePollingTest {

    private CpdlcService service;

    @Before
    public void setUp() {
        service = new CpdlcService("KLM123", "TESTHOPPIEID");
    }

    @Test
    public void testTriggerFastPollingBurstSetsTimestamp() throws Exception {
        Field burstUntilField = CpdlcService.class.getDeclaredField("burstUntilTimestamp");
        burstUntilField.setAccessible(true);

        long before = (long) burstUntilField.get(service);
        assertEquals(0L, before);

        service.triggerFastPollingBurst();

        long after = (long) burstUntilField.get(service);
        assertTrue("Burst timestamp should be set in the future (~60s)", after > System.currentTimeMillis());
    }

    @Test
    public void testSendResponseDoesNotTriggerBurst() throws Exception {
        Field burstUntilField = CpdlcService.class.getDeclaredField("burstUntilTimestamp");
        burstUntilField.setAccessible(true);

        CpdlcMessage incomingMsg = new CpdlcMessage("EHAM_TWR", "cpdlc", "KLM123", "/data2/1//WU/CLIMB FL100");
        service.sendResponse("WILCO", incomingMsg);

        long burstTimestamp = (long) burstUntilField.get(service);
        assertEquals("sendResponse should NOT trigger fast polling burst", 0L, burstTimestamp);
    }

    @Test
    public void testClampingBehaviorWhenTaskHasShortDelay() throws Exception {
        Field currentFetchFutureField = CpdlcService.class.getDeclaredField("currentFetchFuture");
        currentFetchFutureField.setAccessible(true);

        service.start();
        Thread.sleep(100);

        // Current task is active
        Object future = currentFetchFutureField.get(service);
        assertNotNull(future);

        service.triggerFastPollingBurst();
        Object newFuture = currentFetchFutureField.get(service);
        assertNotNull(newFuture);
        service.stop();
    }
}
