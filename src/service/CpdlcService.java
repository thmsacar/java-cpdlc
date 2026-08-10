package service;

import flight.Flight;
import hoppie.AcarsMessage;
import hoppie.CpdlcMessage;
import hoppie.HoppieAPI;
import simbrief.SimbriefAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CpdlcService {

    private static final Pattern HANDOVER_PATTERN = Pattern.compile("HANDOVER\\s+@?([A-Z0-9]{3,8})", Pattern.CASE_INSENSITIVE);

    private final String callsign;
    private final String hoppieID;
    private final HoppieAPI hoppieAPI;
    // Thread-safe lists for background fetching and listener notifications
    private final List<AcarsMessage> messages = new CopyOnWriteArrayList<>();
    private final List<CpdlcListener> listeners = new CopyOnWriteArrayList<>();
    
    /** Currently connected ATS unit callsign. */
    private String currentATS;
    private boolean isLoggedOn = false;
    /** Station callsign for an in-progress logon request. */
    private String pendingLogonStation = "";
    /** Flag indicating an automatic station handover is in progress. */
    private boolean isAutoHandoffPending = false;
    /** Previous ATS unit callsign before a handover. */
    private String previousATS = "";
    /** Next ATS unit callsign expected after a handover. */
    private String nextATS = "";
    /** Standard idle polling interval of 40 seconds ({@link #DEFAULT_POLL_INTERVAL_MS}). */
    private static final long DEFAULT_POLL_INTERVAL_MS = 40_000L;
    /** Accelerated burst polling interval of 15 seconds ({@link #BURST_POLL_INTERVAL_MS}). */
    private static final long BURST_POLL_INTERVAL_MS = 15_000L;
    /** Duration of burst polling mode for 1 minute / 60 seconds ({@link #BURST_DURATION_MS}). */
    private static final long BURST_DURATION_MS = 60_000L;

    /** Expiry timestamp until which accelerated polling ({@link #BURST_POLL_INTERVAL_MS}) is active. */
    private volatile long burstUntilTimestamp = 0L;
    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private ScheduledExecutorService fetcherService;
    private ScheduledFuture<?> currentFetchFuture;
    private final FlightLogManager flightLogManager;

    public CpdlcService(String callsign, String hoppieID) {
        this.callsign = callsign;
        this.hoppieID = hoppieID;
        this.hoppieAPI = new HoppieAPI(hoppieID);
        this.flightLogManager = new FlightLogManager(callsign);
    }

    /** Validates callsign and Hoppie ID credentials via ping. */
    public static boolean validateCredentials(String callsign, String hoppieID) throws IOException {
        HoppieAPI api = new HoppieAPI(hoppieID);
        HoppieAPI.HoppieResponse response = api.sendPing(callsign);
        return response.body().trim().equalsIgnoreCase("ok");
    }

    public void addListener(CpdlcListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CpdlcListener listener) {
        listeners.remove(listener);
    }

    /**
     * Used for adding mockup data, for testing purposes
     */
    public void populateMockData() {
        this.currentATS = "EDGG_CTR";
        this.isLoggedOn = true;

        AcarsMessage m6 = new AcarsMessage("EHAM_TWR", "cpdlc", callsign, "CLEARED PRE-DEPARTURE ROUTE SPY3A RUNWAY 24", false);
        m6.setRead(true);

        AcarsMessage m5 = new AcarsMessage(callsign, "telex", "THY_OPS", "ARRIVED AT GATE E12 FUEL REMAINING 4200KG", true);
        m5.setRead(true);

        AcarsMessage m4 = new AcarsMessage("THY_OPS", "telex", callsign, "DISPATCH SHEET UPDATED FOR FLIGHT THY100", false);
        m4.setRead(false);

        AcarsMessage m3 = new AcarsMessage(callsign, "cpdlc", "EDGG_CTR", "REQUEST DIRECT TO LOGAN", true);
        m3.setRead(true);

        AcarsMessage m2 = new AcarsMessage("EDGG_CTR", "cpdlc", callsign, "CONTACT EDGG ON FREQUENCY 123.450", false);
        m2.setRead(false);

        AcarsMessage m1 = new AcarsMessage("EDGG_CTR", "cpdlc", callsign, "CLIMB TO FL370 WHEN READY", false);
        m1.setRead(false);

        addMessage(m6);
        addMessage(m5);
        addMessage(m4);
        addMessage(m3);
        addMessage(m2);
        addMessage(m1);
    }

    /** Starts initial connection check and periodic message polling. */
    public void start() {
        checkInitialConnection();
        startAutoFetch();
    }

    /** Stops the background message polling service. */
    public void stop() {
        if (fetcherService != null && !fetcherService.isShutdown()) {
            fetcherService.shutdownNow();
        }
        if (flightLogManager != null) {
            flightLogManager.endSession();
        }
    }

    /** Asynchronously checks connection to the Hoppie network. */
    private void checkInitialConnection() {
        setConnectionState(ConnectionState.CONNECTING);
        new Thread(() -> {
            AcarsMessage connectionMsg = hoppieAPI.checkConnection(callsign);
            connectionMsg.setRead(true);
            addMessage(connectionMsg);
            boolean isOk = !connectionMsg.getMessage().startsWith("ERROR");
            setConnectionState(isOk ? ConnectionState.CONNECTED : ConnectionState.DISCONNECTED);
//            populateMockData();
        }).start();
    }

    /** Schedules periodic background polling for incoming messages. */
    private synchronized void startAutoFetch() {
        if (fetcherService != null && !fetcherService.isShutdown()) {
            fetcherService.shutdownNow();
        }
        fetcherService = Executors.newSingleThreadScheduledExecutor();
        scheduleNextFetch(0, TimeUnit.MILLISECONDS);
    }

    /** Schedules the next polling execution after a specified delay. */
    private synchronized void scheduleNextFetch(long delay, TimeUnit unit) {
        if (fetcherService == null || fetcherService.isShutdown()) return;
        currentFetchFuture = fetcherService.schedule(this::runFetchCycle, delay, unit);
    }

    /** Executes one polling cycle to retrieve new messages, then reschedules the next cycle. */
    private void runFetchCycle() {
        try {
            // Fetch unread messages from Hoppie server for current callsign
            List<AcarsMessage> newMessages = hoppieAPI.fetchMessages(this.callsign);
            if (!newMessages.isEmpty()) {
                // Filter out network fetch errors to prevent inbox spam, warning sound spam, and LED flicker
                if (newMessages.size() == 1 && newMessages.get(0).getType().equalsIgnoreCase("system")
                        && newMessages.get(0).getMessage().startsWith("ERROR:")) {
                    setConnectionState(connectionState == ConnectionState.CONNECTED ? ConnectionState.RECONNECTING : ConnectionState.DISCONNECTED);
                } else {
                    for (AcarsMessage msg : newMessages) {
                        if (isDuplicateSystemMessage(msg)) continue;
                        addMessage(msg);
                    }
                    setConnectionState(ConnectionState.CONNECTED);
                }
            } else {
                setConnectionState(ConnectionState.CONNECTED);
            }
        } catch (Exception e) {
            notifyError("Fetch error: " + e.getMessage());
            setConnectionState(connectionState == ConnectionState.CONNECTED ? ConnectionState.RECONNECTING : ConnectionState.DISCONNECTED);
        } finally {
            // Determine next polling delay: BURST_POLL_INTERVAL_MS (15s) if burst mode is active, otherwise DEFAULT_POLL_INTERVAL_MS (40s)
            long nextDelay = (System.currentTimeMillis() < burstUntilTimestamp) 
                    ? BURST_POLL_INTERVAL_MS 
                    : DEFAULT_POLL_INTERVAL_MS;
            scheduleNextFetch(nextDelay, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Triggers 15-second accelerated burst polling ({@link #BURST_POLL_INTERVAL_MS}) for 1 minute ({@link #BURST_DURATION_MS}) after sending an outgoing message or request.
     * Clamps the next poll delay to 15 seconds max if the remaining delay exceeds 15 seconds.
     */
    public synchronized void triggerFastPollingBurst() {
        this.burstUntilTimestamp = System.currentTimeMillis() + BURST_DURATION_MS;

        long remainingMs = (currentFetchFuture != null && !currentFetchFuture.isDone()) 
                ? currentFetchFuture.getDelay(TimeUnit.MILLISECONDS) 
                : -1L;

        // If more than 15 seconds remaining (e.g. 35s left), clamp next fetch to 15 seconds
        if (remainingMs > BURST_POLL_INTERVAL_MS || remainingMs <= 0) {
            if (fetcherService != null && !fetcherService.isShutdown()) {
                fetcherService.shutdownNow();
                fetcherService = Executors.newSingleThreadScheduledExecutor();
                scheduleNextFetch(BURST_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
            }
        }
        // If 15 seconds or less remaining (e.g. 2s left), leave the pending task to fire on its schedule!
    }

    /** Checks if an incoming system message is a duplicate of the last message. */
    private boolean isDuplicateSystemMessage(AcarsMessage msg) {
        if (messages.isEmpty() || !msg.getType().equalsIgnoreCase("system")) return false;
        AcarsMessage lastMsg = messages.get(0);
        return lastMsg.getType().equalsIgnoreCase("system") && 
               msg.getMessage().equalsIgnoreCase(lastMsg.getMessage());
    }

    /**
     * Processes incoming CPDLC messages to handle logon confirmations, handovers, and logoffs.
     * @param msg the message to process
     */
    private void processIncomingMessage(AcarsMessage msg) {
        if (msg == null) return;
        String sender = msg.getFrom() != null ? msg.getFrom().trim() : "";
        String text = msg.getMessage() != null ? msg.getMessage() : "";
        String type = msg.getType() != null ? msg.getType().trim().toUpperCase() : "ACARS";

        boolean isOutgoing = sender.equalsIgnoreCase(callsign);
        if (isOutgoing || "SYSTEM".equalsIgnoreCase(type)) {
            return;
        }

        //--LOGON ACCEPTED--
        if (text.contains("LOGON ACCEPTED") && !pendingLogonStation.isEmpty() && sender.equalsIgnoreCase(pendingLogonStation)) {
            if (isAutoHandoffPending && sender.equalsIgnoreCase(nextATS)) {
                isAutoHandoffPending = false;
                if (previousATS != null && !previousATS.isEmpty()) {
                    sendLogoffToStation(previousATS);
                }
                previousATS = "";
                nextATS = "";
            }
            setCurrentATS(pendingLogonStation);
            pendingLogonStation = "";
            return;
        }

        //--LOGOFF--
        String upperText = text.toUpperCase();
        boolean isLogoff = upperText.contains("LOGOFF") || 
                           upperText.contains("SERVICE TERMINATED") || 
                           upperText.contains("DISCONNECT");

        //--HANDOVER
        Matcher handoverMatcher = HANDOVER_PATTERN.matcher(text);
        boolean isHandover = handoverMatcher.find();

        //--
        if (isLogoff && isLoggedOn()) {
            String targetStation = sender.isEmpty() ? currentATS : sender;
            sendLogoff();
            notifyAutoLogoff(targetStation);
        } else if (isHandover && isLoggedOn()) {
            String targetStation = handoverMatcher.group(1).trim().toUpperCase();
            if (!targetStation.isEmpty() && !targetStation.equalsIgnoreCase(currentATS)) {
                this.previousATS = currentATS;
                this.nextATS = targetStation;
                this.isAutoHandoffPending = true;
                sendLogon(targetStation, "");
                notifyAutoHandover(targetStation);
            }
        }
    }

    /** Sends a telex message to a specified station. */
    public void sendTelex(String station, String message) {
        triggerFastPollingBurst();
        executeAsync(() -> {
            AcarsMessage msg = hoppieAPI.sendTelex(station, callsign, message);
            msg.setRead(true);
            addMessage(msg);
            notifyConnectionStatus(!msg.getType().equalsIgnoreCase("system"));
        });
    }

    /** Sends a CPDLC request message to the active ATS unit. */
    public void sendRequest(String message) {
        if (currentATS == null || currentATS.trim().isEmpty() || !isLoggedOn) {
            notifyError("Cannot send request: Not connected to ATC unit.");
            return;
        }
        triggerFastPollingBurst();
        executeAsync(() -> {
            AcarsMessage msg = hoppieAPI.request(currentATS, callsign, message);
            msg.setRead(true);
            addMessage(msg);
            notifyConnectionStatus(!msg.getType().equalsIgnoreCase("system"));
        });
    }

    /** Sends a CPDLC report message to the active ATS unit. */
    public void sendReport(String message) {
        if (currentATS == null || currentATS.trim().isEmpty() || !isLoggedOn) {
            notifyError("Cannot send report: Not connected to ATC unit.");
            return;
        }
        triggerFastPollingBurst();
        executeAsync(() -> {
            AcarsMessage msg = hoppieAPI.report(currentATS, callsign, message);
            msg.setRead(true);
            addMessage(msg);
            notifyConnectionStatus(!msg.getType().equalsIgnoreCase("system"));
        });
    }

    public void sendDirectRequest(String waypoint, String dueToText) {
        String msg = CpdlcMessageFormatter.formatDirectRequest(waypoint, dueToText);
        if (!msg.isEmpty()) sendRequest(msg);
    }

    public void sendLevelRequest(String level, String dueToText) {
        String msg = CpdlcMessageFormatter.formatLevelRequest(level, dueToText);
        if (!msg.isEmpty()) sendRequest(msg);
    }

    public void sendSpeedRequest(String speedType, String speedValue, String dueToText) {
        String msg = CpdlcMessageFormatter.formatSpeedRequest(speedType, speedValue, dueToText);
        if (!msg.isEmpty()) sendRequest(msg);
    }

    public void sendWhenCanWeExpectRequest(String expectType, String value, String dueToText) {
        String msg = CpdlcMessageFormatter.formatWhenCanWeExpectRequest(expectType, value, dueToText);
        if (!msg.isEmpty()) sendRequest(msg);
    }

    public void sendLevelReport(String status, String level) {
        String msg = CpdlcMessageFormatter.formatLevelReport(status, level);
        if (msg != null) sendReport(msg);
    }

    public void sendSpeedReport(boolean isMach, String speedValue) {
        String msg = CpdlcMessageFormatter.formatSpeedReport(isMach, speedValue);
        if (msg != null) sendReport(msg);
    }

    public void sendPositionReport(String pos, String time, String level, String thereafter, String nextPos, String etaNext) {
        String msg = CpdlcMessageFormatter.formatPositionReport(pos, time, level, thereafter, nextPos, etaNext);
        if (msg != null) sendReport(msg);
    }

    /** Sends a CPDLC logon request to an ATS station. */
    public void sendLogon(String station, String remarks) {
        triggerFastPollingBurst();
        executeAsync(() -> {
            AcarsMessage msg = hoppieAPI.sendLogonATC(station, callsign, remarks);
            msg.setRead(true);
            if (!msg.getType().equalsIgnoreCase("system")) {
                this.pendingLogonStation = station.trim();
                notifyConnectionStatus(true);
            } else {
                this.pendingLogonStation = "";
            }
            addMessage(msg);
        });
    }

    /** Sends a CPDLC logoff request to the active ATS station. */
    public void sendLogoff() {
        triggerFastPollingBurst();
        executeAsync(() -> {
            AcarsMessage msg = hoppieAPI.sendLogoffATC(currentATS, callsign);
            msg.setRead(true);
            if (!msg.getType().equalsIgnoreCase("system")) {
                setCurrentATS(null);
                setNextATS("");
                notifyConnectionStatus(true);
            }
            addMessage(msg);
        });
    }

    /** Sends a CPDLC logoff to a station, used for logging of after a handover */
    public void sendLogoffToStation(String station) {
        executeAsync(() -> {
            if (station != null && !station.trim().isEmpty()) {
                AcarsMessage msg = hoppieAPI.sendLogoffATC(station.trim(), callsign);
                msg.setRead(true);
                addMessage(msg);
            }
        });
    }

    /** Sends a Pre-Departure Clearance (PDC) request to a station. */
    public void sendPdcRequest(String station, Flight flight, String stand, String atis, String remarks) {
        triggerFastPollingBurst();
        executeAsync(() -> {
            AcarsMessage msg = hoppieAPI.sendPdcRequest(station, flight, stand, atis, remarks);
            msg.setRead(true);
            addMessage(msg);
        });
    }

    /** Sends a response (WILCO, UNABLE, ROGER, etc.) to an incoming CPDLC message. */
    public void sendResponse(String responseType, CpdlcMessage originalMsg) {
        executeAsync(() -> {
            AcarsMessage acarsMsg = null;
            switch (responseType.toUpperCase()) {
                case "WILCO": acarsMsg = hoppieAPI.wilco(originalMsg.getFrom(), callsign, originalMsg.getMsgNumber()); break;
                case "UNABLE": acarsMsg = hoppieAPI.unable(originalMsg.getFrom(), callsign, originalMsg.getMsgNumber()); break;
                case "ROGER": acarsMsg = hoppieAPI.roger(originalMsg.getFrom(), callsign, originalMsg.getMsgNumber()); break;
                case "STANDBY": acarsMsg = hoppieAPI.standby(originalMsg.getFrom(), callsign, originalMsg.getMsgNumber()); break;
                case "AFFIRM": acarsMsg = hoppieAPI.affirm(originalMsg.getFrom(), callsign, originalMsg.getMsgNumber()); break;
                case "NEGATIVE": acarsMsg = hoppieAPI.negative(originalMsg.getFrom(), callsign, originalMsg.getMsgNumber()); break;
            }
            if (acarsMsg != null) {
                // Only mark original message as replied AND save response locally IF network transmission succeeded
                if (!acarsMsg.getType().equalsIgnoreCase("system")) {
                    if (originalMsg != null) {
                        originalMsg.setSentResponse(responseType);
                    }
                    addMessage(acarsMsg);
                } else {
                    notifyError(acarsMsg.getMessage());
                }
            }
        });
    }

    /** Fetches flight plan data asynchronously from SimBrief. */
    public void fetchSimbriefData(String simbriefID, SimbriefCallback callback) {
        executeAsync(() -> {
            try {
                SimbriefAPI api = new SimbriefAPI(simbriefID);
                Flight flight = api.getFlight();
                callback.onSuccess(flight);
            } catch (IOException e) {
                notifyError("Simbrief error: " + e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public interface SimbriefCallback {
        void onSuccess(Flight flight);
        void onFailure(Exception e);
    }

    /** Executes a task asynchronously on a new thread. */
    private void executeAsync(Runnable task) {
        // TODO: Replace with a ThreadPoolExecutor for better resource management
        new Thread(task).start(); // Could be replaced by an Executor
    }

    /** Processes, stores, and notifies listeners of a new message. */
    private void addMessage(AcarsMessage message) {
        if (message == null) return;
        if (message.getFrom() != null && message.getFrom().equalsIgnoreCase(callsign)) {
            message.setRead(true);
        }
        processIncomingMessage(message);
        messages.add(0, message);
        if (flightLogManager != null) {
            flightLogManager.logMessage(message);
        }
        for (CpdlcListener l : listeners) {
            l.onMessageReceived(message);
            l.onMessagesUpdated(Collections.unmodifiableList(messages));
        }
    }

    private void setCurrentATS(String ats) {
        this.currentATS = ats;
        this.isLoggedOn = (ats != null);
        if (ats != null && !ats.trim().isEmpty() && nextATS != null && ats.trim().equalsIgnoreCase(nextATS.trim())) {
            this.nextATS = "";
        }
        for (CpdlcListener l : listeners) {
            l.onAtsUnitChanged(ats);
        }
    }

    private void notifyConnectionStatus(boolean isConnected) {
        for (CpdlcListener l : listeners) {
            l.onConnectionStatusChanged(isConnected);
        }
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public synchronized void setConnectionState(ConnectionState newState) {
        if (newState == null || this.connectionState == newState) return;
        ConnectionState oldState = this.connectionState;
        this.connectionState = newState;
        for (CpdlcListener l : listeners) {
            l.onConnectionStateChanged(oldState, newState);
        }
        notifyConnectionStatus(newState == ConnectionState.CONNECTED);
    }

    private void notifyError(String error) {
        for (CpdlcListener l : listeners) {
            l.onError(error);
        }
    }

    private void notifyAutoLogoff(String station) {
        for (CpdlcListener l : listeners) {
            l.onAutoLogoff(station);
        }
    }

    private void notifyAutoHandover(String nextStation) {
        for (CpdlcListener l : listeners) {
            l.onAutoHandover(nextStation);
        }
    }

    // Getters / Setters
    public String getCallsign() { return callsign; }
    public String getCurrentATS() { return currentATS; }
    public String getNextATS() { return nextATS; }
    public void setNextATS(String nextATS) { 
        String cleaned = nextATS != null ? nextATS.trim() : "";
        if (currentATS != null && !currentATS.trim().isEmpty() && cleaned.equalsIgnoreCase(currentATS.trim())) {
            this.nextATS = "";
        } else {
            this.nextATS = cleaned;
        }
    }
    public boolean isLoggedOn() { return isLoggedOn; }
    public boolean isConnected() { return fetcherService != null && !fetcherService.isShutdown(); }
    public List<AcarsMessage> getMessages() { return Collections.unmodifiableList(messages); }
    public FlightLogManager getFlightLogManager() { return flightLogManager; }
}
