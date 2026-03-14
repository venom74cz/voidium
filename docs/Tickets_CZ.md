---
layout: default
title: Ticket systém (CZ)
---

# 🎫 Ticket systém

<div class="hero">
   <p><strong>Ticket systém</strong> umožňuje hráčům vytvářet support tickety z Discordu nebo přímo ze hry. Každý ticket dostane soukromý Discord kanál s generováním přepisu při zavření.</p>

   <div class="note">
      Vyžaduje Discord modul. Zapněte tickety v <code>tickets.json</code> a nastavte ticket kategorii v Discordu.
   </div>

   <h2>Rychlá navigace</h2>
   <div class="card-grid">
      <a class="card" href="#discord-flow">
         <div class="card-title"><span class="card-icon">🤖</span>Discord flow</div>
         <div class="card-desc">Slash příkazy, tlačítko zavření</div>
      </a>
      <a class="card" href="#ingame-flow">
         <div class="card-title"><span class="card-icon">🎮</span>In-game flow</div>
         <div class="card-desc">/ticket a /reply příkazy</div>
      </a>
      <a class="card" href="#konfigurace">
         <div class="card-title"><span class="card-icon">⚙️</span>Konfigurace</div>
         <div class="card-desc">tickets.json pole</div>
      </a>
      <a class="card" href="#prepisy">
         <div class="card-title"><span class="card-icon">📄</span>Přepisy</div>
         <div class="card-desc">TXT nebo JSON při zavření</div>
      </a>
   </div>
</div>

## 🤖 Discord flow {#discord-flow}

### Vytvoření ticketu

- Použijte <code>/ticket create reason:&lt;důvod&gt;</code>
- Vytvoří se soukromý kanál pod nastavenou ticket kategorií
- Kanál vidí pouze tvůrce ticketu a support role
- Pokud je zapnuté auto-přiřazení, je automaticky přiřazen a zmíněn support člen s nejméně aktivními tickety
- Vloží se uvítací embed s tlačítkem **Close**

### Zavření ticketu

- Klikněte na tlačítko **Close** v uvítacím embedu, nebo
- Použijte <code>/ticket close</code> v ticket kanálu
- Pokud jsou zapnuté přepisy, historie konverzace se uloží a nahraje před smazáním kanálu

### Rate limiting

Vytváření ticketů má **60sekundový globální cooldown** pro prevenci rate limitingu Discord API (chyby 429).

### Limity

`maxTicketsPerUser` kontroluje kolik otevřených ticketů může mít jeden uživatel najednou.

## 🎮 In-game flow {#ingame-flow}

### Vytvoření ticketu z Minecraftu

```
/ticket <důvod> <zpráva...>
```

- **důvod** je jedno slovo (bez mezer)
- **zpráva** může obsahovat mezery
- Vyžaduje propojený Discord účet (viz <a href="Discord_CZ.html#linking">Propojení</a>)
- Na Discordu se vytvoří ticket kanál a zpráva se odešle

### Odpověď na ticket

```
/reply <zpráva...>
```

Zprávy se přeposílají z Minecraftu → Discord ticket kanál a naopak. Pokud má hráč Voidium klientský mod, ticket zprávy se zobrazují v dedikované záložce. Na vanilla klientech přijdou jako soukromé chat zprávy.

## ⚙️ Konfigurace {#konfigurace}

Soubor: <code>config/voidium/tickets.json</code>

### Základní nastavení

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `enableTickets` | boolean | `true` | Hlavní přepínač |
| `ticketCategoryId` | string | `""` | ID Discord kategorie pro ticket kanály |
| `supportRoleId` | string | `""` | ID role, která vidí/spravuje všechny tickety |
| `enableAutoAssign` | boolean | `true` | Automaticky přiřadí nový ticket support členovi s nejméně aktivními tickety |
| `maxTicketsPerUser` | int | `3` | Max otevřených ticketů na uživatele |

### Zprávy (Discord)

| Pole | Popis |
|------|-------|
| `ticketCreatedMessage` | Odpověď při vytvoření ticketu |
| `ticketWelcomeMessage` | Uvítací text v ticket embedu |
| `ticketCloseMessage` | Zpráva při zavření ticketu |
| `noPermissionMessage` | Když uživatel nemá oprávnění |
| `ticketLimitReachedMessage` | Když je dosažen max ticketů |
| `ticketAlreadyClosedMessage` | Když se zavírá už zavřený ticket |
| `ticketChannelTopic` | Topic kanálu. Placeholdery: `%user%`, `%reason%` |
| `assignedMessage` | Zpráva při auto-přiřazení support člena. Placeholder: `%assignee%` |

### Zprávy (in-game)

| Pole | Popis |
|------|-------|
| `mcBotNotConnectedMessage` | Chyba když je bot offline |
| `mcGuildNotFoundMessage` | Chyba když guild není nalezen |
| `mcCategoryNotFoundMessage` | Chyba když kategorie nebyla nalezena |
| `mcTicketCreatedMessage` | Zpráva o úspěchu v Minecraftu |
| `mcDiscordNotFoundMessage` | Chyba když hráč není propojený |

### Přepisy

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `enableTranscript` | boolean | `true` | Uloží konverzaci při zavření |
| `transcriptFormat` | string | `"TXT"` | `TXT` nebo `JSON` |
| `transcriptFilename` | string | `"transcript-%user%-%date%"` | Název souboru. Placeholdery: `%user%`, `%date%`, `%reason%` |

## 📄 Přepisy {#prepisy}

Když se ticket zavře se zapnutými přepisy:

1. Voidium stáhne posledních ~100 zpráv z ticket kanálu
2. Zformátuje je jako TXT (plaintext log) nebo JSON (strukturovaná data)
3. Nahraje soubor přepisu do ticket kanálu
4. Počká pár sekund, pak smaže kanál

Soubor přepisu zůstane přístupný v historii zpráv Discordu (nahraný před smazáním).

## Další

- <a href="Commands_CZ.html">Příkazy</a> — <code>/ticket</code> a <code>/reply</code>
- <a href="Discord_CZ.html">Discord</a> — nastavení bota, propojení
