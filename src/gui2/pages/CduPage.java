package gui2.pages;

import gui2.components.CduDisplay;
import gui2.controller.CduController;

/**
 * Contract for a CDU page displaying lines and handling LSK key actions.
 */
public interface CduPage {

    /**
     * Gets the page title.
     */
    String getPageTitle();

    /**
     * Updates the CduDisplay with the lines and header of this page.
     */
    void renderPage(CduDisplay display, CduController controller);

    /**
     * Handles Line Select Key (LSK) presses on this page.
     * @param index 0 to 5 (LSK 1 to LSK 6)
     * @param isLeft true for LSK_L, false for LSK_R
     * @param scratchpad current scratchpad string typed by pilot
     * @param controller reference to navigation/service controller
     */
    void onLskPressed(int index, boolean isLeft, String scratchpad, CduController controller);
}
