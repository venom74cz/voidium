# VOIDIUM v2.0.4 Release Notes

## 🐛 Bug Fixes

### Discord Channel Topic Update - Missing Web Panel Configuration
- **Fixed missing web panel fields** for Discord channel topic auto-update feature
- Added three configuration fields to Discord section:
  - `enableTopicUpdate` - Enable/disable automatic channel topic updates
  - `channelTopicFormat` - Customize topic format with variables (%online%, %max%, %tps%, %uptime%)
  - `uptimeFormat` - Customize uptime display format (%days%d %hours%h %minutes%m)
- Fields are now properly displayed and saved in web manager
- Full English and Czech localization support

## 📝 Technical Details

- Channel topic feature was working in backend but couldn't be configured via web panel
- Topic updates every **6 minutes** (respects Discord API rate limits)
- All configuration now accessible through web interface

## 📦 Compatibility

- **Minecraft**: 1.21.1 - 1.21.10
- **NeoForge**: 21.1.205+
- **Java**: 21+

## 🔄 Migration Notes

No migration needed. If you were using channel topic updates, you can now configure them via web panel instead of editing JSON files manually.

---

**Full Changelog**: v2.0.3...v2.0.4
