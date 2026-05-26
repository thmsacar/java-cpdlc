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
import java.util.concurrent.TimeUnit;

public class CpdlcService {

    private final String callsign;
    private final String hoppieID;
    private final HoppieAPI hoppieAPI;
    private final List<AcarsMessage> messages = new CopyOnWriteArrayList<>();
    private final List<CpdlcListener> listeners = new CopyOnWriteArrayList<>();
    
    private String currentATS;
    private boolean isLoggedOn = false;
    private String pendingLogonStation = "";
    private ScheduledExecutorService fetcherService;

    public CpdlcService(String callsign, String hoppieID) {
        this.callsign = callsign;
        this.hoppieID = hoppieID;
        this.hoppieAPI = new HoppieAPI(hoppieID);
    }

    public void addListener(CpdlcListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CpdlcListener listener) {
        listeners.remove(listener);
    }

    public void start() {
        checkInitialConnection();
        startAutoFetch();
    }

    public void stop() {
        if (fetcherService != null && !fetcherService.isShutdown()) {
            fetcherService.shutdownNow();
        }
    }

    private void checkInitialConnection() {
        new Thread(() -> {
            AcarsMessage connectionMsg = hoppieAPI.checkConnection(callsign);
            addMessage(connectionMsg);
            notifyConnectionStatus(!connectionMsg.getMessage().startsWith("ERROR"));
        }).start();
    }

    private void startAutoFetch() {
        fetcherService = Executors.newSingleThreadScheduledExecutor();
        fetcherService.scheduleAtFixedRate(() -> {
            try {
                List<AcarsMessage> newMessages = hoppieAPI.fetchMessages(this.callsign);
                if (!newMessages.isEmpty()) {
                    for (AcarsMessage msg : newMessages) {
                        if (isDuplicateSystemMessage(msg)) continue;
                        
                        processIncomingMessage(msg);
                        addMessage(msg);
                    }
                    notifyConnectionStatus(true);
                } else {
                    notifyConnectionStatus(true);
                }
            } catch (Exception e) {
                notifyError("Fetch error: " + e.getMessage());
                notifyConnectionStatus(false);
            }
        }, 0, 40, TimeUnit.SECONDS);
    }

    private boolean isDuplicateSystemMessage(AcarsMessage msg) {
        if (messages.isEmpty() || !msg.getType().equalsIgnoreCase("system")) return false;
        AcarsMessage lastMsg = messages.get(0);
        return lastMsg.getType().equalsIgnoreCase("system") && 
               msg.getMessage().equalsIgnoreCase(lastMsg.getMessage());
    }

    private void processIncomingMessage(AcarsMessage msg) {
        String sender = msg.getFrom();
        String text = msg.getMessage();
        if (text.contains("LOGON ACCEPTED") && sender.equalsIgnoreCase(pendingLogonStation)) {
            setCurrentATS(pendingLogonStation);
            pendingLogonStation = "";
        }
    }

    /**
     * Sends a telex message to a specific station.
     * @param station The recipient station callsign.
     * @param message The message content.
     */
    public void sendTelex(String station, String message) {
        executeAsync(() -> {
            AcarsMessage msg = hoppieAPI.sendTelex(station, callsign, message);
            addMessage(msg);
            notifyConnectionStatus(!msg.getType().equalsIgnoreCase("system"));
        });
    }

    /**
     * Sends a CPDLC request message to the currently connected ATS unit.
     * @param message The request message content.
     */
    public void sendRequest(String message) {
        executeAsync(() -> {
            AcarsMessage msg = hoppieAPI.request(currentATS, callsign, message);
            addMessage(msg);
            notifyConnectionStatus(!msg.getType().equalsIgnoreCase("system"));
        });
    }

    public void sendReport(String message) {
        executeAsync(() -> {
            AcarsMessage msg = hoppieAPI.report(currentATS, callsign, message);
            addMessage(msg);
            notifyConnectionStatus(!msg.getType().equalsIgnoreCase("system"));
        });
    }

    public void sendLogon(String station, String remarks) {
        executeAsync(() -> {
            AcarsMessage msg = hoppieAPI.sendLogonATC(station, callsign, remarks);
            if (!msg.getType().equalsIgnoreCase("system")) {
                this.pendingLogonStation = station.trim();
                notifyConnectionStatus(true);
            }
            addMessage(msg);
        });
    }

    public void sendLogoff() {
        executeAsync(() -> {
            AcarsMessage msg = hoppieAPI.sendLogoffATC(currentATS, callsign);
            if (!msg.getType().equalsIgnoreCase("system")) {
                setCurrentATS(null);
                notifyConnectionStatus(true);
            }
            addMessage(msg);
        });
    }

    public void sendPdcRequest(String station, Flight flight, String stand, String atis, String remarks) {
        executeAsync(() -> {
            AcarsMessage msg = hoppieAPI.sendPdcRequest(station, flight, stand, atis, remarks);
            addMessage(msg);
        });
    }

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
                addMessage(acarsMsg);
            }
        });
    }

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

    private void executeAsync(Runnable task) {
        // TODO: Replace with a ThreadPoolExecutor for better resource management
        new Thread(task).start(); // Could be replaced by an Executor
    }

    private void addMessage(AcarsMessage message) {
        if (message == null) return;
        messages.add(0, message);
        for (CpdlcListener l : listeners) {
            l.onMessageReceived(message);
            l.onMessagesUpdated(Collections.unmodifiableList(messages));
        }
    }

    private void setCurrentATS(String ats) {
        this.currentATS = ats;
        this.isLoggedOn = (ats != null);
        for (CpdlcListener l : listeners) {
            l.onAtsUnitChanged(ats);
        }
    }

    private void notifyConnectionStatus(boolean isConnected) {
        for (CpdlcListener l : listeners) {
            l.onConnectionStatusChanged(isConnected);
        }
    }

    private void notifyError(String error) {
        for (CpdlcListener l : listeners) {
            l.onError(error);
        }
    }

    // Getters
    public String getCallsign() { return callsign; }
    public String getCurrentATS() { return currentATS; }
    public boolean isLoggedOn() { return isLoggedOn; }
    public List<AcarsMessage> getMessages() { return Collections.unmodifiableList(messages); }
}
