package hoppie;

/**
 * Enum representing standard CPDLC response requirements.
 */
public enum CpdlcResponseType {
    WILCO_UNABLE,     // WU, Y
    AFFIRM_NEGATIVE,  // AN
    ROGER,            // R
    NONE;             // NE, N, empty, null

    /**
     * Parses a raw response requirement code into a {@link CpdlcResponseType}.
     *
     * @param code The raw response requirement string.
     * @return The corresponding {@link CpdlcResponseType}.
     */
    public static CpdlcResponseType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return NONE;
        }
        switch (code.trim().toUpperCase()) {
            case "WU":
            case "Y":
                return WILCO_UNABLE;
            case "AN":
                return AFFIRM_NEGATIVE;
            case "R":
                return ROGER;
            default:
                return NONE;
        }
    }
}
