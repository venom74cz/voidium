# VOIDIUM v1.3.2 – Release Notes

## 🎯 What's New

### Command Improvements
- **`/voidium status` is now available to all players** (no longer requires OP permissions)
- All other commands remain operator-only for security

### Vote System Enhancements
- **Silent pending vote delivery** – When players log in with pending votes, broadcast/say commands are now filtered out to prevent chat spam
- **Customizable pending vote message** – New configuration option `pendingVoteMessage` in `votes.json`
  - Default: `"&8[&bVoidium&8] &aVyplaceno &e%COUNT% &aodložených hlasů!"`
  - Supports color codes (`&` → `§`)
  - Use `%COUNT%` placeholder for number of pending votes
- Rewards are still delivered properly for each pending vote, just without the global announcements

## 🔧 Configuration Changes

New field in `config/voidium/votes.json` under `logging` section:
```json
"pendingVoteMessage": "&8[&bVoidium&8] &aVyplaceno &e%COUNT% &aodložených hlasů!"
```

## 📝 How It Works

**Before (v1.3.1):**
- Player votes while offline → vote queued
- Player logs in → all reward commands execute (including broadcast) → chat spam with 20+ votes

**Now (v1.3.2):**
- Player votes while offline → vote queued  
- Player logs in → rewards delivered silently (broadcast/say filtered) → single customizable message
- When voting while online → broadcast still works normally

## 🐛 Bug Fixes
- Fixed duplicate command registration for `/voidium status`
- Improved command permission structure

---

**Full Changelog:** v1.3.1...v1.3.2
