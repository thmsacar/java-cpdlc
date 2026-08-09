package gui2.pages;

import gui2.components.CduDisplay;
import gui2.components.CduDisplay.DisplayColor;
import gui2.components.CduDisplay.LineItem;
import gui2.controller.CduController;

/**
 * ATC LOGON / STATUS Page displaying status, callsign, and connect prompt.
 * Dep/Dest fields removed per design.
 */
public class LogonStatusPage implements CduPage {

    private String connectToStation = "";

    @Override
    public String getPageTitle() {
        return "ATC-LOGON/STATUS";
    }

    @Override
    public void renderPage(CduDisplay display, CduController controller) {
        boolean isLoggedOn = controller.getService() != null && controller.getService().isLoggedOn();
        String atcCenter = isLoggedOn ? controller.getService().getCurrentATS() : "";
        String nextAts = controller.getNextAts() != null ? controller.getNextAts().trim() : "";

        display.setHeader("ATC-LOGON/STATUS", "", "");
        display.clearLines();

        // Line 0: Top left CURRENT ATC, Top right NEXT ATC (leave blank if empty)
        LineItem currentAtcItem = isLoggedOn && atcCenter != null && !atcCenter.isEmpty() ?
            new LineItem("CURRENT ATC", atcCenter, DisplayColor.GREEN) :
            new LineItem("", "", DisplayColor.WHITE_DIM);

        LineItem nextAtcItem = !nextAts.isEmpty() ?
            new LineItem("NEXT ATC", nextAts, DisplayColor.CYAN) :
            new LineItem("", "", DisplayColor.WHITE_DIM);

        display.setLine(0, currentAtcItem, nextAtcItem);

        // Line 1: CALLSIGN & CONNECT TO: entry field
        boolean isConnected = controller.getService() != null;
        display.setLine(1, 
            new LineItem("CALLSIGN", controller.getCallsign(), isConnected ? DisplayColor.WHITE_DIM : DisplayColor.WHITE), 
            new LineItem("CONNECT TO:", connectToStation.isEmpty() ? "<" : connectToStation, DisplayColor.CYAN)
        );

        // Line 3: LOGON SEND
        display.setLine(3, 
            new LineItem("", "", DisplayColor.WHITE), 
            new LineItem("", "LOGON SEND>", DisplayColor.CYAN)
        );

        // Line 4: Explicit ATC LOGOFF button
        display.setLine(4, 
            new LineItem("", "", DisplayColor.WHITE), 
            new LineItem("", "ATC LOGOFF>", isLoggedOn ? DisplayColor.AMBER : DisplayColor.WHITE_DIM)
        );

        // Line 5: Navigation (LSK 6L / 6R)
        display.setLine(5, 
            new LineItem("", "<MENU", DisplayColor.WHITE), 
            controller.getMessagesLineItem()
        );
    }

    @Override
    public void onLskPressed(int index, boolean isLeft, String scratchpad, CduController controller) {
        boolean isLoggedOn = controller.getService() != null && controller.getService().isLoggedOn();
        String input = scratchpad != null ? scratchpad.trim().toUpperCase() : "";

        if (isLeft) {
            switch (index) {
                case 1: // LSK 2L: CALLSIGN
                    if (controller.getService() == null && !input.isEmpty()) {
                        controller.setCallsign(input);
                        controller.clearScratchpad();
                    }
                    break;
                case 5: // LSK 6L: <MENU
                    controller.showPage(new MainMenuPage());
                    break;
            }
        } else {
            switch (index) {
                case 1: // LSK 2R: CONNECT TO:
                    if (!input.isEmpty()) {
                        connectToStation = input;
                        controller.clearScratchpad();
                    }
                    break;
                case 3: // LSK 4R: LOGON SEND>
                    String targetStation = !connectToStation.isEmpty() ? connectToStation : input;
                    if (controller.getService() != null && !targetStation.isEmpty()) {
                        controller.setNextAts(targetStation);
                        controller.getService().sendLogon(targetStation, "");
                        controller.setStatusMessage("SENDING LOGON...");
                        connectToStation = ""; // Clear entry field after sending
                        if (!input.isEmpty()) controller.clearScratchpad();
                    } else if (targetStation.isEmpty()) {
                        controller.setStatusMessage("ENTER ATC CENTER");
                    } else {
                        controller.setStatusMessage("NO CONNECTION");
                    }
                    break;
                case 4: // LSK 5R: ATC LOGOFF>
                    if (controller.getService() != null && isLoggedOn) {
                        controller.getService().sendLogoff();
                        controller.setStatusMessage("LOGOFF SENT");
                    } else {
                        controller.setStatusMessage("NO ATC");
                    }
                    break;
                case 5: // LSK 6R: MSG>
                    controller.showPage(new MessageListPage());
                    break;
            }
        }
    }
}
