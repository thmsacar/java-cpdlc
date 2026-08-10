## Version 1.3.1 Release Notes

### Bugfix
- **GUI Newline Character Filtering**: Filtered out carriage return (`\r`) and line feed (`\n`) characters in GUI input text fields, text areas, and clipboard paste to prevent illegal character errors on the Hoppie network, while ignoring Enter key presses in message text areas.

### Features & Improvements
- **Dynamic Burst Polling Mode**: Accelerated background message polling from 40s to 20s for 40s following outgoing requests, reports, telexes, logons, or PDC requests for faster ATC reply retrieval.
- **Safe URL Parameter Encoding**: Robust query parameter encoding for Hoppie network requests, safely escaping special characters (`&`, `#`, `%`, `+`) while preserving `/data2/` header slashes and replacing ASCII `/` in user text with Unicode division slashes (`∕`).
- **CPDLC Response Requirement Enum**: Created `CpdlcResponseType` enum (`WILCO_UNABLE`, `AFFIRM_NEGATIVE`, `ROGER`, `NONE`) and refactored UI detail rendering with clean `switch` statements.

**Full Changelog**: https://github.com/thmsacar/java-cpdlc/compare/v1.3.0...v1.3.1
