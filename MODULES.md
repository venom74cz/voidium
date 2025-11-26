# VOIDIUM - PŘEHLED MODULŮ

Kompletní dokumentace všech funkčních modulů systému Voidium.

---

## 🔄 RESTART MODULE
**Balíček:** `cz.voidium.server.RestartManager`  
**Konfigurace:** `RestartConfig.java`

### Funkce:
- **Plánované restarty** - Automatické restarty v pevně stanovených časech (např. 3:00, 15:00)
- **Intervalové restarty** - Restart každých X hodin
- **Zpožděné restarty** - Restart za X minut od startu serveru
- **Manuální restarty** - Příkaz `/voidium restart <minuty>`
- **Boss bar countdown** - Vizuální odpočet do restartu
- **Strukturovaná varování** - Konfigurovatelná upozornění (např. 30min, 15min, 5min, 1min)
- **Zrušení restartu** - Příkaz `/voidium cancel`
- **Typy restartů** - FIXED_TIME, INTERVAL, DELAYED, MANUAL

---

## 📢 ANNOUNCEMENT MODULE
**Balíček:** `cz.voidium.server.AnnouncementManager`  
**Konfigurace:** `AnnouncementConfig.java`

### Funkce:
- **Plánované oznámení** - Automatické broadcasty v intervalech
- **Více oznámení** - Podpora více různých zpráv s vlastními intervaly
- **Manuální broadcast** - Příkaz `/voidium announce <zpráva>`
- **Barevné kódy** - Plná podpora Minecraft color codes (&a, &b, atd.)
- **Formátování** - Bold, italic, underline pomocí &l, &o, &n
- **Vlastní prefix** - Konfigurovatelný prefix pro všechna oznámení
- **Hot reload** - Změny v konfiguraci bez restartu serveru
- **Broadcast pro OPs** - Speciální zprávy pouze pro operátory

---

## 📊 MONITORING & STATS MODULE
**Balíček:** `cz.voidium.stats.StatsManager`  
**Konfigurace:** `StatsConfig.java`

### Funkce:
- **Real-time TPS tracking** - Sledování ticků za sekundu
- **MSPT monitoring** - Milisekundy na tick
- **Memory usage** - Využití RAM (used/max)
- **Player count tracking** - Počet hráčů online
- **24-hour history** - Ukládání dat za posledních 24 hodin
- **Automatické čištění** - Mazání starých dat
- **Daily reports** - Automatické denní reporty do Discordu
- **Peak statistics** - Sledování maxim (nejvíce hráčů, nejhorší TPS)
- **Average calculations** - Průměrné hodnoty za den
- **JSON persistence** - Ukládání do `voidium_stats_data.json`
- **Příkazy** - `/voidium status`, `/voidium memory`, `/voidium players`

---

## 🌐 WEB CONTROL PANEL MODULE
**Balíček:** `cz.voidium.web.WebManager`  
**Konfigurace:** `WebConfig.java`

### Funkce:
- **Token-based auth** - Bezpečné přihlášení přes konzolový link
- **Config editor** - Editace VŠECH config souborů z prohlížeče
- **Live statistics** - Real-time grafy TPS a počtu hráčů (24h historie)
- **Player management** - Zobrazení online/offline hráčů, kick/ban
- **Discord link management** - Správa propojení Discord-Minecraft účtů
- **Form-based editor** - Uživatelsky přívětivé formuláře místo raw JSON
- **Field validation** - Kontrola správnosti hodnot
- **Field descriptions** - Nápověda u každého pole
- **Dynamic lists** - Přidávání/odebírání položek (announcements, ranks, atd.)
- **Collapsible sections** - Skládací sekce pro přehlednost
- **Sticky save bar** - Plovoucí lišta pro uložení změn
- **Smooth animations** - Plynulé přechody a animace
- **Bilingual UI** - Angličtina a čeština
- **Reset to default** - Obnovení výchozích hodnot
- **Discord Role Designer** - Live preview barev, hex picker, šablony, copy/paste

---

## 👾 DISCORD INTEGRATION MODULE
**Balíček:** `cz.voidium.discord.*`  
**Konfigurace:** `DiscordConfig.java`

### Komponenty:

#### DiscordManager
- **Bot initialization** - Připojení k Discord API pomocí JDA
- **Status messages** - Automatické zprávy o stavu serveru (Starting, Online, Stopping, Offline)
- **Channel management** - Správa všech Discord kanálů
- **Role management** - Synchronizace rolí

