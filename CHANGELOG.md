# Changelog

## [2.2] - 2025-12-19

### Highlights
- Major update to version 2.2!

### General
- Updated mod version to **2.2**.
- Various bug fixes and improvements.

## [2.1.9] - 2025-12-13

### Discord Integration
- **Fixed Webhook Avatars**: Webhooks now correctly resolve player skins using the texture cache, ensuring the correct avatar is displayed even for offline-mode players or when using SkinRestorer.
- **Dynamic Bot Status**: Fixed placeholders (`%online%`, `%max%`, `%tps%`) in the bot's status message. The status now updates dynamically every 30 seconds.
- **Whitelist Verification Flow**: 
    - Implemented "Kick on Movement" logic. Unverified players are now kicked only after they verify world load by moving or after a safety timeout (5 seconds). This fixes the issue where players would see a generic "Disconnected" message instead of the verification code.
    - Fixed newline (`\n`) formatting in the kick message config.

### General
- Updated mod version to **2.1.9**.
