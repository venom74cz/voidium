# VOIDIUM

Intelligent server control for NeoForge servers.

Voidium combines restart automation, announcements, Discord integration, tickets, votes, ranks, entity cleanup, monitoring, AI tooling, and a bundled web admin panel into one mod.

## Links

- Discord: https://discord.com/invite/3JYz3KWutJ
- Releases: https://github.com/venom74cz/voidium/releases
- Issues: https://github.com/venom74cz/voidium/issues
- Wiki CZ: `docs/INDEX_CZ.md`
- Wiki EN: `docs/INDEX_EN.md`

## Core Modules

- Restart system: fixed-time, interval, delayed, and manual restarts with warnings and countdowns.
- Announcements: scheduled rotation and one-off broadcasts with color support.
- Entity Cleaner: configurable cleanup interval, warnings, protections, and whitelists.
- Monitoring: TPS, MSPT, memory, disk, player count, uptime, and history.
- Discord: linking, whitelist flow, bridge, status messages, role styling, and moderation sync.
- Tickets: Discord and in-game support workflow with transcripts and auto-assignment.
- Votes: NuVotifier support, offline vote queue, payout controls, and archive logging.
- Ranks: playtime progression with optional custom conditions and tooltip support.
- Player List: TAB header/footer formatting, player name formatting, and update interval.
- AI: admin assistant, player chat, staged config suggestions, and redacted context.
- Maintenance mode: login block plus web toggle and dashboard banner.
- Skin restorer: offline-mode skin cache with refresh support.

## Web Admin Panel

The web panel is bundled into the mod JAR and served by Voidium itself.

- Stack: React 19, Vite 6, TypeScript, HashRouter.
- Runtime: live dashboard updates, console execution, chat feed, audit feed, AI tools, config editing.
- Config Studio: preview, diff, apply, rollback, locale presets, structured editors, secret masking.
- Auth: bootstrap token via `/voidium web`, persistent admin token, rolling session cookie.
- Demo mode: local frontend preview falls back to mock data when backend endpoints are unavailable.

### Page Map

- `Dashboard`: primary overview, alert banner, live performance cards, history charts, timers, and quick control entry points.
- `Players`: live roster actions and `playerlist.json` editing.
- `Announcements`: rotating message pool and one-off live broadcasts.
- `Restarts`: restart scheduling view and manual restart dispatch.
- `Entity Cleaner`: cleanup controls, heatmap preview, and `entitycleaner.json`.
- `Discord`: Discord runtime state, roles, and `discord.json`.
- `Ranks`: progression preview and `ranks.json`.
- `Votes`: pending queue operations and `votes.json`.
- `Tickets`: live ticket workflow and `tickets.json`.
- `Modules`: runtime module health overview.
- `Server`: host info and direct `server.properties` editor.
- `Statistics`: daily Discord report configuration and `stats.json`.
- `Live Feeds`: chat, console, audit, and alerts.
- `Console`: command execution plus audit trail.
- `AI Assistant`: admin AI tools and `ai.json`.
- `Settings`: panel preferences, access info, `general.json`, and `web.json`.

## Commands

### Operators

- `/voidium reload`
- `/voidium restart <minutes>`
- `/voidium cancel`
- `/voidium announce <message>`
- `/voidium players`
- `/voidium memory`
- `/voidium config`
- `/voidium skin <player>`
- `/voidium votes pending [player]`
- `/voidium votes clear`
- `/voidium clear`
- `/voidium clear items|mobs|xp|arrows`
- `/voidium clear preview`
- `/voidium web`

### Players

- `/voidium status`
- `/link`
- `/ticket <reason> <message>`

## Configuration

All Voidium config files live in `config/voidium/`.

| File | Purpose |
|------|---------|
| `general.json` | Master toggles and shared module settings |
| `web.json` | Web panel port, bind, language, auth, and session settings |
| `announcements.json` | Broadcast rotation and prefix |
| `restart.json` | Restart schedules and restart messages |
| `discord.json` | Discord bot, bridge, linking, whitelist, roles |
| `tickets.json` | Ticket workflow, limits, transcripts, and messages |
| `stats.json` | Scheduled statistics reports |
| `ranks.json` | Rank progression and promotion text |
| `playerlist.json` | TAB header/footer and player naming |
| `votes.json` | Vote listener, rewards, queue logging |
| `entitycleaner.json` | Cleanup interval, protections, whitelists |
| `ai.json` | Admin AI and player AI access rules |

### Storage Files

Stored in `config/voidium/storage/`:

| File | Purpose |
|------|---------|
| `links.json` | Discord to Minecraft account links |
| `pending-votes.json` | Offline vote queue |
| `votes.log` | Plain-text vote log |
| `votes-history.ndjson` | Vote analytics archive |
| `voidium_stats_data.json` | Player statistics data |
| `voidium_ranks_data.json` | Rank assignment data |
| `player_progress.json` | Custom progression counters |
| `skin-cache.json` | Cached skin data |
| `last_restart.txt` | Last restart timestamp |

## Technical

- Minecraft: 1.21.1 to 1.21.10
- Loader: NeoForge 21.1.208+
- Java: 21
- Architecture: server-side core with optional client-side UX layer
- License: MIT

## Documentation

- `CURRENT-STATE.md`: current project state and doc map
- `CHANGELOG.md`: release history and recent changes
- `docs/INDEX_CZ.md`: Czech documentation entry point
- `docs/INDEX_EN.md`: English documentation entry point
- `docs/FutureFeatures_CZ.md`: Czech roadmap
- `docs/FutureFeatures_EN.md`: English roadmap
- `docs_2/`: archive and older working notes

One mod. Complete control. Lower ops friction.
