---
layout: default
title: Localization (EN)
---

# 🌍 Localization

<div class="hero">
   <p><strong>Voidium</strong> supports <strong>English</strong> and <strong>Czech</strong> locale presets. Every user-facing message can be customized per config file, and one-click locale presets let you switch all messages at once.</p>

   <div class="note">
      Locale presets change <strong>config field values</strong> (messages, labels, prefixes). They do not affect the mod's code or commands — commands are always in English.
   </div>

   <h2>Quick Navigation</h2>
   <div class="card-grid">
      <a class="card" href="#how-it-works">
         <div class="card-title"><span class="card-icon">⚙️</span>How It Works</div>
         <div class="card-desc">Locale presets, per-module messages</div>
      </a>
      <a class="card" href="#applying-locale">
         <div class="card-title"><span class="card-icon">🔄</span>Applying a Locale</div>
         <div class="card-desc">Web panel, commands, config</div>
      </a>
      <a class="card" href="#placeholders">
         <div class="card-title"><span class="card-icon">📝</span>Placeholders</div>
         <div class="card-desc">Dynamic values in messages</div>
      </a>
      <a class="card" href="#custom-messages">
         <div class="card-title"><span class="card-icon">✏️</span>Custom Messages</div>
         <div class="card-desc">Override individual messages</div>
      </a>
   </div>
</div>

---

## ⚙️ How It Works {#how-it-works}

Voidium does not use a separate language file. Instead, every message is a **config field** in its respective config file (e.g., `discord.json`, `restart.json`, `tickets.json`).

The `LocalePresets` class provides complete sets of translated values for each module:

| Module | Config File | Translated Fields |
|--------|-------------|-------------------|
| General | `general.json` | `modPrefix` |
| Discord | `discord.json` | 20+ fields: kick messages, link messages, bot responses, status messages |
| Announcements | `announcements.json` | `prefix`, `announcements[]` |
| Ranks | `ranks.json` | `promotionMessage` |
| Votes | `vote.json` | `announcementMessage` |
| Tickets | `tickets.json` | 11 fields: ticket created/welcome/close messages, MC messages |
| Restart | `restart.json` | `warningMessage`, `restartingNowMessage`, `kickMessage` |
| Entity Cleaner | `entitycleaner.json` | `warningMessage`, `cleanupMessage` |
| Stats | `stats.json` | `reportTitle`, `reportPeakLabel`, `reportAverageLabel`, `reportFooter` |
| Player List | `playerlist.json` | `headerLine1–3`, `footerLine1–3` |

### Supported Locales

| Code | Language |
|------|----------|
| `en` | English (default) |
| `cz` | Czech |

---

## 🔄 Applying a Locale {#applying-locale}

### Via Web Panel (Recommended)

1. Open the **Config Studio** in the web panel
2. Use the **Locale Preset** dropdown (or call `/api/config/locale`)
3. Select `en` or `cz`
4. All modules' messages will be updated and saved immediately

### Via In-Game Command

```
/voidium locale <en|cz>
```

This calls `applyLocale()` on every config class, overwriting all message fields and saving each config file.

### Manually

Edit individual config files in `config/voidium/` and change specific message fields. This gives you full control — you can mix languages or write completely custom messages.

<div class="note">
   <strong>Important:</strong> Applying a locale preset <strong>overwrites</strong> any custom message edits. If you've customized messages, back them up before applying a preset.
</div>

---

## 📝 Placeholders {#placeholders}

Messages support dynamic placeholders using the `%name%` syntax. Each message field has its own set of available placeholders:

### Common Placeholders

| Placeholder | Available In | Value |
|-------------|-------------|-------|
| `%player%` | Discord, Ranks, Votes | Minecraft player name |
| `%user%` | Discord chat bridge | Discord username |
| `%message%` | Chat bridge | Chat message text |
| `%code%` | Discord whitelist | 6-digit linking code |
| `%max%` | Discord linking | Max accounts per Discord user |
| `%online%` | Channel topic | Online player count |
| `%uptime%` | Channel topic | Server uptime |
| `%days%`, `%hours%`, `%minutes%` | Uptime format | Uptime components |
| `%rank%` | Ranks | Rank name |
| `%time%` | Restart warning | Time until restart |
| `%count%` | Entity Cleaner | Number of entities removed |
| `%voter%` | Votes | Voter's player name |

Placeholders are replaced at runtime via `String.replace()`. If a placeholder isn't replaced, it appears as-is in the message — this helps debug typos.

---

## ✏️ Custom Messages {#custom-messages}

You can customize any message by editing the config field directly:

### Example: Custom restart warning

In `restart.json`:
```json
{
  "warningMessage": "&c&l⚠ SERVER RESTART in %time%! Save your builds!",
  "restartingNowMessage": "&4Server is restarting NOW!",
  "kickMessage": "Server is restarting. Please reconnect in a moment."
}
```

### Example: Custom Discord link success

In `discord.json`:
```json
{
  "linkSuccessMessage": "✅ Successfully linked to **%player%**! Welcome aboard."
}
```

### Color Codes

All in-game messages support `&` color codes:

| Code | Result | Code | Result |
|------|--------|------|--------|
| `&0`–`&9` | Colors (black–blue) | `&l` | **Bold** |
| `&a`–`&f` | Colors (green–white) | `&o` | *Italic* |
| | | `&n` | Underline |
| | | `&r` | Reset |

### Web Panel Language

The web panel UI language is controlled separately via `web.json`:

```json
{
  "language": "en"
}
```

Set to `"cz"` for Czech UI labels in the Config Studio and dashboard.

---

## Related Pages

- <a href="Config_EN.html">Configuration</a> — All config files reference
- <a href="Web_EN.html">Web Control Panel</a> — Config Studio documentation
- <a href="Discord_EN.html">Discord</a> — Discord bot messages
