package service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.prefs.Preferences;

import static org.junit.Assert.*;

public class UserPreferencesTest {

    private Preferences testPrefs;

    @Before
    public void setUp() {
        // Use an isolated test node so developer's real preferences are never modified
        testPrefs = Preferences.userNodeForPackage(UserPreferencesTest.class);
        UserPreferences.setPreferencesNode(testPrefs);
    }

    @After
    public void tearDown() throws Exception {
        if (testPrefs != null) {
            testPrefs.removeNode(); // Delete temporary test preferences node
        }
        UserPreferences.setPreferencesNode(null); // Reset back to default production node
    }

    @Test
    public void testUserPreferencesGetSet() {
        String testCallsign = "THY123";
        String testHoppie = "HOP123456";
        String testSimbrief = "987654";

        UserPreferences.setLastCallsign(testCallsign);
        UserPreferences.setLastHoppieID(testHoppie);
        UserPreferences.setLastSimbriefID(testSimbrief);

        assertEquals(testCallsign, UserPreferences.getLastCallsign());
        assertEquals(testHoppie, UserPreferences.getLastHoppieID());
        assertEquals(testSimbrief, UserPreferences.getLastSimbriefID());
    }
}
