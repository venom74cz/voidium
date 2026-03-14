---
layout: default
title: Integrace (CZ)
---

# 🔗 Integrace

<div class="hero">
   <p><strong>Voidium</strong> se integruje s externími systémy — Discord bot přes <strong>JDA</strong>, vestavěný <strong>Web Control Panel</strong> a <strong>Mixin</strong> hook do vanilla kódu Minecraftu. Tato stránka vysvětluje, jak každá integrace funguje a jak ji nakonfigurovat.</p>

   <h2>Rychlá navigace</h2>
   <div class="card-grid">
      <a class="card" href="#discord">
         <div class="card-title"><span class="card-icon">🤖</span>Discord Bot</div>
         <div class="card-desc">JDA bot, chat bridge, propojení, tikety</div>
      </a>
      <a class="card" href="#web-panel">
         <div class="card-title"><span class="card-icon">🌐</span>Web Control Panel</div>
         <div class="card-desc">Dashboard, Config Studio, konzole, AI</div>
      </a>
      <a class="card" href="#mixiny">
         <div class="card-title"><span class="card-icon">🧩</span>Mixiny</div>
         <div class="card-desc">Hooky do vanilla kódu pro SkinRestorer</div>
      </a>
   </div>
</div>

---

## 🤖 Discord Bot {#discord}

Voidium obsahuje plnohodnotného Discord bota postaveného na **JDA (Java Discord API)**. Běží uvnitř procesu Minecraft serveru — žádný externí hosting bota není potřeba.

### Funkce

| Funkce | Popis |
|--------|-------|
| **Propojení účtů** | Hráči propojí MC účet 6místným kódem (`/link` v Discordu). 10minutová platnost. Konfigurovatelný max počet účtů na Discord uživatele. |
| **Chat Bridge** | Obousměrný relay MC ↔ Discord zpráv s Markdown konverzí, mapováním emoji a volitelným webhook režimem (avatary s MC skiny). |
| **Tiketový systém** | Hráči vytvoří tiket tlačítkem v Discordu → soukromý kanál. Ve hře příkaz `/ticket`. Přepisy v TXT + JSON. |
| **Whitelist** | Freeze-based ověření — nepropojení hráči se nemohou hýbat ani interagovat, dokud se nepropojí přes Discord. |
| **Kanál konzole** | Živý výstup serverové konzole streamovaný do Discord kanálu přes Log4j appender. ANSI barvy odebrány, zprávy sdruženy. |
| **Status serveru** | Zprávy o příchodu/odchodu/smrti. Aktualizace tématu kanálu s počtem online, max hráčů, uptimem. Konfigurovatelný status aktivity (PLAYING/WATCHING/LISTENING/COMPETING). |
| **Synchronizace banů** | Discord ban → MC ban (výchozí zapnuto). MC → Discord ban sync (konfigurovatelné, výchozí vypnuto). |
| **Správa rolí** | Auto-přiřazení linked role. Prefixy z rolí s hex barvami a řazením priorit. |

### Slash příkazy

| Příkaz | Popis |
|--------|-------|
| `/link <kód>` | Propojit Discord účet s MC pomocí 6místného kódu |
| `/unlink` | Odpojit MC účet |
| `/ticket create` | Vytvořit nový tiket |
| `/ticket close` | Zavřít aktuální tiketový kanál |

### Klíčová nastavení (`discord.json`)

| Pole | Typ | Popis |
|------|-----|-------|
| `botToken` | string | Token Discord bota |
| `guildId` | string | ID cílového Discord serveru |
| `chatChannelId` | string | Kanál pro MC ↔ Discord chat |
| `consoleChannelId` | string | Kanál pro výstup konzole |
| `linkChannelId` | string | Kanál kde se používá `/link` |
| `statusChannelId` | string | Kanál pro join/leave/status (záloha: chat kanál) |
| `linkedRoleId` | string | Role přiřazená při propojení účtu |
| `enableWhitelist` | bool | Zmrazit nepropojené hráče |
| `enableChatBridge` | bool | Zapnout chat relay |
| `chatWebhookUrl` | string | Webhook URL pro režim s avatary skinů |
| `syncBansDiscordToMc` | bool | Synchronizovat Discord bany do MC |
| `rolePrefixes` | map | Discord Role ID → `{prefix, suffix, color, priority}` |

Podrobná dokumentace: <a href="Discord_CZ.html">Discord</a>.

---

## 🌐 Web Control Panel {#web-panel}

Voidium dodává single-page webovou aplikaci servírovanou vestavěným JDK `HttpServer` — žádný Netty, Jetty ani externí web server není potřeba.

### Architektura

