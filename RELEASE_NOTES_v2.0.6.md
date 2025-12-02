# VOIDIUM v2.0.6 Release Notes

## 🌍 Localization - Configurable In-Game Messages

### Restart Module Messages
- **Added 3 configurable messages** for server restart notifications:
  - `warningMessage` - Warning sent before restart (supports `%minutes%` placeholder)
  - `restartingNowMessage` - Final message when restart begins
  - `kickMessage` - Disconnect message shown to players
- All messages support color codes (`&c`, `&#RRGGBB`)
- Added `applyLocale()` method to reset messages to EN or CZ defaults

### Ticket Module Messages (Minecraft-side)
- **Fixed 5 hardcoded Czech messages** that were not configurable:
  - `mcBotNotConnectedMessage` - When Discord bot is offline
  - `mcGuildNotFoundMessage` - When Discord server is not found
  - `mcCategoryNotFoundMessage` - When ticket category is not configured
  - `mcTicketCreatedMessage` - Success message after ticket creation
  - `mcDiscordNotFoundMessage` - When player's Discord account is not found on server
- All messages now configurable via web panel with EN/CZ translations

## 🖥️ Web Manager Updates

### New Configuration Fields
- Restart section now includes message configuration with descriptions
- Ticket section expanded with 5 new MC message fields
- Full EN/CZ localization for all new fields and descriptions

### LocalePresets Updates
- Added `getRestartMessages(String locale)` method
- Extended `getTicketMessages()` with MC-side messages

## 📝 Technical Details

- RestartManager now uses `VoidiumConfig.formatMessage()` for proper color code parsing
- TicketManager messages pulled from TicketConfig instead of hardcoded strings
- All modules now support locale reset via web panel

## 📦 Compatibility

- **Minecraft**: 1.21.1 - 1.21.10
- **NeoForge**: 21.1.208+
- **Java**: 21+

## 🔄 Migration Notes

Existing configurations will continue to work with English defaults. To switch to Czech:
1. Open Web Manager
2. Use "Reset Messages to Czech" button in Configuration tab
3. Save configuration

---

**Full Changelog**: v2.0.5...v2.0.6
