package gui2.pages;

import gui2.components.CduDisplay;
import gui2.components.CduDisplay.DisplayColor;
import gui2.components.CduDisplay.LineItem;
import gui2.controller.CduController;
import hoppie.AcarsMessage;
import hoppie.CpdlcMessage;
import hoppie.TimeFormatter;
import hoppie.CpdlcResponseType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        boolean isOutgoing = message.isOutgoing();
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

        boolean hasReplyPrompts = requiresReplyPrompt(controller);
        int linesPerPage = hasReplyPrompts ? 3 : 5;

        int maxLen = display != null ? display.getLineMaxCharCount() : 24;
        List<String> lines = formatMessageLines(message.getMessage(), maxLen);

        int totalLines = lines.size();
        if (pageOffset >= totalLines && totalLines > 0) {
            pageOffset = Math.max(0, ((totalLines - 1) / linesPerPage) * linesPerPage);
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) totalLines / linesPerPage));
        int currentPageNum = (pageOffset / linesPerPage) + 1;

        String centerSubheader = totalPages > 1 ? currentPageNum + "/" + totalPages : (isSystem ? "SYSTEM" : (isCpdlc ? "CPDLC" : "TELEX"));
        String zuluTime = message.getTimestamp() != null ? TimeFormatter.zuluTime(message.getTimestamp()) : "";

        display.setHeader(headerText, centerSubheader, zuluTime);
        display.clearLines();

        for (int i = 0; i < linesPerPage; i++) {
            int lineIdx = pageOffset + i;
            String textLine = lineIdx < totalLines ? lines.get(lineIdx) : "";
            display.setLine(i, new LineItem("", textLine, DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));
        }

        // Check response prompts for incoming CPDLC messages requiring reply
        if (hasReplyPrompts) {
            CpdlcMessage cpdlc = (CpdlcMessage) message;
            if (cpdlc.hasBeenReplied()) {
                String sent = cpdlc.getSentResponse() != null ? cpdlc.getSentResponse().toUpperCase() : "";
                if ("WILCO".equals(sent) || "STANDBY".equals(sent) || "AFFIRM".equals(sent) || "ROGER".equals(sent)) {
                    display.setLine(3, new LineItem("", "<" + sent, DisplayColor.WHITE_DIM), new LineItem("", "", DisplayColor.WHITE));
                } else if ("UNABLE".equals(sent) || "NEGATIVE".equals(sent)) {
                    display.setLine(3, new LineItem("", "", DisplayColor.WHITE), new LineItem("", sent + ">", DisplayColor.WHITE_DIM));
                }
            } else {
                switch (cpdlc.getParsedResponseType()) {
                    case WILCO_UNABLE:
                        display.setLine(3, new LineItem("", "<WILCO", DisplayColor.GREEN), new LineItem("", "UNABLE>", DisplayColor.AMBER));
                        display.setLine(4, new LineItem("", "<STANDBY", DisplayColor.AMBER), new LineItem("", "", DisplayColor.WHITE));
                        break;
                    case AFFIRM_NEGATIVE:
                        display.setLine(3, new LineItem("", "<AFFIRM", DisplayColor.GREEN), new LineItem("", "NEGATIVE>", DisplayColor.AMBER));
                        display.setLine(4, new LineItem("", "<STANDBY", DisplayColor.AMBER), new LineItem("", "", DisplayColor.WHITE));
                        break;
                    case ROGER:
                        display.setLine(3, new LineItem("", "<ROGER", DisplayColor.CYAN), new LineItem("", "STANDBY>", DisplayColor.AMBER));
                        break;
                    case NONE:
                    default:
                        break;
                }
            }
        }

        // Line 5: Navigation (<BACK and NEXT>/<PREV)
        LineItem rightNav = totalPages > 1 ? 
            new LineItem("", (pageOffset + linesPerPage < totalLines ? "NEXT>" : "<PREV"), DisplayColor.WHITE) :
            new LineItem("", "", DisplayColor.WHITE);

        display.setLine(5, new LineItem("", "<BACK", DisplayColor.WHITE), rightNav);
    }

    private boolean requiresReplyPrompt(CduController controller) {
        if (message == null || message.isOutgoing() || !(message instanceof CpdlcMessage)) return false;

        CpdlcMessage cpdlc = (CpdlcMessage) message;
        if (cpdlc.hasBeenReplied()) return true;

        return cpdlc.getParsedResponseType() != CpdlcResponseType.NONE;
    }

    private List<String> formatMessageLines(String rawMessage, int maxLen) {
        List<String> result = new ArrayList<>();
        if (rawMessage == null || rawMessage.trim().isEmpty()) return result;

        String text = rawMessage.replace("\r", "").replace("@", "\n");
        String[] rawLines = text.split("\n");

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

    @Override
    public void onLskPressed(int index, boolean isLeft, String scratchpad, CduController controller) {
        if (isLeft && index == 5) { // LSK 6L: <BACK
            controller.popPage();
            return;
        }

        boolean hasReplyPrompts = requiresReplyPrompt(controller);
        int linesPerPage = hasReplyPrompts ? 3 : 5;

        int maxLen = (controller != null && controller.getDisplay() != null) ? controller.getDisplay().getLineMaxCharCount() : 24;
        List<String> lines = formatMessageLines(message != null ? message.getMessage() : "", maxLen);

        int totalLines = lines.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalLines / linesPerPage));

        if (!isLeft && index == 5 && totalPages > 1) { // LSK 6R: NEXT> / <PREV
            if (pageOffset + linesPerPage < totalLines) {
                pageOffset += linesPerPage;
            } else {
                pageOffset = 0;
            }
            return;
        }

        if (message != null && !message.isOutgoing() && message instanceof CpdlcMessage && controller.getService() != null) {
            CpdlcMessage cpdlc = (CpdlcMessage) message;
            if (cpdlc.hasBeenReplied()) {
                return;
            }

            switch (cpdlc.getParsedResponseType()) {
                case WILCO_UNABLE:
                    if (isLeft && index == 3) sendResponse("WILCO", controller);
                    if (!isLeft && index == 3) sendResponse("UNABLE", controller);
                    if (isLeft && index == 4) sendResponse("STANDBY", controller);
                    break;
                case AFFIRM_NEGATIVE:
                    if (isLeft && index == 3) sendResponse("AFFIRM", controller);
                    if (!isLeft && index == 3) sendResponse("NEGATIVE", controller);
                    if (isLeft && index == 4) sendResponse("STANDBY", controller);
                    break;
                case ROGER:
                    if (isLeft && index == 3) sendResponse("ROGER", controller);
                    if (isLeft && index == 4) sendResponse("STANDBY", controller);
                    break;
                case NONE:
                default:
                    break;
            }
        }
    }

    private void sendResponse(String type, CduController controller) {
        if (controller.getService() != null && message instanceof CpdlcMessage) {
            CpdlcMessage cpdlc = (CpdlcMessage) message;
            cpdlc.setSentResponse(type);
            controller.getService().sendResponse(type, cpdlc);
            controller.setStatusMessage("CPDLC TO " + message.getFrom());
            controller.popPage();
        }
    }
}
