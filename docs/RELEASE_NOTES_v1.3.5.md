# Release Notes v1.3.5

## 🚀 New Features & Improvements

### 💬 Discord Integration Overhaul
*   **Console Streaming:** Added ability to stream server console output directly to a Discord channel (`enableConsoleLog`, `consoleChannelId`).
*   **Status Messages:** The bot now announces server lifecycle events (Starting, Online, Stopping, Offline).
    *   Messages are sent to the Chat Channel by default (configurable via `statusChannelId`).
*   **Auto-Updating Channel Topic:** The bot can now automatically update the channel topic with live server info.
    *   Displays: Online players, Max players, Uptime.
    *   Update Interval: 6 minutes.
*   **Customizable Uptime Format:** Added `uptimeFormat` configuration to control how uptime is displayed.
    *   Supports variables: `%days%`, `%hours%`, `%minutes%`, `%seconds%`.

### 🛠️ Fixes & Changes
*   **Chat Bridge:** Fixed an issue where the `/say` command was not being bridged to Discord.
*   **Reload Command:** Fixed `/voidium reload` not properly restarting the Discord bot if it was enabled during runtime.
*   **Web Panel:** Updated the Web Interface to support all new Discord configuration fields.
*   **Locale System:** Fixed an issue where resetting the locale (EN/CZ) would not apply to newly added configuration keys.

## ⚙️ Configuration Changes (Discord)
New fields automatically added to `config/discord.json`:
```json
{
  "enableConsoleLog": false,
  "consoleChannelId": "",
  "enableStatusMessages": true,
  "statusChannelId": "", // Defaults to chatChannelId if empty
  "enableTopicUpdate": true,
  "channelTopicFormat": "Online: %online%/%max% | Uptime: %uptime% | Voidium Server",
  "uptimeFormat": "%days%d %hours%h %minutes%m"
}
```
