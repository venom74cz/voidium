# Voidium Server Management Mod

A comprehensive server-side mod for Minecraft 1.21.1 with NeoForge that provides automatic restart management, player announcements, and server monitoring tools.

## 🚀 Features

### 🔄 Advanced Restart System
- **Fixed Time Restarts**: Schedule restarts at specific times (e.g., 6:00 AM, 6:00 PM)
- **Interval Restarts**: Restart every X hours automatically
- **Delay Restarts**: Restart X minutes after server startup
- **Manual Restarts**: Force restart with `/voidium restart <minutes>` command
- **Boss Bar Countdown**: Visual countdown for restarts 10+ minutes (red progress bar)
- **Warning System**: Automatic warnings at 60, 30, 15, 10, 5, 3, 2, 1 minutes before restart

### 📢 Announcement System
- **Automatic Announcements**: Broadcast messages at configurable intervals
- **Color Code Support**: Full Minecraft color code support (&a, &b, &c, etc.)
- **Custom Prefix**: Configurable message prefix
- **Manual Announcements**: Send instant announcements with `/voidium announce <message>`

### 📊 Server Monitoring
- **Server Status**: Real-time TPS, MSPT, memory usage, and player count
- **Player List**: View online players with ping information
- **Memory Monitor**: Track server memory usage and allocation
- **Mod Information**: Display loaded mod count and server details

### 🎮 Interactive GUI
- **In-Game Configuration**: View all settings via `/voidium gui` command
- **Clickable Interface**: Navigate through different configuration sections
- **Real-Time Display**: See current configuration values without editing files

### ⚙️ Advanced Configuration
- **Separated Config Files**: Organized into restart.json, announcements.json, and general.json
- **Detailed Comments**: Each config file includes comprehensive documentation
- **Hot Reload**: Reload configuration without server restart using `/voidium reload`
- **Master Switches**: Enable/disable individual features independently

## 📋 Commands

### For Server Operators (OP Level 2+)
- `/voidium` - Show help and available commands
- `/voidium reload` - Reload configuration files
- `/voidium restart <minutes>` - Schedule manual restart (1-60 minutes)
- `/voidium announce <message>` - Send announcement to all players
- `/voidium players` - List online players with ping
- `/voidium memory` - Display server memory usage
- `/voidium cancel` - Cancel scheduled manual restart
- `/voidium config` - Show configuration file locations
- `/voidium gui` - Open interactive configuration GUI

### For All Players
- `/voidium status` - View server status, TPS, next restart, and general information

## 🛠️ Installation

1. Download the latest version from CurseForge
2. Place the `.jar` file in your server's `mods` folder
3. Start your server
4. Configuration files will be automatically created in `config/voidium/`
5. Edit the configuration files as needed
6. Use `/voidium reload` to apply changes without restarting

## ⚙️ Configuration

### restart.json
```json
// === RESTART CONFIGURATION ===
// Choose restart type: FIXED_TIME (specific times), INTERVAL (every X hours), or DELAY (restart in X minutes)
{
  "restartType": "FIXED_TIME",
  "fixedRestartTimes": ["06:00", "18:00"],
  "intervalHours": 6,
  "delayMinutes": 60
}
```

### announcements.json
```json
// === ANNOUNCEMENT CONFIGURATION ===
// Use & for color codes (e.g., &b = aqua, &e = yellow, &c = red)
{
  "announcements": [
    "&bWelcome to the server!",
    "&eDon't forget to visit our website!"
  ],
  "announcementIntervalMinutes": 30,
  "prefix": "&8[&bVoidium&8]&r "
}
```

### general.json
```json
// === GENERAL CONFIGURATION ===
// Master switches for mod features
{
  "enableMod": true,
  "enableRestarts": true,
  "enableAnnouncements": true,
  "enableBossBar": true,
  "modPrefix": "&8[&bVoidium&8]&r "
}
```

## 🎨 Color Codes

Use `&` followed by a character for colors:
- **Colors**: &0-&9, &a-&f
- **Formatting**: &l (bold), &o (italic), &n (underline), &r (reset)

## 🔧 Requirements

- **Minecraft**: 1.21.1
- **Mod Loader**: NeoForge
- **Side**: Server-side only (not required on client)

## 📝 License

This project is licensed under the MIT License.

## 🐛 Bug Reports & Feature Requests

Please report issues and suggest features on the CurseForge project page.

## 📊 Version History

See [CHANGELOG.md](CHANGELOG.md) for detailed version history and changes.