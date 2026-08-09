package gui2.pages;

import gui2.components.CduDisplay;
import gui2.components.CduDisplay.DisplayColor;
import gui2.components.CduDisplay.LineItem;
import gui2.controller.CduController;
import hoppie.AcarsMessage;
import hoppie.TimeFormatter;

import java.util.List;

/**
 * Messages Inbox / Outbox list page for CDU displaying incoming (<SENDER) and outgoing (RECIPIENT>) items with Zulu time.
 */
public class MessageListPage implements CduPage {

    private int pageOffset = 0;

    @Override
    public String getPageTitle() {
        return "CPDLC MESSAGES";
    }

    @Override
    public void renderPage(CduDisplay display, CduController controller) {
        List<AcarsMessage> msgs = controller.getService() != null ? controller.getService().getMessages() : List.of();
        int total = msgs.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / 4.0));
        int currentPageNum = (pageOffset / 4) + 1;

        display.setHeader("CPDLC MESSAGES", currentPageNum + "/" + totalPages, "");
        display.clearLines();

        for (int i = 0; i < 4; i++) {
            int msgIdx = pageOffset + i;
            if (msgIdx < total) {
                AcarsMessage msg = msgs.get(msgIdx);
                boolean isOutgoing = msg.getFrom() != null && msg.getFrom().equalsIgnoreCase(controller.getCallsign());
                String stationLabel = isOutgoing ? (msg.getTo() != null ? msg.getTo() : "") + ">" : "<" + (msg.getFrom() != null ? msg.getFrom() : "");
                String preview = msg.getMessage() != null ? msg.getMessage().replace("@", " ").replace("\n", " ").replaceAll("\\s+", " ").trim() : "";
                if (preview.length() > 14) preview = preview.substring(0, 14) + "..";

                String zuluTime = msg.getTimestamp() != null ? TimeFormatter.zuluTime(msg.getTimestamp()) : "";

                display.setLine(i, 
                    new LineItem(stationLabel, preview, msg.isRead() ? DisplayColor.WHITE_DIM : DisplayColor.GREEN),
                    new LineItem("", zuluTime, msg.isRead() ? DisplayColor.WHITE_DIM : DisplayColor.CYAN)
                );
            }
        }

        display.setLine(5, 
            new LineItem("", "<MENU", DisplayColor.WHITE),
            new LineItem("", total > pageOffset + 4 ? "NEXT>" : "<PREV", DisplayColor.WHITE)
        );
    }

    @Override
    public void onLskPressed(int index, boolean isLeft, String scratchpad, CduController controller) {
        List<AcarsMessage> msgs = controller.getService() != null ? controller.getService().getMessages() : List.of();

        if (isLeft) {
            if (index >= 0 && index < 4) {
                int msgIdx = pageOffset + index;
                if (msgIdx < msgs.size()) {
                    controller.pushPage(new MessageDetailPage(msgs.get(msgIdx)));
                }
            } else if (index == 5) { // LSK 6L: <MENU
                controller.showPage(new MainMenuPage());
            }
        } else {
            if (index == 5) { // LSK 6R: NEXT> or PREV>
                if (pageOffset + 4 < msgs.size()) {
                    pageOffset += 4;
                } else {
                    pageOffset = 0;
                }
            }
        }
    }
}
