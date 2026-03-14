---
layout: default
title: Troubleshooting (EN)
---

# 🔧 Troubleshooting

<div class="hero">
   <p>Common problems and solutions for <strong>Voidium</strong>. Check the relevant section below before opening a support ticket.</p>

   <h2>Quick Navigation</h2>
   <div class="card-grid">
      <a class="card" href="#general">
         <div class="card-title"><span class="card-icon">⚙️</span>General</div>
         <div class="card-desc">Mod won't load, config errors</div>
      </a>
      <a class="card" href="#discord">
         <div class="card-title"><span class="card-icon">🤖</span>Discord Bot</div>
         <div class="card-desc">Bot offline, linking fails</div>
      </a>
      <a class="card" href="#web-panel">
         <div class="card-title"><span class="card-icon">🌐</span>Web Panel</div>
         <div class="card-desc">Can't connect, auth issues</div>
      </a>
      <a class="card" href="#skins">
         <div class="card-title"><span class="card-icon">🎨</span>Skins</div>
         <div class="card-desc">Skins not loading, Steve head</div>
      </a>
      <a class="card" href="#votes">
         <div class="card-title"><span class="card-icon">🗳️</span>Votes</div>
         <div class="card-desc">Votes not arriving, RSA errors</div>
      </a>
      <a class="card" href="#performance">
         <div class="card-title"><span class="card-icon">📊</span>Performance</div>
         <div class="card-desc">TPS drops, lag</div>
      </a>
   </div>
</div>

---

## ⚙️ General {#general}

### Mod doesn't load / crash on startup

| Symptom | Cause | Solution |
|---------|-------|----------|
| `ClassNotFoundException: cz.voidium...` | Wrong MC/NeoForge version | Verify MC 1.21.1–1.21.10, NeoForge 21.1.208+ |
| `UnsupportedClassVersionError` | Java version too old | Install **Java 21** or newer |
| `JsonSyntaxException` in config | Malformed config JSON | Delete the broken config file and restart — defaults will regenerate |
| Mod loads but all features disabled | `enableMod: false` in general.json | Set `enableMod: true` |

### Config not saving

- Config files are in `config/voidium/`. Make sure the server process has **write permission** to this directory.
- After manual edits, use `/voidium reload` to hot-reload configs.
- The web panel Config Studio creates a **backup** before each change — check `config/voidium/backups/` if you need to revert.

### Commands not working

- All commands require **OP level 2** or higher (permission level 2).
- Check that the specific module is enabled in `general.json`.
- Use `/voidium help` to see all available commands.

---

## 🤖 Discord Bot {#discord}

### Bot doesn't come online

