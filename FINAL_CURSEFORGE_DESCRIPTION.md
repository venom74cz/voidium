# VOIDIUM - SERVER MANAGER

**Comprehensive server management with automatic restarts, announcements, and real-time monitoring**

Transform your Minecraft server administration with this powerful, all-in-one management solution designed for server operators who demand reliability and ease of use.

---

## 🌟 Core Features

### 🔄 **Advanced Restart System**
- **Fixed Time Restarts** - Schedule daily restarts at specific times (6 AM, 6 PM, etc.)
- **Interval Restarts** - Automatic restarts every X hours for consistent uptime
- **Delay Restarts** - Restart X minutes after server startup (NEW!)
- **Manual Control** - Force restarts with `/voidium restart <minutes>` command
- **Visual Countdown** - Boss bar displays restart timer for 10+ minute restarts
- **Smart Warnings** - Automatic notifications at 60, 30, 15, 10, 5, 3, 2, 1 minutes before restart

### 📢 **Advanced Announcements**
- **Automated Broadcasting** - Send rotating messages to players at configurable intervals
- **Rich Formatting** - Full Minecraft color code support (&a, &b, &c, etc.)
- **Custom Prefixes** - Personalize your server's announcement style
- **Manual Messaging** - Instant announcements with `/voidium announce <message>`

### 📊 **Real-Time Monitoring**
- **Performance Metrics** - Live TPS, MSPT tracking for server health
- **Memory Monitoring** - Track RAM usage and allocation in real-time
- **Player Management** - View online players with ping information
- **Server Statistics** - Comprehensive status available to all players

### 🎮 **Interactive Management**
- **In-Game GUI** - View all settings with `/voidium gui` without file editing
- **Hot Reload** - Apply configuration changes with `/voidium reload`
- **Organized Config** - Separate files for restarts, announcements, and general settings
- **Detailed Documentation** - Every config file includes comprehensive comments

---

## 📋 Command Overview

### **Server Operators (OP Level 2+)**
```
/voidium                    - Show help and available commands
/voidium gui               - Open interactive configuration viewer
/voidium restart <minutes> - Schedule manual restart (1-60 minutes)
/voidium announce <message> - Broadcast message to all players
/voidium players           - List online players with ping
/voidium memory            - Display server memory usage
/voidium cancel            - Cancel scheduled manual restart
/voidium config            - Show configuration file locations
/voidium reload            - Reload all configuration files
```

### **All Players**
```
/voidium status - View server TPS, memory, next restart, and general info
```

---

## ⚙️ Easy Configuration

The mod creates three organized configuration files with detailed comments:

### **restart.json** - Restart Management
```json
{
  "restartType": "FIXED_TIME",
  "fixedRestartTimes": ["06:00", "18:00"],
  "intervalHours": 6,
  "delayMinutes": 60
}
```

### **announcements.json** - Player Communication
```json
{
  "announcements": [
    "&bWelcome to the server!",
    "&eDon't forget to visit our website!"
  ],
  "announcementIntervalMinutes": 30,
  "prefix": "&8[&bServer&8]&r "
}
```

### **general.json** - Master Controls
```json
{
  "enableMod": true,
  "enableRestarts": true,
  "enableAnnouncements": true,
  "enableBossBar": true
}
```

---

## 🚀 Quick Start

1. **Download** and place in your server's `mods` folder
2. **Start** your server - configs auto-generate in `config/voidium/`
3. **Customize** settings or use defaults (works out of the box!)
4. **Manage** your server with powerful `/voidium` commands

---

## 🎯 Perfect For

✅ **Public Servers** - Automated maintenance with clear player communication  
✅ **Private Communities** - Easy management for friends and small groups  
✅ **Modded Servers** - Performance monitoring and scheduled maintenance  
✅ **Any Server** - Professional experience with minimal setup  

---

## 🔧 Technical Details

- **Minecraft Version**: 1.21.1
- **Mod Loader**: NeoForge
- **Installation**: Server-side only (clients don't need the mod)
- **Performance**: Lightweight with minimal server impact
- **Compatibility**: Works alongside other server management mods

---

## 💡 Why Choose Voidium?

🎯 **Plug & Play** - Works immediately with sensible defaults  
⚙️ **Highly Configurable** - Customize every aspect to fit your needs  
🖥️ **User Friendly** - In-game GUI eliminates complex file editing  
🏗️ **Professional Grade** - Clean code with proper error handling  
📈 **Actively Developed** - Regular updates and new features  
📚 **Well Documented** - Comprehensive guides and examples included  

---

**Take control of your server with Voidium Server Manager!**

*Professional server management has never been this easy. Download now and experience the difference.*