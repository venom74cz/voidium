# Voidium Release Notes: v1.3.5 → v2.0.0

Complete changelog from v1.3.5 to the latest version (v2.0.0).
Previous public release was v1.3.4.

---

## [2.0.0] - 2025-11-24

### 🎉 Major Version Release

#### Configurable Discord Bot Messages
- **NEW**: All Discord bot responses are now fully customizable via web panel!
- **9 New Configuration Fields**:
  - `invalidCodeMessage` - Message when invalid/expired code is entered
  - `notLinkedMessage` - Message when user is not linked
  - `alreadyLinkedSingleMessage` - Message when already linked (1 account) - Variables: `%uuid%`
  - `alreadyLinkedMultipleMessage` - Message when linked to multiple accounts - Variables: `%count%`
  - `unlinkSuccessMessage` - Message when accounts are unlinked
  - `wrongGuildMessage` - Message when command used in wrong server
  - `ticketCreatedMessage` - Message when ticket is created
  - `ticketClosingMessage` - Message when ticket is closing
  - `textChannelOnlyMessage` - Message when command requires text channel
- **Locale Support**: All messages available in English and Czech presets

#### Field Descriptions & Variables
- **NEW**: Every configuration field now has helpful tooltips!
- **Features**:
  - Shows available placeholders for each field
  - Explains what each option does
  - Styled with purple accent border for visibility
- **Available Placeholders**:
  - **PlayerList**: `%online%`, `%max%`, `%tps%`, `%playtime%`, `%time%`, `%memory%`
  - **Name Format**: `%rank_prefix%`, `%player_name%`, `%rank_suffix%`
  - **Discord Messages**: `%player%`, `%user%`, `%message%`, `%code%`, `%max%`, `%uuid%`, `%count%`
  - **Color Codes**: `&0-f` (legacy), `&#RRGGBB` (hex), `&l/o/n/m/r` (formatting)

#### Role Prefix Editor Improvements
- **Hex Color Support**: Role prefixes now use exact Discord role colors (`&#RRGGBB` format)
- **Auto-Update**: Prefix automatically regenerates when selecting different role
- **Full Field Saving**: Color and Priority fields now save correctly with prefix/suffix

#### Dark Theme Dropdowns
- **NEW**: All select/dropdown elements styled with dark theme
- **Features**:
  - Dark background (`#1e1e2e`) instead of white
  - Custom purple arrow indicator
  - Hover effects with purple accent
  - Better text visibility

### 🐛 Bug Fixes

- **Fixed**: `Content may not be null` error in button interactions (ticket close)
- **Fixed**: Null-safe getters for all bot messages with fallback values
- **Fixed**: Role prefix editor not saving color/priority fields
- **Fixed**: Console spam from PlayerListManager debug logs
- **Fixed**: Console spam from WebManager debug logs
- **Fixed**: TRANSLATIONS object missing config description keys

### 🔧 Technical Improvements

- **All translations exported to JavaScript** (not just a subset)
- **Null-safe bot message getters** prevent crashes from missing config entries
- **Improved CSS** for field descriptions and dropdown menus

---

## [1.3.8] - 2025-11-24

### 🎯 Major New Features

#### Custom Conditions for Auto-Rank System
- **NEW**: Ranks can now require achievements beyond playtime!
- **Supported Conditions**:
  - `KILL_MOBS`: Kill specific mob types (e.g., 100 zombies for "Monster Hunter")
  - `VISIT_BIOMES`: Visit unique biomes (e.g., 5 different biomes for "Explorer")
  - `BREAK_BLOCKS`: Break specific block types (e.g., 1000 stone for "Miner")
  - `PLACE_BLOCKS`: Place specific block types (e.g., 500 planks for "Builder")
- **Progress Tracking**: Automatic player achievement tracking with persistence
- **Smart System**: Progress auto-saves periodically and on server shutdown
- **Example Configuration**:
```json
{
  "type": "PREFIX",
  "value": "&6[Monster Hunter] ",
  "hours": 50,
  "customConditions": [
    {
      "type": "KILL_MOBS",
      "target": "minecraft:zombie",
      "count": 100
    },
    {
      "type": "VISIT_BIOMES",
      "target": "minecraft:nether_wastes",
      "count": 1
    }
  ]
}
```

#### Ticket Transcript System
- **NEW**: Automatically save full ticket conversation history!
- **Two Formats**:
  - **TXT**: Human-readable format with timestamps and author tags
  - **JSON**: Structured data format for programmatic processing
- **Features**:
  - Captures up to 100 messages per ticket
  - Includes Discord embeds in transcript
  - Configurable filename with placeholders (%user%, %date%, %reason%)
  - Automatic upload to Discord channel before deletion
- **Configuration** (via Web Panel):
  - `enableTranscript`: Enable/disable (default: true)
  - `transcriptFormat`: Choose TXT or JSON
  - `transcriptFilename`: Template for filename

