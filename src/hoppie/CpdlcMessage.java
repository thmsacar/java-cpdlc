package hoppie;

public class CpdlcMessage extends AcarsMessage {

    private int msgNumber;
    private int responseNumber;
    private String responseType;

    public CpdlcMessage(String from, String type, String to, String rawContent) {
        super(from, type, to, rawContent);
        parseCpdlcContent(rawContent);
    }

    public CpdlcMessage(String from, String type, String to, String message, int msgNumber, int responseNumber, String responseType) {
        super(from, type, to, message);
        this.msgNumber = msgNumber;
        this.responseNumber = responseNumber;
        this.responseType = responseType;
    }


    public void parseCpdlcContent(String rawContent) {
        // rawContent = "/data2/3//WU/TEST"
        if (rawContent.startsWith("/data2/")) {
            String[] parts = rawContent.split("/", -1);
            // parts[0] = ""
            // parts[1] = "data2"
            // parts[2] = "3" (msgNumber)
            // parts[3] = "" (repliedMsgNumber - null if empty)
            // parts[4] = "WU" (Response Type/Requirement)
            // parts[5] = "TEST" (Actual Message)

            for (String part : parts) {
                System.out.println("-"+part+"-");
            }

            this.msgNumber = Integer.parseInt(parts[2]);
            this.responseNumber = parts[3].isEmpty() ? -1 : Integer.parseInt(parts[3]);
            this.responseType = parts[4];
            this.setMessage(parts[5]);
        }
    }

    public String getResponseType() {return responseType; }
    public int getMsgNumber() { return msgNumber; }

}
