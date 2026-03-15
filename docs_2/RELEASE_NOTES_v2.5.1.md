# Voidium v2.5.1 Release Notes

## Fixes

### Clickable Discord Links in Minecraft Chat
- Discord messages forwarded into Minecraft now create proper clickable URL components.
- URLs are no longer only colored blue; they open correctly via `OPEN_URL` click events.

### Rank Tooltip in TAB
- Time-based rank prefixes and suffixes shown in the player list now preserve their hover tooltip.
- Hover text again shows the player's current played hours and the required hours for the displayed rank.
- This specifically fixes the case where `PlayerList` was enabled and the tooltip was lost.

### Version Metadata Cleanup
- Project version bumped to `2.5.1`.
- Static mod metadata was synced to the current version.

## Technical Notes
- `PlayerListManager` now builds rank prefix/suffix as chat components with `HoverEvent` instead of plain text only.
- `ChatBridge` now parses URLs into chat components with `ClickEvent.Action.OPEN_URL`.

## Installation
1. Build or download `voidium-2.5.1.jar`.
2. Replace the previous mod JAR in the server `mods/` folder.
3. Restart the server.

## Compatibility
- Minecraft: 1.21.1-1.21.11
- NeoForge: 21.1.208+
- Java: 21+
