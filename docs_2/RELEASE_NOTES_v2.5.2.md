# Voidium v2.5.2 Release Notes

## Fixes

### Manual Announce Works Again
- `/voidium announce` no longer fails with `AnnouncementManager is not available!` after config reloads.
- The web dashboard announce action uses the same fixed manager lifecycle.
- Automatic announcements can stay disabled with interval `0` without breaking manual broadcasts.

### Prefixes Stay On Players Only
- Player list prefixes and suffixes now use NeoForge player display events instead of scoreboard team prefixes.
- This fixes the case where prefixes could appear above pets or other named non-player entities.
- Time-based rank formatting still updates for real players in chat and tab list.

### Discord Role Prefix Clearing Fix
- Leaving Discord role prefix or suffix blank now keeps it blank.
- Voidium no longer auto-generates a replacement prefix from the Discord role name when the field is intentionally cleared.

### Rank Toggle Consistency
- If ranks are disabled in `general.json`, player list custom names stop rendering time-based rank decorations as expected.

### Version Metadata Cleanup
- Project version bumped to `2.5.2`.
- Startup OP broadcast now reports the real mod version instead of the stale hardcoded `2.4` string.

## Technical Notes
- `AnnouncementManager` now skips scheduler registration when the configured interval is `0` or lower.
- `Voidium` recreates or shuts down the announcement manager during reload based on the current config state.
- `PlayerListManager` now formats names through `PlayerEvent.NameFormat` and `PlayerEvent.TabListNameFormat` and refreshes player names on the server thread.

## Installation
1. Build or download `voidium-2.5.2.jar`.
2. Replace the previous mod JAR in the server `mods/` folder.
3. Restart the server.

## Compatibility
- Minecraft: 1.21.1-1.21.11
- NeoForge: 21.1.208+
- Java: 21+