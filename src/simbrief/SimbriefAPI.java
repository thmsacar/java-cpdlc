package simbrief;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

import org.json.JSONObject;
import org.json.XML;

public class SimbriefAPI {

    private final String urlStr = "https://www.simbrief.com/api/xml.fetcher.php?username=";
    private final String username;
    private final String url;

    public SimbriefAPI(String username) {
        this.username = username;
        this.url = urlStr + username;

    }

    public SimbriefFlight getFlight() throws IOException {
        Scanner sc = new Scanner(new URL(url).openStream()).useDelimiter("\\A");
        String xml = sc.next();

        JSONObject json = XML.toJSONObject(xml);

        String callsign = json.getJSONObject("OFP")
                .getJSONObject("atc")
                .getString("callsign");

        String origin = json.getJSONObject("OFP")
                .getJSONObject("origin")
                .getString("icao_code");

        String destination = json.getJSONObject("OFP")
                .getJSONObject("destination")
                .getString("icao_code");

        String aircraft = json.getJSONObject("OFP")
                .getJSONObject("aircraft")
                .getString("icao_code");

        Integer offBlockTime = json.getJSONObject("OFP")
                .getJSONObject("times")
                .getInt("est_out");


        return new SimbriefFlight(callsign, origin, destination, aircraft, offBlockTime);


    }



}
