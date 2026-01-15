package hoppie;

import flight.Flight;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HoppieAPI {

    //callsign, acftType, destination, origin, stand, atis, eob
    private final String PDC_TEMPLATE = "REQUEST PREDEP CLEARANCE %s %s TO %s AT %s STAND %s ATIS %s";
    //remarks(free text)
    private final String LOGON_TEMPLATE = "REQUEST LOGON %s";
    private static final String LOGOFF_TEMPLATE = "LOGOFF";

    //request

    ///data2/msgNo/repliedNo/isReplyRequired/messageTxt
    private final String CPDLC_MSG = "/data2/%s/%s/%s/%s";



    private final String urlStr = "http://www.hoppie.nl/acars/system/connect.html/connect.html";
    private final String logon;
    private int cpdlcCounter;

    // Response class to contain HttpResponse (compatible Java8)
    public static class HoppieResponse {
        private final int statusCode;
        private final String body;

        public HoppieResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
        public int statusCode() { return statusCode; }
        public String body() { return body; }
    }

    public HoppieAPI(String logon) {
        this.logon = logon;
        this.cpdlcCounter = 1;
        // Java 8 HTTPS/TLS force protocol to avoid errors
        System.setProperty("https.protocols", "TLSv1.2");
    }

    // Java 8 compatible HTTP GET
    private HoppieResponse sendHttpRequest(String fullUrl) throws IOException {
        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000); // 10 second timeOut
        conn.setReadTimeout(10000);

        int status = conn.getResponseCode();

        // Read response (Java8 compatible)
        String body;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream()))) {
            body = br.lines().collect(Collectors.joining("\n"));
        } finally {
            conn.disconnect();
        }

        return new HoppieResponse(status, body);
    }

    private String createFullUrl(String from, String to, String type, String packet) {
        // Replace spaces with '+' (URL encoding)
        String safePacket = packet.replace(" ", "+");

        return urlStr
                + "?logon=" + logon
                + "&from=" + from
                + "&to=" + to
                + "&type=" + type
                + "&packet=" + safePacket;
    }

    //Sends a request type 'poll', response is the unread messages for 'callsign'
    //Used for fetchMessages method
    private HoppieResponse pollRequest(String callsign) throws IOException {
        String url = createFullUrl(callsign, "SERVER", "poll", "");
        return sendHttpRequest(url);
    }

    //This method should be used when fetching messages to GUI
    public List<AcarsMessage> fetchMessages(String callsign) {
        HoppieResponse response;
        List<AcarsMessage> list = new ArrayList<>();

        try {
            response = pollRequest(callsign);
        } catch (IOException e) {
            list.add(new AcarsMessage("system", "ERROR: "+e.getMessage()));
            return list;
        }

        if(!response.body().trim().startsWith("ok")) {
            list.add(new AcarsMessage("system", "ERROR: "+response.body()));
            return list;
        }

        Pattern p = Pattern.compile("\\{(\\S+)\\s+(\\S+)\\s+\\{([\\s\\S]*?)\\}\\}");
        Matcher m = p.matcher(response.body());

        while (m.find()) {
            String from = m.group(1).trim();
            String type = m.group(2).trim();
            String message = m.group(3).trim();
            if(type.equalsIgnoreCase("cpdlc")){
                list.add(new CpdlcMessage(from, type, callsign, message));
            }else {
                list.add(new AcarsMessage(from, type, callsign, message));
            }
        }

        return list;
    }

    //To send a pre-departure clearance request
    public AcarsMessage sendPdcRequest(String station, Flight flight, String stand, String atis) {
        String pdc = getPdcMessage(
                flight.getCallsign(),
                flight.getAircraft(),
                flight.getOrigin(),
                flight.getDestination(),
                stand,
                atis);
        return sendTelex(station, flight.getCallsign(), pdc);
    }

    //This is where the PDC request template is stored
    private String  getPdcMessage(String callsign, String acftType, String origin, String destination, String stand, String atis) {
        return String.format(PDC_TEMPLATE,
                callsign,
                acftType,
                destination,
                origin,
                stand,
                atis
        );
    }

    public AcarsMessage sendTelex(String station, String callsign, String message) {
        String url = createFullUrl(callsign, station, "telex", message);
        try{
            HoppieResponse response = sendHttpRequest(url);
            if (response.body().trim().startsWith("ok")) {
                return new AcarsMessage(callsign, "telex", station, message);
            }else {
                return new AcarsMessage("system", "ERROR: "+response.body());
            }
        } catch (IOException e) {
            return new AcarsMessage("system", "ERROR: "+e.getMessage());
        }
    }

    private AcarsMessage sendCpdlcMessage(String station, String callsign, String rawText) {
        String url = createFullUrl(callsign, station, "cpdlc", rawText);
        try {
            HoppieResponse response = sendHttpRequest(url);
            if (response.body().trim().startsWith("ok")) {
                cpdlcCounter++;
                return new CpdlcMessage(callsign, "cpdlc", station, rawText);
            }else{
                return new AcarsMessage("system", "ERROR: "+response.body());
            }
        } catch (IOException e) {
            return new AcarsMessage("system", "ERROR: "+e.getMessage());
        }

    }

    public HoppieResponse sendPing(String callsign) throws IOException {
        String url = createFullUrl(callsign, "SERVER", "ping", "");
        return sendHttpRequest(url);
    }

    public AcarsMessage checkConnection(String callsign) {
        try {
            HoppieResponse response = sendPing(callsign);
            if (response.body().trim().startsWith("ok")) {
                return new AcarsMessage("system", "Connected as " + callsign);
            }else{
                return new AcarsMessage("system", "ERROR: " + response.body());
            }
        } catch (IOException e) {
            return new AcarsMessage("system", "ERROR: " + e.getMessage());
        }
    }

    private AcarsMessage cpdlcRequest(String station, String callsign, String text, boolean isReplyRequired) {
        String replyReq = isReplyRequired ? "Y" : "N";
        String rawText = String.format(CPDLC_MSG,
                cpdlcCounter,
                "",
                replyReq,
                text
        );
        return sendCpdlcMessage(station, callsign, rawText);
    }


