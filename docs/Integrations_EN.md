---
layout: default
title: Integrations (EN)
---

# 🔗 Integrations

<div class="hero">
   <p><strong>Voidium</strong> integrates with external systems — a Discord bot via <strong>JDA</strong>, a built-in <strong>Web Control Panel</strong>, and a <strong>Mixin</strong> hook into vanilla Minecraft code. This page explains how each integration works and how to configure it.</p>

   <h2>Quick Navigation</h2>
   <div class="card-grid">
      <a class="card" href="#discord">
         <div class="card-title"><span class="card-icon">🤖</span>Discord Bot</div>
         <div class="card-desc">JDA bot, chat bridge, linking, tickets</div>
      </a>
      <a class="card" href="#web-panel">
         <div class="card-title"><span class="card-icon">🌐</span>Web Control Panel</div>
         <div class="card-desc">Dashboard, Config Studio, console, AI</div>
      </a>
      <a class="card" href="#mixins">
         <div class="card-title"><span class="card-icon">🧩</span>Mixins</div>
         <div class="card-desc">Vanilla code hooks for SkinRestorer</div>
      </a>
   </div>
</div>

---

## 🤖 Discord Bot {#discord}

Voidium includes a fully-featured Discord bot powered by **JDA (Java Discord API)**. It runs inside the Minecraft server process — no external bot hosting needed.

### Features

| Feature | Description |
|---------|-------------|
| **Account Linking** | Players link MC accounts with a 6-digit code (`/link` in Discord). 10-minute TTL. Configurable max accounts per Discord user. |
| **Chat Bridge** | Bidirectional MC ↔ Discord message relay with Markdown conversion, emoji mapping, and optional webhook mode (player skin avatars). |
| **Ticket System** | Players create support tickets via Discord button → private channel. In-game `/ticket` command. Transcripts in TXT + JSON. |
| **Whitelist** | Freeze-based verification — unlinked players can't move or interact until they link via Discord. |
| **Console Channel** | Live server console output streamed to a Discord channel via a Log4j appender. ANSI colors stripped, messages batched. |
| **Server Status** | Join/leave/death messages. Channel topic updates with online count, max players, uptime. Configurable activity status (PLAYING/WATCHING/LISTENING/COMPETING). |
| **Ban Sync** | Discord ban → MC ban (enabled by default). MC → Discord ban sync (configurable, off by default). |
| **Role Management** | Auto-assign a linked role. Role-based prefixes with hex color support and priority ordering. |

### Slash Commands

| Command | Description |
|---------|-------------|
| `/link <code>` | Link Discord account to MC using the 6-digit code |
| `/unlink` | Unlink your MC account |
| `/ticket create` | Create a new support ticket |
| `/ticket close` | Close the current ticket channel |

### Key Config Fields (`discord.json`)

| Field | Type | Description |
|-------|------|-------------|
| `botToken` | string | Discord bot token |
| `guildId` | string | Target Discord server ID |
| `chatChannelId` | string | Channel for MC ↔ Discord chat |
| `consoleChannelId` | string | Channel for console output |
| `linkChannelId` | string | Channel where `/link` is used |
| `statusChannelId` | string | Channel for join/leave/status (falls back to chat channel) |
| `linkedRoleId` | string | Role assigned when account is linked |
| `enableWhitelist` | bool | Freeze unlinked players |
| `enableChatBridge` | bool | Enable chat relay |
| `chatWebhookUrl` | string | Webhook URL for skin-avatar chat mode |
| `syncBansDiscordToMc` | bool | Sync Discord bans to MC |
| `rolePrefixes` | map | Discord Role ID → `{prefix, suffix, color, priority}` |

For detailed Discord docs, see <a href="Discord_EN.html">Discord</a>.

---

## 🌐 Web Control Panel {#web-panel}

Voidium ships a single-page web application served by the JDK's built-in `HttpServer` — no Netty, Jetty, or external web server required.

### Architecture

