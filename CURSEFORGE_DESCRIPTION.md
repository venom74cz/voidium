<!-- Unified English CurseForge Description -->
# VOIDIUM - SERVER MANAGER

**Comprehensive NeoForge server management: automated restarts (fixed / interval / delay / manual), announcements, NuVotifier vote rewards, live performance metrics (TPS/MSPT/memory), in‑game GUI configuration, and instant offline‑mode Mojang skin restoration with a configurable persistent cache.**

---

## ✨ What's New (1.3.1)
- **Pending vote queue** – votes received while player is offline are automatically saved
- **Auto-delivery on login** – pending rewards delivered immediately when player joins server
- **Admin vote commands** – `/voidium votes pending [player]` and `/voidium votes clear`
- **Player notifications** – players are notified about pending vote rewards on login

---

## 🔄 Restart System
- Fixed-Time Restarts (e.g. 06:00, 18:00)
- Interval Restarts (every X hours)
- Delay Mode (restart X minutes after startup)
- Manual Restarts (`/voidium restart <minutes>`)
- Boss bar countdown (10+ min)
- Structured warnings: 60, 30, 15, 10, 5, 3, 2, 1 minutes

## 📢 Announcement Engine
- Rotating scheduled broadcasts
- Color codes (&a–&f, formatting &l, &o, &n, &r)
- Custom prefix & hot reload (`/voidium reload`)
- Manual broadcast: `/voidium announce <message>`

## 📊 Monitoring & Utilities
- TPS / MSPT / memory usage
- Player list with ping
- Server + mod info
- Public status: `/voidium status`

## 🎁 Vote Rewards (NuVotifier)
- Accepts NuVotifier V2 token packets and legacy RSA V1 payloads simultaneously
- **Pending vote queue** – offline votes saved to `pending-votes.json` and delivered on login
- `config/voidium/votes.json` auto-generates shared secret, RSA paths, and command placeholders
- Logs every successful vote to `votes.log` (plain) + `votes-history.ndjson` (analytics)
- Optional OP notifications and verbose diagnostics on listener failure
- Admin commands: `/voidium votes pending [player]` · `/voidium votes clear`

## 🧍 Offline-Mode Skin Restorer
- Early join injection (no relog required)
- Persistent JSON cache: `config/voidium/skin-cache.json`
- Configurable TTL: `skinCacheHours`
- Manual refresh: `/voidium skin <player>`
- Auto-disabled in online mode

## 🎮 In-Game GUI
`/voidium gui` shows all active configuration values in a structured menu—no manual file editing.

## ⚙️ Configuration Files
```
restart.json       - Restart scheduling
announcements.json - Broadcast messages
general.json       - Feature toggles + skinCacheHours
```
Example (general.json):
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

## ✅ Commands (Operators)
`/voidium restart <minutes>` · `/voidium announce <message>` · `/voidium players` · `/voidium memory` · `/voidium cancel` · `/voidium config` · `/voidium reload` · `/voidium skin <player>` · `/voidium votes pending [player]` · `/voidium votes clear` · `/voidium gui`

Players: `/voidium status`

## 🔧 Technical
- Minecraft: 1.21.1
- Loader: NeoForge
- Side: Server-only (clients not required)
- License: MIT
- Lightweight & modular

## 📌 Notes
- `skinCacheHours` below 1 is forced to 1
- Expired cache entries re-fetch lazily at next login
- Safe in online mode (skin feature auto-skips)
- `votes.json` is created automatically with generated shared secret + RSA key pair
- Offline votes are queued in `pending-votes.json` and delivered when player logs in

## 💡 Why Voidium?
Fast setup · Strong observability · Clean code · Actively maintained · In‑game UX · Configurable persistence

---
**Professional server control, zero hassle.**

*Download now and level up your server administration.*