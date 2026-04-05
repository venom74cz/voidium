# VOIDIUM - Aktuální Stav

Snapshot: 2026-04-03

Aktuální verze: 2.5.2

## Co je dnes reálně hotové

### Základ projektu

- NeoForge mod pro Minecraft 1.21.1-1.21.10
- Java 21
- build přes Gradle + ShadowJar
- server-side jádro s volitelnou client-side vrstvou pro moderní chat/rendering

### Produkční moduly

- Discord integrace a account linkování
- ticket systém
- vote systém s NuVotifier V1/V2 a offline pending queue
- auto-rank a playerlist formátování
- daily stats a monitoring
- entity cleaner
- maintenance mode
- skin restorer pro offline-mode setupy
- web panel v2 postavený na React + Vite, bundlovaný do JAR
- AI admin a player AI flow v aktuální 2.5 větvi

### Web panel reality

Web panel už není jen plán nebo basic JSON editor.

Od řady 2.5 je reálně hotové hlavně:

- dashboard s live metrikami
- Config Studio
- live console
- live chat feed
- audit log
- SSE/polling update flow
- maintenance banner a quick actions

## Co ještě zůstává otevřené podle roadmapy

- Discord OAuth login do web panelu
- persistentní AI historie napříč sezeními
- statistiky ticketů v dashboardu
- výběr rank definice pro PLAYTIME AI access mód
- auto role sync podle MC ranků
- backup manager
- granulární event logger
- performance alerts
- crash reporter
- player history
- AFK manager
- temporary bans/mutes
- vanish mode

## Jak číst dokumentaci

Aktivní zdroje pravdy:

- `README.md`
- `CHANGELOG.md`
- `docs/INDEX_CZ.md`
- `docs/INDEX_EN.md`
- `docs/FutureFeatures_CZ.md`
- `docs/FutureFeatures_EN.md`
- `CURRENT-STATE.md`

Historická nebo pracovní vrstva:

- `docs_2/` - staré release notes, návrhy, pracovní specifikace a archivní plány

## Praktické pravidlo

Když se dokumenty rozcházejí, ber v pořadí:

1. `CHANGELOG.md`
2. kód v `src/`
3. `CURRENT-STATE.md`
4. wiki v `docs/`
5. archivní `docs_2/`