#### Player List (TAB) Configuration - Web UI
- **NEW**: Full web interface for custom player list configuration!
- **Features**:
  - Configure header lines 1-3 (with placeholders: %online%, %max%, %tps%)
  - Configure footer lines 1-3 (with placeholders: %tps%, %ping%)
  - Custom player name formatting with rank prefixes/suffixes
  - Default prefix/suffix configuration
  - Option to combine multiple ranks or use highest priority only
  - Configurable update interval (minimum 3 seconds)
- **Fully Localized**: Complete English and Czech translations with tooltips

#### Discord Role Prefix Designer
- **NEW**: Professional color customization tools in web panel!
- **Features**:
  - **Live Color Preview**: See exactly how your prefixes will look
  - **Hex Color Picker**: Visual color selection with validation
  - **Built-in Templates**: Quick-start with Minecraft color presets
  - **Copy/Paste Roles**: Duplicate configurations between roles
  - **Color Validation**: Ensures valid hex codes and formatting
  - **Export/Import**: Share role configurations easily

### 🎨 UI/UX Improvements

#### Enhanced Web Panel
- **Collapsible Config Sections**: All config cards now fold/unfold with click
- **Sticky Save Bar**: Save button always visible at bottom of page
- **Smooth Animations**: Professional transitions for better UX
- **Better Organization**: Long config pages now easier to navigate
- **All Configs Accessible**: 9/9 configs now available (Web, General, Restart, Announcements, Ranks, Discord, Stats, Vote, Tickets, PlayerList)

### 🔧 Technical Improvements

#### New Systems
- **ProgressTracker**: Persistent player achievement tracking system
- **ProgressEventListener**: Automatic event capture for custom conditions
- **Transcript Generator**: Message history capture and formatting

#### Performance
- **Optimized Tracking**: Biome checks throttled to 1/second
- **Session Deduplication**: Biomes counted once per session
- **Batched Saves**: Auto-save every 5 biomes or 10 kills
- **Thread-Safe**: ConcurrentHashMap for all shared data

#### Code Quality
- **~528 Lines Added**: New functionality for custom conditions, transcripts, and UI
- **Zero Build Errors**: Clean compilation
- **Complete Documentation**: Release notes, upgrade instructions, examples

### 🌍 Translations
- **32 New Translation Keys**: English + Czech
- **Complete Coverage**: All new features fully translated
- **Consistent Quality**: Professional, clear descriptions

### 📝 Configuration Files
- **New**: `player_progress.json` - Stores player achievement progress
- **Updated**: `tickets.json` - Added transcript settings (3 new fields)
- **Updated**: `ranks.json` - Added customConditions support

---

## [1.3.7] - 2025-11-23

### Added
- **Enhanced Link Channel**: Now responds to ANY message in link channel with player's link status
- **Automatic Code Processing**: Link channel automatically detects and processes verification codes
- **Ticket System Enhancements**:
  - Two-way communication between Discord tickets and in-game players
  - In-game command: `/ticket <reason> <message>` with initial message support
  - Messages from Discord ticket channels are relayed to linked player in Minecraft
  - Player can reply in-game and message appears in ticket channel

### Fixed
- **Channel ID Handling**: All Discord IDs (channels, roles) now save correctly without precision loss
  - Changed from `long` to `String` type to prevent JavaScript number precision issues
  - Affects: linkChannelId, chatChannelId, consoleChannelId, statsChannelId, ticketCategoryId, supportRoleId, whitelistRoleId
- **Statistics Debug Logging**: Enhanced logging for troubleshooting stats reports
- **Web Panel**: All configuration sections now properly initialized and visible

### Changed
- **Complete Czech Localization**: All ticket messages and UI elements fully translated
- **Config Organization**: Moved `links.json` to proper `config/voidium/` directory
- **Link Channel Behavior**: Now more interactive and user-friendly

---

## [1.3.6] - 2025-11-22

