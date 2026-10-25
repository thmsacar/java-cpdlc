package gui2;

import gui2.components.CduDisplay;
import gui2.controller.CduController;
import gui2.pages.MainMenuPage;
import gui2.pages.MessageDetailPage;
import gui2.pages.MessageListPage;
import hoppie.AcarsMessage;
import org.junit.Before;
import org.junit.Test;
import service.CpdlcService;

import static org.junit.Assert.*;

/**
 * Tests pagination behavior of MessageListPage in gui2:
 * PREV button appears on all pages except the first one (on top of NEXT at LSK 5R).
 * NEXT button appears on all pages except the last one (at LSK 6R).
 */
public class MessageListPageTest {

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
    public void testEmptyMessagesShowsNoPrevAndNoNext() {
        MessageListPage page = new MessageListPage();
        controller.showPage(page);

        assertEquals("", display.getRightLine(4).value);
        assertEquals("", display.getRightLine(5).value);
        assertEquals("<MENU", display.getLeftLine(5).value);
    }

    @Test
    public void testSinglePageShowsNoPrevAndNoNext() {
        for (int i = 1; i <= 3; i++) {
            service.addMessage(new AcarsMessage("ATC", "telex", "KLM123", "MSG " + i, false));
        }

        MessageListPage page = new MessageListPage();
        controller.showPage(page);

        assertEquals("", display.getRightLine(4).value);
        assertEquals("", display.getRightLine(5).value);

        // Clicking LSK 5R (PREV) or LSK 6R (NEXT) has no effect
        page.onLskPressed(4, false, "", controller);
        assertEquals(0, page.getPageOffset());

        page.onLskPressed(5, false, "", controller);
        assertEquals(0, page.getPageOffset());
    }

    @Test
    public void testTwoPagesNextOnFirstPageAndPrevOnLastPage() {
        for (int i = 1; i <= 6; i++) {
            service.addMessage(new AcarsMessage("ATC", "telex", "KLM123", "MSG " + i, false));
        }

        MessageListPage page = new MessageListPage();
        controller.showPage(page);

        // Page 1: PREV absent on line 4, NEXT present on line 5
        assertEquals(0, page.getPageOffset());
        assertEquals("", display.getRightLine(4).value);
        assertEquals("NEXT>", display.getRightLine(5).value);

        // Clicking PREV (LSK 5R) on first page does nothing
        page.onLskPressed(4, false, "", controller);
        assertEquals(0, page.getPageOffset());

        // Clicking NEXT (LSK 6R) navigates to page 2
        page.onLskPressed(5, false, "", controller);
        controller.refreshDisplay();

        assertEquals(4, page.getPageOffset());
        // Page 2: PREV present on line 4 (on top of NEXT), NEXT absent on line 5
        assertEquals("<PREV", display.getRightLine(4).value);
        assertEquals("", display.getRightLine(5).value);

        // Clicking NEXT (LSK 6R) on last page does nothing (does not wrap)
        page.onLskPressed(5, false, "", controller);
        assertEquals(4, page.getPageOffset());

        // Clicking PREV (LSK 5R) navigates back to page 1
        page.onLskPressed(4, false, "", controller);
        controller.refreshDisplay();

        assertEquals(0, page.getPageOffset());
        assertEquals("", display.getRightLine(4).value);
        assertEquals("NEXT>", display.getRightLine(5).value);
    }

    @Test
    public void testThreePagesHasBothPrevAndNextOnMiddlePage() {
        for (int i = 1; i <= 10; i++) {
            service.addMessage(new AcarsMessage("ATC", "telex", "KLM123", "MSG " + i, false));
        }

        MessageListPage page = new MessageListPage();
        controller.showPage(page);

        // Page 1: No PREV, has NEXT
        assertEquals(0, page.getPageOffset());
        assertEquals("", display.getRightLine(4).value);
        assertEquals("NEXT>", display.getRightLine(5).value);

        // Move to Page 2
        page.onLskPressed(5, false, "", controller);
        controller.refreshDisplay();

        assertEquals(4, page.getPageOffset());
        // Page 2: Has PREV on line 4 (LSK 5R) on top of NEXT on line 5 (LSK 6R)
        assertEquals("<PREV", display.getRightLine(4).value);
        assertEquals("NEXT>", display.getRightLine(5).value);

        // Move to Page 3
        page.onLskPressed(5, false, "", controller);
        controller.refreshDisplay();

        assertEquals(8, page.getPageOffset());
        // Page 3: Has PREV on line 4, no NEXT on line 5
        assertEquals("<PREV", display.getRightLine(4).value);
        assertEquals("", display.getRightLine(5).value);

        // Move back to Page 2
        page.onLskPressed(4, false, "", controller);
        controller.refreshDisplay();

        assertEquals(4, page.getPageOffset());
        assertEquals("<PREV", display.getRightLine(4).value);
        assertEquals("NEXT>", display.getRightLine(5).value);
    }

    @Test
    public void testMenuNavigationOnLsk6L() {
        MessageListPage page = new MessageListPage();
        controller.showPage(page);

        // LSK 6L should navigate to MainMenuPage
        page.onLskPressed(5, true, "", controller);
        // Display should now reflect MainMenuPage header
        assertEquals("<ATC LOGON/STATUS", display.getLeftLine(0).value);
    }
}