- **Port:** `WebConfig.port` (default `8081`)
- **Bind address:** `WebConfig.bindAddress` (default `0.0.0.0`)
- **Thread pool:** `CachedThreadPool` — scales automatically
- **Session cleanup:** Scheduled every 5 minutes

### Authentication

Three authentication methods, in order of precedence:

| Method | How it works |
|--------|-------------|
| **Admin Token** | Permanent token (auto-generated UUID) — append `?token=<adminToken>` to URL |
| **Bootstrap Token** | One-time, 10-minute token generated via `/voidium web` command — consumed on first use |
| **Session Cookie** | `voidium_session` HTTP-only cookie with rolling TTL (default 120 minutes) |

### API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/dashboard` | GET | Real-time metrics, player list, scheduled timers, alerts |
| `/api/feeds` | GET | Chat, console, and audit log history |
| `/api/action` | POST | Server actions (restart, announce, entity clean, etc.) |
| `/api/ai/admin` | POST | Admin AI chat assistant |
| `/api/ai/admin/suggest` | POST | AI config suggestions |
| `/api/ai/players` | GET | Player AI chat history |
| `/api/config/schema` | GET | Config structure metadata |
| `/api/config/values` | GET | Current config values |
| `/api/config/defaults` | GET | Default values |
| `/api/config/locale` | POST | Apply locale preset |
| `/api/config/preview` | POST | Preview changes before applying |
| `/api/config/diff` | POST | Diff current vs proposed config |
| `/api/config/apply` | POST | Apply config changes (creates backup) |
| `/api/config/rollback` | POST | Revert to backup |
| `/api/config/reload` | POST | Hot-reload config from files |
| `/api/console/execute` | POST | Execute server commands |
| `/api/discord/roles` | GET | List Discord roles for mapping |
| `/api/logout` | POST | Clear session |

### Key Config Fields (`web.json`)

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `port` | int | `8081` | HTTP port |
| `bindAddress` | string | `0.0.0.0` | Network interface to bind |
| `publicHostname` | string | `localhost` | Hostname shown in bootstrap URLs |
| `adminToken` | string | *(auto)* | Permanent admin authentication token |
| `sessionTtlMinutes` | int | `120` | Session cookie TTL |
| `language` | string | `en` | Web UI language (`en` or `cz`) |

For detailed Web docs, see <a href="Web_EN.html">Web Control Panel</a>.

---

## 🧩 Mixins {#mixins}

Voidium uses **Mixin** to hook into vanilla Minecraft code where NeoForge events are insufficient.

### Registered Mixins

Currently one mixin is registered in `voidium.mixins.json`:

| Mixin Class | Target | Purpose |
|-------------|--------|---------|
| `PlayerListMixin` | `net.minecraft.server.players.PlayerList` | SkinRestorer safety net |

### PlayerListMixin

**Hook point:** `placeNewPlayer()` method, just before `broadcastAll()` sends the player info packet to all clients.

**What it does:**
1. Checks if the server is in **offline mode** (skips in online mode)
2. Checks if **SkinRestorer** is enabled in `general.json`
3. If the player's `GameProfile` has no texture properties, calls `EarlySkinInjector.fetchAndApply()`
4. This ensures the player's skin is set **before** their appearance is broadcast to other players

**Why Mixin?** NeoForge's `PlayerLoggedInEvent` fires *after* the player's info has already been broadcast. The mixin injects *before* broadcast, guaranteeing all clients see the correct skin from the first moment.

### Compatibility

- **Mixin version:** 0.8+
- **Java compatibility:** Java 21
- **`required: false`** — the mod loads even if the mixin fails to apply (graceful degradation)

---

## Related Pages

- <a href="Discord_EN.html">Discord</a> — Full Discord bot documentation
- <a href="Web_EN.html">Web Control Panel</a> — Full web panel documentation
- <a href="SkinRestorer_EN.html">SkinRestorer</a> — Skin system documentation
- <a href="Config_EN.html">Configuration</a> — All config file reference
