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

    private boolean isConfirmingDisconnect = false;

    @Override
    public String getPageTitle() {
        return "ATC INDEX";
    }

    @Override
    public void renderPage(CduDisplay display, CduController controller) {
        if (isConfirmingDisconnect) {
            display.setHeader("ATC INDEX", "CONFIRM DISCONNECT", "");
            display.clearLines();

            display.setLine(2, 
                new LineItem("DISCONNECT HOPPIE?", "", DisplayColor.AMBER), 
                new LineItem("", "", DisplayColor.WHITE)
            );

            display.setLine(3, 
                new LineItem("", "<CONFIRM", DisplayColor.AMBER), 
                new LineItem("", "", DisplayColor.WHITE)
            );

            display.setLine(5, 
                new LineItem("", "<CANCEL", DisplayColor.WHITE), 
                new LineItem("", "", DisplayColor.WHITE)
            );
            return;
        }

        display.setHeader("ATC INDEX", "1/1", "");
        display.clearLines();

        display.setLine(0, new LineItem("", "<ATC LOGON/STATUS", DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));
        display.setLine(1, new LineItem("", "<TELEX", DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));
        display.setLine(2, new LineItem("", "<PDC REQUEST", DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));
        display.setLine(3, new LineItem("", "<REQUESTS", DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));
        display.setLine(4, new LineItem("", "<REPORTS", DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));

        display.setLine(5, 
            new LineItem("", "<DISCONNECT", DisplayColor.AMBER), 
            controller.getMessagesLineItem()
        );
    }

    @Override
    public void onLskPressed(int index, boolean isLeft, String scratchpad, CduController controller) {
        if (isConfirmingDisconnect) {
            if (isLeft && index == 3) { // LSK 4L: <CONFIRM
                if (controller.getService() != null) {
                    controller.getService().stop();
                    controller.setService(null);
                }
                controller.setStatusMessage("DISCONNECTED");
                controller.showPage(new CduLoginPage());
            } else if (isLeft && index == 5) { // LSK 6L: <CANCEL
                isConfirmingDisconnect = false;
                controller.refreshDisplay();
            }
            return;
        }

        if (isLeft) {
            switch (index) {
                case 0: controller.showPage(new LogonStatusPage()); break;
                case 1: controller.showPage(new TelexPage()); break;
                case 2: controller.showPage(new PdcPage()); break;
                case 3: controller.showPage(new RequestPage()); break;
                case 4: controller.showPage(new ReportPage()); break;
                case 5: isConfirmingDisconnect = true; controller.refreshDisplay(); break; // LSK 6L: <DISCONNECT
            }
        } else {
            if (index == 5) { // LSK 6R: MESSAGES
                controller.showPage(new MessageListPage());
            }
        }
    }
}
