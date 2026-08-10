## Unreleased Changes

### Features & Improvements
- **Dynamic Burst Polling Mode**: Accelerated background message polling from 40s to 15s for 1 minute following outgoing requests, reports, telexes, logons, or PDC requests for faster ATC reply retrieval.
- **Safe URL Parameter Encoding**: Robust query parameter encoding for Hoppie network requests, safely escaping special characters (`&`, `#`, `%`, `+`) while preserving `/data2/` header slashes and replacing ASCII `/` in user text with Unicode division slashes (`∕`).
- **CPDLC Response Requirement Enum**: Created `CpdlcResponseType` enum (`WILCO_UNABLE`, `AFFIRM_NEGATIVE`, `ROGER`, `NONE`) and refactored UI detail rendering with clean `switch` statements.

---

## Version 1.3.0 Release Notes

### Features & Enhancements in v1.3.0
- **Automatic CPDLC Handover & Logoff**: Implemented automatic logoff and handover protocol handling for seamless station transitions.
- **Redesigned Message List**: Enhanced message list items with station headers, message content previews, direction arrows, and right-aligned Zulu timestamps.

**Full Changelog**: https://github.com/thmsacar/java-cpdlc/compare/v1.2.0...v1.3.0