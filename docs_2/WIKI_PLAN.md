# Voidium – WIKI plán

Cíl: vytvořit kompletní, velmi detailní dokumentaci pro celý mod (správce serveru + uživatelé + vývojáři).

## 1) Struktura WIKI (navržené kapitoly)
Pozn.: Každá stránka bude mít **dvě verze**: CZ a EN (samostatné soubory).
Hlavní rozcestník bude mít výběr jazyka: `docs/wiki/INDEX.md` → `INDEX_CZ.md` / `INDEX_EN.md`.
1. Úvod
   - Co je Voidium
   - Podporované verze (NeoForge, MC)
   - Rychlé odkazy
2. Instalace a požadavky
   - Serverové požadavky
   - Základní instalace
   - Aktualizace / migrace verzí
3. Rychlý start (15 minut)
   - Minimální konfigurace
   - Ověření funkčnosti
4. Konfigurace (soubor po souboru)
   - general.json
   - discord.json
   - tickets.json
   - web.json
   - stats.json
   - ranks.json
   - votes.json
   - playerlist.json
   - entitycleaner.json
   - announcements.json
   - restart.json
   - storage (links.json, pending votes, atd.)
5. Moduly (detailní technický popis)
   - Discord integrace
   - Ticket systém
   - Web panel
   - Auto-rank
   - Stats & reporty
   - Vote (NuVotifier)
   - Playerlist (TAB)
   - Entity cleaner
   - Skin restorer
   - Restart & announcements
6. Příkazy (MC + Discord)
   - /voidium ...
   - /ticket ...
   - Discord slash příkazy
7. Web panel
   - Přístup, bezpečnost, API
8. Lokalizace a texty
   - EN/CZ preset
   - Placeholdery
9. Integrace a kompatibilita
   - JDA, webhooky, mixiny
10. Troubleshooting
   - Nejčastější chyby
   - Debug postup
11. Changelog & release notes

## 2) Milníky
- M1: Discord část (setup, konfigurace, link workflow, chat bridge, logy, status, topic, ban sync, ticket)
- M2: Konfigurace souborů (všechny json)
- M3: Příkazy + web panel
- M4: Ostatní moduly + troubleshooting

## 3) Dnešní fokus
- Vytvořit detailní WIKI pro Discord modul:
   - CZ: [docs/wiki/Discord_CZ.md](wiki/Discord_CZ.md)
   - EN: [docs/wiki/Discord_EN.md](wiki/Discord_EN.md)