#### DiscordWhitelist
- **Whitelist system** - Vyžadování Discord propojení pro vstup na server
- **Max accounts limit** - Limit účtů na jeden Discord (konfigurovatelné)
- **Kick on disconnect** - Automatické vyhození nepropojených hráčů
- **Custom messages** - Vlastní zprávy pro nepropojené hráče

#### LinkManager
- **Account linking** - Propojení Discord ↔ Minecraft účtů
- **Code verification** - 6místný kód pro ověření
- **Smart link channel** - Automatické odpovědi na zprávy v link kanálu
- **Auto-processing** - Automatické zpracování ověřovacích kódů
- **Role assignment** - Automatické přidělení role po propojení
- **Persistent storage** - Ukládání do `links.json`
- **Příkaz** - `/link` in-game

#### ChatBridge
- **Two-way chat sync** - Obousměrná synchronizace chatu Discord ↔ Minecraft
- **Emoji translation** - Převod Discord emoji na text
- **Markdown formatting** - Podpora **bold**, *italic*, atd.
- **Username display** - Zobrazení jmen s rolemi/prefixy
- **Death messages** - Přeposílání death messages do Discordu
- **Join/leave messages** - Oznámení o připojení/odpojení hráčů

#### DiscordConsoleAppender
- **Console streaming** - Streamování konzole do Discord kanálu
- **Batched messages** - Dávkové odesílání pro výkon
- **Log filtering** - Filtrování důležitých logů
- **Real-time monitoring** - Sledování serveru v reálném čase

#### TicketManager
- **Ticket creation** - Vytváření support ticketů z Discordu i in-game
- **Private channels** - Soukromé kanály pro každý ticket
- **Two-way messaging** - Komunikace Discord ↔ in-game
- **Support role** - Automatický přístup pro support team
- **Ticket limits** - Limit ticketů na uživatele
- **Close button** - Jednoduché zavření ticketu
- **Transcripts** - Automatické ukládání historie (TXT/JSON)
- **Příkaz** - `/ticket <důvod> <zpráva>`

#### Additional Features
- **Channel topic updater** - Automatická aktualizace topic s live stats
- **Ban synchronization** - Obousměrná synchronizace banů Discord ↔ Minecraft
- **Daily stats reports** - Automatické denní reporty výkonu
- **Role sync** - Mapování Discord rolí na in-game permissions
- **Webhook support** - Logování eventů přes webhooky

---

## 🎫 TICKET SYSTEM MODULE
**Balíček:** `cz.voidium.discord.TicketManager`  
**Konfigurace:** `TicketConfig.java`

### Funkce:
- **Discord ticket creation** - Vytváření ticketů z Discordu
- **In-game ticket creation** - Příkaz `/ticket <důvod> <zpráva>`
- **Private channels** - Každý ticket = soukromý kanál
- **Automatic notifications** - Ping uživatele při vytvoření
- **Support role integration** - Automatický přístup pro support
- **Two-way communication** - Zprávy Discord ↔ in-game
- **Ticket limits** - Max ticketů na uživatele (anti-spam)
- **Easy closing** - Tlačítko pro zavření
- **Full transcripts** - Kompletní historie konverzace (TXT nebo JSON)
- **Bilingual** - Angličtina a čeština
- **Full customization** - Všechny zprávy konfigurovatelné

---

## 📈 AUTO-RANK SYSTEM MODULE
**Balíček:** `cz.voidium.ranks.*`  
**Konfigurace:** `RanksConfig.java`

### Komponenty:

#### RankManager
- **Automatic promotions** - Automatické povyšování na základě podmínek
- **Playtime tracking** - Sledování času stráveného na serveru
- **AFK detection** - Detekce AFK hráčů (neovlivňuje playtime)
- **Custom rewards** - Spouštění příkazů při povýšení
- **Prefix/Suffix support** - Podpora pro prefix i suffix ranky
- **Persistent storage** - Ukládání do `rank_storage.json`

#### ProgressTracker
- **Achievement tracking** - Sledování pokroku hráčů
- **Multiple conditions** - Více typů podmínek současně
- **Persistent progress** - Trvalé ukládání pokroku
- **JSON storage** - Ukládání do `progress_data.json`

#### ProgressEventListener
- **VISIT_BIOMES** - Sledování navštívených biomů
- **KILL_MOBS** - Počítání zabitých mobů (podle typu)
- **BREAK_BLOCKS** - Sledování vytěžených bloků (podle typu)
- **PLACE_BLOCKS** - Počítání postavených bloků (podle typu)
- **Session tracking** - Sledování per-session dat
- **Auto-save** - Periodické ukládání pokroku

