package gui2.pages;

import gui2.components.CduDisplay;
import gui2.components.CduDisplay.DisplayColor;
import gui2.components.CduDisplay.LineItem;
import gui2.controller.CduController;

/**
 * CPDLC Reports Page supporting POSITION, LEVEL, and SPEED report types with silent INVALID ENTRY validation.
 */
public class ReportPage implements CduPage {

    private String reportType = "POSITION"; // POSITION, LEVEL, SPEED

    // Position fields
    private String position = "";
    private String time = "";
    private String altitude = "";
    private String nextFix = "";
    private String etaNext = "";
    private String thereafter = "";

    // Level fields
    private String levelStatus = "MAINTAINING"; // MAINTAINING, CLIMBING, DESCENDING

    // Speed fields
    private String speed = "";
    private String speedMode = "IAS"; // IAS, MACH

    @Override
    public String getPageTitle() {
        return "CPDLC REPORTS";
    }

    @Override
    public void renderPage(CduDisplay display, CduController controller) {
        display.setHeader("CPDLC REPORTS", "<" + reportType + ">", "");
        display.clearLines();

        // Line 0: Report Type Selection
        display.setLine(0, 
            new LineItem("TYPE", "<" + reportType + ">", DisplayColor.CYAN),
            new LineItem("", "", DisplayColor.WHITE)
        );

        if ("POSITION".equals(reportType)) {
            display.setLine(1, 
                new LineItem("POS", position.isEmpty() ? "----" : position, DisplayColor.WHITE),
                new LineItem("TIME", time.isEmpty() ? "----" : time, DisplayColor.WHITE)
            );
            display.setLine(2, 
                new LineItem("ALT", altitude.isEmpty() ? "----" : altitude, DisplayColor.WHITE),
                new LineItem("NEXT FIX", nextFix.isEmpty() ? "----" : nextFix, DisplayColor.WHITE)
            );
            display.setLine(3, 
                new LineItem("ETA NEXT", etaNext.isEmpty() ? "----" : etaNext, DisplayColor.WHITE),
                new LineItem("THEREAFTER", thereafter.isEmpty() ? "----" : thereafter, DisplayColor.WHITE)
            );
        } else if ("LEVEL".equals(reportType)) {
            display.setLine(1, 
                new LineItem("FL / ALT", altitude.isEmpty() ? "----" : altitude, DisplayColor.WHITE),
                new LineItem("STATUS", "<" + levelStatus + ">", DisplayColor.CYAN)
            );
        } else if ("SPEED".equals(reportType)) {
            String speedDisp = speed.isEmpty() ? "----" : ("MACH".equals(speedMode) ? "." + speed : speed);
            display.setLine(1, 
                new LineItem("SPEED", speedDisp, DisplayColor.WHITE),
                new LineItem("MODE", "<" + speedMode + ">", DisplayColor.CYAN)
            );
        }

        display.setLine(4, 
            new LineItem("", "", DisplayColor.WHITE),
            new LineItem("", "SEND REPORT>", DisplayColor.GREEN)
        );

        display.setLine(5, 
            new LineItem("", "<MENU", DisplayColor.WHITE),
            controller.getMessagesLineItem()
        );
    }

