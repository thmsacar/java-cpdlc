package gui2;

import gui2.components.CduDisplay;
import gui2.controller.CduController;
import gui2.pages.CduLoginPage;
import gui2.pages.MainMenuPage;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests scratchpad text entry behavior for uppercase conversion and login page exception.
 */
public class CduControllerScratchpadTest {

    private CduController controller;

    @Before
    public void setUp() {
        CduDisplay display = new CduDisplay();
        controller = new CduController(display);
    }

    @Test
    public void testLowerCaseAllowedOnLoginPage() {
        controller.showPage(new CduLoginPage());
        controller.handleKeyTyped("a");
        controller.handleKeyTyped("B");
        controller.handleKeyTyped("c");

        assertEquals("aBc", controller.getScratchpad());

        controller.handlePaste("dEf12");
        assertEquals("aBcdEf12", controller.getScratchpad());
    }

    @Test
    public void testLowerCaseConvertedToUppercaseOnOtherPages() {
        controller.showPage(new MainMenuPage());
        controller.clearScratchpad();

        controller.handleKeyTyped("a");
        controller.handleKeyTyped("B");
        controller.handleKeyTyped("c");

        assertEquals("ABC", controller.getScratchpad());

        controller.handlePaste("dEf12");
        assertEquals("ABCDEF12", controller.getScratchpad());
    }

    @Test
    public void testSwitchingFromLoginPageToOtherPageConvertsScratchpadToUppercase() {
        controller.showPage(new CduLoginPage());
        controller.handleKeyTyped("a");
        controller.handleKeyTyped("b");
        assertEquals("ab", controller.getScratchpad());

        controller.showPage(new MainMenuPage());
        assertEquals("AB", controller.getScratchpad());
    }

    @Test
    public void testNonAsciiSpecialCharactersIgnoredExceptOnLoginPage() {
        // Non-login page: non-ASCII characters (ä, ö, ü, é, ç, ñ, ğ) should be ignored, ASCII symbols (@, /, -, #) accepted
        controller.showPage(new MainMenuPage());
        controller.clearScratchpad();

        controller.handleKeyTyped("h");
        controller.handleKeyTyped("ä");
        controller.handleKeyTyped("l");
        controller.handleKeyTyped("l");
        controller.handleKeyTyped("ö");
        controller.handleKeyTyped("@");
        controller.handleKeyTyped("/");

        assertEquals("HLL@/", controller.getScratchpad());

        controller.handlePaste("Tëst-123_ç");
        assertEquals("HLL@/TST-123_", controller.getScratchpad());

        // Login page: non-ASCII characters ignored, but lowercase ASCII allowed
        controller.showPage(new CduLoginPage());
        controller.clearScratchpad();
        controller.handleKeyTyped("h");
        controller.handleKeyTyped("ä");
        controller.handleKeyTyped("e");
        controller.handleKeyTyped("l");
        controller.handleKeyTyped("l");
        controller.handleKeyTyped("ö");
        controller.handleKeyTyped("1");
        controller.handleKeyTyped("@");
        assertEquals("hell1@", controller.getScratchpad());
    }
}
