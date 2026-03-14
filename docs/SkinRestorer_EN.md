---
layout: default
title: Skin Restorer (EN)
---

# 🧍 Skin Restorer

<div class="hero">
   <p><strong>Skin Restorer</strong> fetches and applies real Minecraft skins for players on offline-mode servers. Skins are injected on join (no relog needed) and cached locally to reduce Mojang API calls.</p>

   <div class="note">
      To enable: set <code>enableSkinRestorer: true</code> in <code>general.json</code>. Automatically disabled in online-mode.
   </div>

   <h2>Jump to</h2>
   <div class="card-grid">
      <a class="card" href="#how-it-works">
         <div class="card-title"><span class="card-icon">⚙️</span>How it works</div>
         <div class="card-desc">Fetch, inject, cache</div>
      </a>
      <a class="card" href="#config">
         <div class="card-title"><span class="card-icon">📝</span>Configuration</div>
         <div class="card-desc">Cache TTL, manual refresh</div>
      </a>
      <a class="card" href="#commands">
         <div class="card-title"><span class="card-icon">⌨️</span>Commands</div>
         <div class="card-desc">Manual skin refresh</div>
      </a>
   </div>
</div>

## ⚙️ How it works {#how-it-works}

1. **Player joins** — Voidium checks if the player has a cached skin
2. **Cache hit** — If the skin is cached and not expired, it's applied immediately
3. **Cache miss** — Voidium queries the Mojang API:
   - First: resolve player name → official UUID via `api.mojang.com`
   - Then: fetch skin texture data (value + signature) via `sessionserver.mojang.com`
4. **Injection** — The skin is applied to the player's game profile on login (early join injection — no relog required)
5. **Cache save** — The skin data is stored in `skin-cache.json` with a timestamp

### Smart features

- **Auto-disabled in online mode** — When the server is in online mode, real skins are already available
- **Webhook integration** — When chat bridge uses webhooks, the correct skin avatar is resolved even for offline-mode players

## 📝 Configuration {#config}

The Skin Restorer is configured via `general.json`:

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enableSkinRestorer` | boolean | `true` | Master switch |
| `skinCacheHours` | int | `24` | How long to keep cached skins before re-fetching (hours, min 1) |

### Cache file

Skins are cached in <code>config/voidium/storage/skin-cache.json</code>. Each entry contains:

- Player name → skin texture value + signature + timestamp
- Entries older than `skinCacheHours` are re-fetched on next join

<div class="note">
   <strong>Tip:</strong> Set <code>skinCacheHours</code> higher (e.g. 48) to reduce Mojang API calls, or lower (e.g. 6) if players frequently change skins.
</div>

## ⌨️ Commands {#commands}

| Command | Permission | Description |
|---------|-----------|-------------|
| `/voidium skin <player>` | OP | Force-refresh an online player's skin |

The command re-fetches the skin from Mojang API immediately, updating both the cache and the player's in-game appearance.

## Related

- <a href="Commands_EN.html">Commands</a>
- <a href="Config_EN.html">Configuration</a>