#### RankStorage
- **Player data persistence** - Ukládání dat hráčů
- **Playtime tracking** - Celkový čas na serveru
- **Rank history** - Historie povýšení
- **JSON format** - Čitelný formát dat

### Příklad použití:
```json
{
  "rankName": "Explorer",
  "playtimeMinutes": 600,
  "customConditions": {
    "VISIT_BIOMES": {"count": 10},
    "KILL_MOBS": {"minecraft:zombie": 50}
  }
}
```

---

## 🎁 VOTE REWARDS MODULE (NuVotifier)
**Balíček:** `cz.voidium.vote.*`  
**Konfigurace:** `VoteConfig.java` (votes.json)

### Komponenty:

#### VoteManager
- **NuVotifier V2 support** - Token-based autentizace
- **Legacy V1 support** - RSA signature validation
- **Dual protocol** - Současná podpora V1 i V2
- **Auto-generated keys** - Automatické generování RSA klíčů a shared secret
- **Reward commands** - Spouštění příkazů při hlasování
- **Persistent storage** - Ukládání do `votes.json`

#### PendingVoteQueue
- **Offline vote queue** - Ukládání hlasů offline hráčů
- **Auto-delivery** - Automatické doručení při přihlášení
- **Silent delivery** - Tichá distribuce (bez spam v chatu)
- **Persistent queue** - Ukládání do `pending-votes.json`
- **Admin commands** - `/voidium votes pending [player]`, `/voidium votes clear`

#### VoteListener
- **TCP listener** - Naslouchání na konfigurovaném portu
- **Handshake validation** - Ověření správnosti připojení
- **Signature verification** - Kontrola RSA podpisů (V1)
- **Token validation** - Ověření tokenů (V2)
- **Error handling** - Robustní zpracování chyb

#### VoteKeyUtil
- **RSA key generation** - Generování 2048-bit RSA klíčů
- **Shared secret generation** - 16-character random secret
- **Key persistence** - Ukládání klíčů do konfigurace

### Funkce:
- **Dual logging** - `votes.log` (plain text) + `votes-history.ndjson` (analytics)
- **OP notifications** - Volitelné oznámení pro operátory
- **Verbose diagnostics** - Detailní diagnostika při selhání
- **Auto-retry** - Automatické opakování při chybách
- **Vote statistics** - Sledování počtu hlasů

---

## 🧍 OFFLINE-MODE SKIN RESTORER MODULE
**Balíček:** `cz.voidium.skin.*` + `cz.voidium.server.SkinRestorer`  
**Konfigurace:** `GeneralConfig.java`

### Komponenty:

#### SkinRestorer
- **Early join injection** - Aplikace skinů před spawnem (bez relogu)
- **Automatic fetching** - Automatické stahování skinů z Mojang API
- **Manual refresh** - Příkaz `/voidium skin <player>`
- **Online-mode detection** - Automatické vypnutí v online módu

#### SkinCache
- **Persistent cache** - Ukládání skinů do `skin_cache.json`
- **TTL support** - Konfigurovatelná doba platnosti (hodiny)
- **Automatic expiration** - Automatické mazání starých záznamů
- **Refresh on login** - Obnovení expirovaných skinů při přihlášení

#### SkinFetcher
- **Mojang API integration** - Stahování z Mojang API
- **UUID lookup** - Převod jména na UUID
- **Texture download** - Stahování texture dat
- **Error handling** - Robustní zpracování chyb

#### EarlySkinInjector
- **Mixin injection** - Injekce před spawnem hráče
- **Property manipulation** - Úprava GameProfile properties
- **Seamless application** - Aplikace bez viditelného efektu

#### SkinData
- **Data structure** - Struktura pro ukládání skin dat
- **Timestamp tracking** - Sledování času stažení
- **Serialization** - Serializace do JSON

### Funkce:
- **No relog required** - Skin se aplikuje okamžitě
- **Configurable TTL** - `skinCacheHours` (minimum 1 hodina)
- **Safe in online mode** - Automaticky se vypne
- **Persistent cache** - Přežije restart serveru

---

## 🎨 PLAYER LIST (TAB) CUSTOMIZATION MODULE
**Balíček:** `cz.voidium.playerlist.PlayerListManager`  
**Konfigurace:** `PlayerListConfig.java`

