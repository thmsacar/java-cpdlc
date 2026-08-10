package gui2.pages;

import gui2.components.CduDisplay;
import gui2.components.CduDisplay.DisplayColor;
import gui2.components.CduDisplay.LineItem;
import gui2.controller.CduController;

/**
 * ACARS Telex Messaging Page.
 */
public class TelexPage implements CduPage {

    private String targetStation = "";
    private String messageText = "";

    @Override
    public String getPageTitle() {
        return "ACARS TELEX";
    }

    @Override
    public void renderPage(CduDisplay display, CduController controller) {
        display.setHeader("ACARS TELEX", "", "");
        display.clearLines();

        display.setLine(0, 
            new LineItem("STATION", targetStation.isEmpty() ? "----" : targetStation, DisplayColor.WHITE),
            new LineItem("", "", DisplayColor.WHITE)
        );

        display.setLine(1, 
            new LineItem("MESSAGE", messageText.isEmpty() ? "[]" : messageText, DisplayColor.CYAN),
            new LineItem("", "", DisplayColor.WHITE)
        );

        display.setLine(3, 
            new LineItem("", "", DisplayColor.WHITE),
            new LineItem("", "SEND TELEX>", DisplayColor.GREEN)
        );

        display.setLine(5, 
            new LineItem("", "<MENU", DisplayColor.WHITE),
            controller.getMessagesLineItem()
        );
    }

    @Override
    public void onLskPressed(int index, boolean isLeft, String scratchpad, CduController controller) {
        if (isLeft) {
            if (index == 0) { // LSK 1L: STATION
                if (!scratchpad.isEmpty()) {
                    targetStation = scratchpad.trim().toUpperCase();
                    controller.clearScratchpad();
                }
            } else if (index == 1) { // LSK 2L: MSG TEXT
                if (!scratchpad.isEmpty()) {
                    messageText = scratchpad.trim().toUpperCase();
                    controller.clearScratchpad();
                }
            } else if (index == 5) { // LSK 6L: <MENU
                controller.showPage(new MainMenuPage());
            }
        } else {
            if (index == 3) { // LSK 4R: SEND TELEX>
                if (controller.getService() != null && !targetStation.isEmpty() && !messageText.isEmpty()) {
                    controller.getService().sendTelex(targetStation, messageText);
                    controller.setStatusMessage("SENDING TELEX TO " + targetStation);
                    messageText = "";
                } else {
                    controller.setStatusMessage("ENTER STATION & MSG");
                }
            } else if (index == 5) { // LSK 6R: MESSAGES>
                controller.showPage(new MessageListPage());
            }
        }
    }
}
