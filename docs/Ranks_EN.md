---
layout: default
title: Auto‑Rank (EN)
---

# 🏅 Auto‑Rank

<div class="hero">
   <p><strong>Auto‑Rank</strong> promotes players based on playtime and optional custom conditions (mob kills, biome visits, block mining/placing). Each rank can assign a prefix or suffix with full color code &amp; RGB support.</p>

   <div class="note">
      To enable: set <code>enableRanks: true</code> in <code>general.json</code>. Then customize ranks in <code>ranks.json</code>.
   </div>

   <h2>Jump to</h2>
   <div class="card-grid">
      <a class="card" href="#how-it-works">
         <div class="card-title"><span class="card-icon">⚙️</span>How it works</div>
         <div class="card-desc">Playtime, conditions, promotions</div>
      </a>
      <a class="card" href="#config">
         <div class="card-title"><span class="card-icon">📝</span>Configuration</div>
         <div class="card-desc">ranks.json fields</div>
      </a>
      <a class="card" href="#custom-conditions">
         <div class="card-title"><span class="card-icon">🎯</span>Custom conditions</div>
         <div class="card-desc">KILL, VISIT, BREAK, PLACE</div>
      </a>
      <a class="card" href="#tooltips">
         <div class="card-title"><span class="card-icon">💬</span>Hover tooltips</div>
         <div class="card-desc">Playtime info on hover</div>
      </a>
   </div>
</div>

## ⚙️ How it works {#how-it-works}

1. **Playtime check** — Voidium reads the player's `PLAY_TIME` stat (ticks → hours) every `checkIntervalMinutes` (default 5).
2. **Custom conditions** — If a rank definition includes custom conditions, the player must also meet those (tracked via `ProgressTracker`).
3. **Highest wins** — The rank with the highest `hours` requirement that the player qualifies for is applied.
4. **Promotion message** — When a player reaches a new rank for the first time, they see `promotionMessage` in chat.
5. **Display** — The rank prefix/suffix appears in the TAB list (if Player List module is enabled) or in the chat display name.

## 📝 Configuration {#config}

File: <code>config/voidium/ranks.json</code>

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enableAutoRanks` | boolean | `true` | Master switch |
| `checkIntervalMinutes` | int | `5` | How often to check playtime (min 1) |
| `promotionMessage` | string | `&aCongratulations! You have earned the rank &b%rank%&a!` | Message on rank up. Placeholders: `%rank%`, `{player}`, `{hours}` |
| `tooltipPlayed` | string | `§7Played: §f%hours%h` | Hover text showing played hours |
| `tooltipRequired` | string | `§7Required: §f%hours%h` | Hover text showing required hours |
| `ranks` | array | _(see below)_ | List of rank definitions |

### Rank definition

Each entry in the `ranks` array:

```json
{
  "type": "PREFIX",
  "value": "&6[Veteran] ",
  "hours": 100,
  "customConditions": []
}
```

| Field | Description |
|-------|-------------|
| `type` | `"PREFIX"` or `"SUFFIX"` |
| `value` | The text shown (supports `&` color codes, `&#RRGGBB` hex) |
| `hours` | Minimum playtime hours required |
| `customConditions` | Optional array of extra requirements |

### Default ranks

| Rank | Type | Hours | Display |
|------|------|-------|---------|
| Member | PREFIX | 10 | `&7[Member] ` |
| Veteran | PREFIX | 100 | `&6[Veteran] ` |
| Star | SUFFIX | 500 | ` &e★` |

## 🎯 Custom conditions {#custom-conditions}

Custom conditions let you require achievements beyond playtime. Each condition has a `type` and a `count`:

```json
{
  "type": "PREFIX",
  "value": "&c[Hunter] ",
  "hours": 50,
  "customConditions": [
    { "type": "KILL", "count": 500 }
  ]
}
```

### Condition types

| Type | Tracks | Example |
|------|--------|---------|
| `KILL` | Mob kills (any type) | 500 kills → "Hunter" |
| `VISIT` | Unique biome visits | 10 biomes → "Explorer" |
| `BREAK` | Blocks broken (any type) | 1000 blocks → "Miner" |
| `PLACE` | Blocks placed (any type) | 500 blocks → "Builder" |

Progress is tracked per-player in <code>config/voidium/storage/player_progress.json</code> and persists across restarts.

You can combine multiple conditions on a single rank:

```json
{
  "type": "SUFFIX",
  "value": " &d♦",
  "hours": 200,
  "customConditions": [
    { "type": "KILL", "count": 1000 },
    { "type": "VISIT", "count": 15 }
  ]
}
```

The player must meet **all** conditions (AND logic).

## 💬 Hover tooltips {#tooltips}

When a player's rank prefix or suffix is displayed, hovering over it shows:

- **Played hours** — formatted via `tooltipPlayed` (e.g. `Played: 142.5h`)
- **Required hours** — formatted via `tooltipRequired` (e.g. `Required: 100h`)

This works in chat and in the TAB player list.

## Integration with Player List

When the Player List module is enabled (`enablePlayerList: true` in `general.json`), rank prefixes and suffixes are applied via scoreboard teams in TAB. The `RankManager` defers to `PlayerListManager` to avoid duplicate display.

When Player List is disabled, ranks are applied directly to the player's display name in chat.

## Related

- <a href="Commands_EN.html">Commands</a> — <code>/voidium status</code> shows rank info
- <a href="PlayerList_EN.html">Player List</a> — TAB integration
- <a href="Config_EN.html">Configuration</a> — all config files
