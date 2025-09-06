# Changelog

## [1.2.2] - 2024-12-19
### Added
- Detailed comments in all configuration files explaining each setting
- Configuration files now include usage examples and color code references

### Changed
- Improved configuration file documentation for better user experience

## [1.2.1] - 2024-12-19
### Changed
- Complete translation to English language
- All user-facing text, commands, and messages are now in English
- Updated default announcements to English

## [1.2.0] - 2024-12-19
### Added
- Interactive GUI system accessible via `/voidium gui` command
- Clickable menu system for viewing all configuration settings
- Complete configuration display in-game

### Changed
- Split configuration into separate files (restart.json, announcements.json, general.json)
- Improved configuration organization and management

## [1.1.1] - 2024-12-19
### Added
- Complete GUI implementation with all configuration values displayed
- Detailed restart, announcement, and general settings views
- Back navigation buttons in all GUI menus

## [1.1.0] - 2024-12-19
### Added
- Basic GUI framework with main menu
- Clickable buttons for different configuration sections

## [1.0.9] - 2024-12-19
### Added
- Separated configuration files for better organization
- RestartConfig, AnnouncementConfig, and GeneralConfig classes

## [1.0.8] - 2024-12-19
### Added
- `/voidium players` command - List online players with ping
- `/voidium memory` command - Display server memory usage
- `/voidium cancel` command - Cancel scheduled manual restarts
- `/voidium config` command - Show configuration file locations
- Boss bar countdown for manual restarts (10+ minutes)
- Kick players with "RESTART" reason before server restart

## [1.0.7] - 2024-12-19
### Added
- `/voidium status` command available to all players
- Server status display with TPS, MSPT, and detailed restart information
- Server name, mod count, and performance metrics
- Countdown timer for next restart

## [1.0.6] - 2024-12-19
### Added
- Enhanced status command with comprehensive server information
- TPS (Ticks Per Second) and MSPT (Milliseconds Per Tick) monitoring

## [1.0.5] - 2024-12-19
### Added
- Public status command accessible by all players
- Detailed restart information and countdown

## [1.0.4] - 2024-12-19
### Added
- Manual restart functionality via `/voidium restart <minutes>` command
- Configurable restart warnings at multiple intervals
- Improved restart scheduling system

## [1.0.3] - 2024-12-19
### Added
- Announcement system with configurable messages and intervals
- `/voidium announce` command for manual announcements
- Color code support in announcements

## [1.0.2] - 2024-12-19
### Added
- Configuration reload functionality via `/voidium reload` command
- Improved error handling and logging

## [1.0.1] - 2024-12-19
### Added
- Basic command structure and help system
- Configuration file support

## [1.0.0] - 2024-12-19
### Added
- Initial release
- Automatic server restart system with fixed times and interval modes
- Basic configuration system
- Command framework