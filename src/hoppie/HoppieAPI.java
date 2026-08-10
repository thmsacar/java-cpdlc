package hoppie;

import flight.Flight;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Client for the Hoppie ACARS network.
 * Handles the low-level HTTP communication with the Hoppie server.
 */
public class HoppieAPI {

    //callsign, acftType, destination, origin, stand, atis, eob, remarks
    private final String PDC_TEMPLATE = "REQUEST PREDEP CLEARANCE %s %s TO %s AT %s STAND %s ATIS %s %s";
    //remarks(free text)
    private final String LOGON_TEMPLATE = "REQUEST LOGON %s";
    private static final String LOGOFF_TEMPLATE = "LOGOFF";

    //request

    ///data2/msgNo/repliedNo/isReplyRequired/messageTxt
    private final String CPDLC_MSG = "/data2/%s/%s/%s/%s";



    private final String urlStr = "https://www.hoppie.nl/acars/system/connect.html/connect.html";
    private final String logon;
    /** Counter for generating unique CPDLC message sequence numbers. */
    private final AtomicInteger cpdlcCounter;

    /** Wrapper for HTTP status code and response body. */
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

    /**
     * Constructs a new HoppieAPI client.
     * @param logon The user's Hoppie ID.
     */
    public HoppieAPI(String logon) {
        this.logon = logon;
        this.cpdlcCounter = new AtomicInteger(1);
        // Java 8 HTTPS/TLS force protocol to avoid errors
        System.setProperty("https.protocols", "TLSv1.2");
    }

