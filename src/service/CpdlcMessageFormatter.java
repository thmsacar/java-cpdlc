package service;

/**
 * Service utility class responsible for formatting CPDLC requests and reports.
 * Keeps message formatting rules completely decoupled from UI components.
 */
public class
CpdlcMessageFormatter {

    private CpdlcMessageFormatter() {
        // Utility class
    }

    /**
     * Formats a "DUE TO" clause.
     */
    public static String formatDueTo(String reasonType, String freeText) {
        if (reasonType == null || reasonType.trim().isEmpty()) {
            return "";
        }
        String type = reasonType.trim().toUpperCase();
        if ("FREE TEXT".equals(type) || "FREETEXT".equals(type)) {
            if (freeText == null || freeText.trim().isEmpty()) return "";
            type = freeText.trim().toUpperCase();
        }
        if (type.isEmpty()) return "";
        if (type.startsWith("DUE TO ")) {
            return type;
        }
        return "DUE TO " + type;
    }

    /**
     * Formats a "REQUEST DIRECT TO" message.
     */
    public static String formatDirectRequest(String waypoint, String dueToText) {
        if (waypoint == null || waypoint.trim().isEmpty()) {
            return "";
        }
        String msg = "REQUEST DIRECT TO " + waypoint.trim();
        String due = formatDueTo(dueToText, "");
        if (!due.isEmpty()) {
            msg += " " + due;
        }
        return msg;
    }

    /**
     * Formats a "REQUEST LEVEL" message.
     */
    public static String formatLevelRequest(String level, String dueToText) {
        if (level == null || level.trim().isEmpty()) {
            return "";
        }
        String msg = "REQUEST LEVEL " + level.trim();
        String due = formatDueTo(dueToText, "");
        if (!due.isEmpty()) {
            msg += " " + due;
        }
        return msg;
    }

    /**
     * Formats a "REQUEST SPEED" message.
     * @param speedType "ias" or "mach"
     */
    public static String formatSpeedRequest(String speedType, String speedValue, String dueToText) {
        if (speedValue == null || speedValue.trim().isEmpty()) {
            return "";
        }
        String prefix;
        if (speedType != null && speedType.trim().equalsIgnoreCase("mach")) {
            prefix = "REQUEST SPEED M.";
        } else {
            prefix = "REQUEST SPEED IAS ";
        }
        String msg = prefix + speedValue.trim();
        String due = formatDueTo(dueToText, "");
        if (!due.isEmpty()) {
            msg += " " + due;
        }
        return msg;
    }

    /**
     * Formats a "WHEN CAN WE EXPECT" message.
     */
    public static String formatWhenCanWeExpectRequest(String expectType, String value, String dueToText) {
        if (value == null || value.trim().isEmpty() || expectType == null || expectType.trim().isEmpty()) {
            return "";
        }
        String msg = "WHEN CAN WE EXPECT " + expectType.trim() + " " + value.trim();
        String due = formatDueTo(dueToText, "");
        if (!due.isEmpty()) {
            msg += " " + due;
        }
        return msg;
    }

    /**
     * Formats a Level Report.
     */
    public static String formatLevelReport(String status, String level) {
        if (level == null || level.trim().isEmpty()) {
            return null;
        }
        String st = (status != null && !status.trim().isEmpty()) ? status.trim() : "MAINTAINING";
        return st + " LEVEL " + level.trim();
    }

    /**
     * Formats a Speed Report.
     */
    public static String formatSpeedReport(boolean isMach, String speedValue) {
        if (speedValue == null || speedValue.trim().isEmpty()) {
            return null;
        }
        String type = isMach ? "M." : "IAS ";
        return "PRESENT SPEED " + type + speedValue.trim();
    }

    /**
     * Formats a Position Report.
     */
    public static String formatPositionReport(String pos, String time, String level, 
                                              String thereafter, String nextPos, String etaNext) {
        if (pos == null || pos.trim().isEmpty() || 
            time == null || time.trim().isEmpty() || 
            level == null || level.trim().isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("POSITION ").append(pos.trim())
          .append(" AT ").append(time.trim()).append("Z")
          .append(" LEVEL ").append(level.trim());

        if (nextPos != null && !nextPos.trim().isEmpty()) {
            sb.append("@ESTIMATING ").append(nextPos.trim());
            if (etaNext != null && !etaNext.trim().isEmpty()) {
                sb.append(" AT ").append(etaNext.trim());
            }
        }
        if (thereafter != null && !thereafter.trim().isEmpty()) {
            sb.append("@THEREAFTER ").append(thereafter.trim());
        }
        return sb.toString();
    }
}
