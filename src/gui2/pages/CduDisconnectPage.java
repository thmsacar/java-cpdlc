package gui2.pages;

import gui2.components.CduDisplay;
import gui2.components.CduDisplay.DisplayColor;
import gui2.components.CduDisplay.LineItem;
import gui2.controller.CduController;

/**
 * Disconnect prompt page for CDU.
 * When exitOnDisconnect is true (window close button clicked), confirming disconnect stops service and exits the application window.
 * When exitOnDisconnect is false (menu disconnect button clicked), confirming disconnect stops service and returns to CduLoginPage without closing the window.
 */
public class CduDisconnectPage implements CduPage {

    private final boolean exitOnDisconnect;

    public CduDisconnectPage(boolean exitOnDisconnect) {
        this.exitOnDisconnect = exitOnDisconnect;
    }

    public boolean isExitOnDisconnect() {
        return exitOnDisconnect;
    }

    @Override
    public String getPageTitle() {
        return exitOnDisconnect ? "DISCONNECT & EXIT" : "DISCONNECT";
    }

    @Override
    public void renderPage(CduDisplay display, CduController controller) {
        String cs = controller != null ? controller.getCallsign() : "";
        String headerTitle = (cs != null && !cs.trim().isEmpty()) ? cs.trim().toUpperCase() + " - ATC INDEX" : "ATC INDEX";
        String subHeader = exitOnDisconnect ? "DISCONNECT & EXIT" : "CONFIRM DISCONNECT";

        display.setHeader(headerTitle, subHeader, "");
        display.clearLines();

        boolean isConnected = controller != null && controller.getService() != null;
        String linePrompt = exitOnDisconnect ? "DISCONNECT & EXIT?" : (isConnected ? "DISCONNECT HOPPIE?" : "EXIT APPLICATION?");

        display.setLine(2, 
            new LineItem(linePrompt, "", DisplayColor.AMBER), 
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
    }

    @Override
    public void onLskPressed(int index, boolean isLeft, String scratchpad, CduController controller) {
        if (isLeft) {
            if (index == 3) { // LSK 4L: <CONFIRM
                if (controller.getService() != null) {
                    controller.getService().stop();
                    controller.setService(null);
                }
                controller.setStatusMessage("DISCONNECTED");
                if (exitOnDisconnect) {
                    System.exit(0);
                } else {
                    controller.showPage(new CduLoginPage());
                }
            } else if (index == 5) { // LSK 6L: <CANCEL
                controller.popPage();
            }
        }
    }
}