    /** Sends a raw HTTP GET request with automatic retry for transient failures. */
    private HoppieResponse sendHttpRequestWithRetry(String fullUrl, int maxRetries) throws IOException {
        IOException lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HoppieResponse response = sendHttpRequest(fullUrl);
                // Retry on server-side HTTP 5xx errors (500, 502, 503, 504)
                if (response.statusCode() >= 500 && attempt < maxRetries) {
                    try { Thread.sleep(300L * attempt); } catch (InterruptedException ignored) {}
                    continue;
                }
                return response;
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    try { Thread.sleep(300L * attempt); } catch (InterruptedException ignored) {}
                }
            }
        }
        if (lastException != null) throw lastException;
        throw new IOException("Network request failed after " + maxRetries + " attempts");
    }

    /** Sends a raw HTTP GET request to the given URL. */
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

    /** Parses error responses from Hoppie server payloads or HTTP status codes. */
    public static String parseErrorMessage(HoppieResponse response) {
        if (response == null) return "ERROR: No response from server";
        String body = response.body() != null ? response.body().trim() : "";

        // Check for Hoppie payload error pattern: error {reason}
        Pattern p = Pattern.compile("error\\s*\\{([^}]+)\\}", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(body);
        if (m.find()) {
            return "ERROR: " + m.group(1).trim();
        }

        if (response.statusCode() >= 400) {
            return "ERROR: HTTP " + response.statusCode() + (body.isEmpty() ? "" : " (" + body + ")");
        }

        return body.startsWith("ERROR:") ? body : "ERROR: " + body;
    }

    /** Constructs the full Hoppie API URL with query parameters. */
    private String createFullUrl(String from, String to, String type, String packet) {
        return urlStr
                + "?logon=" + encodeParam(logon)
                + "&from=" + encodeParam(from)
                + "&to=" + encodeParam(to)
                + "&type=" + encodeParam(type)
                + "&packet=" + encodeParam(packet);
    }

    private static String encodeParam(String param) {
        if (param == null) return "";
        try {
            return URLEncoder.encode(param, "UTF-8").replace("%2F", "/");
        } catch (UnsupportedEncodingException e) {
            return param.replace(" ", "+");
        }
    }

    /** Sends a poll request to fetch unread messages from the server. */
    private HoppieResponse pollRequest(String callsign) throws IOException {
        String url = createFullUrl(callsign, "SERVER", "poll", "");
        return sendHttpRequestWithRetry(url, 2);
    }

    /**
     * Fetches unread messages for the given callsign from the Hoppie network.
     * @param callsign The aircraft callsign.
     * @return A list of received AcarsMessages.
     */
    public List<AcarsMessage> fetchMessages(String callsign) {
        HoppieResponse response;
        List<AcarsMessage> list = new ArrayList<>();

        try {
            response = pollRequest(callsign);
        } catch (IOException e) {
            list.add(new AcarsMessage("system", "ERROR: " + e.getMessage()));
            return list;
        }

        if (!response.body().trim().startsWith("ok")) {
            list.add(new AcarsMessage("system", parseErrorMessage(response)));
            return list;
        }

        return parsePollResponseBody(response.body(), callsign);
    }

    /** Parses raw Hoppie poll response body into a list of AcarsMessages. */
    public static List<AcarsMessage> parsePollResponseBody(String body, String callsign) {
        List<AcarsMessage> list = new ArrayList<>();
        if (body == null || body.trim().isEmpty()) return list;

        Pattern p = Pattern.compile("\\{(\\S+)\\s+(\\S+)\\s+\\{([\\s\\S]*?)\\}\\}");
        Matcher m = p.matcher(body);

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

    /**
     * Sends a Pre-Departure Clearance (PDC) request.
     */
    public AcarsMessage sendPdcRequest(String station, Flight flight, String stand, String atis, String remarks) {
        String pdc = getPdcMessage(
                flight.getCallsign(),
                flight.getAircraft(),
                flight.getOrigin(),
                flight.getDestination(),
                stand,
                atis,
                remarks);
        return sendTelex(station, flight.getCallsign(), pdc);
    }

    //This is where the PDC request template is stored
    private String  getPdcMessage(String callsign, String acftType, String origin, String destination, String stand, String atis, String remarks) {
        return String.format(PDC_TEMPLATE,
                callsign,
                acftType,
                destination,
                origin,
                stand,
                atis,
                remarks
        );
    }

    /**
     * Sanitizes user text by replacing ASCII slashes ('/') with a Unicode division slash ('∕', \u2215)
     * to prevent CPDLC packet corruption and URL query string issues.
     */
    public static String safeUserText(String text) {
        if (text == null) return "";
        return text.replace('/', '\u2215').trim();
    }

    /**
     * Sends a telex message to a specific station.
     * @param station The recipient station callsign.
     * @param callsign The sender's callsign.
     * @param message The message content.
     * @return The resulting AcarsMessage or an error system message.
     */
    public AcarsMessage sendTelex(String station, String callsign, String message) {
        String cleanStation = safeUserText(station);
        String cleanCallsign = safeUserText(callsign);
        String cleanMessage = safeUserText(message);
        String url = createFullUrl(cleanCallsign, cleanStation, "telex", cleanMessage);
        try{
            HoppieResponse response = sendHttpRequestWithRetry(url, 3);
            if (response.body().trim().startsWith("ok")) {
                return new AcarsMessage(cleanCallsign, "telex", cleanStation, cleanMessage, true);
            }else {
                return new AcarsMessage("system", parseErrorMessage(response));
            }
        } catch (IOException e) {
            return new AcarsMessage("system", "ERROR: "+e.getMessage());
        }
    }

    /** Transmits a raw CPDLC formatted message packet. */
    private AcarsMessage sendCpdlcMessage(String station, String callsign, String rawText) {
        String url = createFullUrl(callsign, station, "cpdlc", rawText);
        try {
            HoppieResponse response = sendHttpRequestWithRetry(url, 3);
            if (response.body().trim().startsWith("ok")) {
                cpdlcCounter.getAndIncrement();
                return new CpdlcMessage(callsign, "cpdlc", station, rawText, true);
            }else{
                return new AcarsMessage("system", parseErrorMessage(response));
            }
        } catch (IOException e) {
            return new AcarsMessage("system", "ERROR: "+e.getMessage());
        }

    }

    /** Sends a ping request to check server reachability. */
    public HoppieResponse sendPing(String callsign) throws IOException {
        String url = createFullUrl(callsign, "SERVER", "ping", "");
        return sendHttpRequestWithRetry(url, 3);
    }

    /** Verifies network connection and callsign authorization on Hoppie. */
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

    /** Formats and sends a CPDLC request packet. */
    private AcarsMessage cpdlcRequest(String station, String callsign, String text, boolean isReplyRequired) {
        return cpdlcRequest(station, callsign, text, isReplyRequired, -1);
    }

    /** Formats and sends a CPDLC response packet. */
    private AcarsMessage cpdlcRequest(String station, String callsign, String text, boolean isReplyRequired, int repliedMsg) {
        String replyReq = isReplyRequired ? "Y" : "N";
        String cleanStation = safeUserText(station);
        String cleanCallsign = safeUserText(callsign);
        String cleanText = safeUserText(text);
        String rawText = String.format(CPDLC_MSG,
                cpdlcCounter.get(),
                repliedMsg > 0 ? String.valueOf(repliedMsg) : "",
                replyReq,
                cleanText
        );
        return sendCpdlcMessage(cleanStation, cleanCallsign, rawText);
    }

    /** Sends a CPDLC logon request to an ATC station. */
    public AcarsMessage sendLogonATC(String station, String callsign, String freeText) {
        if (freeText==null) freeText="";
        String logonMsg = String.format(LOGON_TEMPLATE, freeText);
        return cpdlcRequest(station, callsign, logonMsg, true);
    }

    /** Sends a CPDLC logoff request to an ATC station. */
    public AcarsMessage sendLogoffATC(String station, String callsign) {
        return cpdlcRequest(station, callsign, LOGOFF_TEMPLATE, false);
    }

    // --- CPDLC Response Methods ---

    /** Sends a WILCO response to an uplink CPDLC message. */
    public AcarsMessage wilco(String station, String callsign, int repliedMsg) {
        return this.cpdlcRequest(station, callsign, "WILCO", false, repliedMsg );
    }

    /** Sends a ROGER response to an uplink CPDLC message. */
    public AcarsMessage roger(String station, String callsign, int repliedMsg) {
        return this.cpdlcRequest(station, callsign, "ROGER", false , repliedMsg);
    }

    /** Sends an AFFIRM response to an uplink CPDLC message. */
    public AcarsMessage affirm(String station, String callsign, int repliedMsg) {
        return this.cpdlcRequest(station, callsign, "AFFIRM", false, repliedMsg);
    }

    /** Sends a NEGATIVE response to an uplink CPDLC message. */
    public AcarsMessage negative(String station, String callsign, int repliedMsg) {
        return this.cpdlcRequest(station, callsign, "NEGATIVE", false, repliedMsg);
    }

    /** Sends an UNABLE response to an uplink CPDLC message. */
    public AcarsMessage unable(String station, String callsign, int repliedMsg) {
        return this.cpdlcRequest(station, callsign, "UNABLE", false, repliedMsg);
    }

    /** Sends a STANDBY response to an uplink CPDLC message. */
    public AcarsMessage standby(String station, String callsign, int repliedMsg) {
        return this.cpdlcRequest(station, callsign, "STANDBY", false, repliedMsg);
    }

    /**
     * Sends a CPDLC request message that requires a reply.
     * @param station The recipient ATS unit.
     * @param callsign The sender's callsign.
     * @param text The message text.
     * @return The resulting CPDLC message.
     */
    public AcarsMessage request(String station, String callsign, String text) {
        return this.cpdlcRequest(station, callsign, text, true);
    }

    /**
     * Sends a CPDLC report message that does not require a reply.
     * @param station The recipient ATS unit.
     * @param callsign The sender's callsign.
     * @param text The message text.
     * @return The resulting CPDLC message.
     */
    public AcarsMessage report(String station, String callsign, String text) {
        return this.cpdlcRequest(station, callsign, text, false);
    }

}
