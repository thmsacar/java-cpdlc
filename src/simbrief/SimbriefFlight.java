package simbrief;

import flight.Flight;


public class SimbriefFlight extends Flight {

    public SimbriefFlight(String callsign, String origin, String destination, String aircraft, Integer offBlockTime) {
        super(callsign, origin, destination, aircraft, offBlockTime);

    }

    @Override
    public String toString() {
        return "SimbriefFlight{" +
                "callsign='" + this.getCallsign() + '\'' +
                ", origin='" + this.getOrigin() + '\'' +
                ", destination='" + this.getDestination() + '\'' +
                ", aircraft='" + this.getAircraft() + '\'' +
                ", offBlockTime=" + this.getOffBlockTime() +
                ", " + this.getLocalOffBlockTime() +
                '}';
    }
}
