---
layout: default
title: Announcements (EN)
---

# 📢 Announcements

<div class="hero">
   <p><strong>Announcements</strong> broadcast rotating messages to all online players at a configurable interval. Supports color codes and a custom prefix.</p>

   <div class="note">
      To enable: set <code>enableAnnouncements: true</code> in <code>general.json</code>. Configure messages in <code>announcements.json</code>.
   </div>

   <h2>Jump to</h2>
   <div class="card-grid">
      <a class="card" href="#how-it-works">
         <div class="card-title"><span class="card-icon">⚙️</span>How it works</div>
         <div class="card-desc">Rotation, intervals, prefix</div>
      </a>
      <a class="card" href="#config">
         <div class="card-title"><span class="card-icon">📝</span>Configuration</div>
         <div class="card-desc">announcements.json fields</div>
      </a>
      <a class="card" href="#commands">
         <div class="card-title"><span class="card-icon">⌨️</span>Commands</div>
         <div class="card-desc">Manual broadcast</div>
      </a>
   </div>
</div>

## ⚙️ How it works {#how-it-works}

1. Voidium loads the list of announcements from config
2. Every `announcementIntervalMinutes` (default 30), the next message in the list is broadcast to all online players
3. Messages rotate in order — after the last message, it loops back to the first
4. Each message is prefixed with the configurable `prefix`

## 📝 Configuration {#config}

File: <code>config/voidium/announcements.json</code>

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `announcements` | string[] | `["&bWelcome to the server!", "&eDon't forget to visit our website!"]` | List of messages to rotate |
| `announcementIntervalMinutes` | int | `30` | Minutes between broadcasts (0 = disabled) |
| `prefix` | string | `&8[&bVoidium&8]&r ` | Prefix prepended to every message |

### Example config

```json
{
  "announcements": [
    "&bWelcome to the server!",
    "&eDon't forget to visit our website!",
    "&aUse /ticket to contact staff!"
  ],
  "announcementIntervalMinutes": 15,
  "prefix": "&8[&bServer&8]&r "
}
```

### Color codes

Use `&` for color codes:

| Code | Color | Code | Style |
|------|-------|------|-------|
| `&0`–`&9` | Black to Blue | `&l` | **Bold** |
| `&a`–`&f` | Green to White | `&o` | *Italic* |
| | | `&n` | Underline |
| | | `&r` | Reset |

## ⌨️ Commands {#commands}

| Command | Permission | Description |
|---------|-----------|-------------|
| `/voidium announce <message>` | OP | Send a one-time broadcast to all players |

The manual broadcast uses the configured `prefix`.

## Related

- <a href="Commands_EN.html">Commands</a>
- <a href="Config_EN.html">Configuration</a>
