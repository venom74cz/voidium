## [1.3.4] - 2025-11-20
### Added
- **Web Control Panel**:
    - Added "Offline Players" list (using `usercache.json`)
    - Added full localization support (EN/CZ) for the Web UI
    - Added "Reset to Default" buttons for configuration (EN/CZ)
- **Discord Integration**:
    - **Whitelist System**: Require players to link their Discord account to join
    - **Account Linking**: Secure code-based verification (`/link <code>`)
    - **Chat Bridge**: Two-way chat synchronization between Minecraft and Discord
    - **Ban Synchronization**: Sync bans between game and Discord server
    - **Daily Stats**: Automated performance reports sent to a specific channel
- **Auto-Rank System**:
    - **Playtime Tracking**: Automatically tracks player activity
    - **Automatic Promotions**: Promotes players based on configurable playtime milestones
    - **Custom Rewards**: Executes commands upon promotion
- **Infrastructure**:
    - Added GitHub Actions workflow for automated builds and releases

### Changed
- **Configuration**:
    - Default configuration values (Discord, Vote messages) are now in English for new installations
    - Existing configurations can be reset to English or Czech via the Web UI

### Notes
- Major update to the Web Interface, making it more usable and accessible
- Version bumped to 1.3.4