### Funkce:
- **Custom header** - 3 řádky nad seznamem hráčů
- **Custom footer** - 3 řádky pod seznamem hráčů
- **Live placeholders** - %online%, %max%, %tps%, %ping%
- **Player name formatting** - Prefix/suffix s color codes
- **Discord role integration** - Automatické prefixy podle Discord rolí
- **Default prefix/suffix** - Fallback pro hráče bez rolí
- **Multiple rank modes** - Kombinace všech rolí nebo jen nejvyšší priorita
- **Live updates** - Konfigurovatelný refresh interval (min 3s)
- **Color code support** - Plná podpora Minecraft color codes
- **Priority system** - Řazení rolí podle priority

### Příklad:
```
Header:
  Line 1: &6&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
  Line 2: &e&lMŮJ SERVER &7| &fOnline: &a%online%&7/&a%max% &7| &fTPS: &a%tps%
  Line 3: &6&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬

Player: &c[ADMIN] &fPlayerName &7(%ping%ms)

Footer:
  Line 1: &6&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
  Line 2: &7Discord: &bdiscord.gg/example
  Line 3: &6&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
```

---

## 🔧 CONFIGURATION MODULE
**Balíček:** `cz.voidium.config.*`

### Config soubory:

#### VoidiumConfig (general.json)
- **Module toggles** - Zapnutí/vypnutí jednotlivých modulů
- **Global settings** - Globální nastavení

#### RestartConfig (restarts.json)
- **Restart types** - FIXED_TIME, INTERVAL, DELAYED
- **Warning times** - Časy varování před restartem
- **Boss bar settings** - Nastavení boss baru

#### AnnouncementConfig (announcements.json)
- **Multiple announcements** - Více oznámení s vlastními intervaly
- **Prefix/suffix** - Vlastní prefix a suffix
- **Color codes** - Podpora barev

#### DiscordConfig (discord.json)
- **Bot token** - Discord bot token
- **Channel IDs** - ID všech kanálů (chat, console, status, atd.)
- **Role IDs** - ID rolí (linked, support, atd.)
- **Status messages** - Zprávy pro různé stavy serveru
- **Whitelist settings** - Nastavení whitelistu

#### WebConfig (web.json)
- **Port** - Port pro web panel
- **Token** - Autentizační token
- **CORS settings** - CORS nastavení

#### StatsConfig (stats.json)
- **Collection interval** - Interval sběru dat
- **Retention period** - Doba uchovávání dat
- **Daily report settings** - Nastavení denních reportů

#### RanksConfig (ranks.json)
- **Rank definitions** - Definice ranků
- **Playtime requirements** - Požadavky na playtime
- **Custom conditions** - Vlastní podmínky
- **Reward commands** - Příkazy při povýšení

#### TicketConfig (tickets.json)
- **Category ID** - ID kategorie pro tickety
- **Support role ID** - ID support role
- **Max tickets** - Max ticketů na uživatele
- **Messages** - Všechny zprávy systému

#### PlayerListConfig (playerlist.json)
- **Header/footer** - Nastavení hlavičky a patičky
- **Prefix/suffix** - Prefixy a suffixy
- **Discord role mapping** - Mapování Discord rolí
- **Update interval** - Interval aktualizace

#### VoteConfig (votes.json)
- **Port** - Port pro NuVotifier
- **RSA keys** - RSA klíče (auto-generated)
- **Shared secret** - Shared secret (auto-generated)
- **Reward commands** - Příkazy při hlasování

#### LocalePresets
- **English** - Anglické překlady
- **Czech** - České překlady
- **Reset to default** - Obnovení výchozích hodnot

---

## 🎮 COMMANDS MODULE
**Balíček:** `cz.voidium.commands.*`

### VoidiumCommand
**Operátoři:**
- `/voidium restart <minuty>` - Naplánovat restart
- `/voidium cancel` - Zrušit restart
- `/voidium announce <zpráva>` - Broadcast zpráva
- `/voidium players` - Seznam hráčů s pingem
- `/voidium memory` - Využití paměti
- `/voidium config` - Reload konfigurace
- `/voidium reload` - Reload všech managerů
- `/voidium skin <player>` - Obnovit skin hráče
- `/voidium votes pending [player]` - Zobrazit čekající hlasy
- `/voidium votes clear` - Vymazat čekající hlasy

**Hráči:**
- `/voidium status` - Status serveru (TPS, paměť, uptime)

### TicketCommand
**Hráči:**
- `/ticket <důvod> <zpráva>` - Vytvořit support ticket

**Operátoři:**
- Správa ticketů přes Discord

---

## 🔌 MIXIN MODULE
**Balíček:** `cz.voidium.mixin.*`

