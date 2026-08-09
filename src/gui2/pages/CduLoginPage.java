package gui2.pages;

import gui2.components.CduDisplay;
import gui2.components.CduDisplay.DisplayColor;
import gui2.components.CduDisplay.LineItem;
import gui2.controller.CduController;
import service.CpdlcService;
import service.UserPreferences;

/**
 * CDU Login / Connection setup page requiring Callsign and Hoppie ID entry.
 */
public class CduLoginPage implements CduPage {

    private String callsign = UserPreferences.getLastCallsign() != null ? UserPreferences.getLastCallsign() : "";
    private String hoppieID = UserPreferences.getLastHoppieID() != null ? UserPreferences.getLastHoppieID() : "";
    private String simbriefID = UserPreferences.getLastSimbriefID() != null ? UserPreferences.getLastSimbriefID() : "";

    @Override
    public String getPageTitle() {
        return "ACARS / CPDLC LOGIN";
    }

    @Override
    public void renderPage(CduDisplay display, CduController controller) {
        display.setHeader("ACARS / CPDLC LOGIN", "DISCONNECTED", "");
        display.clearLines();

        String safeCallsign = callsign != null ? callsign : "";
        String safeHoppie = hoppieID != null ? hoppieID : "";
        String safeSimbrief = simbriefID != null ? simbriefID : "";

        display.setLine(0, 
            new LineItem("CALLSIGN", safeCallsign.isEmpty() ? "----" : safeCallsign, DisplayColor.WHITE),
            new LineItem("", "", DisplayColor.WHITE)
        );

        display.setLine(1, 
            new LineItem("HOPPIE ID", safeHoppie.isEmpty() ? "----------------" : maskHoppieID(safeHoppie), DisplayColor.WHITE),
            new LineItem("", "", DisplayColor.WHITE)
        );

        display.setLine(2, 
            new LineItem("SIMBRIEF ID", safeSimbrief.isEmpty() ? "----" : safeSimbrief, DisplayColor.WHITE_DIM),
            new LineItem("", "", DisplayColor.WHITE)
        );

        display.setLine(4, 
            new LineItem("", "", DisplayColor.WHITE),
            new LineItem("", "CONNECT>", DisplayColor.GREEN)
        );

        display.setLine(5, 
            new LineItem("", "", DisplayColor.WHITE),
            new LineItem("", "", DisplayColor.WHITE)
        );
    }

    private String maskHoppieID(String id) {
        if (id == null) return "";
        if (id.length() <= 4) return id;
        return id.substring(0, 2) + "****" + id.substring(id.length() - 2);
    }

    @Override
    public void onLskPressed(int index, boolean isLeft, String scratchpad, CduController controller) {
        String input = scratchpad != null ? scratchpad.trim() : "";

        if (isLeft) {
            switch (index) {
                case 0: // LSK 1L: CALLSIGN
                    if (!input.isEmpty()) {
                        callsign = input.toUpperCase();
                        controller.setCallsign(callsign);
                        controller.clearScratchpad();
                    }
                    break;
                case 1: // LSK 2L: HOPPIE ID
                    if (!input.isEmpty()) {
                        hoppieID = input;
                        controller.setHoppieID(hoppieID);
                        controller.clearScratchpad();
                    }
                    break;
                case 2: // LSK 3L: SIMBRIEF ID
                    if (!input.isEmpty()) {
                        simbriefID = input.toUpperCase();
                        controller.setSimbriefID(simbriefID);
                        controller.clearScratchpad();
                    }
                    break;
            }
        } else {
            if (index == 4) { // LSK 5R: CONNECT>
                String targetCallsign = callsign != null ? callsign.trim() : "";
                String targetHoppie = hoppieID != null ? hoppieID.trim() : "";

                if (targetCallsign.isEmpty() || targetHoppie.isEmpty()) {
                    controller.setStatusMessage("ENTER CALLSIGN & HOPPIE ID");
                    return;
                }

                controller.setStatusMessage("VALIDATING CREDENTIALS...");
                new Thread(() -> {
                    try {
                        boolean valid = CpdlcService.validateCredentials(targetCallsign, targetHoppie);
                        if (valid) {
                            javax.swing.SwingUtilities.invokeLater(() -> controller.connect(targetCallsign, targetHoppie));
                        } else {
                            controller.setStatusMessage("INVALID HOPPIE ID");
                        }
                    } catch (Exception e) {
                        controller.setStatusMessage("CONNECTION FAILED: " + e.getMessage());
                    }
                }).start();
            }
        }
    }
}
