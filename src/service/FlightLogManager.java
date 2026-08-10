package service;

import hoppie.AcarsMessage;
import hoppie.CpdlcMessage;
import hoppie.TimeFormatter;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages real-time flight session logging to disk and log file export.
 * Formats ACARS and CPDLC messages into ACARS thermal printer style flight logs.
 */
public class FlightLogManager {

    private final String callsign;
    private final File logFile;
    private final ExecutorService logExecutor = Executors.newSingleThreadExecutor();
    
    private int cpdlcCount = 0;
    private int telexCount = 0;
    private int systemCount = 0;
    private boolean sessionEnded = false;

    public FlightLogManager(String callsign) {
        this.callsign = callsign != null && !callsign.trim().isEmpty() ? callsign.trim().toUpperCase() : "UNKNOWN";
        this.logFile = createLogFile(this.callsign);
        writeSessionHeader();
    }

    /** Helper method to get the user's ~/.java-cpdlc/logs directory. */
    public static File getLogDirectory() {
        String userHome = System.getProperty("user.home");
        File dir = new File(new File(userHome, ".java-cpdlc"), "logs");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /** Creates a timestamped log file for the given callsign. */
    private static File createLogFile(String callsign) {
        File dir = getLogDirectory();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = dateFormat.format(new Date());
        String filename = String.format("cpdlc_log_%s_%s.log", callsign, timestamp);
        return new File(dir, filename);
    }

    /** Writes header banner when flight session opens. */
    private void writeSessionHeader() {
        logExecutor.execute(() -> {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)))) {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                String zuluStart = isoFormat.format(new Date()) + " ZULU";

                out.println("============================================================");
                out.println("JAVA-CPDLC FLIGHT LOG - CALLSIGN: " + callsign);
                out.println("SESSION STARTED: " + zuluStart);
                out.println("LOG FILE: " + logFile.getAbsolutePath());
                out.println("============================================================");
                out.println();
                out.flush();
            } catch (IOException e) {
                System.err.println("Failed to write log header: " + e.getMessage());
            }
        });
    }

    /** Appends an incoming or outgoing AcarsMessage asynchronously to the flight log. */
    public void logMessage(AcarsMessage message) {
        if (message == null || sessionEnded) return;

        logExecutor.execute(() -> {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)))) {
                String timeStr = message.getTimestamp() != null ? TimeFormatter.zuluTime(message.getTimestamp()) : "00:00:00Z";
                boolean isOutgoing = message.isOutgoing();
                boolean isCpdlc = message instanceof CpdlcMessage || "CPDLC".equalsIgnoreCase(message.getType());
                boolean isSystem = "SYSTEM".equalsIgnoreCase(message.getType());

                String directionStr;
                String targetStation;

                if (isSystem) {
                    systemCount++;
                    out.printf("[%s] SYSTEM: %s%n", timeStr, message.getMessage());
                } else {
                    if (isCpdlc) cpdlcCount++; else telexCount++;

                    if (isOutgoing) {
                        directionStr = "OUTBOUND " + (isCpdlc ? "CPDLC" : "TELEX") + " -> ";
                        targetStation = message.getTo() != null ? message.getTo() : "UNKNOWN";
                    } else {
                        directionStr = "INBOUND " + (isCpdlc ? "CPDLC" : "TELEX") + " <- ";
                        targetStation = message.getFrom() != null ? message.getFrom() : "UNKNOWN";
                    }

                    out.printf("[%s] %s%s: %s%n", timeStr, directionStr, targetStation, message.getMessage());
                }
                out.flush();
            } catch (IOException e) {
                System.err.println("Failed to append message to flight log: " + e.getMessage());
            }
        });
    }

    /** Closes the flight session and appends total message statistics. */
    public void endSession() {
        if (sessionEnded) return;
        sessionEnded = true;

        logExecutor.execute(() -> {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)))) {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                String zuluEnd = isoFormat.format(new Date()) + " ZULU";

                out.println();
                out.println("============================================================");
                out.println("SESSION ENDED: " + zuluEnd);
                out.printf("TOTAL MESSAGES: %d (CPDLC: %d, TELEX: %d, SYSTEM: %d)%n",
                        (cpdlcCount + telexCount + systemCount), cpdlcCount, telexCount, systemCount);
                out.println("============================================================");
                out.flush();
            } catch (IOException e) {
                System.err.println("Failed to write log footer: " + e.getMessage());
            } finally {
                logExecutor.shutdown();
            }
        });
    }

    /** Returns the active log file instance. */
    public File getLogFile() {
        return logFile;
    }

    /** Opens the active log file using the host OS default text viewer. */
    public boolean openActiveLogFile() {
        return openLogFile(logFile);
    }

    /** Opens a specified log file using the host OS default text viewer. */
    public static boolean openLogFile(File file) {
        if (file == null || !file.exists()) return false;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Failed to open log file in OS: " + e.getMessage());
        }
        return false;
    }

    /** Lists all existing flight log files sorted by modification date descending. */
    public static List<File> listLogFiles() {
        List<File> result = new ArrayList<>();
        File dir = getLogDirectory();
        File[] files = dir.listFiles((d, name) -> name.startsWith("cpdlc_log_") && name.endsWith(".log"));
        if (files != null) {
            java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            for (File f : files) {
                result.add(f);
            }
        }
        return result;
    }
}
