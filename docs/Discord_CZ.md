---
layout: default
title: Discord integrace (CZ)
---

# Discord integrace – Voidium WIKI (CZ)

<div class="hero">
   <p><strong>Modul Discord</strong> propojuje server s Discordem: whitelist/linkování, chat bridge, logy, statusy, topic a tickety.</p>
   <div class="section-grid">
      <div class="section-card"><div class="title"><a href="#1-rychly-prehled-funkci">Přehled funkcí</a></div><div class="card-desc">Co modul umí</div></div>
      <div class="section-card"><div class="title"><a href="#4-konfigurace-discordjson">Konfigurace</a></div><div class="card-desc">Všechny klíče a význam</div></div>
      <div class="section-card"><div class="title"><a href="#6-linkovani-uctu--jak-to-funguje">Link workflow</a></div><div class="card-desc">Kódy, whitelist, role</div></div>
      <div class="section-card"><div class="title"><a href="#12-ticket-system-discord--mc">Tickety</a></div><div class="card-desc">Discord + in‑game</div></div>
   </div>
</div>

<div class="note">
Tip: Nezapomeň povolit bot intents (<code>GUILD_MEMBERS</code>, <code>MESSAGE_CONTENT</code>).
</div>

## 1) Rychlý přehled funkcí
- **Whitelist + Linkování účtů** (Discord ↔ Minecraft)
- **Chat Bridge** (MC → Discord a Discord → MC)
- **Webhook zprávy** s avatary podle skinu
- **Console streaming** (serverové logy do Discordu)
- **Status zprávy** (Starting/Online/Stopping/Offline)
- **Channel topic updater** (online hráči, uptime, TPS)
- **Ban Sync** (Discord → MC i MC → Discord)
- **Ticket systém** (Discord slash příkazy + in-game /ticket)

---

## 2) Požadavky
1. **Discord bot účet** (Discord Developer Portal)
2. **Token bota** (neukládat veřejně)
3. **Povolené intents**:
   - `GUILD_MEMBERS`
   - `MESSAGE_CONTENT`
4. Bot musí mít přístup do zvolených kanálů (chat, link, console, status, ticket kategorie).

---

## 3) Instalace & první spuštění
1. Vytvoř bota a přidej ho na server (OAuth2 URL Generator).
2. Povol intents v Developer Portalu.
3. Otevři konfiguraci: `config/voidium/discord.json`.
4. Nastav základní hodnoty:
   - `enableDiscord: true`
   - `botToken: ...`
   - `guildId: ...`
   - `chatChannelId / linkChannelId / consoleChannelId / statusChannelId`
5. Restartuj server nebo použij `/voidium reload`.

Tip: Pokud nechceš některou část používat, ponech příslušné ID prázdné.

---

## 4) Konfigurace: `discord.json`
Níže jsou všechny klíčové položky s popisem.

### 4.1 Základ
- `enableDiscord` – zapíná celou integraci
- `botToken` – token bota
- `guildId` – ID Discord serveru

### 4.2 Aktivita bota
- `botActivityType` – `PLAYING | WATCHING | LISTENING | COMPETING`
- `botActivityText` – text aktivity, podporuje placeholdery (viz níže)

### 4.3 Whitelist / linkování
- `enableWhitelist` – zapíná whitelist systém
- `kickMessage` – zpráva hráči s kódem
- `verificationHintMessage` – doplňková nápověda
- `linkSuccessMessage` – zpráva po propojení
- `alreadyLinkedMessage` – když je dosažen limit účtů
- `maxAccountsPerDiscord` – max. účtů na 1 Discord

### 4.4 Kanály
- `chatChannelId` – Discord kanál pro chat bridge
- `consoleChannelId` – kanál pro logy
- `linkChannelId` – kanál pro linkování (odeslané zprávy se mažou)
- `statusChannelId` – kanál pro status zprávy (pokud je prázdné, použije se `chatChannelId`)

### 4.5 Status zprávy
- `enableStatusMessages` – zapíná status zprávy
- `statusMessageStarting`
- `statusMessageStarted`
- `statusMessageStopping`
- `statusMessageStopped`

### 4.6 Role & prefixy
- `linkedRoleId` – role při linknutí přes link kanál (`linkChannelId`)
- `rolePrefixes` – mapování Discord role → prefix/suffix/barva
- `useHexColors` – true = hex barvy (Voidium klient), false = MC barvy

### 4.7 Ban Sync
- `syncBansDiscordToMc` – ban na Discordu banuje MC účet
- `syncBansMcToDiscord` – ban v MC banuje Discord účet

### 4.8 Chat Bridge
- `enableChatBridge`
- `minecraftToDiscordFormat`
- `discordToMinecraftFormat`
- `translateEmojis` – aktuálně ovlivňuje jen ticket zprávy (chat používá Unicode → alias mapování)

### 4.9 Webhook
- `chatWebhookUrl` – pokud je nastaven, MC → Discord jde přes webhook (s avatarem podle skinu)

### 4.10 Bot Messages (odpovědi bota)
- `invalidCodeMessage`
- `notLinkedMessage`
- `alreadyLinkedSingleMessage` (proměnná: `%uuid%`)
- `alreadyLinkedMultipleMessage` (proměnná: `%count%`)
- `unlinkSuccessMessage`
- `wrongGuildMessage`
- `ticketCreatedMessage`
- `ticketClosingMessage`
- `textChannelOnlyMessage`

