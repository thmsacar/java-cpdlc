package flight;

import java.util.Date;

public class Flight {

    private String callsign;
    private String origin;
    private String destination;
    private String aircraft;
    private Date offBlockTime;
    private final TimeFormatter timeFormatter;


    public Flight(String callsign, String origin, String destination, String aircraft, Integer offBlockTime) {
        this.callsign = callsign;
        this.origin = origin;
        this.destination = destination;
        this.aircraft = aircraft;

        long timeStampSec = offBlockTime;
        this.offBlockTime = new Date(timeStampSec * 1000L);
        this.timeFormatter = new TimeFormatter();
    }


    public String getOffBlockTime() {
        return timeFormatter.zuluTime(offBlockTime);
    }

    public String getLocalOffBlockTime() {
        return timeFormatter.localTime(offBlockTime);
    }

    public String getDestination() {
        return destination;
    }

    public String getOrigin() {
        return origin;
    }

    public String getAircraft() {
        return aircraft;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setOffBlockTime(Date offBlockTime) {
        this.offBlockTime = offBlockTime;
    }

    public void setAircraft(String aircraft) {
        this.aircraft = aircraft;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }
}
