# VOIDIUM – INTELLIGENT SERVER CONTROL
## [DISCORD](https://discord.com/invite/3JYz3KWutJ)

**🚀 The Ultimate All-in-One Server Control System: Automate Everything, Monitor Everything, Control Everything – From Restarts to Ranks, Discord to Data, All Through One Powerful Web Panel.**

_Next-generation server management made with AI_

## 🎨 Modern In-Game Chat (Client-Side Optional)

> Install Voidium on the client to unlock the **Modern Chat Interface**:

*   **Discord-Style UI**: sleek glassmorphism design with channel tabs
*   **Private Ticket Channels**: Tickets open in a dedicated tab, separating support history from global chat
*   **Direct Messaging**: Create private DMs or Group Chats with a simple Wizard interface
*   **Emoji Rendering**: See Discord emojis directly in Minecraft chat


---

## 🔄 Restart System

*   Fixed-time, interval, delayed, and manual restarts
*   Boss bar countdown and structured warnings
*   Configurable restart types and schedules
*   Customizable warning messages with color codes

## 📢 Announcements

*   Scheduled and manual broadcasts
*   Color codes, formatting, custom prefix, hot reload
*   Multiple announcement support with intervals

## 🧹 Entity Cleaner (ClearLag Alternative)

*   **Automatic Cleanup**: Configurable interval (default 5 minutes)
*   **Warning System**: Alerts at 30s, 10s, 5s before cleanup
*   **Safe by Default**: Only removes items, XP orbs, and arrows - mobs are protected
*   **Entity Types**: Dropped items, XP orbs, arrows, passive mobs, hostile mobs
*   **Protection System**: 
    *   Named entities (name tags) protected by default
    *   Tamed animals protected by default
    *   Entity whitelist (villagers, iron golems, etc.)
    *   Item whitelist (diamonds, netherite, etc.)
*   **Commands**: `/voidium clear`, `/voidium clear items|mobs|xp|arrows`, `/voidium clear preview`
*   **Farm-Friendly**: Won't break mob farms or animal farms with default settings

## 📊 Monitoring & Utilities

*   Real-time TPS, MSPT, memory usage tracking
*   Player list with ping display
*   Server and mod info, public status
*   Live performance graphs (24-hour history)
*   Automated daily statistics reports

## 🌐 Web Control Panel

*   **React 19 + Vite 6 + TypeScript**: Modern SPA with 16 modular components, bundled into the JAR — no external dependencies
*   **Full Admin Dashboard**: Real-time server metrics, player list, alert banners, and performance graphs
*   **Config Studio**: Visual editor for all 12 config modules with preview, diff, apply, rollback, and locale presets
*   **Live Console**: Server log streaming with command execution (safety-allowlisted commands only)
*   **Live Chat Feed**: Real-time chat history viewer
*   **Scheduled Task Timeline**: Visual progress bars for restart, entity cleaner, and stats report timers
*   **SVG Performance Graphs**: 24-hour history charts for player count and TPS
*   **Secret Masking**: Sensitive fields (bot tokens, API keys) shown as `••••••` — never written back
*   **Audit Trail**: All actions logged to `web-audit.log` with timestamp and source
*   **3 Auth Methods**: Admin token (permanent), bootstrap token (one-time via `/voidium web`), session cookie (rolling TTL)
*   **Bilingual UI**: English and Czech interface via `web.json` `language` field

## 🤖 AI Admin Assistant

*   **Admin AI Chat**: Ask the AI about server status, config optimization, and player management
*   **AI Config Suggestions**: AI proposes config changes → diff preview → confirm before apply
*   **Per-Player Chat**: Players can talk to AI in-game with conversation history (max 16 turns per player)
*   **Incident Review**: One-click analysis of recent console + chat logs for anomalies and security concerns
*   **Moderation Guardrails**: Input filter blocks exploit/cheat keywords; response length & prompt size capped
*   **Access Gating**: 5 modes — ALL, PLAYTIME, DISCORD_ROLE, or combinations
*   **World / Game Mode Restrictions**: Disable AI in specific dimensions or game modes
*   **Secret Redaction**: API keys, tokens, and secrets are stripped from AI context

## 🛡️ Maintenance Mode

*   **Login Block**: Only OPs can join when maintenance mode is active — others get a bilingual disconnect message
*   **Dashboard Banner**: "⚠ MAINTENANCE MODE ACTIVE" with one-click toggle
*   **Web Panel Control**: Enable/disable from dashboard or Config Studio

## 👾 Discord Integration

*   **Whitelist System**: Require players to link their Discord account to join (configurable max accounts per Discord)
*   **Account Linking**: Secure code-based verification with automatic role assignment
*   **Rename on Link**: Optionally rename Discord nickname to in-game name upon linking (`renameOnLink` toggle)
*   **Smart Link Channel**: Responds to any message with link status, automatically processes verification codes
*   **Chat Bridge**: Two-way chat synchronization with emoji translation (:smile: -> 😄) and markdown formatting
*   **Smart Avatars**: Webhooks automatically resolve and display the correct player skin (even for offline-mode/restored skins)
*   **Console Streaming**: Stream server console logs directly to a Discord channel (batched for performance)
*   **Status Messages**: Automated server lifecycle announcements (Starting, Online, Stopping, Offline) with debug logging
*   **Channel Topic Updater**: Automatically updates channel topic with live stats (Online/Max players, Uptime)
*   **Ban Synchronization**: Bidirectional ban sync between game and Discord server (mixin-based, automatic)
*   **Event Embeds**: Player join/leave/death messages use colored Discord embeds with player skin thumbnails
*   **Daily Stats**: Automated performance reports with configurable labels and messages
*   **Role Sync**: Map Discord roles to in-game permissions
*   **Webhooks**: Support for logging events via Discord webhooks

