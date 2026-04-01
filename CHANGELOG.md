# Voidium Changelog

## [2.5.2] - 2026-04-01

### Fixes
- **Manual Announce Recovery**: `/voidium announce` and the web announce action now work again after reloads and in setups where automatic announcements are disabled with interval `0`.
- **Prefixes Only For Real Players**: Custom prefixes and suffixes in the player list are now applied through player-only name events instead of scoreboard team prefixes, so they no longer leak onto pets or other named entities.
- **Clearing Discord Role Prefixes**: Empty Discord role prefix and suffix fields are now respected as truly empty values instead of being auto-regenerated from the Discord role name.
- **Global Rank Toggle Respected**: When ranks are disabled in `general.json`, player list custom names no longer keep showing time-based rank decorations.
- **Version Metadata Sync**: Updated project version metadata to `2.5.2` for this release.

## [2.5.1] - 2026-03-15

### Fixes
- **Clickable Discord Links in MC Chat**: Discord bridge messages now create proper `ClickEvent.OPEN_URL` components instead of only coloring URLs blue, so links are clickable again in chat.
- **Rank Hover Tooltip in TAB**: Time-based rank prefixes and suffixes in the player list now keep their hover tooltip when PlayerList is enabled, including played hours and required hours for the displayed rank.
- **Version Metadata Sync**: Updated project version metadata to `2.5.1` for the current release.

## [2.5] - 2026-03-14

### 🌐 Web Control Panel — React + Vite Rewrite
- **React 19 + Vite 6 + TypeScript**: Web panel frontend completely rewritten from inline Java strings (~1600 lines) to a proper React SPA with type-safe components, bundled into the mod JAR.
- **16 Modular Components**: Hero, MetricGrid, TimerGrid, Timeline, QuickActions, SecurityPanel, PlayerRoster, ModuleHealth, VoteQueue, TicketPanel, StatsCharts, ConfigStudio, Console, LiveFeeds, AiPanel, MaintenanceBanner.
- **SSE Real-time Updates**: Dashboard uses Server-Sent Events (`/api/events`) for 3-second push updates instead of 10-second polling. Automatic fallback to polling on connection loss.
- **Dark / Light Theme**: Theme toggle in SecurityPanel with dark mode as default. Persistent via localStorage, anti-flash inline script prevents theme flicker on load.
- **Locale Selector**: Language dropdown (English / Čeština) in SecurityPanel, persisted to localStorage, compatible with `?lang=cz` URL parameter.
- **JSON Schema Export**: New `/api/config/schema/export` endpoint returns full configuration schema in JSON Schema draft-07 format for external tooling and validation.
- **API Rate Limiting**: 120 requests per minute per IP with sliding window on AI endpoints (`/api/ai/*`). Returns 429 with `Retry-After` header when exceeded.
- **Static Asset Serving**: WebManager serves pre-built assets from classpath (`/web/`) with content-type detection and immutable caching for hashed assets.
- **Dev Mode Support**: Vite dev server with API proxy for hot-reload during development.
- **Full Single-Page Dashboard**: Real-time server metrics, player list, scheduled task timers, and alert banners.
- **Config Studio**: Complete visual config editor for all 12 modules — schema-driven forms with preview, diff, apply, rollback, defaults, and locale preset support.
- **RGB Color Picker**: Inline color helper for `&#RRGGBB` text fields in Config Studio.
- **Structured Editors**: Dedicated editors for rank lists and Discord role prefix mappings.
- **Impact Classification**: Each config field change is labeled as "live-safe" or "restart-required".
- **Live Console**: Stream server logs in the browser with syntax highlighting and command execution.
- **Live Chat Feed**: Real-time chat history from `ChatHistoryManager`.
- **Audit Trail**: All web panel actions logged to `web-audit.log` with timestamp, action, source, and status.
- **SVG Performance Graphs**: 24-hour history charts for player count and TPS.
- **`/voidium web` Re-enabled**: Generate bootstrap URL + persistent admin URL directly from in-game.- **Toast Notifications**: Non-blocking toast system (success, error, warning, info) with auto-dismiss. Replaces generic alerts for all user-facing feedback.
- **System Info Cards**: CPU usage, RAM usage, and disk usage metrics in the dashboard (via `OperatingSystemMXBean` + `FileStore`).
- **Server Icon**: Server favicon displayed in the dashboard hero section (`/api/server-icon` endpoint).
- **Server Properties Editor**: View and edit `server.properties` directly from the Web panel (`/api/server-properties` GET/POST).
- **MC Text Preview**: Live preview of Minecraft color codes (`&a`, `&b`, `&#RRGGBB`, etc.) in Config Studio text fields.
- **HTTP Error Feedback**: Frontend properly surfaces server error messages (rate limit, validation, etc.) instead of generic "Action failed".
- **Custom Conditions Hints**: Rank editor shows format examples and available condition types (KILL, VISIT, BREAK, PLACE) below the JSON textarea.
### � Discord Improvements
- **MC → Discord Ban Sync**: Fixed mixin registration — `StoredUserEntryAccessor` and `UserBanListMixin` were implemented but not registered in `voidium.mixins.json`. Bans now properly sync bidirectionally.
- **Event Embeds**: Player join/leave/death messages now use colored Discord embeds with player skin thumbnails (green for join, red for leave, gray for death) instead of plain text.