### Added
- **Player List (TAB) Customization System**:
  - Custom header with 3 configurable lines
  - Custom footer with 3 configurable lines
  - Player name formatting with rank prefixes/suffixes
  - Support for color codes (& and &#RRGGBB)
  - Option to combine multiple ranks or use highest priority
  - Configurable update interval (minimum 3 seconds)
  - Live placeholders: %online%, %max%, %tps%, %ping%, %rank_prefix%, %player_name%, %rank_suffix%

### Technical
- **New Config File**: `playerlist.json` with full customization options
- **PlayerListManager**: Automatic updates and formatting
- **Integration**: Works seamlessly with Auto-Rank system

---

## [1.3.5] - 2025-11-21

### Added
- **Vote System Enhancements**:
  - Better error handling and logging
  - Improved pending vote queue management
  - Enhanced OP notifications

### Fixed
- **Configuration Loading**: Fixed initialization order issues
- **Memory Management**: Improved cleanup on server shutdown
- **Stats Reporting**: Fixed timing issues with daily reports

### Changed
- **Code Refactoring**: Improved overall code organization
- **Performance**: Optimized event handling

---

## 📊 Summary of Changes (v1.3.5 → v2.0.0)

### New Major Features (6)
1. ✅ **Configurable Bot Messages** - All Discord bot responses customizable (v2.0.0)
2. ✅ **Field Descriptions** - Helpful tooltips with placeholders for all config fields (v2.0.0)
3. ✅ **Custom Rank Conditions** - Achievement-based rank requirements (v1.3.8)
4. ✅ **Ticket Transcripts** - Automatic conversation history save (v1.3.8)
5. ✅ **Player List Web UI** - TAB customization through browser (v1.3.8)
6. ✅ **Discord Role Designer** - Professional prefix/color tools with hex support (v1.3.8, improved v2.0.0)

### System Enhancements
- Enhanced link channel with automatic code processing
- Two-way ticket communication (Discord ↔ Minecraft)
- Collapsible config sections in web panel
- Sticky save bar for better UX
- Progress tracking system with persistence
- Channel ID handling fixes (String instead of long)

### New Configuration Options
- 3 new ticket config fields (transcript settings)
- 13 new player list config fields
- Custom conditions support in ranks
- New progress tracking storage

### Technical Stats
- **Files Created**: 5 (ProgressTracker, ProgressEventListener, PlayerListManager, 2 docs)
- **Files Modified**: 12+ files across multiple releases
- **Lines Added**: 700+ lines of new functionality
- **Translation Keys**: 50+ new entries (EN + CZ)
- **Build Status**: ✅ All versions compile successfully

### Performance & Stability
- Thread-safe implementations (ConcurrentHashMap)
- Optimized event handling (throttled checks)
- Batched auto-saves for efficiency
- Better error handling and logging
- Memory management improvements

---

## 🚀 Upgrade Instructions

### From v1.3.4 to v1.3.8

**Automatic Updates**:
- All new config fields are added automatically on first start
- Existing configurations are preserved
- New files created as needed:
  - `config/voidium/player_progress.json` (created on first player action)
  - Updated: `tickets.json`, `ranks.json`, `playerlist.json`

**Web Panel Changes**:
1. Access web panel at `http://localhost:8080`
2. Navigate to "Config" tab
3. New sections available:
   - **Player List (TAB)** - Configure custom player list
   - **Tickets** - New transcript settings at bottom
   - **Discord** - Enhanced role prefix designer

**Manual Configuration** (Optional):

**1. Custom Rank Conditions** - Edit `config/voidium/ranks.json`:
```json
{
  "type": "PREFIX",
  "value": "&6[Explorer] ",
  "hours": 20,
  "customConditions": [
    {
      "type": "VISIT_BIOMES",
      "target": "minecraft:desert",
      "count": 1
    },
    {
      "type": "VISIT_BIOMES",
      "target": "minecraft:jungle",
      "count": 1
    }
  ]
}
```

**2. Ticket Transcripts** - Enabled by default, customize in web panel or `tickets.json`:
```json
{
  "enableTranscript": true,
  "transcriptFormat": "TXT",
  "transcriptFilename": "ticket-%user%-%date%.txt"
}
```

**3. Player List** - Configure through web panel or edit `playerlist.json`

### Breaking Changes
**None** - All changes are backward compatible!

---

## 🐛 Known Issues & Limitations

### Custom Conditions
- ⚠️ No web UI for adding custom conditions yet (requires manual JSON editing)
- ⚠️ Block break/place tracking does NOT auto-save (only on server stop)
- 💡 **Tip**: Add conditions via JSON editor, web UI coming in v1.4.0

### Ticket Transcripts
- ⚠️ Limited to last 100 messages (Discord API constraint)
- ⚠️ Transcript uploads before channel deletion (5 second window)
- ✅ Both limitations are acceptable for normal use cases

### Player List
- ✅ No known issues - fully functional

---

## 🎯 Future Roadmap (v1.4.0+)

### Planned Features
1. **Custom Conditions Web UI**:
   - Visual condition builder
   - Dropdown for condition types
   - Auto-complete for targets (mobs, biomes, blocks)

2. **Progress Viewer**:
   - In-game command: `/voidium progress [player]`
   - Show current progress toward next rank
   - List all completed conditions

3. **Transcript Enhancements**:
   - Optional database storage
   - Searchable transcript archive
   - HTML format with styling

4. **Performance Dashboard**:
   - Real-time server metrics
   - Historical performance graphs
   - Resource usage monitoring

---

## 📞 Support & Community

- **GitHub**: https://github.com/venom74cz/voidium
- **Issues**: Report bugs or request features via GitHub Issues
- **Wiki**: Full documentation available in repository

---

## ❤️ Credits

**Development**: Adam J.  
**AI Assistant**: Claude (Anthropic)  
**Minecraft Version**: 1.21.1  
**NeoForge Version**: 21.1.78  
**License**: MIT  

---

**Thank you for using Voidium! 🚀**

_"One mod. Complete control. Zero complexity."_
