# Voidium Server Manager 1.2.8 Release Notes

A comprehensive NeoForge (1.21.1) server management suite: automated restarts (fixed / interval / delay / manual), announcements, live performance metrics (TPS/MSPT/memory), in‑game GUI, and instant offline‑mode Mojang skin restoration backed by a configurable persistent cache.

---
## ✨ What’s New in 1.2.8
**Added**
- Configurable TTL for persistent Skin Cache (`skinCacheHours` in `general.json`, default 24h, minimum 1)

**Changed**
- Unified SLF4J logging across skin subsystem (SkinCache, EarlySkinInjector, SkinRestorer)
- Startup broadcast version updated to 1.2.8
- License alignment (MIT declared in mods.toml) and logo embedded

**Notes**
- Values for `skinCacheHours` below 1 are forced to 1
- Expired skin entries are lazily re-fetched on next player login
- Offline-mode skin injection still happens instantly during join (no relog)

---
## 🧍 Offline-Mode Skin Flow (Recap)
1. Early injector attempts Mojang fetch (fast timeouts)
2. Persistent JSON cache: `config/voidium/skin-cache.json`
3. Fallback restorer runs only if early stage failed
4. Manual refresh: `/voidium skin <player>`
5. Auto-disabled when server runs in online mode

---
## ⚙️ Configuration Snippet
```json
{
  "enableMod": true,
  "enableRestarts": true,
  "enableAnnouncements": true,
  "enableBossBar": true,
  "enableSkinRestorer": true,
  "skinCacheHours": 24
}
```

---
## ✅ Operator Commands (Quick Reference)
`/voidium restart <minutes>` · `/voidium announce <message>` · `/voidium players` · `/voidium memory` · `/voidium cancel` · `/voidium config` · `/voidium reload` · `/voidium skin <player>` · `/voidium gui`  
Players: `/voidium status`

---
## 🔧 Technical
- Minecraft: 1.21.1
- Loader: NeoForge
- Side: Server-only (clients not required)
- License: MIT
- Logo: `voidium.png` packaged inside the JAR

---
## 📌 Since 1.2.5 (Aggregate Summary)
- Instant offline-mode skin injection + persistent cache
- Offline-mode Mojang skin restorer + manual refresh command
- Configurable skin cache TTL
- Unified structured logging
- Fixed DELAY restart timing & accurate status countdown

---
**Professional server control.** If you like the project, consider starring the repository.
