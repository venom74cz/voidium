# Voidium v2.5.3 Release Notes

## Fixes

### Player List Prefix Duplication After Refresh
- Player list name formatting now always rebuilds from the player's raw base name.
- This prevents duplicated prefixes after repeated refreshes, reloads, or post-update name recalculation.
- Prefix and suffix rendering now stay anchored to a clean player name instead of reusing an already formatted event result.

### Version Metadata Cleanup
- Project version bumped to `2.5.3`.
- Release metadata is aligned with the new patch release.

## Technical Notes
- `PlayerListManager` no longer uses the current event display component as the formatting input for `PlayerEvent.NameFormat` and `PlayerEvent.TabListNameFormat`.
- Name formatting is now idempotent across repeated `refreshDisplayName()` and `refreshTabListName()` calls.

## Installation
1. Build or download `voidium-2.5.3.jar`.
2. Replace the previous mod JAR in the server `mods/` folder.
3. Restart the server.

## Compatibility
- Minecraft: 1.21.1-1.21.11
- NeoForge: 21.1.208+
- Java: 21+