---
layout: default
title: Restart System (EN)
---

# 🔄 Restart System

<div class="hero">
   <p><strong>Restart System</strong> provides automatic and manual server restarts with configurable schedules, player warnings, and graceful shutdown.</p>

   <div class="note">
      To enable: set <code>enableRestarts: true</code> in <code>general.json</code>. Configure schedules in <code>restart.json</code>.
   </div>

   <h2>Jump to</h2>
   <div class="card-grid">
      <a class="card" href="#restart-types">
         <div class="card-title"><span class="card-icon">⏰</span>Restart types</div>
         <div class="card-desc">FIXED_TIME, INTERVAL, DELAY</div>
      </a>
      <a class="card" href="#config">
         <div class="card-title"><span class="card-icon">⚙️</span>Configuration</div>
         <div class="card-desc">restart.json fields</div>
      </a>
      <a class="card" href="#commands">
         <div class="card-title"><span class="card-icon">⌨️</span>Commands</div>
         <div class="card-desc">Manual restart & cancel</div>
      </a>
      <a class="card" href="#warnings">
         <div class="card-title"><span class="card-icon">⚠️</span>Warnings</div>
         <div class="card-desc">Pre-restart countdown</div>
      </a>
   </div>
</div>

## ⏰ Restart types {#restart-types}

Voidium supports three restart scheduling modes:

| Type | Description |
|------|-------------|
| `FIXED_TIME` | Restart at specific times of day (e.g. 06:00, 18:00). Default mode. |
| `INTERVAL` | Restart every X hours after the last restart. |
| `DELAY` | Restart X minutes after server startup. |

Only one type is active at a time — set via `restartType` in the config.

## ⚙️ Configuration {#config}

File: <code>config/voidium/restart.json</code>

### Schedule

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `restartType` | enum | `FIXED_TIME` | `FIXED_TIME`, `INTERVAL`, or `DELAY` |
| `fixedRestartTimes` | array | `["06:00", "18:00"]` | Times of day to restart (HH:MM, 24-hour). Only used with `FIXED_TIME`. |
| `intervalHours` | int | `6` | Hours between restarts. Only used with `INTERVAL`. |
| `delayMinutes` | int | `60` | Minutes after startup. Only used with `DELAY`. |

### Messages

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `warningMessage` | string | `&cServer restart in %minutes% minutes!` | Warning broadcast. Placeholder: `%minutes%` |
| `restartingNowMessage` | string | `&cServer is restarting now!` | Final message before shutdown |
| `kickMessage` | string | `&cServer is restarting. Please reconnect in a few minutes.` | Disconnect screen text |

### Example config

```json
{
  "restartType": "FIXED_TIME",
  "fixedRestartTimes": ["06:00", "18:00"],
  "intervalHours": 6,
  "delayMinutes": 60,
  "warningMessage": "&cServer restart in %minutes% minutes!",
  "restartingNowMessage": "&cServer is restarting now!",
  "kickMessage": "&cServer is restarting. Please reconnect in a few minutes."
}
```

## ⌨️ Commands {#commands}

| Command | Permission | Description |
|---------|-----------|-------------|
| `/voidium restart <minutes>` | OP | Schedule a manual restart in 1–60 minutes |
| `/voidium cancel` | OP | Cancel a pending manual restart |

Manual restarts use the same warning/kick messages as automatic restarts.

## ⚠️ Warnings {#warnings}

Before a restart executes, Voidium sends warning messages at configured intervals. The `warningMessage` is broadcast with the `%minutes%` placeholder replaced by remaining time.

When the countdown reaches zero:
1. `restartingNowMessage` is broadcast
2. All players are kicked with `kickMessage`
3. The server shuts down

<div class="note">
   For automatic external restart loops, use your hosting panel's auto-restart feature or a wrapper script that relaunches the server after exit.
</div>

## Related

- <a href="Commands_EN.html">Commands</a>
- <a href="Config_EN.html">Configuration</a>
