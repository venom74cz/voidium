---
layout: default
title: Discord (CZ)
---

# 🤖 Discord

<div class="hero">
   <p><strong>Modul Discord</strong> propojuje Voidium s Discord serverem: whitelist/linkování, chat bridge, streamování konzole, status/topic a tickety.</p>

   <div class="note">
      Nezapomeň povolit bot intents: <code>GUILD_MEMBERS</code> a <code>MESSAGE_CONTENT</code>.
   </div>

   <h2>Rychlá navigace</h2>
   <div class="card-grid">
      <a class="card" href="#setup">
         <div class="card-title"><span class="card-icon">✅</span>Setup</div>
         <div class="card-desc">Bot, intents, práva</div>
      </a>
      <a class="card" href="#config">
         <div class="card-title"><span class="card-icon">⚙️</span>Konfigurace</div>
         <div class="card-desc">discord.json + tickets.json</div>
      </a>
      <a class="card" href="#linking">
         <div class="card-title"><span class="card-icon">🔗</span>Linkování</div>
         <div class="card-desc">Kódy, link kanál, role</div>
      </a>
      <a class="card" href="#chat-bridge">
         <div class="card-title"><span class="card-icon">💬</span>Chat bridge</div>
         <div class="card-desc">MC ↔ Discord zprávy</div>
      </a>
      <a class="card" href="#tickets">
         <div class="card-title"><span class="card-icon">🎫</span>Tickety</div>
         <div class="card-desc">Discord slash + in-game /ticket</div>
      </a>
      <a class="card" href="#troubleshooting">
         <div class="card-title"><span class="card-icon">🧯</span>Problémy</div>
         <div class="card-desc">Nejčastější chyby</div>
      </a>
   </div>
</div>

## ✅ Setup {#setup}

Potřebuješ:

- Discord bot (Developer Portal) přidaný do guildy
- Povolené intents: <code>GUILD_MEMBERS</code> a <code>MESSAGE_CONTENT</code>
- Doporučené kanály: <strong>chat</strong>, <strong>link</strong>, <strong>console</strong>, <strong>status</strong> + <strong>ticket kategorie</strong>

Rychlý checklist:

1. Uprav <code>config/voidium/discord.json</code>
2. Nastav <code>enableDiscord: true</code>, <code>botToken</code>, <code>guildId</code>
3. Nastav kanály:
   - <code>chatChannelId</code> (pro chat bridge)
   - <code>linkChannelId</code> (doporučeno; viz poznámka)
4. Restart serveru (hlavně při změně tokenu), nebo zkus <code>/voidium reload</code> pro menší změny

<div class="note">
   <strong>Důležité:</strong> ponech zapnutý intent <code>MESSAGE_CONTENT</code>, aby bot mohl číst zprávy pro chat bridge.
</div>

## ⚙️ Konfigurace {#config}

Voidium používá dva soubory:

- <code>config/voidium/discord.json</code> (bot, kanály, whitelist, chat bridge, status/topic, ban sync)
- <code>config/voidium/tickets.json</code> (ticket systém)

### discord.json (klíčové položky)

**Základ**

- <code>enableDiscord</code>, <code>botToken</code>, <code>guildId</code>

**Kanály**

- <code>chatChannelId</code> — chat bridge kanál
- <code>consoleChannelId</code> — streamování logů
- <code>linkChannelId</code> — link kanál (zprávy s kódem se mažou)
- <code>statusChannelId</code> — status zprávy; když je prázdné, použije se <code>chatChannelId</code>

**Whitelist & linkování**

- <code>enableWhitelist</code>
- <code>kickMessage</code>, <code>verificationHintMessage</code>
- <code>linkSuccessMessage</code>, <code>alreadyLinkedMessage</code>, <code>maxAccountsPerDiscord</code>

**Chat bridge**

- <code>enableChatBridge</code>
- <code>minecraftToDiscordFormat</code>, <code>discordToMinecraftFormat</code>
- <code>chatWebhookUrl</code> (volitelné; webhook pro MC → Discord s avatarem podle skinu)

<div class="note">
   <strong>Emoji poznámka:</strong> Discord → MC chat vždy mapuje několik Unicode emoji na <code>:alias:</code> (kvůli renderingu ve Voidium klientu). Přepínač <code>translateEmojis</code> se aktuálně používá jen pro forward ticket zpráv (a efekt může být omezený podle mapování).
</div>

**Status / topic**

- <code>enableStatusMessages</code> + status texty
- <code>enableTopicUpdate</code>, <code>channelTopicFormat</code>, <code>uptimeFormat</code>

**Ban sync**

- <code>syncBansDiscordToMc</code>, <code>syncBansMcToDiscord</code>
- Ban sync používá Mixin injekci (<code>UserBanListMixin</code>) pro automatickou detekci — žádný polling

**Odpovědi bota**

