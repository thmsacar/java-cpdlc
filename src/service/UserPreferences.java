package service;

import java.util.prefs.Preferences;

public class UserPreferences {

    private static final Preferences prefs = Preferences.userNodeForPackage(UserPreferences.class);

    private static final String KEY_CALLSIGN = "lastCallsign";
    private static final String KEY_HOPPIE_ID = "lastHoppieID";
    private static final String KEY_SIMBRIEF_ID = "lastSimbriefID";

    public static String getLastCallsign() {
        return prefs.get(KEY_CALLSIGN, "");
    }

    public static void setLastCallsign(String callsign) {
        prefs.put(KEY_CALLSIGN, callsign);
    }

    public static String getLastHoppieID() {
        return prefs.get(KEY_HOPPIE_ID, "");
    }

    public static void setLastHoppieID(String hoppieID) {
        prefs.put(KEY_HOPPIE_ID, hoppieID);
    }

    public static String getLastSimbriefID() {
        return prefs.get(KEY_SIMBRIEF_ID, "");
    }

    public static void setLastSimbriefID(String simbriefID) {
        prefs.put(KEY_SIMBRIEF_ID, simbriefID);
    }
}
