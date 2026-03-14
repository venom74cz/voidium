---
layout: default
title: Stats & Reports (EN)
---

# 📊 Stats & Reports

<div class="hero">
   <p><strong>Stats module</strong> tracks server performance metrics (TPS, player count) and sends automated daily reports to a Discord channel.</p>

   <div class="note">
      To enable: set <code>enableStats: true</code> in <code>general.json</code>. Configure the report channel in <code>stats.json</code>.
   </div>

   <h2>Jump to</h2>
   <div class="card-grid">
      <a class="card" href="#overview">
         <div class="card-title"><span class="card-icon">📈</span>Overview</div>
         <div class="card-desc">What is tracked and how</div>
      </a>
      <a class="card" href="#config">
         <div class="card-title"><span class="card-icon">⚙️</span>Configuration</div>
         <div class="card-desc">stats.json fields</div>
      </a>
      <a class="card" href="#daily-report">
         <div class="card-title"><span class="card-icon">📋</span>Daily report</div>
         <div class="card-desc">Discord embed format</div>
      </a>
   </div>
</div>

## 📈 Overview {#overview}

Voidium samples player count every minute and tracks:

- **Peak players** — highest online count in the last 24 hours
- **Average players** — mean online count over the period
- **TPS** — real-time ticks per second via `TpsTracker`

The data is kept in memory for the current reporting period and reset after the daily report is sent.

## ⚙️ Configuration {#config}

File: <code>config/voidium/stats.json</code>

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enableStats` | boolean | `true` | Master switch |
| `reportChannelId` | string | `""` | Discord channel ID for daily reports |
| `reportTime` | string | `"09:00"` | Time of day to send the report (HH:mm, 24h) |
| `reportTitle` | string | `📊 Daily Statistics - %date%` | Embed title. Placeholder: `%date%` |
| `reportPeakLabel` | string | `Peak Players` | Label for peak field |
| `reportAverageLabel` | string | `Average Players` | Label for average field |
| `reportFooter` | string | `Voidium Stats` | Embed footer text |

### Example config

```json
{
  "enableStats": true,
  "reportChannelId": "1234567890",
  "reportTime": "09:00",
  "reportTitle": "📊 Daily Statistics - %date%",
  "reportPeakLabel": "Peak Players",
  "reportAverageLabel": "Average Players",
  "reportFooter": "Voidium Stats"
}
```

## 📋 Daily report {#daily-report}

At the configured `reportTime`, Voidium sends a Discord embed to `reportChannelId`:

- **Title**: `reportTitle` with `%date%` replaced
- **Fields**: Peak players, Average players
- **Color**: Cyan
- **Footer**: `reportFooter`

<div class="note">
   <strong>Requirement:</strong> Discord module must be enabled and the bot must have permission to send messages in the report channel.
</div>

## Related

- <a href="Discord_EN.html">Discord</a> — bot setup
- <a href="Config_EN.html">Configuration</a> — all config files
