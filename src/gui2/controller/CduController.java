package gui2.controller;

import flight.Flight;
import gui.SoundManager;
import gui2.components.CduDisplay;
import gui2.pages.CduDisconnectPage;
import gui2.pages.CduLoginPage;
import gui2.pages.CduPage;
import gui2.pages.MainMenuPage;
import gui2.pages.MessageDetailPage;
import gui2.pages.MessageListPage;
import gui2.pages.PdcPage;
import gui2.pages.ReportPage;
import gui2.pages.RequestPage;
import hoppie.AcarsMessage;
import hoppie.CpdlcMessage;
import service.ConnectionState;
import service.CpdlcListener;
import service.CpdlcService;
import service.UserPreferences;

import javax.swing.SwingUtilities;
import java.awt.Taskbar;
import java.awt.Window;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main Controller for CDU UI managing page navigation stack, scratchpad input,
 * manual login connection, automatic CPDLC handoff detection, visual LED indicators (MSG, CONN solid, FAIL),
 * OS taskbar notifications on incoming messages/errors, sound notifications, and CpdlcService integration.
 */
public class CduController implements CpdlcListener {

    private final CduDisplay display;
    private final Deque<CduPage> pageStack = new ArrayDeque<>();
    private CduPage currentPage;

    private Consumer<Boolean> msgLedConsumer;
    private Consumer<Boolean> execLedConsumer;
    private Consumer<Boolean> failLedConsumer;

    private CpdlcService service;
    private String callsign = "";
    private String hoppieID = "";
    private String simbriefID = "";

    private String atcCenter = "";
    private String nextAts = ""; // Next Data Authority (NDA)

    private String scratchpad = "";
    private String statusMessage = "ACARS READY - ENTER CREDENTIALS";

    public CduController(CduDisplay display) {
        this.display = display;
        if (this.display != null) {
            this.display.setOnResizeListener(this::refreshDisplay);
        }
        loadPreferences();
    }

    public void setMsgLedConsumer(Consumer<Boolean> consumer) { this.msgLedConsumer = consumer; }
    public void setExecLedConsumer(Consumer<Boolean> consumer) { this.execLedConsumer = consumer; }
    public void setFailLedConsumer(Consumer<Boolean> consumer) { this.failLedConsumer = consumer; }

    private void setMsgLed(boolean active) {
        if (msgLedConsumer != null) msgLedConsumer.accept(active);
    }
    private void setExecLed(boolean active) {
        if (execLedConsumer != null) execLedConsumer.accept(active);
    }
    private void setFailLed(boolean active) {
        if (failLedConsumer != null) failLedConsumer.accept(active);
    }