- **Port:** `WebConfig.port` (výchozí `8081`)
- **Bind adresa:** `WebConfig.bindAddress` (výchozí `0.0.0.0`)
- **Thread pool:** `CachedThreadPool` — automaticky škáluje
- **Čištění sessions:** Každých 5 minut

### Autentizace

Tři metody autentizace, v pořadí priority:

| Metoda | Jak funguje |
|--------|------------|
| **Admin Token** | Permanentní token (auto-generované UUID) — přidejte `?token=<adminToken>` do URL |
| **Bootstrap Token** | Jednorázový, 10minutový token vygenerovaný příkazem `/voidium web` — spotřebuje se při prvním použití |
| **Session Cookie** | `voidium_session` HTTP-only cookie s rolling TTL (výchozí 120 minut) |

### API Endpointy

| Endpoint | Metoda | Účel |
|----------|--------|------|
| `/api/dashboard` | GET | Metriky, seznam hráčů, naplánované časovače, upozornění |
| `/api/feeds` | GET | Chat, konzole a audit log |
| `/api/action` | POST | Akce serveru (restart, announce, entity clean, atd.) |
| `/api/ai/admin` | POST | Admin AI chat asistent |
| `/api/ai/admin/suggest` | POST | AI návrhy konfigurace |
| `/api/ai/players` | GET | Historie AI chatů hráčů |
| `/api/config/schema` | GET | Metadata struktury configů |
| `/api/config/values` | GET | Aktuální hodnoty configů |
| `/api/config/defaults` | GET | Výchozí hodnoty |
| `/api/config/locale` | POST | Aplikovat locale preset |
| `/api/config/preview` | POST | Náhled změn před aplikací |
| `/api/config/diff` | POST | Diff aktuální vs navrhovaný config |
| `/api/config/apply` | POST | Aplikovat změny (vytvoří zálohu) |
| `/api/config/rollback` | POST | Vrátit k záloze |
| `/api/config/reload` | POST | Hot-reload configu ze souborů |
| `/api/console/execute` | POST | Spustit serverové příkazy |
| `/api/discord/roles` | GET | Seznam Discord rolí pro mapování |
| `/api/logout` | POST | Odhlášení |

### Klíčová nastavení (`web.json`)

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `port` | int | `8081` | HTTP port |
| `bindAddress` | string | `0.0.0.0` | Síťové rozhraní |
| `publicHostname` | string | `localhost` | Hostname zobrazený v bootstrap URL |
| `adminToken` | string | *(auto)* | Permanentní autentizační token |
| `sessionTtlMinutes` | int | `120` | TTL session cookie |
| `language` | string | `en` | Jazyk web UI (`en` nebo `cz`) |

Podrobná dokumentace: <a href="Web_CZ.html">Web Control Panel</a>.

---

## 🧩 Mixiny {#mixiny}

Voidium používá **Mixin** pro hookování do vanilla kódu Minecraftu tam, kde NeoForge eventy nestačí.

### Registrované mixiny

Aktuálně je registrován jeden mixin v `voidium.mixins.json`:

| Mixin třída | Cíl | Účel |
|-------------|-----|------|
| `PlayerListMixin` | `net.minecraft.server.players.PlayerList` | SkinRestorer záchranná síť |

### PlayerListMixin

**Hook point:** Metoda `placeNewPlayer()`, těsně před tím, než `broadcastAll()` odešle info paket o hráči všem klientům.

**Co dělá:**
1. Zkontroluje, zda je server v **offline módu** (v online módu přeskočí)
2. Zkontroluje, zda je **SkinRestorer** zapnutý v `general.json`
3. Pokud `GameProfile` hráče nemá texture properties, zavolá `EarlySkinInjector.fetchAndApply()`
4. Tím zajistí, že skin hráče je nastaven **před** tím, než je jeho vzhled broadcastován ostatním hráčům

**Proč Mixin?** NeoForge `PlayerLoggedInEvent` se spustí *po* tom, co už byly informace o hráči broadcastovány. Mixin injektuje *před* broadcast, čímž garantuje, že všichni klienti vidí správný skin od první chvíle.

### Kompatibilita

- **Verze Mixin:** 0.8+
- **Java kompatibilita:** Java 21
- **`required: false`** — mod se načte i pokud se mixin nepodaří aplikovat (graceful degradation)

---

## Související stránky

- <a href="Discord_CZ.html">Discord</a> — Kompletní dokumentace Discord bota
- <a href="Web_CZ.html">Web Control Panel</a> — Kompletní dokumentace web panelu
- <a href="SkinRestorer_CZ.html">SkinRestorer</a> — Dokumentace skin systému
- <a href="Config_CZ.html">Konfigurace</a> — Reference všech konfiguračních souborů
