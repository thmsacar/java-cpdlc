package hoppie;

public class CpdlcMessage extends AcarsMessage {

    private int msgNumber;
    private int responseNumber;
    private String responseType;
    private String sentResponse;

    public CpdlcMessage(String from, String type, String to, String rawContent, boolean isOutgoing) {
        super(from, type, to, rawContent, isOutgoing);
        parseCpdlcContent(rawContent);
    }

    public CpdlcMessage(String from, String type, String to, String rawContent) {
        this(from, type, to, rawContent, false);
    }

    public CpdlcMessage(String from, String type, String to, String message, int msgNumber, int responseNumber, String responseType, boolean isOutgoing) {
        super(from, type, to, message, isOutgoing);
        this.msgNumber = msgNumber;
        this.responseNumber = responseNumber;
        this.responseType = responseType;
    }

    public CpdlcMessage(String from, String type, String to, String message, int msgNumber, int responseNumber, String responseType) {
        this(from, type, to, message, msgNumber, responseNumber, responseType, false);
    }

    public void parseCpdlcContent(String rawContent) {
        // rawContent = "/data2/3//WU/TEST"
        if (rawContent != null && rawContent.startsWith("/data2/")) {
            String[] parts = rawContent.split("/", 6);
            // parts[0] = ""
            // parts[1] = "data2"
            // parts[2] = "3" (msgNumber)
            // parts[3] = "" (repliedMsgNumber - null if empty)
            // parts[4] = "WU" (Response Type/Requirement)
            // parts[5] = "TEST" (Actual Message)

            this.msgNumber = Integer.parseInt(parts[2]);
            this.responseNumber = parts[3].isEmpty() ? -1 : Integer.parseInt(parts[3]);
            this.responseType = parts[4];
            this.setMessage(parts[5]);
        }
    }

    public String getResponseType() { return responseType; }
    public CpdlcResponseType getParsedResponseType() { return CpdlcResponseType.fromCode(responseType); }
    public int getMsgNumber() { return msgNumber; }
    public String getSentResponse() { return sentResponse; }
    public void setSentResponse(String sentResponse) { this.sentResponse = sentResponse; }
    public boolean hasBeenReplied() { return sentResponse != null && !sentResponse.isEmpty(); }
}
