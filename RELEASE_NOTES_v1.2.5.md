# Voidium v1.2.5 - Hotfix Release

## 🔧 What's Fixed in v1.2.5

### **DELAY Restart Timing Fix**
- **Fixed DELAY restart calculation** - now correctly counts from server startup time
- **Accurate status display** - `/voidium status` shows proper countdown for DELAY restarts
- **Improved time tracking** - separate tracking for server start time vs last restart time

### **Before vs After:**
- **Before**: DELAY restart showed interval time (incorrect)
- **After**: DELAY restart shows accurate countdown from server startup ✅

---

## 🎯 DELAY Restart Now Works Perfectly

### **Configuration Example:**
```json
{
  "restartType": "DELAY",
  "delayMinutes": 60
}
```

### **What You'll See:**
- **Status**: "in 0h 59m (delay restart)" - counts down properly
- **Accurate timing**: Restart happens exactly X minutes after server start
- **Proper warnings**: All warning messages at correct intervals

---

## 🚀 Complete Feature Set (All Working)

### 🔄 **Three Restart Types**
1. **FIXED_TIME** - Restart at specific times (6:00 AM, 6:00 PM)
2. **INTERVAL** - Restart every X hours
3. **DELAY** - Restart X minutes after server startup ✅ **FIXED**

### 📢 **Player Communication**
- Automated announcements with color support
- Manual announcement broadcasting
- Smart warning system before restarts

### 📊 **Server Monitoring**
- Real-time TPS and MSPT tracking
- Memory usage monitoring
- Player management with ping info

### 🎮 **Management Tools**
- Manual restart commands
- Boss bar countdown (10+ minutes)
- Configuration hot-reload
- Interactive GUI system

---

## 📋 Quick Commands

**For Operators:**
- `/voidium restart <minutes>` - Manual restart
- `/voidium status` - Server status (shows correct DELAY time)
- `/voidium cancel` - Cancel restart
- `/voidium reload` - Reload config

**For Everyone:**
- `/voidium status` - View server status and accurate restart countdown

---

## 🛠️ Installation

1. Download `voidium-1.2.5.jar`
2. Replace previous version in `mods` folder
3. Start server - DELAY restart now works correctly!

---

## 🔧 System Requirements

- **Minecraft**: 1.21.1
- **Mod Loader**: NeoForge
- **Java**: 21+
- **Installation**: Server-side only

---

**DELAY restart is now 100% accurate! Download v1.2.5 for reliable server management.**