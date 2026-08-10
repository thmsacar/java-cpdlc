package service;

import org.junit.Test;
import static org.junit.Assert.*;

/** Tests semantic version comparison and GitHub API release checking in UpdateChecker. */
public class UpdateCheckerTest {

    /** Verifies cleaning of prefix characters from version tags. */
    @Test
    public void testCleanVersionString() {
        assertEquals("1.1.2", UpdateChecker.cleanVersionString("v1.1.2"));
        assertEquals("1.1.2", UpdateChecker.cleanVersionString("V1.1.2"));
        assertEquals("1.1.2", UpdateChecker.cleanVersionString("1.1.2"));
        assertEquals("", UpdateChecker.cleanVersionString(null));
    }

    /** Verifies semantic versioning comparison logic. */
    @Test
    public void testIsNewerVersion() {
        // Newer version cases
        assertTrue(UpdateChecker.isNewerVersion("1.1.2", "1.1.3"));
        assertTrue(UpdateChecker.isNewerVersion("1.1.2", "v1.2.0"));
        assertTrue(UpdateChecker.isNewerVersion("1.1.2", "2.0.0"));
        assertTrue(UpdateChecker.isNewerVersion("1.1.2", "1.1.2.1"));

        // Equal or older version cases
        assertFalse(UpdateChecker.isNewerVersion("1.1.2", "1.1.2"));
        assertFalse(UpdateChecker.isNewerVersion("1.1.2", "v1.1.2"));
        assertFalse(UpdateChecker.isNewerVersion("1.1.2", "1.1.1"));
        assertFalse(UpdateChecker.isNewerVersion("1.1.2", "1.0.9"));
        assertFalse(UpdateChecker.isNewerVersion("1.1.2", null));
        assertFalse(UpdateChecker.isNewerVersion("1.1.2", ""));
    }

    /** Verifies online fetching of the latest release from the GitHub Releases API. */
    @Test
    public void testFetchLatestRelease() {
        try {
            UpdateChecker.ReleaseInfo info = UpdateChecker.fetchLatestRelease();
            assertNotNull("GitHub API should return release info if connected to internet", info);
            assertNotNull(info.version);
            assertNotNull(info.htmlUrl);
            System.out.println("Fetched latest release version: " + info.version + " (" + info.htmlUrl + ")");
        } catch (Exception e) {
            System.out.println("Network offline or GitHub API rate limited: " + e.getMessage());
        }
    }
}
