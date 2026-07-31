## Version 1.1.2 Release Notes

### Features & Fixes in v1.1.2
- **Unread Messages Indicator**: Introduced unread message tracking for incoming datalink messages with bold text and color-coded indicator dots (`●`):
  - Cyan accent for CPDLC messages
  - Soft warm gold accent for TELEX messages
  - Soft coral accent for SYSTEM messages
- **Sound Alert Fix**: Resolved a bug where initial connection confirmation played an error warning sound upon connecting.
- **Message List Interaction**: Improved message selection behavior to update read status upon mouse release.

**Full Changelog**: https://github.com/thmsacar/java-cpdlc/compare/v1.1.1...v1.1.2