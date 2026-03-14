---
layout: default
title: Playerlist (EN)
---

# 🧍 Playerlist (TAB)

<div class="hero">
   <p><strong>Player List</strong> customizes the TAB menu with configurable header, footer, and player name formatting. Supports live placeholders, rank prefixes/suffixes from both Discord roles and the Auto‑Rank system.</p>

   <div class="note">
      To enable: set <code>enablePlayerList: true</code> in <code>general.json</code>. Configure in <code>playerlist.json</code>.
   </div>

   <h2>Jump to</h2>
   <div class="card-grid">
      <a class="card" href="#header-footer">
         <div class="card-title"><span class="card-icon">📋</span>Header & Footer</div>
         <div class="card-desc">3 lines each with placeholders</div>
      </a>
      <a class="card" href="#player-names">
         <div class="card-title"><span class="card-icon">👤</span>Player names</div>
         <div class="card-desc">Prefixes, suffixes, formatting</div>
      </a>
      <a class="card" href="#config">
         <div class="card-title"><span class="card-icon">⚙️</span>Configuration</div>
         <div class="card-desc">playerlist.json fields</div>
      </a>
   </div>
</div>

## 📋 Header & Footer {#header-footer}

The TAB list header and footer each have 3 configurable lines. Use `§` color codes and placeholders:

### Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%online%` | Current online player count |
| `%max%` | Maximum player slots |
| `%tps%` | Server TPS |
| `%ping%` | Player's ping in ms |
| `%playtime%` | Player's playtime in hours |
| `%time%` | Current server time |
| `%memory%` | JVM memory usage |

### Default header

```
§b§l✦ VOIDIUM SERVER ✦
§7Online: §a%online%§7/§a%max%
```

### Default footer

```
§7TPS: §a%tps%
§7Ping: §e%ping%ms
```

## 👤 Player names {#player-names}

### Name format

Player names in TAB are formatted using `playerNameFormat`:

```
%rank_prefix%%player_name%%rank_suffix%
```

### Rank sources

Voidium combines ranks from multiple sources:

1. **Discord roles** — prefix/suffix from `rolePrefixes` in `discord.json`
2. **Time-based ranks** — from the Auto-Rank system (`ranks.json`)

When `combineMultipleRanks` is `true`, all applicable prefixes/suffixes are combined. When `false`, only the highest priority is used.

### Default prefix/suffix

Players without any rank get `defaultPrefix` and `defaultSuffix` (default: `§7` gray and empty).

## ⚙️ Configuration {#config}

File: <code>config/voidium/playerlist.json</code>

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enableCustomPlayerList` | boolean | `true` | Master switch |
| `headerLine1` | string | `§b§l✦ VOIDIUM SERVER ✦` | Header line 1 |
| `headerLine2` | string | `§7Online: §a%online%§7/§a%max%` | Header line 2 |
| `headerLine3` | string | `""` | Header line 3 |
| `footerLine1` | string | `§7TPS: §a%tps%` | Footer line 1 |
| `footerLine2` | string | `§7Ping: §e%ping%ms` | Footer line 2 |
| `footerLine3` | string | `""` | Footer line 3 |
| `enableCustomNames` | boolean | `true` | Enable player name formatting |
| `playerNameFormat` | string | `%rank_prefix%%player_name%%rank_suffix%` | Name format template |
| `defaultPrefix` | string | `§7` | Fallback prefix |
| `defaultSuffix` | string | `""` | Fallback suffix |
| `combineMultipleRanks` | boolean | `true` | Combine all ranks or use highest only |
| `updateIntervalSeconds` | int | `5` | Update interval (min 3) |

## Related

- <a href="Ranks_EN.html">Auto-Rank</a> — time-based rank system
- <a href="Discord_EN.html">Discord</a> — role prefixes
- <a href="Config_EN.html">Configuration</a>