## 🎫 Ticket System

*   **Discord Ticket Creation**: Players can create support tickets from Discord or in-game
*   **Private Channels**: Each ticket gets a private channel visible only to the creator and support team
*   **Secure Routing**: Messages from Discord ticket channels are routed **only** to the ticket creator (via private tab if client-mod installed, or chat if vanilla)
*   **Two-Way Communication**: Seamless conversation between Discord and In-Game
*   **Smart Command**: `/ticket <reason> <message>` - Creates ticket with initial message
*   **Automatic Notifications**: Pings user when ticket is created
*   **Support Role Integration**: Configurable support role with automatic channel access
*   **Ticket Limits**: Configurable max tickets per user to prevent abuse
*   **Easy Closing**: Close button for quick ticket resolution
*   **Auto-Assignment**: New tickets are automatically assigned to the support member with the fewest active tickets
*   **📄 Ticket Transcripts**: Automatically saves full conversation history (TXT or JSON format) when ticket closes
*   **Full Customization**: All messages and settings configurable via web panel or config files
*   **Bilingual**: Complete English and Czech translations

## 📈 Auto-Rank System

*   **Playtime Tracking**: Automatically tracks player activity (with AFK detection)
*   **Automatic Promotions**: Promotes players based on configurable playtime milestones
*   **🎯 Custom Conditions**: Set additional requirements beyond playtime:
    *   Kill any mobs (e.g., 100 kills for "Monster Hunter")
    *   Visit unique biomes (e.g., explore 10 biomes for "Explorer")
    *   Break any blocks (e.g., mine 1000 blocks for "Miner")
    *   Place any blocks (e.g., place 500 blocks for "Builder")
*   **Progress Tracking**: All player achievements automatically saved and persistent
*   **Custom Rewards**: Executes commands (e.g., permission group changes) upon promotion
*   **Flexible Configuration**: Support for prefix/suffix ranks with custom values
*   **Hover Tooltips**: Rank prefix/suffix shows played hours and required hours on hover
*   **Full RGB Colors**: Chat supports `&#RRGGBB` hex colors everywhere with automatic color reset

## 🎁 Vote Rewards (NuVotifier)

*   Accepts NuVotifier V2 token packets and legacy RSA V1 payloads simultaneously
*   Automatic handshake, signature validation, and configurable reward commands (`votes.json`)
*   **Pending vote queue** – offline votes are saved and delivered when player logs in (with silent delivery to prevent chat spam)
*   Auto-generated RSA keys and 16-character shared secret when missing
*   Dual logging: `votes.log` (plain text) + `votes-history.ndjson` (analytics)
*   Optional OP notifications and verbose diagnostics on listener failure
*   Admin commands: `/voidium votes pending [player]` · `/voidium votes clear`

## 🧍 Offline-Mode Skin Restorer

*   Early join injection (no relog required)
*   Persistent JSON cache, configurable TTL
*   Manual refresh, auto-disabled in online mode

## 🎁 Player List (TAB) Customization

*   **Custom Header & Footer**: Configure 3 lines each with live placeholders (%online%, %max%, %tps%, %ping%)
*   **Player Name Formatting**: Add rank prefixes/suffixes with full color code support
*   **Default Prefix/Suffix**: Fallback formatting for players without Discord roles
*   **Multiple Rank Modes**: Combine all applicable ranks or show only highest priority
*   **Web Panel Integration**: Full configuration through browser interface
*   **Live Updates**: Configurable refresh interval (minimum 3 seconds)

---

## ✅ Commands

### Operators
`/voidium reload` · `/voidium restart <minutes>` · `/voidium cancel` · `/voidium announce <message>` · `/voidium players` · `/voidium memory` · `/voidium config` · `/voidium skin <player>` · `/voidium votes pending [player]` · `/voidium votes clear` · `/voidium clear` · `/voidium clear items|mobs|xp|arrows` · `/voidium clear preview`

### Players
`/voidium status` · `/link` · `/ticket <reason> <message>`

---

## 🔧 Technical

*   **Minecraft**: 1.21.1-1.21.10
*   **Loader**: NeoForge 21.1.208+ (Java 21)
*   **Architecture**: Hybrid (Server-side core, Client-side optional for UI)
*   **License**: MIT
*   **Lightweight & modular**

---

## 📌 Configuration Files

All configuration files are stored in `config/voidium/`:

| File | Description |
|------|-------------|
| `general.json` | Master toggles for all modules |
| `restart.json` | Restart schedules and messages |
| `announcements.json` | Broadcast messages and intervals |
| `discord.json` | Discord bot settings, chat bridge, whitelist |
| `stats.json` | Daily statistics reports |
| `ranks.json` | Auto-rank milestones and conditions |
| `votes.json` | Vote rewards and NuVotifier settings |
| `tickets.json` | Ticket system messages and limits |
| `playerlist.json` | TAB header/footer customization |
| `entitycleaner.json` | Entity cleanup settings and whitelists |
| `web.json` | Web panel port and authentication |

### Storage Files (in `config/voidium/storage/`):
| File | Description |
|------|-------------|
| `links.json` | Discord-Minecraft account links |
| `pending-votes.json` | Offline vote queue |
| `votes.log` | Vote history (plain text) |
| `votes-history.ndjson` | Vote analytics |
| `voidium_stats_data.json` | Player statistics |
| `voidium_ranks_data.json` | Rank assignments |
| `player_progress.json` | Achievement progress |
| `skin-cache.json` | Cached player skins |
| `last_restart.txt` | Last restart timestamp |

---

**One mod. Complete control. Zero complexity.**