### PlayerListMixin
- **TAB list injection** - Injekce do player listu
- **Header/footer override** - Přepsání hlavičky/patičky
- **Name formatting** - Formátování jmen hráčů

### ServerLoginPacketListenerImplMixin
- **Login interception** - Zachycení přihlášení
- **Whitelist check** - Kontrola Discord propojení
- **Skin injection** - Injekce skinů před spawnem

### UserBanListMixin
- **Ban synchronization** - Synchronizace banů s Discordem
- **Bidirectional sync** - Obousměrná synchronizace

### StoredUserEntryAccessor
- **Data access** - Přístup k interním datům
- **Profile manipulation** - Manipulace s GameProfile

---

## 📦 PERSISTENCE & STORAGE

### JSON soubory v `config/voidium/`:
- `general.json` - Hlavní konfigurace
- `restarts.json` - Konfigurace restartů
- `announcements.json` - Konfigurace oznámení
- `discord.json` - Discord konfigurace
- `web.json` - Web panel konfigurace
- `stats.json` - Stats konfigurace
- `ranks.json` - Ranks konfigurace
- `tickets.json` - Ticket konfigurace
- `playerlist.json` - Player list konfigurace
- `votes.json` - Vote konfigurace + RSA klíče
- `links.json` - Discord-Minecraft propojení
- `pending-votes.json` - Čekající hlasy
- `rank_storage.json` - Data hráčů (playtime, ranky)
- `progress_data.json` - Pokrok hráčů (achievements)
- `voidium_stats_data.json` - Statistická data (24h historie)
- `skin_cache.json` - Cache skinů

### Log soubory:
- `votes.log` - Plain text log hlasů
- `votes-history.ndjson` - NDJSON log pro analytics

---

## 🔐 SECURITY & AUTHENTICATION

### Web Panel:
- **Token-based auth** - Jednorázový token z konzole
- **Session management** - Správa sessions
- **CORS protection** - CORS ochrana

### Discord:
- **Bot token** - Bezpečný Discord bot token
- **Role verification** - Ověření rolí
- **Permission checks** - Kontrola oprávnění

### Vote System:
- **RSA signatures** - 2048-bit RSA podpisy
- **Token validation** - Ověření tokenů
- **Shared secret** - 16-character secret

---

## 📊 PERFORMANCE & OPTIMIZATION

### Optimalizace:
- **Async operations** - Asynchronní operace kde možné
- **Batched messages** - Dávkové odesílání zpráv
- **Cached data** - Cachování často používaných dat
- **Periodic cleanup** - Periodické čištění starých dat
- **Efficient storage** - Efektivní ukládání dat
- **Minimal overhead** - Minimální overhead na server

### Monitoring:
- **TPS tracking** - Sledování výkonu serveru
- **Memory monitoring** - Sledování paměti
- **Player count** - Sledování počtu hráčů
- **Statistics collection** - Sběr statistik

---

## 🌍 LOCALIZATION

### Podporované jazyky:
- **English** - Kompletní anglická lokalizace
- **Czech** - Kompletní česká lokalizace

### Lokalizované komponenty:
- Web panel UI
- Discord zprávy
- In-game zprávy
- Config descriptions
- Error messages
- Help texts

---

## 🔄 LIFECYCLE & EVENT HANDLING

### Server Events:
- **ServerStartingEvent** - Inicializace Discord manageru
- **ServerStartedEvent** - Start všech managerů
- **ServerStoppingEvent** - Graceful shutdown
- **ServerStoppedEvent** - Finální cleanup

### Player Events:
- **PlayerLoggedInEvent** - Přihlášení hráče
- **PlayerLoggedOutEvent** - Odhlášení hráče
- **PlayerTickEvent** - Tick hráče (pro tracking)

### Custom Events:
- **LivingDeathEvent** - Smrt entity (pro kill tracking)
- **BlockEvent.BreakEvent** - Rozbití bloku
- **BlockEvent.EntityPlaceEvent** - Postavení bloku

---

## 🎯 INTEGRATION POINTS

### Discord Integration:
- JDA 5.0.0-beta.24
- WebSocket connection
- Event listeners
- Command handlers

### Web Integration:
- HTTP server
- REST API
- WebSocket (pro live data)
- JSON API

### Minecraft Integration:
- NeoForge events
- Mixins
- Commands
- Player data

---

**Celkem modulů: 12**  
**Celkem config souborů: 11**  
**Celkem příkazů: 13**  
**Podporované jazyky: 2**  
**Verze Minecraft: 1.21.1 - 1.21.10**  
**Loader: NeoForge 21.1.208+**