### 🎫 Ticket Auto-Assignment
- **Auto-Assign Support Member**: New tickets are automatically assigned to the support team member with the fewest active tickets.
- **Configurable**: `enableAutoAssign` toggle and `assignedMessage` template in `ticket.json`.

### �🔐 Security & Session Hardening
- **Secret Masking**: Sensitive config fields (`botToken`, `adminToken`, `sharedSecret`, API keys) are shown as `••••••` in Config Studio. Masked values are never written back.
- **Session Cleanup Thread**: `Voidium-SessionCleaner` daemon purges expired bootstrap tokens and sessions every 5 minutes.
- **Session Sliding**: Active sessions are renewed on each authenticated request.
- **HttpOnly Cookies**: `voidium_session` cookie set with `HttpOnly; SameSite=Lax; Path=/`.
- **Configurable Session TTL**: `sessionTtlMinutes` in `web.json` (default 120, minimum 5).
- **Console Command Allowlist**: Web console only permits safe commands (messaging, teleport, utility, voidium).
- **AI Secret Redaction**: Config context sent to LLM has apiKey, botToken, sharedSecret, adminToken, and chatWebhookUrl redacted.

### 🛡️ Maintenance Mode
- **Login Block**: When `maintenanceMode: true` in `general.json`, only OPs (permission level 2+) can join. Non-OPs get a bilingual disconnect message.
- **Dashboard Banner**: "⚠ MAINTENANCE MODE ACTIVE" banner with one-click disable button.
- **Web Panel Toggle**: `maintenance_on` / `maintenance_off` actions from the dashboard.
- **Config Studio Warning**: Preview shows a warning when maintenance mode is enabled.

### 🤖 AI System
- **Per-Player Conversation History**: Each player gets their own conversation state (max 16 turns), stored in-memory keyed by UUID.
- **Player History Panel**: `/api/ai/players` endpoint shows all player conversations sorted by last activity.
- **Moderation Guardrails**: Input filter blocks keywords (hack, exploit, dupe, cheat, crash, grief, xray, bypass, injection, etc.). Blocked messages return a moderation notice.
- **Response Length Cap**: AI responses capped at 4000 characters.
- **Prompt Length Limit**: Player prompts capped at `playerPromptMaxLength` (default 280 chars).
- **Per-Player Cooldown**: `playerCooldownSeconds` (default 20) prevents spam.
- **World / Game Mode Restrictions**: `disabledWorlds` and `disabledGameModes` lists in `ai.json` block AI usage in specific dimensions or game modes.
- **Access Gating**: 5 modes — ALL, PLAYTIME, DISCORD_ROLE, PLAYTIME_OR_DISCORD_ROLE, PLAYTIME_AND_DISCORD_ROLE.
- **AI Config Suggestions**: `/api/ai/admin/suggest` — AI proposes config changes, staged in Config Studio with diff preview before apply.
- **Incident Review**: One-click button grabs last 30 console + 20 chat lines, sends to AI for anomaly/error/security analysis.