| Symptom | Cause | Solution |
|---------|-------|----------|
| `LOGIN_FAILED` in logs | Invalid bot token | Regenerate token in [Discord Developer Portal](https://discord.com/developers/applications) and update `discord.json` |
| Bot starts but no slash commands | Missing guild ID | Set `guildId` in `discord.json` to your Discord server ID |
| `Missing Access` errors | Insufficient bot permissions | Grant the bot **Administrator** or at minimum: Send Messages, Manage Channels, Manage Roles, Read Message History |
| Bot online but ignores commands | Wrong `linkChannelId` | Verify channel IDs in `discord.json` — use Discord Developer Mode to copy IDs |

### Account linking fails

- Player must be **online** in Minecraft when running `/link` in Discord.
- The 6-digit code expires after **10 minutes** — get a new one in-game.
- Check `maxAccountsPerDiscord` — if set to 1, the Discord user must `/unlink` first.
- The bot must have access to the `linkChannelId` channel.

### Chat bridge not working

- Ensure `enableChatBridge: true` in `discord.json`.
- Verify `chatChannelId` is set correctly.
- For webhook mode (skin avatars): create a webhook in the chat channel and paste the URL in `chatWebhookUrl`.

### Status messages not appearing

- Set `enableStatusMessages: true` in `discord.json`.
- If `statusChannelId` is empty, status messages use the `chatChannelId`.

---

## 🌐 Web Panel {#web-panel}

### Can't connect to web panel

| Symptom | Cause | Solution |
|---------|-------|----------|
| Connection refused | Web module disabled | Set `enableWeb: true` in `general.json` |
| Connection timeout | Port blocked by firewall | Open port `8081` (or your custom port) in firewall / hosting panel |
| Page loads but blank | JavaScript error | Clear browser cache, try incognito mode |
| `ERR_CONNECTION_RESET` | `bindAddress` mismatch | Use `0.0.0.0` to listen on all interfaces |

### Authentication issues

- **"Invalid token"**: The admin token may have been regenerated. Check `web.json` for the current `adminToken`.
- **Bootstrap token expired**: Bootstrap tokens are **one-time, 10 minutes**. Generate a new one with `/voidium web`.
- **Session expired**: Default session TTL is 120 minutes. Increase `sessionTtlMinutes` if needed.
- **Cookie not set**: Ensure you're accessing via HTTP (not HTTPS unless you have a reverse proxy configured). The `voidium_session` cookie requires proper domain matching.

### Console not showing output

- The console feed uses Server-Sent Events (SSE). Some reverse proxies buffer SSE — configure your proxy to disable buffering for `/api/feeds`.
- Check that `consoleChannelId` is set in `discord.json` (the web console shares the same Log4j appender).

---

## 🎨 Skins {#skins}

### Skins not loading (Steve/Alex fallback)

| Symptom | Cause | Solution |
|---------|-------|----------|
| All players have Steve skin | SkinRestorer disabled | Set `enableSkinRestorer: true` in `general.json` |
| Skins load after a delay | Cache miss on first join | Normal — skin is fetched from Mojang API on first join and cached for `skinCacheHours` (default 24) |
| Skins work but sometimes reset | Cache expired | Increase `skinCacheHours` in `general.json` |
| `Connection timed out` in logs | Mojang API unreachable | Check server internet connectivity. Skins will load from cache if available. |

### SkinRestorer only works in offline mode

This is by design. In online mode, Minecraft handles skins natively. SkinRestorer only activates when `server.properties` has `online-mode=false`.

---

## 🗳️ Votes {#votes}

### Votes not arriving

| Symptom | Cause | Solution |
|---------|-------|----------|
| No vote events at all | Vote module disabled | Set `enableVote: true` in `general.json` |
| Voting site says "server offline" | Wrong port / not reachable | Ensure the Votifier port (config `port`, default `8192`) is open and forwarded |
| `Invalid RSA key` errors | Corrupted keys | Delete `RSA/` folder in config and restart — new keys will be generated |
| `Invalid shared secret` | V2 protocol mismatch | Verify `sharedSecret` in `vote.json` matches the token on the voting site |
| Votes arrive but no reward | Empty `commands` list | Add reward commands to the `commands` array in `vote.json` |

### Setting up for the first time

1. Enable vote module in `general.json`
2. Start the server — RSA keys will generate automatically
3. On the voting site, use **Votifier** protocol and enter your server IP + vote port
4. For V2 sites, also paste the `sharedSecret` from `vote.json`
5. Test with the voting site's test button

---

## 📊 Performance {#performance}

### TPS drops

- **Entity Cleaner:** If TPS drops correlate with many entities, enable Entity Cleaner and tune the interval.
- **Chat Bridge:** Webhook mode has slightly more overhead than regular mode. If TPS is critical, use regular chat bridge.
- **Stats:** The stats reporter runs daily and takes minimal resources. It should not cause TPS issues.
- **AI features:** AI calls are async and do not block the server tick. However, many simultaneous AI requests could impact network.

### High memory usage

- Check `skinCacheHours` — a very long TTL with many unique players can grow the skin cache.
- Entity Cleaner helps control entity count, which directly affects memory.
- The web panel's thread pool scales dynamically — under heavy load it creates more threads.

---

## 📋 Getting Help {#getting-help}

If your issue isn't listed above:

1. Check the **server log** (`logs/latest.log`) for error messages from `[Voidium]`
2. Check the **web panel audit log** for recent changes
3. Try `/voidium reload` to re-read all configs
4. Delete the specific config file and restart to regenerate defaults
5. Open a ticket in our Discord server

## Related Pages

- <a href="Config_EN.html">Configuration</a> — All config files reference
- <a href="Commands_EN.html">Commands</a> — Full command list
- <a href="Install_EN.html">Installation</a> — Setup guide
