package gui2.pages;

import gui2.components.CduDisplay;
import gui2.components.CduDisplay.DisplayColor;
import gui2.components.CduDisplay.LineItem;
import gui2.controller.CduController;

/**
 * CPDLC Requests Page for DIRECT, ALTITUDE (FLxxx format), SPEED (IAS/MACH mode),
 * and EXPECT (WHEN CAN WE EXPECT LEVEL / SPEED / DIRECT TO) with strict input validation.
 */
public class RequestPage implements CduPage {

    private String reqType = "DIRECT"; // DIRECT, ALTITUDE, SPEED, EXPECT
    private String expectSubtype = "LEVEL"; // LEVEL, SPEED, DIRECT
    private String value = "";
    private String speedMode = "IAS"; // IAS, MACH
    private String dueTo = "";

    @Override
    public String getPageTitle() {
        return "CPDLC REQUESTS";
    }

    @Override
    public void renderPage(CduDisplay display, CduController controller) {
        display.setHeader("CPDLC REQUESTS", "<" + reqType + ">", "");
        display.clearLines();

        boolean isExpect = "EXPECT".equals(reqType);
        boolean isSpeed = "SPEED".equals(reqType) || (isExpect && "SPEED".equals(expectSubtype));

        display.setLine(0, 
            new LineItem("REQ TYPE", "<" + reqType + ">", DisplayColor.CYAN),
            new LineItem(isExpect ? "EXPECT" : "", isExpect ? "<" + expectSubtype + ">" : "", DisplayColor.CYAN)
        );

        String valueDisplay = value.isEmpty() ? "[]" : value;
        if (isSpeed && "MACH".equals(speedMode) && !value.isEmpty()) {
            valueDisplay = "." + value;
        }

        display.setLine(1, 
            new LineItem("VALUE", valueDisplay, DisplayColor.WHITE),
            new LineItem(isSpeed ? "MODE" : "", isSpeed ? "<" + speedMode + ">" : "", DisplayColor.CYAN)
        );

        display.setLine(2, 
            new LineItem("DUE TO", dueTo.isEmpty() ? "<NONE>" : dueTo, DisplayColor.WHITE_DIM),
            new LineItem("", "", DisplayColor.WHITE)
        );

        display.setLine(3, 
            new LineItem("", "", DisplayColor.WHITE),
            new LineItem("", "SEND REQ>", DisplayColor.GREEN)
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
                case 0: // LSK 1L: REQ TYPE
                    if ("DIRECT".equals(reqType)) reqType = "ALTITUDE";
                    else if ("ALTITUDE".equals(reqType)) reqType = "SPEED";
                    else if ("SPEED".equals(reqType)) reqType = "EXPECT";
                    else reqType = "DIRECT";
                    value = "";
                    break;
                case 1: // LSK 2L: VALUE
                    if (validateAndSetValue(input, controller)) {
                        controller.clearScratchpad();
                    }
                    break;
                case 2: // LSK 3L: DUE TO
                    if (!input.isEmpty()) {
                        dueTo = input;
                        controller.clearScratchpad();
                    } else {
                        if (dueTo.isEmpty()) dueTo = "WEATHER";
                        else if ("WEATHER".equals(dueTo)) dueTo = "PERFORMANCE";
                        else dueTo = "";
                    }
                    break;
                case 5: // LSK 6L: <MENU
                    controller.showPage(new MainMenuPage());
                    break;
            }
        } else {
            switch (index) {
                case 0: // LSK 1R: EXPECT SUBTYPE (LEVEL / SPEED / DIRECT)
                    if ("EXPECT".equals(reqType)) {
                        if ("LEVEL".equals(expectSubtype)) expectSubtype = "SPEED";
                        else if ("SPEED".equals(expectSubtype)) expectSubtype = "DIRECT";
                        else expectSubtype = "LEVEL";
                        value = "";
                    }
                    break;
                case 1: // LSK 2R: SPEED MODE (IAS / MACH)
                    boolean isSpeed = "SPEED".equals(reqType) || ("EXPECT".equals(reqType) && "SPEED".equals(expectSubtype));
                    if (isSpeed) {
                        speedMode = "IAS".equals(speedMode) ? "MACH" : "IAS";
                        value = ""; // reset value on mode toggle
                    }
                    break;
                case 3: // LSK 4R: SEND REQ>
                    if (controller.getService() != null && !value.isEmpty()) {
                        if (!controller.getService().isLoggedOn() || controller.getService().getCurrentATS() == null || controller.getService().getCurrentATS().isEmpty()) {
                            controller.setStatusMessage("NOT LOGGED ON");
                            break;
                        }

                        if ("DIRECT".equals(reqType)) {
                            controller.getService().sendDirectRequest(value, dueTo);
                        } else if ("ALTITUDE".equals(reqType)) {
                            controller.getService().sendLevelRequest(value, dueTo);
                        } else if ("SPEED".equals(reqType)) {
                            controller.getService().sendSpeedRequest(speedMode, value, dueTo);
                        } else if ("EXPECT".equals(reqType)) {
                            String expTypeStr = "LEVEL".equals(expectSubtype) ? "LEVEL" :
                                               ("SPEED".equals(expectSubtype) ? ("MACH".equals(speedMode) ? "SPEED M." + value : "SPEED IAS " + value) : "DIRECT TO");
                            String expValStr = "SPEED".equals(expectSubtype) ? "" : value;
                            controller.getService().sendWhenCanWeExpectRequest(expTypeStr, expValStr, dueTo);
                        }

                        String sentDisplay = ("SPEED".equals(reqType) || ("EXPECT".equals(reqType) && "SPEED".equals(expectSubtype))) && "MACH".equals(speedMode) ? "." + value : value;
                        controller.setStatusMessage("REQ SENT: " + reqType + " " + sentDisplay);
                        value = "";
                    } else if (value.isEmpty()) {
                        controller.setStatusMessage("ENTER VALUE");
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

    private boolean validateAndSetValue(String input, CduController controller) {
        if (input == null || input.isEmpty()) {
            controller.setStatusMessage("INVALID ENTRY");
            return false;
        }

        String targetType = "EXPECT".equals(reqType) ? expectSubtype : reqType;

        if ("ALTITUDE".equals(targetType) || "LEVEL".equals(targetType)) {
            if (input.matches("^FL\\d{3}$")) {
                int fl = Integer.parseInt(input.substring(2));
                if (fl >= 10 && fl <= 600) {
                    this.value = input;
                    return true;
                }
            } else if (input.matches("^\\d{3}$")) {
                int fl = Integer.parseInt(input);
                if (fl >= 10 && fl <= 600) {
                    this.value = "FL" + String.format("%03d", fl);
                    return true;
                }
            } else if (input.matches("^\\d{1,2}$")) {
                int fl = Integer.parseInt(input);
                if (fl >= 1 && fl <= 60) {
                    this.value = "FL" + String.format("%03d", fl);
                    return true;
                }
            } else if (input.matches("^\\d{4,5}$")) {
                int alt = Integer.parseInt(input);
                if (alt >= 1000 && alt <= 60000) {
                    this.value = String.valueOf(alt);
                    return true;
                }
            }
        } else if ("SPEED".equals(targetType)) {
            if ("IAS".equals(speedMode)) {
                if (input.matches("^\\d{2,3}$")) {
                    int spd = Integer.parseInt(input);
                    if (spd >= 50 && spd <= 450) {
                        this.value = String.valueOf(spd);
                        return true;
                    }
                }
            } else if ("MACH".equals(speedMode)) {
                String mDigits = input.replaceAll("[^0-9]", "");
                if (mDigits.length() == 2 || mDigits.length() == 3) {
                    if (mDigits.startsWith("0") && mDigits.length() == 3) {
                        mDigits = mDigits.substring(1);
                    }
                    if (mDigits.length() == 2) {
                        int mVal = Integer.parseInt(mDigits);
                        if (mVal >= 50 && mVal <= 99) {
                            this.value = mDigits;
                            return true;
                        }
                    }
                }
            }
        } else if ("DIRECT".equals(targetType)) {
            if (input.matches("^[A-Z0-9]{2,7}$")) {
                this.value = input;
                return true;
            }
        }

        controller.setStatusMessage("INVALID ENTRY");
        return false;
    }
}
