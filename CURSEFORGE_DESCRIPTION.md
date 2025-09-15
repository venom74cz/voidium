# VOIDIUM - SERVER MANAGER

**A powerful NeoForge server management suite for Minecraft 1.21.1: automated restarts, announcements, status & performance tooling, GUI control, and instant offline‑mode skin restoration (persistent configurable cache).**

Immediate offline-mode Mojang skin application with persistent caching (TTL konfigurovatelný pomocí `skinCacheHours`).

---

## 🌟 Key Features

### 🔄 **Smart Restart System**
- **Fixed Time Restarts**: Schedule restarts at specific times (perfect for daily maintenance)
- **Interval Restarts**: Automatic restarts every X hours
- **Manual Control**: Force restarts with customizable countdown timers
- **Visual Countdown**: Boss bar shows restart countdown for 10+ minute restarts
- **Smart Warnings**: Automatic player notifications at 60, 30, 15, 10, 5, 3, 2, 1 minutes

### 📢 **Advanced Announcements**
- **Automated Broadcasting**: Send messages to players at configurable intervals
- **Rich Formatting**: Full Minecraft color code support with custom prefixes
- **Instant Messaging**: Manual announcement commands for important updates
- **Flexible Configuration**: Easy-to-edit message lists with hot-reload support

### 📊 **Real-Time Monitoring**
- **Performance Metrics**: Live TPS, MSPT, and memory usage tracking
- **Player Management**: View online players with ping information
- **Server Statistics**: Comprehensive server status available to all players
- **Resource Monitoring**: Track memory allocation and server performance

### 🎮 **Interactive GUI System**
- **In-Game Configuration**: View all settings without leaving Minecraft
- **Clickable Interface**: Navigate through different configuration sections easily
- **Live Updates**: See current configuration values in real-time
- **User-Friendly**: No need to edit files manually

---

## 🚀 Quick Start

1. **Install**: Drop the mod file into your server's `mods` folder
2. **Start**: Launch your server - configuration files are created automatically
3. **Configure**: Edit files in `config/voidium/` or use the in-game GUI
4. **Manage**: Use `/voidium` commands to control your server

---

## 📋 Command Overview

### **For Server Operators**
- `/voidium gui` - Interactive configuration interface
- `/voidium restart <minutes>` - Schedule manual restart
- `/voidium announce <message>` - Broadcast to all players
- `/voidium players` - List online players with ping
- `/voidium memory` - Server memory usage
- `/voidium cancel` - Cancel scheduled restart
- `/voidium reload` - Reload configuration

### **For All Players**
- `/voidium status` - View server status, TPS, and next restart time

---

## ⚙️ Configuration Made Easy

The mod creates three organized configuration files with detailed comments:

- **`restart.json`** - Restart scheduling and timing
- **`announcements.json`** - Player messages and broadcasting
- **`general.json`** - Master switches and mod behavior

Each file includes comprehensive documentation and examples! New in 1.2.8: `skinCacheHours` in `general.json` for skin cache TTL (hours, default 24).

---

## 🎯 Perfect For

- **Public Servers**: Automated maintenance and player communication
- **Private Servers**: Easy server management for friends and communities  
- **Modded Servers**: Performance monitoring and restart scheduling
- **Any Server**: Improved player experience with clear communication

---

## 🔧 Technical Details

- **Minecraft Version**: 1.21.1
- **Mod Loader**: NeoForge
- **Installation**: Server-side only (clients don't need the mod)
- **Performance**: Lightweight with minimal server impact
- **Compatibility**: Works with other server management mods

---

## 📈 Why Choose Voidium?

✅ **Easy Setup** - Works out of the box with sensible defaults  
✅ **Highly Configurable** - Customize every aspect to fit your server  
✅ **User Friendly** - In-game GUI eliminates file editing  
✅ **Professional** - Clean, organized code with proper error handling  
✅ **Active Development** - Regular updates and feature additions  
✅ **Well Documented** - Comprehensive guides and examples  

---

**Transform your server management experience with Voidium!**

*Download now and give your players the professional server experience they deserve.*