# Changelog
# Changelog

## [1.3.0] - 2025-09-25
### Added
- Kompletní Votifier modul (`votes.json`, VoteManager, VoteListener) s podporou NuVotifier V2 i legacy RSA v1 současně
- Automatické generování 16-znakového shared secretu a RSA klíčů (pokud chybí) v konfiguraci hlasování
- Pokročilé logování hlasů (plain text i NDJSON archiv) a možnost upozornit OP hráče při chybách

### Changed
- Startup sekvence nyní spouští VoteManager a publikuje verzi 1.3.0 v OP oznámení
- V2 handshake zrcadlí oficiální NuVotifier (challenge, binární rámec, JSON odpovědi `{"status":"ok"}`)

### Notes
- `votes.json` je generován automaticky v `config/voidium/`; veřejný klíč najdete v `votifier_rsa_public.pem`
- Pro tokenový provoz (NuVotifier V2) ponechte shared secret v configu, legacy V1 funguje paralelně přes RSA klíče

## [1.2.8] - 2025-09-15
### Added
- Konfigurovatelný TTL pro persistentní Skin Cache (`skinCacheHours` v `general.json`, výchozí 24h)

### Changed
- Unifikované logování pro Skin subsystem (SkinCache, EarlySkinInjector, SkinRestorer) přes SLF4J
- Broadcast verze aktualizován na 1.2.8

### Notes
- Minimální hodnota `skinCacheHours` je 1 (nižší hodnoty jsou automaticky zvýšeny)
- Při expiraci je záznam odstraněn a skin se znovu stáhne při dalším přihlášení

## [1.2.7] - 2025-09-15
### Added
- **Persistent Skin Cache** – ukládá Mojang skin data do `config/voidium/skin-cache.json` (24h TTL)

### Changed
- Skin Restorer nyní aplikuje skiny okamžitě při připojování (mixin do `PlayerList.placeNewPlayer`)
- Fallback SkinRestorer po přihlášení je zjednodušen (pouze pokud early injection nezískal skin)
- Odstraněny hacky (opakované ADD/REMOVE, přepínání gamemode) – klient dostane správný skin hned při první tab list synchronizaci

### Notes
- `/voidium skin <player>` stále funguje pro manuální obnovu (cache-respektující)
- Staré záznamy v cache se automaticky obnoví po expiraci (24h)

## [1.2.6] - 2025-09-15
### Added
- **Skin Restorer (offline mode)** – načítá Mojang skiny i když je server v `online-mode=false`
- `/voidium skin <player>` příkaz pro ruční obnovení skinu

### Changed
- Vylepšené logování (diagnostika restartů a skin fetchování)
- README + dokumentace rozšířena o Skin Restorer sekci

### Notes
- Okamžitá aplikace skinu může na některých klientech vyžadovat relog (známé omezení klienta)
- Bezpečně deaktivováno automaticky pokud je server v online módu

## [1.2.5] - 2024-12-19
### Fixed
- **DELAY restart timing** - now correctly calculates from server startup time instead of last restart
- Status command now shows accurate countdown for DELAY restart type
- Improved DELAY restart info display with proper time calculation

## [1.2.4] - 2024-12-19
### Added
- **DELAY restart type** - restart server X minutes after startup
- Enhanced restart scheduling with three flexible options
- Improved debug logging for troubleshooting restart issues

### Fixed
- Restart functionality completely overhauled and working properly
- Boss bar timing calculations corrected
- Config loading with proper error handling
- Scheduler management improved

### Changed
- Restart configuration now supports three types: FIXED_TIME, INTERVAL, and DELAY

## [1.2.3] - 2024-12-19
### Fixed
- Major restart system overhaul with debug logging
- Improved error handling and validation
- Fixed scheduler task management
- Corrected boss bar countdown timing

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