# VOIDIUM – SERVER MANAGER

Simple and powerful NeoForge server management: automated restarts, announcements, vote rewards, live performance metrics, and offline-mode skin restoration.

_Mod made with AI_

## 🔄 Restart System

*   Fixed-time, interval, delayed, and manual restarts
*   Boss bar countdown and structured warnings

## 📢 Announcements

*   Scheduled and manual broadcasts
*   Color codes, formatting, custom prefix, hot reload

## 📊 Monitoring & Utilities

*   TPS, MSPT, memory usage
*   Player list with ping
*   Server and mod info, public status

## 🌐 Web Control Panel

*   **Full Configuration Management**: Edit all mod configs (Discord, Votes, Restarts, Ranks, etc.) directly from your browser.
*   **Player Management**: View online and offline players, kick/ban users, and manage Discord links.
*   **Localization**: Fully translated into English and Czech.
*   **Secure Access**: Token-based authentication via console link.
*   **Smart Editor**: User-friendly form-based editor with descriptions and "Reset to Default" options.

## 👾 Discord Integration

*   **Whitelist System**: Require players to link their Discord account to join.
*   **Account Linking**: Secure code-based verification (`/link <code>`).
*   **Chat Bridge**: Two-way chat synchronization between Minecraft and Discord.
*   **Ban Synchronization**: Sync bans between game and Discord server.
*   **Daily Stats**: Automated performance reports sent to a specific channel.
*   **Webhooks**: Support for logging events via webhooks.

## 📈 Auto-Rank System

*   **Playtime Tracking**: Automatically tracks player activity.
*   **Automatic Promotions**: Promotes players based on configurable playtime milestones.
*   **Custom Rewards**: Executes commands (e.g., permission group changes) upon promotion.

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

## ✅ Commands (Operators)

`/voidium restart <minutes>` · `/voidium announce <message>` · `/voidium players` · `/voidium memory` · `/voidium cancel` · `/voidium config` · `/voidium reload` · `/voidium skin <player>` · `/voidium votes pending [player]` · `/voidium votes clear`  
Players: `/voidium status`

## 🔧 Technical

*   Minecraft: 1.21.1
*   Loader: NeoForge
*   Server-only (clients not required)
*   License: MIT
*   Lightweight & modular

## 📌 Notes

*   `skinCacheHours` below 1 is forced to 1
*   Expired cache entries refresh at next login
*   Safe in online mode (skin feature auto-skips)
*   `votes.json` lives in `config/voidium/` with generated shared secret + RSA keys
*   Offline votes are queued in `pending-votes.json` and delivered on player login

**Professional server control, zero hassle.**
