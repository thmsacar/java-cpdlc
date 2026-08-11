package hoppie;

import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Model representing an ACARS/CPDLC message.
 */
public class AcarsMessage {

    private final Date timestamp;
    private final String from;
    private final String type;
    private final String to;
    private String message;
    private boolean isRead = false;
    /** Flag indicating if the message was sent by this aircraft client. */
    private boolean isOutgoing = false;
    /** Flag indicating if this system error message was caused by an I/O network failure. */
    private boolean isNetworkError = false;

    public AcarsMessage(String from, String type, String to, String message, boolean isOutgoing) {
        this.timestamp = new Date();
        this.from = from;
        this.type = type;
        this.to = to;
        this.message = message;
        this.isRead = false;
        this.isOutgoing = isOutgoing;
    }

    public AcarsMessage(String from, String type, String to, String message) {
        this(from, type, to, message, false);
    }

    /** Constructs a system message. */
    public AcarsMessage(String type, String message) {
        this("system", type, "system", message, false);
    }

    public boolean isNetworkError() {
        return isNetworkError;
    }

    public void setNetworkError(boolean isNetworkError) {
        this.isNetworkError = isNetworkError;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    /** Checks callsign and sets the outgoing status of the message. */
    public boolean isOutgoing(String callsign) {
        if (callsign != null && from != null && from.equalsIgnoreCase(callsign)) {
            this.isOutgoing = true;
        }
        return this.isOutgoing;
    }

    public void setOutgoing(boolean outgoing) {
        this.isOutgoing = outgoing;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        this.isRead = read;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getFrom() {
        return from;
    }

    public String getType() {
        return type;
    }

    public String getTo() {
        return to;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Formats the message for display in the main message list.
     * @return A map containing header, preview, time, entry, and symbol.
     */
    public HashMap<String, String> getListFormat() {
        String header;
        String symbol;

        if ("system".equalsIgnoreCase(this.getType())) {
            header = "(SYSTEM)>";
            symbol = " ";
        } else if (isOutgoing) {
            header = "(" + (this.getTo() != null ? this.getTo() : "") + ")<";
            symbol = decodeUnicode("\\u2B08");
        } else {
            header = "(" + (this.getFrom() != null ? this.getFrom() : "") + ")>";
            symbol = decodeUnicode("\\u2B0A");
        }

        String rawMsg = this.getMessage() != null ? this.getMessage() : "";
        String cleanMsg = rawMsg.replace('@', ' ').replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        String preview;
        if (cleanMsg.length() > 30) {
            preview = cleanMsg.substring(0, 30) + "...";
        } else {
            preview = cleanMsg;
        }

        String time = TimeFormatter.zuluTime(this.getTimestamp());
        String entry = header + (preview.isEmpty() ? "" : " " + preview);

        HashMap<String, String> map = new HashMap<>();
        map.put("header", header);
        map.put("preview", preview);
        map.put("time", time);
        map.put("entry", entry);
        map.put("symbol", symbol);
        return map;
    }

    public HashMap<String, String> getListFormat(String callsign){
        if (callsign != null && from != null && from.equalsIgnoreCase(callsign)) {
            this.isOutgoing = true;
        }
        return getListFormat();
    }

    /**
     * Formats the full message text for detail display views.
     */
    public String getDetailFormat() {
        String entry = TimeFormatter.zuluTime(this.getTimestamp());
        if ("system".equalsIgnoreCase(this.getType())) {
            entry += " SYSTEM:\n" + this.getMessage();
        } else {
            String contact;
            if (isOutgoing) {
                contact = "TO " + this.getTo();
            } else {
                contact = "FROM " + this.getFrom();
            }
            if ("telex".equalsIgnoreCase(this.getType())) {
                entry += " TELEX " + contact + ": \n" + this.getMessage();
            } else if ("cpdlc".equalsIgnoreCase(this.getType())) {
                String msg = this.getMessage().replace("@","\n"); //@ should be replaced by newline
                entry += " CPDLC " + contact + ": \n" + msg;
            }
        }
        return entry;
    }

    public String getDetailFormat(String callsign){
        if (callsign != null && from != null && from.equalsIgnoreCase(callsign)) {
            this.isOutgoing = true;
        }
        return getDetailFormat();
    }

    /**
     * Decodes Unicode escape sequences (e.g. \\u2B08) into characters.
     */
    public static String decodeUnicode(String input) {
        if (input == null || !input.contains("\\u")) return input;

        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int codePoint = Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(sb, Character.toString((char) codePoint));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    @Override
    public String toString() {
        return "AcarsMessage{" +
                "timestamp=" + timestamp +
                ", from='" + from + '\'' +
                ", type='" + type + '\'' +
                ", to='" + to + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
