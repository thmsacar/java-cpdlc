package gui2.pages;

import gui2.components.CduDisplay;
import gui2.components.CduDisplay.DisplayColor;
import gui2.components.CduDisplay.LineItem;
import gui2.controller.CduController;
import hoppie.AcarsMessage;
import hoppie.CpdlcMessage;
import hoppie.TimeFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Message Detail & Reply page for CDU with CPDLC TO/FROM and TELEX TO/FROM headers,
 * automatic line wrapping, multi-page pagination (NEXT/PREV), and reply prompts.
 */
public class MessageDetailPage implements CduPage {

    private final AcarsMessage message;
    private int pageOffset = 0;

    public MessageDetailPage(AcarsMessage message) {
        this.message = message;
        if (message != null) {
            message.setRead(true);
        }
    }

    @Override
    public String getPageTitle() {
        return "MESSAGE DETAIL";
    }

    @Override
    public void renderPage(CduDisplay display, CduController controller) {
        if (message == null) {
            display.setHeader("MESSAGE DETAIL", "", "");
            display.clearLines();
            display.setLine(5, new LineItem("", "<BACK", DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));
            return;
        }

        boolean isOutgoing = message.getFrom() != null && message.getFrom().equalsIgnoreCase(controller.getCallsign());
        boolean isCpdlc = message instanceof CpdlcMessage || "CPDLC".equalsIgnoreCase(message.getType());
        boolean isSystem = "SYSTEM".equalsIgnoreCase(message.getType());

        String headerText;
        if (isSystem) {
            headerText = "SYSTEM MSG";
        } else if (isOutgoing) {
            headerText = (isCpdlc ? "CPDLC TO: " : "TELEX TO: ") + (message.getTo() != null ? message.getTo() : "");
        } else {
            headerText = (isCpdlc ? "CPDLC FROM: " : "TELEX FROM: ") + (message.getFrom() != null ? message.getFrom() : "");
        }

        List<String> lines = formatMessageLines(message.getMessage());
        int totalLines = lines.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalLines / 3.0));
        int currentPageNum = (pageOffset / 3) + 1;

        String centerSubheader = totalPages > 1 ? currentPageNum + "/" + totalPages : (isSystem ? "SYSTEM" : (isCpdlc ? "CPDLC" : "TELEX"));
        String zuluTime = message.getTimestamp() != null ? TimeFormatter.zuluTime(message.getTimestamp()) : "";

        display.setHeader(headerText, centerSubheader, zuluTime);
        display.clearLines();

        for (int i = 0; i < 3; i++) {
            int lineIdx = pageOffset + i;
            String textLine = lineIdx < totalLines ? lines.get(lineIdx) : "";
            display.setLine(i, new LineItem("", textLine, DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));
        }

        // Check response prompts for incoming CPDLC messages requiring reply
        if (!isOutgoing && message instanceof CpdlcMessage) {
            CpdlcMessage cpdlc = (CpdlcMessage) message;
            String resType = cpdlc.getResponseType() != null ? cpdlc.getResponseType().trim().toUpperCase() : "";

            if (isWilcoUnableRequired(resType)) {
                display.setLine(3, new LineItem("", "<WILCO", DisplayColor.GREEN), new LineItem("", "UNABLE>", DisplayColor.AMBER));
                display.setLine(4, new LineItem("", "<STANDBY", DisplayColor.AMBER), new LineItem("", "", DisplayColor.WHITE));
            } else if ("AN".equals(resType) || "A/N".equals(resType) || "AFFIRM".equals(resType)) {
                display.setLine(3, new LineItem("", "<AFFIRM", DisplayColor.GREEN), new LineItem("", "NEGATIVE>", DisplayColor.AMBER));
                display.setLine(4, new LineItem("", "<STANDBY", DisplayColor.AMBER), new LineItem("", "", DisplayColor.WHITE));
            } else if ("R".equals(resType) || "ROGER".equals(resType)) {
                display.setLine(3, new LineItem("", "<ROGER", DisplayColor.CYAN), new LineItem("", "STANDBY>", DisplayColor.AMBER));
            }
        }

        // Line 5: Navigation (<BACK and NEXT>/<PREV)
        LineItem rightNav = totalPages > 1 ? 
            new LineItem("", (pageOffset + 3 < totalLines ? "NEXT>" : "<PREV"), DisplayColor.WHITE) :
            new LineItem("", "", DisplayColor.WHITE);

        display.setLine(5, new LineItem("", "<BACK", DisplayColor.WHITE), rightNav);
    }

    private List<String> formatMessageLines(String rawMessage) {
        List<String> result = new ArrayList<>();
        if (rawMessage == null || rawMessage.trim().isEmpty()) return result;

        String text = rawMessage.replace("\r", "").replace("@", "\n");
        String[] rawLines = text.split("\n");
        int maxLen = 22;

        for (String raw : rawLines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            while (line.length() > maxLen) {
                int splitIdx = line.lastIndexOf(' ', maxLen);
                if (splitIdx <= 0) splitIdx = maxLen;
                result.add(line.substring(0, splitIdx).trim());
                line = line.substring(splitIdx).trim();
            }
            if (!line.isEmpty()) {
                result.add(line);
            }
        }
        return result;
    }

    private boolean isWilcoUnableRequired(String resType) {
        if (resType == null || resType.isEmpty() || "N".equals(resType) || "NE".equals(resType) || "NO".equals(resType)) {
            return false;
        }
        return "Y".equals(resType) || "YES".equals(resType) || "WU".equals(resType) || "W/U".equals(resType) || "WILCO".equals(resType);
    }

    @Override
    public void onLskPressed(int index, boolean isLeft, String scratchpad, CduController controller) {
        if (isLeft && index == 5) { // LSK 6L: <BACK
            controller.popPage();
            return;
        }

        List<String> lines = formatMessageLines(message != null ? message.getMessage() : "");
        int totalLines = lines.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalLines / 3.0));

        if (!isLeft && index == 5 && totalPages > 1) { // LSK 6R: NEXT> / <PREV
            if (pageOffset + 3 < totalLines) {
                pageOffset += 3;
            } else {
                pageOffset = 0;
            }
            return;
        }

        boolean isOutgoing = message != null && message.getFrom() != null && message.getFrom().equalsIgnoreCase(controller.getCallsign());

        if (!isOutgoing && message instanceof CpdlcMessage && controller.getService() != null) {
            CpdlcMessage cpdlc = (CpdlcMessage) message;
            String resType = cpdlc.getResponseType() != null ? cpdlc.getResponseType().trim().toUpperCase() : "";

            if (isWilcoUnableRequired(resType)) {
                if (isLeft && index == 3) sendResponse("WILCO", controller);
                if (!isLeft && index == 3) sendResponse("UNABLE", controller);
                if (isLeft && index == 4) sendResponse("STANDBY", controller);
            } else if ("AN".equals(resType) || "A/N".equals(resType) || "AFFIRM".equals(resType)) {
                if (isLeft && index == 3) sendResponse("AFFIRM", controller);
                if (!isLeft && index == 3) sendResponse("NEGATIVE", controller);
                if (isLeft && index == 4) sendResponse("STANDBY", controller);
            } else if ("R".equals(resType) || "ROGER".equals(resType)) {
                if (isLeft && index == 3) sendResponse("ROGER", controller);
                if (isLeft && index == 4) sendResponse("STANDBY", controller);
            }
        }
    }

    private void sendResponse(String type, CduController controller) {
        if (controller.getService() != null && message instanceof CpdlcMessage) {
            controller.getService().sendResponse(type, (CpdlcMessage) message);
            controller.setStatusMessage("CPDLC TO " + message.getFrom());
            controller.popPage();
        }
    }
}