    @Override
    public void onLskPressed(int index, boolean isLeft, String scratchpad, CduController controller) {
        String input = scratchpad != null ? scratchpad.trim().toUpperCase() : "";

        if (isLeft) {
            switch (index) {
                case 0: // LSK 1L: Cycle REPORT TYPE
                    if ("POSITION".equals(reportType)) reportType = "LEVEL";
                    else if ("LEVEL".equals(reportType)) reportType = "SPEED";
                    else reportType = "POSITION";
                    break;
                case 1: // LSK 2L
                    if ("POSITION".equals(reportType)) {
                        if (validateFix(input, controller)) { position = input; controller.clearScratchpad(); }
                    } else if ("LEVEL".equals(reportType)) {
                        String validAlt = parseAltitude(input, controller);
                        if (validAlt != null) { altitude = validAlt; controller.clearScratchpad(); }
                    } else if ("SPEED".equals(reportType)) {
                        String validSpd = parseSpeed(input, speedMode, controller);
                        if (validSpd != null) { speed = validSpd; controller.clearScratchpad(); }
                    }
                    break;
                case 2: // LSK 3L
                    if ("POSITION".equals(reportType)) {
                        String validAlt = parseAltitude(input, controller);
                        if (validAlt != null) { altitude = validAlt; controller.clearScratchpad(); }
                    }
                    break;
                case 3: // LSK 4L
                    if ("POSITION".equals(reportType)) {
                        if (validateTime(input, controller)) { etaNext = input; controller.clearScratchpad(); }
                    }
                    break;
                case 5: // LSK 6L: <MENU
                    controller.showPage(new MainMenuPage());
                    break;
            }
        } else {
            switch (index) {
                case 1: // LSK 2R
                    if ("POSITION".equals(reportType)) {
                        if (validateTime(input, controller)) { time = input; controller.clearScratchpad(); }
                    } else if ("LEVEL".equals(reportType)) {
                        if ("MAINTAINING".equals(levelStatus)) levelStatus = "CLIMBING";
                        else if ("CLIMBING".equals(levelStatus)) levelStatus = "DESCENDING";
                        else levelStatus = "MAINTAINING";
                    } else if ("SPEED".equals(reportType)) {
                        speedMode = "IAS".equals(speedMode) ? "MACH" : "IAS";
                        speed = "";
                    }
                    break;
                case 2: // LSK 3R
                    if ("POSITION".equals(reportType)) {
                        if (validateFix(input, controller)) { nextFix = input; controller.clearScratchpad(); }
                    }
                    break;
                case 3: // LSK 4R
                    if ("POSITION".equals(reportType)) {
                        if (validateFix(input, controller)) { thereafter = input; controller.clearScratchpad(); }
                    }
                    break;
                case 4: // LSK 5R: SEND REPORT>
                    if (controller.getService() != null) {
                        if (!controller.getService().isLoggedOn() || controller.getService().getCurrentATS() == null || controller.getService().getCurrentATS().isEmpty()) {
                            controller.setStatusMessage("NOT LOGGED ON");
                            break;
                        }

                        if ("POSITION".equals(reportType)) {
                            if (!position.isEmpty() && !altitude.isEmpty()) {
                                controller.getService().sendPositionReport(position, time, altitude, thereafter, nextFix, etaNext);
                                controller.setStatusMessage("POS REPORT SENT");
                            } else {
                                controller.setStatusMessage("ENTER POS & ALT");
                            }
                        } else if ("LEVEL".equals(reportType)) {
                            if (!altitude.isEmpty()) {
                                controller.getService().sendReport(levelStatus + " LEVEL " + altitude);
                                controller.setStatusMessage("LEVEL REPORT SENT");
                            } else {
                                controller.setStatusMessage("ENTER ALTITUDE");
                            }
                        } else if ("SPEED".equals(reportType)) {
                            if (!speed.isEmpty()) {
                                String spdText = "MACH".equals(speedMode) ? "M." + speed : "IAS " + speed;
                                controller.getService().sendReport("PRESENT SPEED " + spdText);
                                controller.setStatusMessage("SPEED REPORT SENT");
                            } else {
                                controller.setStatusMessage("ENTER SPEED");
                            }
                        }
                    } else {
                        controller.setStatusMessage("NO CONNECTION");
                    }
                    break;
                case 5: // LSK 6R: MESSAGES>
                    controller.showPage(new MessageListPage());
                    break;
            }
        }
    }

    private boolean validateFix(String input, CduController controller) {
        if (input == null || !input.matches("^[A-Z0-9]{2,7}$")) {
            controller.setStatusMessage("INVALID ENTRY");
            return false;
        }
        return true;
    }

    private boolean validateTime(String input, CduController controller) {
        if (input == null || !input.matches("^\\d{4}$")) {
            controller.setStatusMessage("INVALID ENTRY");
            return false;
        }
        int hh = Integer.parseInt(input.substring(0, 2));
        int mm = Integer.parseInt(input.substring(2, 4));
        if (hh >= 0 && hh <= 23 && mm >= 0 && mm <= 59) {
            return true;
        }
        controller.setStatusMessage("INVALID ENTRY");
        return false;
    }

    private String parseAltitude(String input, CduController controller) {
        if (input == null || input.isEmpty()) {
            controller.setStatusMessage("INVALID ENTRY");
            return null;
        }
        if (input.matches("^FL\\d{3}$")) {
            int fl = Integer.parseInt(input.substring(2));
            if (fl >= 10 && fl <= 600) return input;
        } else if (input.matches("^\\d{3}$")) {
            int fl = Integer.parseInt(input);
            if (fl >= 10 && fl <= 600) return "FL" + String.format("%03d", fl);
        } else if (input.matches("^\\d{1,2}$")) {
            int fl = Integer.parseInt(input);
            if (fl >= 1 && fl <= 60) return "FL" + String.format("%03d", fl);
        } else if (input.matches("^\\d{4,5}$")) {
            int alt = Integer.parseInt(input);
            if (alt >= 1000 && alt <= 60000) return String.valueOf(alt);
        }
        controller.setStatusMessage("INVALID ENTRY");
        return null;
    }

    private String parseSpeed(String input, String mode, CduController controller) {
        if (input == null || input.isEmpty()) {
            controller.setStatusMessage("INVALID ENTRY");
            return null;
        }
        if ("IAS".equals(mode)) {
            if (input.matches("^\\d{2,3}$")) {
                int spd = Integer.parseInt(input);
                if (spd >= 50 && spd <= 450) return String.valueOf(spd);
            }
        } else if ("MACH".equals(mode)) {
            String mDigits = input.replaceAll("[^0-9]", "");
            if (mDigits.length() == 2 || mDigits.length() == 3) {
                if (mDigits.startsWith("0") && mDigits.length() == 3) mDigits = mDigits.substring(1);
                if (mDigits.length() == 2) {
                    int mVal = Integer.parseInt(mDigits);
                    if (mVal >= 50 && mVal <= 99) return mDigits;
                }
            }
        }
        controller.setStatusMessage("INVALID ENTRY");
        return null;
    }
}
