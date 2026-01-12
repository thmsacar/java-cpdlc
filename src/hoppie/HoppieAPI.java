package hoppie;

//import flight.Flight;
//import org.json.JSONObject;
//
//import java.io.IOException;
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class HoppieAPI {
//
//    private final String urlStr = "http://www.hoppie.nl/acars/system/connect.html/";
//    private String logon;
//    private int cpdlcCounter;
//
//    public HoppieAPI(String logon) {
//        this.logon = logon;
//        this.cpdlcCounter = 1;
//    }
//
//    //TODO Exception Handling nasil olmali
//    public HttpResponse<String> sendHttpRequest(URI uri) throws IOException, InterruptedException {
//        HttpClient client = HttpClient.newHttpClient();
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(uri)
//                .GET()
//                .build();
//
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//        return response;
//    }
//
//    private URI createURI(String from, String to, String type, String packet) {
//        packet = packet.replace(" ", "+");
//
//        String rawUrl =
//                "http://www.hoppie.nl/acars/system/connect.html/connect.html"
//                        + "?logon=" + logon
//                        + "&from=" + from
//                        + "&to=" + to
//                        + "&type=" + type
//                        + "&packet=" + packet;
//
//        return URI.create(rawUrl);
////        Map<String, String> params = Map.of(
////                "logon", logon,
////                "from", from,
////                "to", to,
////                "type", type,
////                "packet", packet
////        );
////
////        String query = params.entrySet().stream()
////                .map(e -> e.getKey() + "=" +
////                        URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
////                .collect(Collectors.joining("&"));
////
////        URI uri = new URI(
////                "http",
////                "www.hoppie.nl",
////                "/acars/system/connect.html/connect.html",
////                query,
////                null);
////
////        return uri;
//    }
//
//    public HttpResponse<String> pollRequest(String callsign) throws IOException, InterruptedException {
//        URI uri = createURI(callsign, "SERVER", "poll", "");
//        HttpResponse<String> response = sendHttpRequest(uri);
//        return response;
//    }
//
//    public List<AcarsMessage> fetchMessages(String callsign) throws IOException, InterruptedException {
//        HttpResponse<String> response = pollRequest(callsign);
//        List<AcarsMessage> list = new ArrayList<>();
//
//        Pattern p = Pattern.compile(
//                "\\{(\\S+)\\s+(\\S+)\\s+\\{([\\s\\S]*?)\\}\\}"
//        );
//        Matcher m = p.matcher(response.body());
//
//        while (m.find()) {
//            String from = m.group(1);
//            String type = m.group(2);
//            String message = m.group(3).trim();
//
//            list.add(new AcarsMessage(from, type, callsign, message));
//        }
//        return list;
//    }
//
//    //TODO PDC Request aux gerekli mi??
//    public HttpResponse<String> sendPDCRequest(String station, Flight flight, String stand, String atis) throws IOException, InterruptedException {
//        HttpResponse<String> response = this.sendPDCrequestAux(station, flight.getCallsign(), flight.getAircraft(), flight.getOrigin(), flight.getDestination(), stand, atis, flight.getOffBlockTime());
//        return response;
//    }
//
//    private HttpResponse<String> sendPDCrequestAux(String station, String callsign, String acftType, String origin, String destination, String stand, String atis, String eob) throws IOException, InterruptedException {
//        String pdc = "REQUEST PREDEP CLEARANCE "+callsign+" "+acftType+" TO "+destination+" AT "+origin+" STAND "+stand+" ATIS "+atis+ " EOB "+eob;
//
//        URI uri = this.createURI(callsign, station, "telex", pdc);
//
//
//        HttpResponse response = this.sendHttpRequest(uri);
//
//        return response;
//    }
//
//    private HttpResponse<String> cpdlcRequest(String station, String callsign, String text, boolean isReplyRequired) throws IOException, InterruptedException {
//        String replyReq = isReplyRequired ? "Y" : "N";
//        String cpdlc = "/data2/"+cpdlcCounter+"//"+replyReq+"/"+text;
//        URI uri  = createURI(callsign, station, "cpdlc", cpdlc);
//        HttpResponse<String> response = this.sendHttpRequest(uri);
//        if (response.statusCode() == 200) {cpdlcCounter++;}
//        return response;
//    }
//
//    private HttpResponse<String> cpdlcRequest(String station, String callsign, String text, boolean isReplyRequired,int repliedMsg) throws IOException, InterruptedException {
//        String replyReq = isReplyRequired ? "Y" : "N";
//        String cpdlc = "/data2/"+cpdlcCounter+"/"+repliedMsg+"/"+replyReq+"/"+text;
//        URI uri  = createURI(callsign, station, "cpdlc", cpdlc);
//        HttpResponse<String> response = this.sendHttpRequest(uri);
//        if (response.statusCode() == 200) {cpdlcCounter++;}
//        return response;
//    }
//
//    public HttpResponse<String> sendLogonATC(String station, String callsign, String freeText) throws IOException, InterruptedException {
//        String logonMsg = "REQUEST LOGON "+freeText;
//        return cpdlcRequest(station, callsign, logonMsg, true);
//    }
//
//    //NE not required
//    //R rOGER
//    //WU
//    //AN affirm negative
//
//    public HttpResponse<String> wilco(String station, String callsign, int repliedMsg) throws IOException, InterruptedException {
//        return this.cpdlcRequest(station, callsign, "WILCO", false, repliedMsg );
//    }
//
//    public HttpResponse<String> roger(String station, String callsign, int repliedMsg) throws IOException, InterruptedException {
//        return this.cpdlcRequest(station, callsign, "ROGER", false , repliedMsg);
//    }
//
//    public HttpResponse<String> affirm(String station, String callsign, int repliedMsg) throws IOException, InterruptedException {
//        return this.cpdlcRequest(station, callsign, "AFFIRM", false, repliedMsg);
//    }
//
//    public HttpResponse<String> negative(String station, String callsign, int repliedMsg) throws IOException, InterruptedException {
//        return this.cpdlcRequest(station, callsign, "NEGATIVE", false, repliedMsg);
//    }
//
//    public HttpResponse<String> unable(String station, String callsign, int repliedMsg) throws IOException, InterruptedException {
//        return this.cpdlcRequest(station, callsign, "UNABLE", false, repliedMsg);
//    }
//
//    public HttpResponse<String> standby(String station, String callsign, int repliedMsg) throws IOException, InterruptedException {
//        return this.cpdlcRequest(station, callsign, "STANDBY", false, repliedMsg);
//    }
//
//
//}

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
    private final String PDC_TEMPLATE = "REQUEST PREDEP CLEARANCE %s %s TO %s AT %s STAND %s ATIS %s EOB %s";
    //remarks(free text)
    private final String LOGON_TEMPLATE = "REQUEST LOGON %s";
    ///data2/msgNo/repliedNo/isReplyRequired/messageTxt
    private final String CPDLC_MSG = "/data2/%s/%s/%s/%s";


    private final String urlStr = "http://www.hoppie.nl/acars/system/connect.html/connect.html";
    private String logon;
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
    public HoppieResponse sendHttpRequest(String fullUrl) throws IOException {
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

    public int getCpdlcCounter() {
        return cpdlcCounter;
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
    public List<AcarsMessage> fetchMessages(String callsign) throws IOException, RuntimeException {
        HoppieResponse response = pollRequest(callsign);
        if(!response.body().trim().startsWith("ok")) {
            throw new RuntimeException("Could not fetch messages.");
        }
        List<AcarsMessage> list = new ArrayList<>();

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
    public HoppieResponse sendPDCRequest(String station, Flight flight, String stand, String atis) throws IOException {
        return this.sendPDCrequestAux(station, flight.getCallsign(), flight.getAircraft(), flight.getOrigin(), flight.getDestination(), stand, atis, flight.getOffBlockTime());
    }

    //This is where the PDC request template is stored
    private HoppieResponse sendPDCrequestAux(String station, String callsign, String acftType, String origin, String destination, String stand, String atis, String eob) throws IOException {
        String pdc = String.format(PDC_TEMPLATE,
                callsign,
                acftType,
                destination,
                origin,
                stand,
                atis,
                eob
        );
        String url = createFullUrl(callsign, station, "telex", pdc);
        return sendHttpRequest(url);
    }

    public HoppieResponse sendTelex(String station, String callsign, String message) throws IOException {
        String url = createFullUrl(callsign, station, "telex", message);
        return sendHttpRequest(url);
    }

    public HoppieResponse sendPing(String callsign) throws IOException {
        String url = createFullUrl(callsign, "SERVER", "ping", "");
        return sendHttpRequest(url);
    }

    private HoppieResponse cpdlcRequest(String station, String callsign, String text, boolean isReplyRequired) throws IOException {
        String replyReq = isReplyRequired ? "Y" : "N";
        String cpdlc = String.format(CPDLC_MSG,
                    cpdlcCounter,
                    "",
                    replyReq,
                    text
                );
//        String cpdlc = "/data2/"+cpdlcCounter+"//"+replyReq+"/"+text;
        String url = createFullUrl(callsign, station, "cpdlc", cpdlc);
        HoppieResponse response = sendHttpRequest(url);
        if (response.statusCode() == 200) {cpdlcCounter++;}
        return response;
    }

    private HoppieResponse cpdlcRequest(String station, String callsign, String text, boolean isReplyRequired, int repliedMsg) throws IOException {
        String replyReq = isReplyRequired ? "Y" : "N";        String cpdlc = String.format(CPDLC_MSG,
                cpdlcCounter,
                repliedMsg,
                replyReq,
                text
        );
//        String cpdlc = "/data2/"+cpdlcCounter+"/"+repliedMsg+"/"+replyReq+"/"+text;
        String url = createFullUrl(callsign, station, "cpdlc", cpdlc);
        HoppieResponse response = sendHttpRequest(url);
        if (response.statusCode() == 200) {cpdlcCounter++;}
        return response;
    }

    public HoppieResponse sendLogonATC(String station, String callsign, String freeText) throws IOException {
        if (freeText==null) freeText="";
        String logonMsg = String.format(LOGON_TEMPLATE, freeText);
        return cpdlcRequest(station, callsign, logonMsg, true);
    }

    // --- CPDLC Response Metodları ---

    public HoppieResponse wilco(String station, String callsign, int repliedMsg) throws IOException {
        return this.cpdlcRequest(station, callsign, "WILCO", false, repliedMsg );
    }

    public HoppieResponse roger(String station, String callsign, int repliedMsg) throws IOException {
        return this.cpdlcRequest(station, callsign, "ROGER", false , repliedMsg);
    }

    public HoppieResponse affirm(String station, String callsign, int repliedMsg) throws IOException {
        return this.cpdlcRequest(station, callsign, "AFFIRM", false, repliedMsg);
    }

    public HoppieResponse negative(String station, String callsign, int repliedMsg) throws IOException {
        return this.cpdlcRequest(station, callsign, "NEGATIVE", false, repliedMsg);
    }

    public HoppieResponse unable(String station, String callsign, int repliedMsg) throws IOException {
        return this.cpdlcRequest(station, callsign, "UNABLE", false, repliedMsg);
    }

    public HoppieResponse standby(String station, String callsign, int repliedMsg) throws IOException {
        return this.cpdlcRequest(station, callsign, "STANDBY", false, repliedMsg);
    }
}