### 📊 Dashboard Improvements
- **Scheduled Task Timeline**: Visual timeline panel with progress bars for restart, entity cleaner, and stats report timers.
- **Live Timer Ticking**: Timer cards and timeline update every second.
- **Vote Queue Inspector**: Pending vote snapshot, payout, and clear actions from dashboard.
- **Boss Protection Status**: EntityCleaner section shows boss protection toggle state.

### 🔧 Bug Fixes
- **Config Studio Diff**: Fixed phantom diff showing ~23 changes after apply. Root cause: GSON `deepCopy()` converted integers to doubles, so `equalsValue(24, 24.0)` returned false.
- **Config Studio State**: Diff, summary, and preview errors are now cleared after successful apply, rollback, or reload.
- **AnnouncementManager Reload**: Fixed `ScheduledThreadPoolExecutor` "Task rejected" crash on config reload — scheduler is now recreated instead of reusing a terminated instance.
- **Mixin Registration**: `StoredUserEntryAccessor` and `UserBanListMixin` were implemented but not registered in `voidium.mixins.json`.

## [2.4] - 2026-03-11

### 🎨 Chat System Overhaul
- **Full RGB Color Support**: Chat now supports `&#RRGGBB` hex colors everywhere, not just on lines with emojis.
- **Recursive Component Parsing**: Refactored `ChatColorParser` to walk the Component tree recursively, preserving existing styles (HoverEvent, ClickEvent).
- **§ Code Support**: Minecraft's `§` color codes are now normalized and rendered correctly alongside `&` codes.
- **Color Bleeding Fix**: Automatic color reset before message text prevents rank prefix colors from leaking into player messages.

### 🏅 Rank Tooltip
- **Hover Tooltip on Rank Prefix/Suffix**: Hovering over a rank prefix or suffix now shows played hours and required hours.
- **Configurable Tooltip Text**: New `tooltipPlayed` and `tooltipRequired` fields in `ranks.json` (default EN, overridable).
- **Promotion Message**: Default text changed from Czech to English.

### 👾 Discord Rename on Link
- **Nickname Rename**: After linking a Minecraft account, the player's Discord nickname is automatically changed to their in-game name.
- **Configurable**: New `renameOnLink` toggle in `discord.json` (default: `false`).
- Works on both `/link` slash command and link channel message verification.

### 🌐 Web Panel
- **Old Web Editor Removed**: The legacy `WebManager` (4734 lines) has been fully removed.
- **New Web Editor**: 🚧 Work In Progress – a modern replacement is being developed.

### 🔄 Reload Improvements
- **`/voidium reload`**: Now fully restarts `RankManager` and `StatsManager` in addition to all configs, Discord, PlayerList, VoteManager, and EntityCleaner.
- `/voidium web` command re-enabled in v2.5.

### 📊 Player List
- **Time-Based Ranks in TAB**: Player list now checks playtime and displays matching rank prefixes/suffixes with hover tooltips.

## [2.3.5] - 2026-02-06

### 💬 Discord Bridge Fix
- **Discord → MC Forwarding**: Messages from the Discord chat channel are now processed even when `linkChannelId` is not configured.

### 📚 Documentation
- **Web Panel Docs**: Added full Web panel documentation (EN/CZ).
- **Discord Docs**: Updated troubleshooting notes to match current behavior.

## [2.3.4] - 2025-12-28

### 🗳️ Vote System
- **Legacy Timestamp Support**: Removed the 24h age limit check for votes. This fixes compatibility with voting sites that send invalid timestamps (e.g., 1970).

### 📦 Build & Release
- **Discord Notification**: Updated release announcements to ping a specific role instead of `@everyone`.

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
