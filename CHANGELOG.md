# Voidium Changelog

## [2.3.3] - 2025-12-27

### 🗳️ Vote System Hardening
- **Vote Age Limit**: Added protection against "vote spam" caused by delayed delivery from voting sites.
  - New config option `maxVoteAgeHours` (default: 24h).
  - Votes older than this limit are safely ignored instead of flooding the chat.
- **Connectivity Fix**: Solved "Unable to read server response (1941)" error on minecraft-list.cz.
  - Implemented smart packet reading (no more blocking).
  - Added 5-second socket timeout to prevent stalled connections.
- **V1 Compatibility**: Enhanced detection logic to ensure 100% reliable handling of legacy V1 votes involved in edge cases.

## [2.3.2] - 2025-12-26

(See released version)

## [2.3.1] - 2025-12-22

### 💬 Chat Improvements
- **Start Emoji Navigation**: Fixed arrow key navigation in the emoji suggestion popup (previously required restarting selection).
- **Better ESC Handling**: Pressing ESC now correctly closes the suggestion popup without blocking the chat close action.

### 📦 Build & Release
- **CurseForge Environments**: Added `Client` and `Server` tags to release artifacts.
- **Version Compatibility**: Verified and expanded support for Minecraft versions 1.21.1 through 1.21.11.

## [2.3.0] - 2025-12-22

### 📊 Tablist & Monitoring
- **Fixed TPS Display**: The server TPS in the tablist now displays correct values (previously stuck at 20) thanks to a new `TpsTracker` implementation

### 📦 Build & Release
- **CurseForge Integration**: Fixed the automated release workflow to correctly name artifacts (e.g., `voidium-2.3.0.jar`) and apply proper metadata for Minecraft 1.21.1/NeoForge

### 🐛 Bug Fixes
- **General Stability**: Various improvements to chat stability and client compatibility

## [2.2.4] - 2025-12-21

### 🔄 Account Linking Rework
- **Freeze Instead of Kick**: Players who haven't linked their Discord account are now **frozen in place** instead of being kicked
- Players can join the server normally but cannot move until they complete the verification
- Verification code is displayed directly in chat with periodic reminders (every 10 seconds)
- Automatic unfreeze when account is successfully linked via Discord
- Clean player experience - no more disconnect screens

### ⚙️ New Configuration Options
- Added `verificationHintMessage` - customizable hint text shown after the verification code
- Both verification messages are now fully configurable via Web Manager
- Variables: `%code%` for the verification code

### 🔧 Restart Module Fix
- **Hot Reload**: Changing restart times via Web Manager now **immediately reschedules** the RestartManager
- No server restart required when adding/changing restart times
- Automatic log message confirms reload: `"RestartManager reloaded with new configuration"`

### 🛠️ Technical Improvements
- Fixed ConcurrentModificationException in PlayerListManager
- Added detailed logging for Votifier V1 RSA decryption diagnostics
- Better error messages for vote payload debugging

---

## [2.2.3] - 2025-12-20

### 🎫 Ticket System Improvements
- Async ticket creation with proper Discord API handling
- Added cooldown mechanism for rate limit prevention

### 💬 Chat Improvements
- Implemented text wrapping for long messages in Modern Chat overlay

---

## [2.2.2] - 2025-12-19

### 🔧 Compatibility
- Fixed client compatibility for players without Voidium mod
- Fallback to vanilla chat for non-Voidium clients

### 🏗️ Build System
- Added multi-platform build support (Windows EXE, Linux AppImage)
- Configurable advanced JVM flags toggle

---

## Previous Versions
See GitHub releases for full history.
