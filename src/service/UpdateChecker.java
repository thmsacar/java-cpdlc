package service;

import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility for checking newer application versions via GitHub Releases.
 */
public class UpdateChecker {

    /** Current application version string. */
    public static final String CURRENT_VERSION = "1.3.0";
    private static final String GITHUB_RELEASES_API = "https://api.github.com/repos/thmsacar/java-cpdlc/releases/latest";

    /**
     * Asynchronously checks GitHub for updates and displays a dialog if a newer version is available.
     */
    public static void checkForUpdatesAsync(Component parent) {
        Thread thread = new Thread(() -> {
            try {
                ReleaseInfo latestRelease = fetchLatestRelease();
                if (latestRelease != null && isNewerVersion(CURRENT_VERSION, latestRelease.version)) {
                    SwingUtilities.invokeLater(() -> gui.UpdateDialog.showUpdateDialog(parent, CURRENT_VERSION, latestRelease));
                }
            } catch (Exception e) {
                System.err.println("Update check failed: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Fetches the latest release details from GitHub Releases API.
     */
    public static ReleaseInfo fetchLatestRelease() throws Exception {
        URL url = new URL(GITHUB_RELEASES_API);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent", "java-cpdlc");
        conn.setRequestProperty("Accept", "application/vnd.github+json");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            reader.close();

            JSONObject json = new JSONObject(builder.toString());
            String tagName = json.optString("tag_name", "");
            String htmlUrl = json.optString("html_url", "https://github.com/thmsacar/java-cpdlc/releases/latest");
            String body = json.optString("body", "");
            String cleanVersion = cleanVersionString(tagName);

            return new ReleaseInfo(cleanVersion, tagName, htmlUrl, body);
        }
        return null;
    }

    /**
     * Strips leading 'v' or 'V' prefixes from version strings.
     */
    public static String cleanVersionString(String rawVersion) {
        if (rawVersion == null) return "";
        rawVersion = rawVersion.trim();
        if (rawVersion.startsWith("v") || rawVersion.startsWith("V")) {
            rawVersion = rawVersion.substring(1);
        }
        return rawVersion;
    }

    /**
     * Compares version strings to determine if remote is newer than current.
     */
    public static boolean isNewerVersion(String current, String remote) {
        if (remote == null || remote.trim().isEmpty()) return false;

        String cleanCurrent = cleanVersionString(current);
        String cleanRemote = cleanVersionString(remote);

        String[] currentParts = cleanCurrent.split("[^0-9]+");
        String[] remoteParts = cleanRemote.split("[^0-9]+");

        int length = Math.max(currentParts.length, remoteParts.length);
        for (int i = 0; i < length; i++) {
            int currentNum = (i < currentParts.length && !currentParts[i].isEmpty()) ? Integer.parseInt(currentParts[i]) : 0;
            int remoteNum = (i < remoteParts.length && !remoteParts[i].isEmpty()) ? Integer.parseInt(remoteParts[i]) : 0;

            if (remoteNum > currentNum) {
                return true;
            } else if (remoteNum < currentNum) {
                return false;
            }
        }
        return false;
    }

    /**
     * Container for GitHub release details.
     */
    public static class ReleaseInfo {
        public final String version;
        public final String tagName;
        public final String htmlUrl;
        public final String releaseNotes;

        public ReleaseInfo(String version, String tagName, String htmlUrl, String releaseNotes) {
            this.version = version;
            this.tagName = tagName;
            this.htmlUrl = htmlUrl;
            this.releaseNotes = releaseNotes;
        }
    }
}
