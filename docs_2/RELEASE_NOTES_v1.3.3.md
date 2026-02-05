## [1.3.3] - 2025-10-25
### Changed
- /say (oznámení o hlasu) se nyní provádí vždy ihned po přijetí hlasu, i když hráč není online
- Ostatní příkazy (např. /give) se provádí pouze pokud je hráč online
- Při vyplácení pending votes (po přihlášení) se /say již nespouští, pouze odměna

### Notes
- Úprava zajišťuje, že hlasování je vždy veřejně oznámeno v chatu, i když hráč není přítomen
- Verze zvýšena na 1.3.3

## [1.3.3] - 2025-10-25
### Changed
- /say (vote announcement) is now always executed immediately when a vote is received, even if the player is offline
- Other commands (e.g. /give) are only executed if the player is online
- When delivering pending votes (after login), /say is not executed again, only the reward

### Notes
- This change ensures that voting is always publicly announced in chat, even if the player is not present
- Version bumped to 1.3.3