//    private HoppieResponse cpdlcRequest(String station, String callsign, String text, boolean isReplyRequired) throws IOException {
//        String replyReq = isReplyRequired ? "Y" : "N";
//        String cpdlc = String.format(CPDLC_MSG,
//                    cpdlcCounter,
//                    "",
//                    replyReq,
//                    text
//                );
////        String cpdlc = "/data2/"+cpdlcCounter+"//"+replyReq+"/"+text;
//        String url = createFullUrl(callsign, station, "cpdlc", cpdlc);
//        HoppieResponse response = sendHttpRequest(url);
//        if (response.statusCode() == 200) {cpdlcCounter++;}
//        return response;
//    }

    private AcarsMessage cpdlcRequest(String station, String callsign, String text, boolean isReplyRequired, int repliedMsg) {
        String replyReq = isReplyRequired ? "Y" : "N";
        String rawText = String.format(CPDLC_MSG,
                cpdlcCounter,
                repliedMsg,
                replyReq,
                text
        );
        return sendCpdlcMessage(station, callsign, rawText);
    }


//    private HoppieResponse cpdlcRequest(String station, String callsign, String text, boolean isReplyRequired, int repliedMsg) throws IOException {
//        String replyReq = isReplyRequired ? "Y" : "N";
//        String cpdlc = String.format(CPDLC_MSG,
//                cpdlcCounter,
//                repliedMsg,
//                replyReq,
//                text
//        );
////        String cpdlc = "/data2/"+cpdlcCounter+"/"+repliedMsg+"/"+replyReq+"/"+text;
//        String url = createFullUrl(callsign, station, "cpdlc", cpdlc);
//        HoppieResponse response = sendHttpRequest(url);
//        if (response.statusCode() == 200) {cpdlcCounter++;}
//        return response;
//    }

    public AcarsMessage sendLogonATC(String station, String callsign, String freeText) {
        if (freeText==null) freeText="";
        String logonMsg = String.format(LOGON_TEMPLATE, freeText);
        return cpdlcRequest(station, callsign, logonMsg, true);
    }

    public AcarsMessage sendLogoffATC(String station, String callsign) {
        return cpdlcRequest(station, callsign, LOGOFF_TEMPLATE, false);
    }

    // --- CPDLC Response Methods ---  DEPRECATED
//    public HoppieResponse wilco(String station, String callsign, int repliedMsg) throws IOException {
//        return this.cpdlcRequest(station, callsign, "WILCO", false, repliedMsg );
//    }
//
//    public HoppieResponse roger(String station, String callsign, int repliedMsg) throws IOException {
//        return this.cpdlcRequest(station, callsign, "ROGER", false , repliedMsg);
//    }
//
//    public HoppieResponse affirm(String station, String callsign, int repliedMsg) throws IOException {
//        return this.cpdlcRequest(station, callsign, "AFFIRM", false, repliedMsg);
//    }
//
//    public HoppieResponse negative(String station, String callsign, int repliedMsg) throws IOException {
//        return this.cpdlcRequest(station, callsign, "NEGATIVE", false, repliedMsg);
//    }
//
//    public HoppieResponse unable(String station, String callsign, int repliedMsg) throws IOException {
//        return this.cpdlcRequest(station, callsign, "UNABLE", false, repliedMsg);
//    }
//
//    public HoppieResponse standby(String station, String callsign, int repliedMsg) throws IOException {
//        return this.cpdlcRequest(station, callsign, "STANDBY", false, repliedMsg);
//    }

    // --- CPDLC Response Methods ---
    public AcarsMessage wilco(String station, String callsign, int repliedMsg) {
        return this.cpdlcRequest(station, callsign, "WILCO", false, repliedMsg );
    }

    public AcarsMessage roger(String station, String callsign, int repliedMsg) {
        return this.cpdlcRequest(station, callsign, "ROGER", false , repliedMsg);
    }

    public AcarsMessage affirm(String station, String callsign, int repliedMsg) {
        return this.cpdlcRequest(station, callsign, "AFFIRM", false, repliedMsg);
    }

    public AcarsMessage negative(String station, String callsign, int repliedMsg) {
        return this.cpdlcRequest(station, callsign, "NEGATIVE", false, repliedMsg);
    }

    public AcarsMessage unable(String station, String callsign, int repliedMsg) {
        return this.cpdlcRequest(station, callsign, "UNABLE", false, repliedMsg);
    }

    public AcarsMessage standby(String station, String callsign, int repliedMsg) {
        return this.cpdlcRequest(station, callsign, "STANDBY", false, repliedMsg);
    }

}