    public void triggerUserAttention() {
        if (display == null) return;
        SwingUtilities.invokeLater(() -> {
            Window window = SwingUtilities.getWindowAncestor(display);
            if (window != null && !window.isActive()) {
                try {
                    if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.USER_ATTENTION)) {
                        Taskbar.getTaskbar().requestUserAttention(true, true);
                    }
                } catch (Throwable ignored) {}

                try {
                    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                        try {
                            Class<?> appClass = Class.forName("com.apple.eawt.Application");
                            Object application = appClass.getMethod("getApplication").invoke(null);
                            appClass.getMethod("requestUserAttention", boolean.class).invoke(application, true);
                        } catch (Throwable t) {
                            window.toFront();
                        }
                    } else {
                        window.toFront();
                    }
                } catch (Throwable ignored) {}
            }
        });
    }

    private void loadPreferences() {
        this.callsign = UserPreferences.getLastCallsign();
        this.hoppieID = UserPreferences.getLastHoppieID();
        this.simbriefID = UserPreferences.getLastSimbriefID();
    }

    public void connect(String callsign, String hoppieID) {
        this.callsign = callsign.trim().toUpperCase();
        this.hoppieID = hoppieID.trim();

        UserPreferences.setLastCallsign(this.callsign);
        UserPreferences.setLastHoppieID(this.hoppieID);

        if (service != null) {
            service.stop();
        }

        this.service = new CpdlcService(this.callsign, this.hoppieID);
        this.service.addListener(this);
        this.service.start();

        this.statusMessage = "CONNECTED TO HOPPIE";
        setFailLed(false);
        setExecLed(true);

        // Open Menu page directly on connection
        showPage(new MainMenuPage());
        refreshDisplay();
    }

    public void handleWindowClose() {
        if (service == null) {
            System.exit(0);
            return;
        }
        if (currentPage instanceof CduDisconnectPage && ((CduDisconnectPage) currentPage).isExitOnDisconnect()) {
            return;
        }
        pushPage(new CduDisconnectPage(true));
    }

    public void showPage(CduPage page) {
        if (page == null) return;
        this.currentPage = page;
        refreshDisplay();
    }

    public void pushPage(CduPage page) {
        if (currentPage != null) {
            pageStack.push(currentPage);
        }
        showPage(page);
    }

    public boolean hasPreviousPage() {
        return !pageStack.isEmpty();
    }

    public void popPage() {
        if (!pageStack.isEmpty()) {
            showPage(pageStack.pop());
        } else {
            showPage(new MainMenuPage());
        }
    }

    public void onLskPressed(int index, boolean isLeft) {
        if (currentPage != null) {
            currentPage.onLskPressed(index, isLeft, scratchpad, this);
            refreshDisplay();
        }
    }

    public void handleKeyTyped(String key) {
        if (key == null || key.isEmpty()) return;

        switch (key) {
            case "CLR":
                if (!scratchpad.isEmpty()) {
                    scratchpad = scratchpad.substring(0, scratchpad.length() - 1);
                }
                break;
            case "DEL":
                scratchpad = "";
                break;
            case "SP":
            case " ":
                scratchpad += " ";
                break;
            case "MENU":
                showPage(new MainMenuPage());
                return;
            default:
                if (key.length() == 1) {
                    scratchpad += key;
                }
                break;
        }
        refreshDisplay();
    }

    public void handlePaste(String pasteText) {
        if (pasteText != null && !pasteText.isEmpty()) {
            this.scratchpad += pasteText;
            refreshDisplay();
        }
    }

    public void clearScratchpad() {
        this.scratchpad = "";
        refreshDisplay();
    }

    public void refreshDisplay() {
        if (display == null) return;

        if (currentPage != null) {
            currentPage.renderPage(display, this);
        }
        display.setScratchpad(scratchpad);
        display.setStatusText(statusMessage);

        // Update LEDs
        if (service != null) {
            boolean hasUnread = service.getMessages().stream().anyMatch(m -> !m.isRead());
            setMsgLed(hasUnread);
            setExecLed(service.isConnected());
        } else {
            setExecLed(false);
        }
    }

    // CpdlcListener Callbacks
    @Override
    public void onMessageReceived(AcarsMessage message) {
        if (message != null) {
            String from = message.getFrom() != null ? message.getFrom().trim() : "";
            String to = message.getTo() != null ? message.getTo().trim() : "";
            String type = message.getType() != null ? message.getType().trim().toUpperCase() : "ACARS";
            String text = message.getMessage() != null ? message.getMessage() : "";

            boolean isOutgoing = from.equalsIgnoreCase(callsign);
            boolean isCpdlc = "CPDLC".equalsIgnoreCase(type) || message instanceof CpdlcMessage;

            if ("SYSTEM".equalsIgnoreCase(type)) {
                // Do NOT play sound for initial connection messages
                if (text.toLowerCase().startsWith("connected as") || text.contains("CONNECTED TO")) {
                    setFailLed(false);
                    setExecLed(true);
                    if (service != null && service.isLoggedOn()) {
                        statusMessage = "LOGGED TO " + service.getCurrentATS();
                    } else {
                        statusMessage = "CONNECTED TO HOPPIE";
                    }
                } else {
                    SoundManager.playWarning();
                    setMsgLed(true);
                    statusMessage = text;
                    String lowerText = text.toLowerCase();
                    if (lowerText.startsWith("error") || lowerText.contains("timeout") || lowerText.contains("timed out") || lowerText.contains("failed")) {
                        setFailLed(true);
                        setNextAts("");
                    }
                    triggerUserAttention();
                }
            } else if (isOutgoing) {
                // Sent message: status bar says CPDLC TO or TELEX TO
                setFailLed(false);
                setExecLed(true);
                if (!statusMessage.startsWith("AUTO LOGOFF")) {
                    statusMessage = (isCpdlc ? "CPDLC TO " : "TELEX TO ") + to;
                }
                if (currentPage instanceof PdcPage || currentPage instanceof RequestPage || currentPage instanceof ReportPage) {
                    SwingUtilities.invokeLater(() -> showPage(new MessageListPage()));
                }
            } else {
                // Incoming message: status bar says CPDLC FROM or TELEX FROM
                SoundManager.playNotification();
                setMsgLed(true);
                setFailLed(false);
                setExecLed(true);
                statusMessage = (isCpdlc ? "CPDLC FROM " : "TELEX FROM ") + from;
                triggerUserAttention();
            }
        }
        refreshDisplay();
    }

    @Override
    public void onMessagesUpdated(List<AcarsMessage> messages) {
        refreshDisplay();
    }

    @Override
    public void onAutoLogoff(String station) {
        String targetStation = (station != null && !station.isEmpty()) ? station : atcCenter;
        this.statusMessage = "AUTO LOGOFF " + targetStation;
        refreshDisplay();
    }

    @Override
    public void onAutoHandover(String nextStation) {
        if (nextStation != null && !nextStation.isEmpty()) {
            setNextAts(nextStation);
            this.statusMessage = "AUTO HANDOVER " + nextStation;
        }
        refreshDisplay();
    }

    @Override
    public void onAtsUnitChanged(String atsUnit) {
        if (atsUnit != null) {
            this.atcCenter = atsUnit;
            this.statusMessage = "LOGGED TO " + atsUnit;
            if (atsUnit.equalsIgnoreCase(this.nextAts) || atsUnit.equalsIgnoreCase(atcCenter) || (service != null && atsUnit.equalsIgnoreCase(service.getNextATS()))) {
                setNextAts("");
            }
        } else {
            if (!statusMessage.startsWith("AUTO LOGOFF") && !statusMessage.startsWith("AUTO HANDOVER")) {
                this.statusMessage = "NO ATC";
            }
        }
        refreshDisplay();
    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        // Maintained for backwards compatibility; state handling delegated to onConnectionStateChanged
    }

    @Override
    public void onConnectionStateChanged(ConnectionState oldState, ConnectionState newState) {
        if (newState == ConnectionState.CONNECTED) {
            setExecLed(true);
            setFailLed(false);
            if (statusMessage != null && (statusMessage.startsWith("AUTO LOGOFF") || statusMessage.startsWith("AUTO HANDOVER"))) {
                // Preserve AUTO LOGOFF and AUTO HANDOVER status message
            } else if (service != null && service.isLoggedOn()) {
                statusMessage = "LOGGED TO " + service.getCurrentATS();
            } else {
                statusMessage = "CONNECTED TO HOPPIE";
            }
        } else if (newState == ConnectionState.RECONNECTING) {
            setExecLed(false);
            setFailLed(true);
            statusMessage = "RECONNECTING...";
        } else if (newState == ConnectionState.CONNECTING) {
            setExecLed(false);
            setFailLed(false);
            statusMessage = "CONNECTING...";
        } else { // DISCONNECTED
            setExecLed(false);
            setFailLed(true);
            statusMessage = "HOPPIE DISCONNECTED";
            if (oldState == ConnectionState.CONNECTED || oldState == ConnectionState.RECONNECTING) {
                SoundManager.playWarning();
                triggerUserAttention();
            }
        }
        refreshDisplay();
    }

    @Override
    public void onError(String errorMessage) {
        SoundManager.playWarning();
        setFailLed(true);
        statusMessage = "ERROR: " + errorMessage;
        triggerUserAttention();
        refreshDisplay();
    }

    // Getters / Setters
    public CduDisplay getDisplay() { return display; }
    public CpdlcService getService() { return service; }
    public void setService(CpdlcService service) { 
        this.service = service; 
        if (service != null) service.addListener(this);
    }

    public String getCallsign() { return callsign; }
    public void setCallsign(String callsign) { this.callsign = callsign; UserPreferences.setLastCallsign(callsign); }

    public String getHoppieID() { return hoppieID; }
    public void setHoppieID(String hoppieID) { this.hoppieID = hoppieID; UserPreferences.setLastHoppieID(hoppieID); }

    public String getSimbriefID() { return simbriefID; }
    public void setSimbriefID(String simbriefID) { this.simbriefID = simbriefID; UserPreferences.setLastSimbriefID(simbriefID); }

    public String getAtcCenter() { return atcCenter; }
    public void setAtcCenter(String atcCenter) { this.atcCenter = atcCenter; }

    public String getNextAts() { 
        String current = (service != null && service.getCurrentATS() != null) ? service.getCurrentATS().trim() : (atcCenter != null ? atcCenter.trim() : "");
        String next = (service != null && service.getNextATS() != null && !service.getNextATS().isEmpty()) 
                ? service.getNextATS().trim() 
                : (nextAts != null ? nextAts.trim() : "");
        if (!current.isEmpty() && !next.isEmpty() && current.equalsIgnoreCase(next)) {
            return "";
        }
        return next; 
    }
    public void setNextAts(String nextAts) { 
        String cleaned = nextAts != null ? nextAts.trim() : "";
        String current = (service != null && service.getCurrentATS() != null) ? service.getCurrentATS().trim() : (atcCenter != null ? atcCenter.trim() : "");
        if (!current.isEmpty() && cleaned.equalsIgnoreCase(current)) {
            cleaned = "";
        }
        this.nextAts = cleaned; 
        if (service != null) service.setNextATS(this.nextAts);
        refreshDisplay(); 
    }

    public String getScratchpad() { return scratchpad; }
    public void setScratchpad(String scratchpad) { this.scratchpad = scratchpad; refreshDisplay(); }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; refreshDisplay(); }

    public String getMessagesLabel() {
        int unreadCount = service != null ? 
            (int) service.getMessages().stream().filter(m -> !m.isRead() && (m.getFrom() == null || !m.getFrom().equalsIgnoreCase(callsign))).count() : 0;
        return unreadCount > 0 ? "MESSAGES [" + unreadCount + "]>" : "MESSAGES>";
    }

    public CduDisplay.LineItem getMessagesLineItem() {
        int unreadCount = service != null ? 
            (int) service.getMessages().stream().filter(m -> !m.isRead() && (m.getFrom() == null || !m.getFrom().equalsIgnoreCase(callsign))).count() : 0;
        String label = unreadCount > 0 ? "MESSAGES [" + unreadCount + "]>" : "MESSAGES>";
        CduDisplay.DisplayColor color = unreadCount > 0 ? CduDisplay.DisplayColor.CYAN : CduDisplay.DisplayColor.WHITE;
        return new CduDisplay.LineItem("", label, color);
    }
}
