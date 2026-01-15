package flight;

import java.util.Date;

public class Flight {

    private String callsign;
    private String origin;
    private String destination;
    private String aircraft;


    public Flight(String callsign, String origin, String destination, String aircraft) {
        this.callsign = callsign;
        this.origin = origin;
        this.destination = destination;
        this.aircraft = aircraft;

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

    @Override
    public String toString() {
        return "Flight{" +
                "callsign='" + callsign + '\'' +
                ", origin='" + origin + '\'' +
                ", destination='" + destination + '\'' +
                ", aircraft='" + aircraft + '\'' +
                '}';
    }
}