- <code>invalidCodeMessage</code>, <code>notLinkedMessage</code>
- <code>alreadyLinkedSingleMessage</code> (<code>%uuid%</code>), <code>alreadyLinkedMultipleMessage</code> (<code>%count%</code>)
- <code>unlinkSuccessMessage</code>, <code>wrongGuildMessage</code>
- <code>ticketCreatedMessage</code>, <code>ticketClosingMessage</code>, <code>textChannelOnlyMessage</code>

**Role & prefixy**

- <code>linkedRoleId</code> — role při úspěšném linknutí (flow přes link kanál)
- <code>rolePrefixes</code> + <code>useHexColors</code>

### tickets.json (TicketConfig)

Soubor: <code>config/voidium/tickets.json</code>

- <code>enableTickets</code>
- <code>ticketCategoryId</code>, <code>supportRoleId</code>
- <code>ticketChannelTopic</code> (proměnné: <code>%user%</code>, <code>%reason%</code>)
- <code>maxTicketsPerUser</code>
- Zprávy: <code>ticketCreatedMessage</code>, <code>ticketWelcomeMessage</code>, <code>ticketCloseMessage</code>, <code>noPermissionMessage</code>, <code>ticketLimitReachedMessage</code>, <code>ticketAlreadyClosedMessage</code>
- Transcript: <code>enableTranscript</code>, <code>transcriptFormat</code> (<code>TXT</code>/<code>JSON</code>), <code>transcriptFilename</code> (podporuje <code>%user%</code>, <code>%date%</code>, <code>%reason%</code>)
- In-game zprávy (barvy přes <code>&</code>): <code>mcBotNotConnectedMessage</code>, <code>mcGuildNotFoundMessage</code>, <code>mcCategoryNotFoundMessage</code>, <code>mcTicketCreatedMessage</code>, <code>mcDiscordNotFoundMessage</code>

## 🔗 Whitelist & linkování {#linking}

### In-game (whitelist flow)

Když je whitelist zapnutý a hráč není propojený:

1. Hráč se připojí a zůstane <strong>zmrazený</strong> (ne kicketnutý).
2. V chatu dostane 6místný kód (<code>kickMessage</code> + <code>verificationHintMessage</code>).
3. Kód vyprší po <strong>10 minutách</strong>.
4. Po úspěšném linku se hráč odblokuje.

### Na Discordu

- Slash příkazy:
   - <code>/link code:&lt;code&gt;</code>
   - <code>/unlink</code>
- Link kanál:
   - Uživatel napíše kód do <code>linkChannelId</code>
   - Bot zprávu smaže a odpoví (odpověď se po pár sekundách smaže)
   - Volitelně přiřadí <code>linkedRoleId</code>

Data se ukládají do <code>config/voidium/storage/links.json</code>.

## 💬 Chat bridge {#chat-bridge}

### MC → Discord

- Trigger: běžný chat + <code>/say</code>
- Posílá se do <code>chatChannelId</code>
- Použije <code>minecraftToDiscordFormat</code>
- Pokud je <code>chatWebhookUrl</code> vyplněné, použije se webhook (avatar podle skinu)

### Discord → MC

- Zprávy z <code>chatChannelId</code> se forwardují do Minecraftu (když je chat bridge zapnutý)
- Základní Markdown se převádí na MC formát (tučné/kurzíva/podtržení/přeškrtnutí)
- Některé Unicode emoji se převádí na <code>:alias:</code>

### Join / leave / death

Když je chat bridge zapnutý, join/leave/death eventy se posílají do Discordu jako **barevné embedy** s miniaturou skinu hráče:
- **Join** — zelený embed
- **Leave** — červený embed
- **Death** — šedý embed

## 🎫 Tickety {#tickets}

### Discord slash příkazy

- <code>/ticket create reason:&lt;reason&gt;</code>
- <code>/ticket close</code>

Vytváření ticketů má rate-limit (globální cooldown ~60 sekund), aby se zabránilo Discord 429.

### In-game

- <code>/ticket &lt;reason&gt; &lt;message...&gt;</code>
- <code>/reply &lt;message...&gt;</code>

Detailní syntaxe je tady: <a href="Commands_CZ.html">Příkazy</a>.

### Transcripty

Pokud jsou transcripty zapnuté:

- Voidium stáhne až ~100 posledních zpráv
- Nahraje TXT/JSON soubor do ticket kanálu
- A pak kanál pár sekund po uzavření smaže

## 🧯 Řešení problémů {#troubleshooting}

**Bot se nespustí**

- Ověř <code>botToken</code> a <code>guildId</code>
- Ověř, že jsou povolené intents v Developer Portalu

**Discord → MC zprávy nechodí**

- <code>enableDiscord</code> a <code>enableChatBridge</code> musí být true
- Zkontroluj <code>chatChannelId</code>
- Ověř intent <code>MESSAGE_CONTENT</code>

**Linkování nefunguje**

- Kód platí ~10 minut
- Ověř práva v <code>linkChannelId</code> (send + delete)

**Ticket se nevytvoří**

- Ověř <code>ticketCategoryId</code> + práva bota
- Počítej s globálním cooldownem ~60s

## Další

- <a href="Config_CZ.html">Konfigurace</a>
- <a href="Commands_CZ.html">Příkazy</a>
