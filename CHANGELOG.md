# Changelog

## [2.2.3] - 2025-12-20

### Bug Fixes
- **Ticket System Fix**: Fixed ticket creation showing "success" message before actually creating the ticket on Discord. Now properly waits for Discord API response and shows error if creation fails.
- **Chat Text Wrapping**: Fixed long messages not wrapping in chat overlay and chat screen. Messages now properly wrap to multiple lines based on screen width.

### Improvements
- Added detailed error handling and logging for ticket creation
- Added scissor clipping in chat screen to prevent text overflow

## [2.2.2] - 2025-12-19

### Bug Fixes
- **Module Conflict Fix**: Relocated `org.apache.commons.collections4` to `voidium.shadow.collections4` to prevent Java module resolution conflict with `glsl.transformer` mod.

## [2.2.1] - 2025-12-19

### Post-Release Fixes
- **Vanilla Client Support**: Fixed `voidium:sync_chat_history` channel missing error, allowing vanilla clients to connect.

### Features (from 2.2)
- **Discord Ticket System**: 
    - Implemented a bi-directional ticket system.
    - Players use `/ticket` to contact staff.
    - Staff replies from Discord ticket channels are routed privately to the player.
- **Commands**: Added `/ticket` and `/reply`.

## [2.2] - 2025-12-19

### Features
- **Discord Ticket System**: 
    - Implemented a bi-directional ticket system.
    - Players use `/ticket` to contact staff.
    - Staff replies from Discord ticket channels are routed privately to the player.
- **Commands**: Added `/ticket` and `/reply`.

## [2.1.9] - 2025-12-13

### Discord Integration
- **Fixed Webhook Avatars**: Webhooks now correctly resolve player skins using the texture cache, ensuring the correct avatar is displayed even for offline-mode players or when using SkinRestorer.
- **Dynamic Bot Status**: Fixed placeholders (`%online%`, `%max%`, `%tps%`) in the bot's status message. The status now updates dynamically every 30 seconds.
- **Whitelist Verification Flow**: 
    - Implemented "Kick on Movement" logic. Unverified players are now kicked only after they verify world load by moving or after a safety timeout (5 seconds). This fixes the issue where players would see a generic "Disconnected" message instead of the verification code.
    - Fixed newline (`\n`) formatting in the kick message config.

### General
- Updated mod version to **2.1.9**.
