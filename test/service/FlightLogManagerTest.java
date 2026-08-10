package service;

import hoppie.AcarsMessage;
import hoppie.CpdlcMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Tests FlightLogManager session creation, message logging, and log directory listing.
 */
public class FlightLogManagerTest {

    private FlightLogManager logManager;

    @Before
    public void setUp() {
        logManager = new FlightLogManager("KLM789");
    }

    @After
    public void tearDown() {
        if (logManager != null) {
            logManager.endSession();
        }
    }

    /** Verifies that a valid log file is created in the ~/.java-cpdlc/logs directory. */
    @Test
    public void testLogFileCreation() {
        File logFile = logManager.getLogFile();
        assertNotNull("Log file should not be null", logFile);
        assertTrue("Log filename should contain callsign", logFile.getName().contains("KLM789"));
        assertTrue("Log directory should exist", FlightLogManager.getLogDirectory().exists());
    }

    /** Verifies that logging ACARS and CPDLC messages writes non-empty content to the log file. */
    @Test
    public void testLogMessageWriting() throws Exception {
        AcarsMessage telex = new AcarsMessage("EHAM_DEL", "telex", "KLM789", "PDC CLRD TO EGLL VIA VALKO1S", false);
        CpdlcMessage cpdlc = new CpdlcMessage("EDGG_CTR", "cpdlc", "KLM789", "/data2/1//WU/CLIMB TO FL350");

        logManager.logMessage(telex);
        logManager.logMessage(cpdlc);
        logManager.endSession();

        // Allow async writer executor to complete
        Thread.sleep(200);

        File file = logManager.getLogFile();
        assertTrue("Log file should exist on disk", file.exists());
        String content = new String(Files.readAllBytes(file.toPath()));

        assertTrue("Log content should contain header callsign", content.contains("KLM789"));
        assertTrue("Log content should contain telex message text", content.contains("PDC CLRD TO EGLL"));
        assertTrue("Log content should contain CPDLC message text", content.contains("CLIMB TO FL350"));
        assertTrue("Log content should contain session end summary", content.contains("SESSION ENDED"));
    }
}
