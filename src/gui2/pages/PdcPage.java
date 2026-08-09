package gui2.pages;

import flight.Flight;
import gui2.components.CduDisplay;
import gui2.components.CduDisplay.DisplayColor;
import gui2.components.CduDisplay.LineItem;
import gui2.controller.CduController;
import service.CpdlcService;
import service.UserPreferences;

/**
 * Pre-Departure Clearance (PDC) Request Page with independent Station entry and Simbrief fetch support.
 */
public class PdcPage implements CduPage {

    private String targetStation = "";
    private String simbriefID = UserPreferences.getLastSimbriefID();
    private String origin = "";
    private String dest = "";
    private String acftType = "";
    private String stand = "";
    private String atis = "";

    @Override
    public String getPageTitle() {
        return "PDC CLEARANCE REQUEST";
    }

    @Override
    public void renderPage(CduDisplay display, CduController controller) {
        display.setHeader("PDC CLEARANCE REQ", "", "");
        display.clearLines();

        display.setLine(0, 
            new LineItem("STATION", targetStation.isEmpty() ? "----" : targetStation, DisplayColor.WHITE),
            new LineItem("SIMBRIEF ID", simbriefID.isEmpty() ? "----" : simbriefID, DisplayColor.WHITE_DIM)
        );

        display.setLine(1, 
            new LineItem("DEP", origin.isEmpty() ? "----" : origin, DisplayColor.WHITE),
            new LineItem("DEST", dest.isEmpty() ? "----" : dest, DisplayColor.WHITE)
        );

        display.setLine(2, 
            new LineItem("ACFT", acftType.isEmpty() ? "----" : acftType, DisplayColor.WHITE),
            new LineItem("STAND", stand.isEmpty() ? "----" : stand, DisplayColor.WHITE)
        );

        display.setLine(3, 
            new LineItem("ATIS", atis.isEmpty() ? "-" : atis, DisplayColor.WHITE),
            new LineItem("", "<FETCH SIMBRIEF", DisplayColor.CYAN)
        );

        display.setLine(4, 
            new LineItem("", "", DisplayColor.WHITE),
            new LineItem("", "SEND PDC>", DisplayColor.GREEN)
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
                case 0: // LSK 1L: STATION
                    if (!input.isEmpty()) {
                        targetStation = input;
                        controller.clearScratchpad();
                    }
                    break;
                case 1: // LSK 2L: DEP
                    if (!input.isEmpty()) { 
                        origin = input; 
                        if (targetStation.isEmpty()) targetStation = origin;
                        controller.clearScratchpad(); 
                    }
                    break;
                case 2: // LSK 3L: ACFT
                    if (!input.isEmpty()) { acftType = input; controller.clearScratchpad(); }
                    break;
                case 3: // LSK 4L: ATIS
                    if (!input.isEmpty()) { atis = input; controller.clearScratchpad(); }
                    break;
                case 5: // LSK 6L: <MENU
                    controller.showPage(new MainMenuPage());
                    break;
            }
        } else {
            switch (index) {
                case 0: // LSK 1R: SIMBRIEF ID
                    if (!input.isEmpty()) {
                        simbriefID = input;
                        UserPreferences.setLastSimbriefID(simbriefID);
                        controller.clearScratchpad();
                    }
                    break;
                case 1: // LSK 2R: DEST
                    if (!input.isEmpty()) { dest = input; controller.clearScratchpad(); }
                    break;
                case 2: // LSK 3R: STAND
                    if (!input.isEmpty()) { stand = input; controller.clearScratchpad(); }
                    break;
                case 3: // LSK 4R: <FETCH SIMBRIEF
                    if (controller.getService() != null && !simbriefID.isEmpty()) {
                        controller.setStatusMessage("FETCHING SIMBRIEF...");
                        controller.getService().fetchSimbriefData(simbriefID, new CpdlcService.SimbriefCallback() {
                            @Override
                            public void onSuccess(Flight flight) {
                                if (flight != null) {
                                    origin = flight.getOrigin() != null ? flight.getOrigin() : "";
                                    dest = flight.getDestination() != null ? flight.getDestination() : "";
                                    acftType = flight.getAircraft() != null ? flight.getAircraft() : "";
                                    if (targetStation.isEmpty()) targetStation = origin;
                                    controller.setStatusMessage("SIMBRIEF LOADED");
                                }
                            }

                            @Override
                            public void onFailure(Exception e) {
                                controller.setStatusMessage("SIMBRIEF ERROR");
                            }
                        });
                    } else if (simbriefID.isEmpty()) {
                        controller.setStatusMessage("ENTER SIMBRIEF ID");
                    }
                    break;
                case 4: // LSK 5R: SEND PDC>
                    if (controller.getService() != null) {
                        String destinationStation = !targetStation.isEmpty() ? targetStation : origin;
                        if (destinationStation.isEmpty()) {
                            controller.setStatusMessage("ENTER STATION");
                            return;
                        }
                        if (origin.isEmpty() || dest.isEmpty() || acftType.isEmpty()) {
                            controller.setStatusMessage("ENTER DEP/DEST/ACFT");
                            return;
                        }

                        Flight f = new Flight(controller.getCallsign(), origin, dest, acftType);
                        controller.getService().sendPdcRequest(destinationStation, f, stand, atis, "");
                        controller.setStatusMessage("TELEX TO " + destinationStation);
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
}
