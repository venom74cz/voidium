# Release Notes v1.3.6

## 🆕 Novinky
### 🎫 Discord Ticket Systém
- Kompletní implementace ticketovacího systému
- Příkaz `/ticket` pro vytvoření ticketu (funguje na Discordu i ve hře!)
- Podpora tlačítek pro zavírání ticketů
- Konfigurovatelné kategorie, role a zprávy přes Web Panel
- Omezení počtu ticketů na uživatele

### 📈 Statistiky a Grafy
- Přidán živý graf výkonu na Web Panel (Dashboard)
- Zobrazuje historii počtu hráčů a TPS za posledních 24 hodin
- Nový API endpoint `/api/stats/history`

### 🌐 Web Panel
- Přidána podpora lokalizace (Čeština/Angličtina)
- Nová sekce pro konfiguraci Ticketů
- Vylepšené UI a oprava drobných chyb

## 🔧 Technické změny
- Optimalizace `StatsManager` pro sběr dat
- Refactoring `WebManager` pro lepší čitelnost a správu konfigurace
