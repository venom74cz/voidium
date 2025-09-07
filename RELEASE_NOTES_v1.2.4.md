# Voidium v1.2.4 - Release Notes

## 🎉 What's New in v1.2.4

### 🔄 **NEW: DELAY Restart Type**
- **Delay Restarts**: Configure server to restart X minutes after startup
- Perfect for testing, maintenance windows, or one-time restarts
- Set `"restartType": "DELAY"` and `"delayMinutes": 60` in restart.json

### 🛠️ **Major Restart System Fixes**
- **Completely overhauled restart functionality** - now working reliably
- **Enhanced debug logging** for troubleshooting restart issues
- **Improved error handling** with proper validation
- **Fixed boss bar timing** calculations for accurate countdowns
- **Better scheduler management** with proper task cancellation

### ⚙️ **Three Restart Types Available**
1. **FIXED_TIME** - Restart at specific times (6:00 AM, 6:00 PM)
2. **INTERVAL** - Restart every X hours
3. **DELAY** - Restart X minutes after server startup

---

## 🔧 Configuration Examples

### Delay Restart (New!)
```json
{
  "restartType": "DELAY",
  "delayMinutes": 60
}
```

### Fixed Time Restart
```json
{
  "restartType": "FIXED_TIME",
  "fixedRestartTimes": ["06:00", "18:00"]
}
```

### Interval Restart
```json
{
  "restartType": "INTERVAL",
  "intervalHours": 6
}
```

---

## 🚀 Complete Feature Set

### 🔄 **Restart Management**
- Three flexible restart types with reliable scheduling
- Manual restart commands with countdown timers
- Boss bar visual countdown for 10+ minute restarts
- Smart warning system at multiple intervals
- Proper task cancellation and rescheduling

### 📢 **Player Communication**
- Automated announcements with color support
- Manual announcement broadcasting
- Customizable message prefixes
- Hot-reload configuration support

### 📊 **Server Monitoring**
- Real-time TPS and MSPT tracking
- Memory usage monitoring
- Online player list with ping information
- Comprehensive server status display

### 🎮 **Interactive Interface**
- In-game GUI for viewing all settings
- Clickable navigation system
- Live configuration display
- No file editing required

---

## 📋 Quick Command Reference

**For Operators:**
- `/voidium restart <minutes>` - Schedule restart
- `/voidium cancel` - Cancel scheduled restart
- `/voidium announce <message>` - Broadcast message
- `/voidium players` - List players with ping
- `/voidium memory` - Show memory usage
- `/voidium reload` - Reload configuration
- `/voidium gui` - Open configuration interface

**For Everyone:**
- `/voidium status` - View server status and next restart

---

## 🛠️ Installation & Upgrade

1. Download `voidium-1.2.4.jar`
2. Replace old version in `mods` folder
3. Start server (configs auto-update)
4. Enjoy reliable restart functionality!

---

## 🔧 System Requirements

- **Minecraft**: 1.21.1
- **Mod Loader**: NeoForge
- **Java**: 21+
- **Installation**: Server-side only

---

## 📈 Upgrade Notes

**From v1.2.2 or earlier:**
- Restart functionality has been completely rewritten
- All existing configurations are preserved
- New `delayMinutes` option added to restart.json
- Debug logging added for troubleshooting

---

**Ready for reliable server management? Download Voidium v1.2.4 today!**