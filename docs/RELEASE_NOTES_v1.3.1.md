# Release Notes - Voidium 1.3.1

**Release Date:** October 11, 2025  
**Minecraft Version:** 1.21.1  
**NeoForge Version:** 21.1.208

---

## ✨ What's New in 1.3.1

### 🎁 Pending Vote Queue System
- **Offline vote handling** – votes received when player is offline are automatically queued
- **Auto-delivery on login** – pending rewards are delivered immediately when player joins
- **Persistent storage** – queue survives server restarts via `pending-votes.json`
- **Player notifications** – players are notified about pending rewards on login

### 🔧 Admin Commands
- `/voidium votes pending` – view total pending votes across all players
- `/voidium votes pending <player>` – check specific player's pending vote count
- `/voidium votes clear` – clear all pending votes (admin emergency tool)

### 📊 Smart Vote Processing
- Vote system now checks if player is online before executing rewards
- Offline votes are logged and queued automatically
- Queue state is loaded on server startup
- Thread-safe queue implementation with file locking

---

## 🔄 Upgrade Guide

### From 1.3.0
1. **Stop server**
2. Replace jar: `voidium-1.3.0.jar` → `voidium-1.3.1.jar`
3. **Start server** – new `pending-votes.json` file will be auto-created
4. No config changes required

### Configuration
The `votes.json` config automatically includes new field:
```json
{
  "logging": {
    "pendingQueueFile": "pending-votes.json"
  }
}
```

---

## 📝 Key Files Modified

- **New:** `PendingVoteQueue.java` – thread-safe persistent queue
- **Modified:** `VoteManager.java` – login event handler, queue initialization
- **Modified:** `VoteListener.java` – online check before reward execution
- **Modified:** `VoteConfig.java` – added `pendingQueueFile` parameter
- **Modified:** `VoidiumCommand.java` – new vote management commands

---

## 🎯 Use Cases

### Scenario 1: Player votes while offline
1. Player votes on voting site
2. Vote is received by server
3. Vote is logged and added to pending queue
4. Player logs in later
5. **Automatic:** Queue detects login and delivers reward
6. Player gets notification about delivered rewards

### Scenario 2: Admin monitoring
- Check total pending: `/voidium votes pending`
- Check specific player: `/voidium votes pending Steve`
- Emergency clear: `/voidium votes clear` (use with caution!)

---

## ⚙️ Technical Details

### Queue Architecture
- **File:** `config/voidium/pending-votes.json`
- **Format:** JSON array of pending vote objects
- **Locking:** ReentrantLock for thread safety
- **Persistence:** Auto-save on every enqueue/dequeue

### Vote Object Structure
```json
{
  "username": "player_name",
  "serviceName": "voting_site",
  "address": "ip_address",
  "timestamp": 1234567890,
  "queuedAt": 1234567900
}
```

---

## 🐛 Known Issues
None reported for this release.

---

## 🔗 Links
- **GitHub:** https://github.com/venom74cz/voidium
- **CurseForge:** [pending]
- **Issues:** https://github.com/venom74cz/voidium/issues

---

## 📦 Build Information
- **Build Command:** `gradlew build`
- **Artifact:** `voidium-1.3.1.jar`
- **Java Version:** 21
- **License:** MIT

---

## 💡 Tips
- Regular players can't see pending votes (security)
- Queue persists through crashes/restarts
- Offline votes never expire (delivered eventually)
- Use `/voidium votes pending` to monitor queue health

---

**Enjoy the improved vote system! 🎉**
