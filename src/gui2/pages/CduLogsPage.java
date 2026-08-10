package gui2.pages;

import gui2.components.CduDisplay;
import gui2.components.CduDisplay.DisplayColor;
import gui2.components.CduDisplay.LineItem;
import gui2.controller.CduController;
import service.FlightLogManager;

import java.io.File;
import java.util.List;

/**
 * CDU Page displaying real-time flight log status and offering prompts to open/view flight log files.
 */
public class CduLogsPage implements CduPage {

    @Override
    public String getPageTitle() {
        return "FLIGHT LOGS";
    }

    @Override
    public void renderPage(CduDisplay display, CduController controller) {
        display.setHeader("FLIGHT LOGS", "1/1", "");
        display.clearLines();

        FlightLogManager mgr = controller.getService() != null ? controller.getService().getFlightLogManager() : null;
        File activeFile = mgr != null ? mgr.getLogFile() : null;
        String activeName = activeFile != null ? activeFile.getName() : "NONE";

        display.setLine(0, new LineItem("ACTIVE LOG", activeName, DisplayColor.CYAN), new LineItem("", "", DisplayColor.WHITE));
        display.setLine(1, new LineItem("LOCATION", "~/.java-cpdlc/logs", DisplayColor.WHITE_DIM), new LineItem("", "", DisplayColor.WHITE));

        display.setLine(3, new LineItem("", "<OPEN ACTIVE LOG", DisplayColor.GREEN), new LineItem("", "", DisplayColor.WHITE));
        display.setLine(4, new LineItem("", "<OPEN LOG DIR", DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));

        display.setLine(5, new LineItem("", "<BACK", DisplayColor.WHITE), new LineItem("", "", DisplayColor.WHITE));
    }

    @Override
    public void onLskPressed(int index, boolean isLeft, String scratchpad, CduController controller) {
        if (isLeft) {
            FlightLogManager mgr = controller.getService() != null ? controller.getService().getFlightLogManager() : null;

            switch (index) {
                case 3: // LSK 4L: <OPEN ACTIVE LOG
                    if (mgr != null && mgr.openActiveLogFile()) {
                        controller.setStatusMessage("OPENED ACTIVE LOG");
                    } else {
                        controller.setStatusMessage("NO LOG FILE FOUND");
                    }
                    break;
                case 4: // LSK 5L: <OPEN LOG DIR
                    if (FlightLogManager.openLogFile(FlightLogManager.getLogDirectory())) {
                        controller.setStatusMessage("OPENED LOG DIR");
                    } else {
                        controller.setStatusMessage("FAILED TO OPEN DIR");
                    }
                    break;
                case 5: // LSK 6L: <BACK
                    controller.popPage();
                    break;
            }
        }
    }
}
