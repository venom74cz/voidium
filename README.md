# VOIDIUM – INTELLIGENT SERVER CONTROL
## DISCORD: https://discord.com/invite/3JYz3KWutJ

**🚀 The Ultimate All-in-One Server Control System: Automate Everything, Monitor Everything, Control Everything – From Restarts to Ranks, Discord to Data, All Through One Powerful Web Panel.**

_Next-generation server management made with AI_

## 🔄 Restart System

*   Fixed-time, interval, delayed, and manual restarts
*   Boss bar countdown and structured warnings
*   Configurable restart types and schedules

## 📢 Announcements

*   Scheduled and manual broadcasts
*   Color codes, formatting, custom prefix, hot reload
*   Multiple announcement support with intervals

## 📊 Monitoring & Utilities

*   Real-time TPS, MSPT, memory usage tracking
*   Player list with ping display
*   Server and mod info, public status
*   Live performance graphs (24-hour history)
*   Automated daily statistics reports

## 🌐 Web Control Panel

*   **Full Configuration Management**: Edit ALL mod configs (Discord, Votes, Restarts, Ranks, Tickets, Stats, PlayerList) directly from your browser
*   **Player Management**: View online and offline players, kick/ban users, and manage Discord links
*   **Live Statistics**: Real-time graphs showing player count and TPS over 24 hours
*   **Localization**: Fully translated into English and Czech with "Reset to Default" functionality
*   **Secure Access**: Token-based authentication via console link
*   **Smart Editor**: User-friendly form-based editor with field descriptions and validation
*   **Dynamic Updates**: Add/remove list items, manage complex configurations without file editing
*   **🎨 Enhanced UX**: Collapsible config sections, sticky save bar, smooth animations
*   **🎭 Discord Role Prefix Designer**: Live color preview, hex color picker, validation, templates, copy/paste between roles

## 👾 Discord Integration

*   **Whitelist System**: Require players to link their Discord account to join (configurable max accounts per Discord)
*   **Account Linking**: Secure code-based verification with automatic role assignment
*   **Smart Link Channel**: Responds to any message with link status, automatically processes verification codes
*   **Chat Bridge**: Two-way chat synchronization with emoji translation and markdown formatting
*   **Console Streaming**: Stream server console logs directly to a Discord channel (batched for performance)
*   **Status Messages**: Automated server lifecycle announcements (Starting, Online, Stopping, Offline) with debug logging
*   **Channel Topic Updater**: Automatically updates channel topic with live stats (Online/Max players, Uptime)
*   **Ban Synchronization**: Bidirectional ban sync between game and Discord server
*   **Daily Stats**: Automated performance reports with peak and average player counts
*   **Role Sync**: Map Discord roles to in-game permissions
*   **Webhooks**: Support for logging events via Discord webhooks

## 🎫 Ticket System

*   **Discord Ticket Creation**: Players can create support tickets from Discord or in-game
*   **Private Channels**: Each ticket gets a private channel visible only to the creator and support team
*   **Two-Way Communication**: Messages in ticket channels are relayed to the linked player in-game
*   **Smart Command**: `/ticket <reason> <message>` - Creates ticket with initial message
*   **Automatic Notifications**: Pings user when ticket is created
*   **Support Role Integration**: Configurable support role with automatic channel access
*   **Ticket Limits**: Configurable max tickets per user to prevent abuse
*   **Easy Closing**: Close button for quick ticket resolution
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

## ✅ Commands

### Operators
`/voidium restart <minutes>` · `/voidium announce <message>` · `/voidium players` · `/voidium memory` · `/voidium cancel` · `/voidium config` · `/voidium reload` · `/voidium skin <player>` · `/voidium votes pending [player]` · `/voidium votes clear`

### Players
`/voidium status` · `/link` · `/ticket <reason> <message>`

## 🔧 Technical

*   Minecraft: 1.21.1-1.21.10
*   Loader: NeoForge (java21)
*   Server-only (clients not required)
*   License: MIT
*   Lightweight & modular

## 📌 Notes

*   All configuration files are stored in `config/voidium/` directory
*   Discord Channel IDs and Role IDs are properly handled as strings (no precision loss)
*   `skinCacheHours` below 1 is forced to 1
*   Expired cache entries refresh at next login
*   Safe in online mode (skin feature auto-skips)
*   `votes.json` includes generated shared secret + RSA keys
*   Offline votes are queued in `pending-votes.json` and delivered on player login
*   Discord-Minecraft links are stored in `links.json`
*   Statistics data is saved in `voidium_stats_data.json`
*   Web panel accessible via token link printed to console on server start


**One mod. Complete control. Zero complexity.**
