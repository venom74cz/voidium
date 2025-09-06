# Voidium v1.2.2 - Release Notes

## 🎉 What's New in v1.2.2

### 📝 Enhanced Documentation
- **Detailed Config Comments**: All configuration files now include comprehensive comments explaining each setting
- **Usage Examples**: Clear examples for time formats, color codes, and configuration options
- **Better User Experience**: No more guessing what each setting does!

### 🔧 Configuration Improvements
- **restart.json**: Detailed explanations of FIXED_TIME vs INTERVAL restart types
- **announcements.json**: Complete color code reference and formatting guide
- **general.json**: Clear descriptions of all master switches and their effects

---

## 🚀 Complete Feature Set

### 🔄 **Restart Management**
- Automatic restarts at fixed times or intervals
- Manual restart commands with countdown timers
- Boss bar visual countdown for 10+ minute restarts
- Smart warning system at multiple intervals

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
- `/voidium gui` - Open configuration interface
- `/voidium restart <minutes>` - Schedule restart
- `/voidium announce <message>` - Broadcast message
- `/voidium players` - List players with ping
- `/voidium memory` - Show memory usage
- `/voidium reload` - Reload configuration

**For Everyone:**
- `/voidium status` - View server status and next restart

---

## 🛠️ Installation & Setup

1. Download `voidium-1.2.2.jar`
2. Place in your server's `mods` folder
3. Start server (configs auto-generate)
4. Customize settings in `config/voidium/`
5. Use `/voidium reload` to apply changes

---

## 🔧 System Requirements

- **Minecraft**: 1.21.1
- **Mod Loader**: NeoForge
- **Installation**: Server-side only
- **Permissions**: OP level 2+ for management commands

---

## 📈 Upgrade Notes

If upgrading from previous versions:
- Configuration files will be automatically updated with comments
- All existing settings are preserved
- No manual migration required

---

**Ready to enhance your server management? Download Voidium v1.2.2 today!**