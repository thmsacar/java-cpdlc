package hoppie;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AcarsMessage {

    private final Date timestamp;
    private final String from;
    private final String type;
    private final String to;
    private String message;

    public AcarsMessage(String from, String type, String to, String message) {
        this.timestamp = new Date();
        this.from = from;
        this.type = type;
        this.to = to;
        this.message = message;
    }

    public AcarsMessage(String type, String message) {
        this.from = "system";
        this.to = "system";
        this.timestamp = new Date();
        this.type = type;
        this.message = message;
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

    public String getListFormat(String callsign){
        String entry;
        if ("system".equalsIgnoreCase(this.getType())) {
            entry = "SYSTEM: " + this.getMessage();
        } else {
            String contact;
            String arrow;
            if (this.getFrom().equalsIgnoreCase(callsign)) {
                contact = "TO " + this.getTo();
                arrow = "\\u2B08 ";
            } else {
                contact = "FROM " + this.getFrom();
                arrow = "\\u2B0A ";
            }
            entry = arrow + TimeFormatter.zuluTime(this.getTimestamp());
            if ("telex".equalsIgnoreCase(this.getType())) {
                entry += " TELEX " + contact;// + ": " + this.getMessage();
            } else if ("cpdlc".equalsIgnoreCase(this.getType())) {
                entry += " CPDLC " + contact;// + ": " + this.getMessage();
            }
        }
        return decodeUnicode(entry);
    }

    public String getDetailFormat(String callsign){
        String entry = TimeFormatter.zuluTime(this.getTimestamp());
        if ("system".equalsIgnoreCase(this.getType())) {
            entry += " SYSTEM:\n" + this.getMessage();
        } else {
            String contact;
            if (this.getFrom().equalsIgnoreCase(callsign)) {
                contact = "TO " + this.getTo();
            } else {
                contact = "FROM " + this.getFrom() ;
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
