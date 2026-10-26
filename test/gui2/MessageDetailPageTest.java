package gui2;

import gui2.components.CduDisplay;
import gui2.controller.CduController;
import gui2.pages.MainMenuPage;
import gui2.pages.MessageDetailPage;
import hoppie.AcarsMessage;
import hoppie.CpdlcMessage;
import org.junit.Before;
import org.junit.Test;
import service.CpdlcService;

import static org.junit.Assert.*;

/**
 * Tests pagination and navigation behavior of MessageDetailPage in gui2:
 * - Single-page messages: LSK 6L is <BACK, LSK 6R is empty.
 * - Multi-page messages:
 *   - Page 1: LSK 6L is <BACK, LSK 6R is NEXT>.
 *   - Pages 2..N-1: LSK 6L is <PREV, LSK 6R is NEXT>.
 *   - Last page (N): LSK 6L is <PREV, LSK 6R is BACK>.
 * - Reply prompts (Lines 3 & 4) remain completely intact on all pages.
 */
public class MessageDetailPageTest {

    private CduDisplay display;
    private CduController controller;
    private CpdlcService service;

    @Before
    public void setUp() {
        display = new CduDisplay();
        controller = new CduController(display);
        service = new CpdlcService("KLM123", "test_logon");
        controller.setService(service);
    }

    @Test
    public void testSinglePageMessage() {
        AcarsMessage msg = new AcarsMessage("ATC", "telex", "KLM123", "SHORT MSG", false);
        MessageDetailPage page = new MessageDetailPage(msg);
        controller.pushPage(page);

        assertEquals("<BACK", display.getLeftLine(5).value);
        assertEquals("", display.getRightLine(5).value);

        // Clicking LSK 6L pops page back
        page.onLskPressed(5, true, "", controller);
        assertFalse(controller.hasPreviousPage());
    }

    @Test
    public void testTwoPageMessagePagination() {
        // With CPDLC reply prompt, linesPerPage = 3. A 5-line message spans 2 pages.
        String body = "/data2/1//WU/LINE ONE@LINE TWO@LINE THREE@LINE FOUR@LINE FIVE";
        CpdlcMessage msg = new CpdlcMessage("EDGG_CTR", "cpdlc", "KLM123", body);

        MessageDetailPage page = new MessageDetailPage(msg);
        controller.pushPage(page);

        // Page 1 of 2
        assertEquals(0, page.getPageOffset());
        assertEquals("<BACK", display.getLeftLine(5).value);
        assertEquals("NEXT>", display.getRightLine(5).value);
        assertEquals("<WILCO", display.getLeftLine(3).value);
        assertEquals("UNABLE>", display.getRightLine(3).value);
        assertEquals("<STANDBY", display.getLeftLine(4).value);
        assertEquals("FREE TEXT>", display.getRightLine(4).value);

        // Click NEXT (LSK 6R)
        page.onLskPressed(5, false, "", controller);
        controller.refreshDisplay();

        // Page 2 of 2
        assertEquals(3, page.getPageOffset());
        assertEquals("<PREV", display.getLeftLine(5).value);
        assertEquals("BACK>", display.getRightLine(5).value);
        // Reply prompts still intact on Page 2
        assertEquals("<WILCO", display.getLeftLine(3).value);
        assertEquals("UNABLE>", display.getRightLine(3).value);
        assertEquals("<STANDBY", display.getLeftLine(4).value);
        assertEquals("FREE TEXT>", display.getRightLine(4).value);

        // Click PREV (LSK 6L) -> returns to Page 1
        page.onLskPressed(5, true, "", controller);
        controller.refreshDisplay();

        assertEquals(0, page.getPageOffset());
        assertEquals("<BACK", display.getLeftLine(5).value);
        assertEquals("NEXT>", display.getRightLine(5).value);

        // Advance to Page 2 again and click BACK> (LSK 6R) -> pops page
        page.onLskPressed(5, false, "", controller);
        controller.refreshDisplay();
        assertEquals(3, page.getPageOffset());

        page.onLskPressed(5, false, "", controller);
        // Page should now be popped
        assertFalse(controller.hasPreviousPage());
    }

    @Test
    public void testFourPagePdcClearancePagination() {
        // 11 lines with reply prompts (linesPerPage = 3) -> 4 pages
        StringBuilder sb = new StringBuilder("/data2/1//WU/");
        for (int i = 1; i <= 11; i++) {
            if (i > 1) sb.append("@");
            sb.append("PDC LINE ").append(i);
        }
        CpdlcMessage msg = new CpdlcMessage("EHAM_DEL", "cpdlc", "KLM123", sb.toString());

        MessageDetailPage page = new MessageDetailPage(msg);
        controller.pushPage(page);

        // Page 1/4
        assertEquals(0, page.getPageOffset());
        assertEquals("<BACK", display.getLeftLine(5).value);
        assertEquals("NEXT>", display.getRightLine(5).value);

        // Advance to Page 2/4
        page.onLskPressed(5, false, "", controller);
        controller.refreshDisplay();
        assertEquals(3, page.getPageOffset());
        assertEquals("<PREV", display.getLeftLine(5).value);
        assertEquals("NEXT>", display.getRightLine(5).value);

        // Advance to Page 3/4
        page.onLskPressed(5, false, "", controller);
        controller.refreshDisplay();
        assertEquals(6, page.getPageOffset());
        assertEquals("<PREV", display.getLeftLine(5).value);
        assertEquals("NEXT>", display.getRightLine(5).value);

        // Step back to Page 2/4 via LSK 6L (<PREV)
        page.onLskPressed(5, true, "", controller);
        controller.refreshDisplay();
        assertEquals(3, page.getPageOffset());
        assertEquals("<PREV", display.getLeftLine(5).value);
        assertEquals("NEXT>", display.getRightLine(5).value);

        // Step forward to Page 3/4
        page.onLskPressed(5, false, "", controller);
        controller.refreshDisplay();
        assertEquals(6, page.getPageOffset());

        // Advance to Page 4/4 (last page)
        page.onLskPressed(5, false, "", controller);
        controller.refreshDisplay();
        assertEquals(9, page.getPageOffset());
        assertEquals("<PREV", display.getLeftLine(5).value);
        assertEquals("BACK>", display.getRightLine(5).value);

        // Click BACK> (LSK 6R) on last page to exit
        page.onLskPressed(5, false, "", controller);
        assertFalse(controller.hasPreviousPage());
    }
}