### 4.11 Topic updater
- `enableTopicUpdate`
- `channelTopicFormat`
- `uptimeFormat`

---

## 5) Placeholdery
### 5.1 Whitelist / Link
- `%code%` – ověřovací kód
- `%player%` – jméno hráče (pokud je dostupné) / jinak UUID
- `%max%` – max účtů na Discord

### 5.2 Chat a formáty
- `%player%` – jméno hráče (MC → Discord)
- `%message%` – obsah zprávy
- `%user%` – Discord uživatel (Discord → MC)

### 5.3 Status / Activity / Topic
- `%online%` – aktuální online hráči
- `%max%` – max hráčů
- `%tps%` – TPS (aktuálně placeholder)
- `%uptime%` – uptime serveru

### 5.4 Bot messages
- `%uuid%` – UUID propojeného účtu
- `%count%` – počet propojených účtů

---

## 6) Linkování účtů – jak to funguje
### 6.1 In-game workflow (Whitelist)
1. Hráč se připojí.
2. Pokud není propojen, **zůstane „zmrazený“** na místě.
3. V chatu dostane kód (`kickMessage`) + hint (`verificationHintMessage`).
4. Po úspěšném linku se automaticky odblokuje.

Poznámka: Kód vyprší po **10 minutách**.

### 6.2 Link přes Discord slash příkaz
- `/link <code>` – propojí Discord účet s MC účtem.
- `/unlink` – odpojí všechny účty od Discordu.

### 6.3 Link přes link kanál
- Do kanálu `linkChannelId` stačí napsat kód.
- Bot zprávu smaže (kvůli bezpečnosti) a pošle odpověď.
- Pokud už je účet propojen, zobrazí se odpověď s `alreadyLinked*`.

### 6.4 Uložení dat
- Linky se ukládají do `config/voidium/storage/links.json`.

---

## 7) Chat Bridge
### 7.1 MC → Discord
- Odesílá zprávy z MC do `chatChannelId`.
- Používá `minecraftToDiscordFormat`.
- Pokud je nastaven `chatWebhookUrl`, použije webhook s avatarem podle skinu.

### 7.2 Discord → MC
- Zprávy z `chatChannelId` se posílají do MC.
- Markdown se převádí na MC formát:
  - `**bold**` → tučné
  - `*italic*` → kurzíva
  - `__underline__` → podtržení
  - `~~strike~~` → přeškrtnutí
- Unicode emoji se mapují na aliasy (`:smile:`) pro klient rendering.

### 7.3 Join/Leave/Death
- Join/Leave/Death se posílají do Discordu jako zprávy.

---

## 8) Console streaming (logy)
- Aktivuje se automaticky, pokud je `consoleChannelId` vyplněn.
- Logy jsou posílány v batchi každé ~3 sekundy.
- Používá ANSI barvy podle úrovně logu.

---

## 9) Status zprávy
- Zprávy se posílají do `statusChannelId`.
- Typicky se používají při startu/stopu serveru.

---

## 10) Channel Topic Updater
- Pokud je `enableTopicUpdate = true`, bot každých ~10 minut aktualizuje topic kanálu `chatChannelId`.
- Používá `channelTopicFormat` a `uptimeFormat`.

---

## 11) Ban Synchronizace
### 11.1 Discord → MC
- Při banu na Discordu se banují všechny propojené MC účty.

### 11.2 MC → Discord
- Pokud je `syncBansMcToDiscord = true`, ban v MC banuje odpovídající Discord účet.

---

## 12) Ticket systém (Discord + MC)
### 12.1 Discord slash příkazy
- `/ticket create <reason>` – vytvoří ticket
- `/ticket close` – uzavře ticket

### 12.2 In-game příkaz
- `/ticket <reason> <message>` – vytvoří ticket z MC

### 12.3 Konfigurace: `tickets.json`
- `enableTickets`
- `ticketCategoryId`
- `supportRoleId`
- `ticketChannelTopic`
- `maxTicketsPerUser`
- `ticketWelcomeMessage`
- `ticketCloseMessage`
- `ticketLimitReachedMessage`
- `enableTranscript` + `transcriptFormat` + `transcriptFilename`

### 12.4 Transcripty
- Podporuje TXT i JSON.
- Po uzavření ticketu se transcript nahraje do kanálu.

---

## 13) Troubleshooting
**Bot se nespustí:**
- Zkontroluj `botToken`.
- Zkontroluj, že bot má povolené intents.

**Chat Bridge nefunguje:**
- Zkontroluj `enableChatBridge`.
- Ověř `chatChannelId`.

**Linkování nefunguje:**
- Zkontroluj `linkChannelId`.
- Ověř, že kód není expirovaný.

**Logy nechodí:**
- Ověř `consoleChannelId`.
- Bot musí mít práva posílat zprávy do kanálu.

**Ticket se nevytvoří:**
- Ověř `ticketCategoryId`.
- Discord rate-limit: mezi tickety je 60s cooldown.

---

## 14) Bezpečnostní doporučení
- Token nikdy nesdílej.
- Link kanál drž soukromý.
- Omez práva bota na minimum.

---

## 15) Související soubory
- `config/voidium/discord.json`
- `config/voidium/tickets.json`
- `config/voidium/storage/links.json`
