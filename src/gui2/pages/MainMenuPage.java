package gui2.pages;

import gui2.components.CduDisplay;
import gui2.components.CduDisplay.DisplayColor;
import gui2.components.CduDisplay.LineItem;
import gui2.controller.CduController;

/**
 * Main ATC Index Menu Page with unread message counter, navigation,
 * and Disconnect confirmation prompt.
 */
public class MainMenuPage implements CduPage {

    @Override
    public String getPageTitle() {
        return "ATC INDEX";
    }

    @Override
    public void renderPage(CduDisplay display, CduController controller) {
        String cs = controller != null ? controller.getCallsign() : "";
        String headerTitle = (cs != null && !cs.trim().isEmpty()) ? cs.trim().toUpperCase() + " - ATC INDEX" : "ATC INDEX";

        display.setHeader(headerTitle, "1/1", "");
        display.clearLines();

        boolean isConnectedToAtc = controller.getService() != null 
            && controller.getService().isLoggedOn() 
            && controller.getService().getCurrentATS() != null 
            && !controller.getService().getCurrentATS().trim().isEmpty();

        display.setLine(0, new LineItem("", "<ATC LOGON/STATUS", DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));
        display.setLine(1, new LineItem("", "<TELEX", DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));
        display.setLine(2, new LineItem("", "<PDC REQUEST", DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));
        display.setLine(3, isConnectedToAtc ? new LineItem("", "<REQUESTS", DisplayColor.WHITE) : new LineItem("", "", DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));
        display.setLine(4, isConnectedToAtc ? new LineItem("", "<REPORTS", DisplayColor.WHITE) : new LineItem("", "", DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));

        display.setLine(5, 
            new LineItem("", "<DISCONNECT", DisplayColor.AMBER), 
            controller.getMessagesLineItem()
        );
    }

    @Override
    public void onLskPressed(int index, boolean isLeft, String scratchpad, CduController controller) {
        boolean isConnectedToAtc = controller.getService() != null 
            && controller.getService().isLoggedOn() 
            && controller.getService().getCurrentATS() != null 
            && !controller.getService().getCurrentATS().trim().isEmpty();

        if (isLeft) {
            switch (index) {
                case 0: controller.showPage(new LogonStatusPage()); break;
                case 1: controller.showPage(new TelexPage()); break;
                case 2: controller.showPage(new PdcPage()); break;
                case 3: if (isConnectedToAtc) controller.showPage(new RequestPage()); break;
                case 4: if (isConnectedToAtc) controller.showPage(new ReportPage()); break;
                case 5: controller.pushPage(new CduDisconnectPage(false)); break; // LSK 6L: <DISCONNECT (stay open on disconnect)
            }
        } else {
            if (index == 5) { // LSK 6R: MESSAGES
                controller.showPage(new MessageListPage());
            }
        }
    }
}
