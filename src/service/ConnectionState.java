package service;

/**
 * Represents the network connection lifecycle states for Hoppie network communications.
 */
public enum ConnectionState {
    /** Client is offline or logged off. */
    DISCONNECTED,
    /** Network connection check or logon request is in progress. */
    CONNECTING,
    /** Successfully connected and active on the Hoppie network. */
    CONNECTED,
    /** Network drop occurred during active session; background reconnection polling is in progress. */
    RECONNECTING
}
