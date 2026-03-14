---
layout: default
title: Ticket System (EN)
---

# 🎫 Ticket System

<div class="hero">
   <p><strong>Ticket System</strong> lets players create support tickets from Discord or in-game. Each ticket gets a private Discord channel with transcript generation on close.</p>

   <div class="note">
      Requires Discord module. Enable tickets in <code>tickets.json</code> and set up a ticket category in Discord.
   </div>

   <h2>Jump to</h2>
   <div class="card-grid">
      <a class="card" href="#discord-flow">
         <div class="card-title"><span class="card-icon">🤖</span>Discord flow</div>
         <div class="card-desc">Slash commands, close button</div>
      </a>
      <a class="card" href="#ingame-flow">
         <div class="card-title"><span class="card-icon">🎮</span>In-game flow</div>
         <div class="card-desc">/ticket and /reply commands</div>
      </a>
      <a class="card" href="#config">
         <div class="card-title"><span class="card-icon">⚙️</span>Configuration</div>
         <div class="card-desc">tickets.json fields</div>
      </a>
      <a class="card" href="#transcripts">
         <div class="card-title"><span class="card-icon">📄</span>Transcripts</div>
         <div class="card-desc">TXT or JSON save on close</div>
      </a>
   </div>
</div>

## 🤖 Discord flow {#discord-flow}

### Creating a ticket

- Use <code>/ticket create reason:&lt;reason&gt;</code>
- A private channel is created under the configured ticket category
- The channel is visible only to the ticket creator and the support role
- If auto-assign is enabled, the support member with the fewest active tickets is automatically assigned and mentioned
- A welcome embed with a **Close** button is posted

### Closing a ticket

- Click the **Close** button in the welcome embed, or
- Use <code>/ticket close</code> inside the ticket channel
- If transcripts are enabled, conversation history is saved and uploaded before the channel is deleted

### Rate limiting

Ticket creation has a **60-second global cooldown** to prevent Discord API rate limiting (429 errors).

### Limits

`maxTicketsPerUser` controls how many open tickets a single user can have at once.

## 🎮 In-game flow {#ingame-flow}

### Creating a ticket from Minecraft

```
/ticket <reason> <message...>
```

- **reason** is a single word (no spaces)
- **message** can contain spaces
- Requires a linked Discord account (see <a href="Discord_EN.html#linking">Linking</a>)
- A ticket channel is created on Discord and the message is posted

### Replying to a ticket

```
/reply <message...>
```

Messages are relayed from Minecraft → Discord ticket channel and vice versa. If the player has the Voidium client mod, ticket messages appear in a dedicated tab. On vanilla clients, they arrive as private chat messages.

## ⚙️ Configuration {#config}

File: <code>config/voidium/tickets.json</code>

### Core settings

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enableTickets` | boolean | `true` | Master switch |
| `ticketCategoryId` | string | `""` | Discord category ID where ticket channels are created |
| `supportRoleId` | string | `""` | Role ID that can view/manage all tickets |
| `enableAutoAssign` | boolean | `true` | Automatically assign new tickets to the support member with fewest active tickets |
| `maxTicketsPerUser` | int | `3` | Max open tickets per user |

### Messages (Discord)

| Field | Description |
|-------|-------------|
| `ticketCreatedMessage` | Reply when ticket is created |
| `ticketWelcomeMessage` | Welcome text in the ticket embed |
| `ticketCloseMessage` | Message when ticket is closed |
| `noPermissionMessage` | Shown when user lacks permission |
| `ticketLimitReachedMessage` | Shown when max tickets reached |
| `ticketAlreadyClosedMessage` | Shown when closing already-closed ticket |
| `ticketChannelTopic` | Channel topic. Placeholders: `%user%`, `%reason%` |
| `assignedMessage` | Message posted when a support member is auto-assigned. Placeholder: `%assignee%` |

### Messages (in-game)

| Field | Description |
|-------|-------------|
| `mcBotNotConnectedMessage` | Error when bot is offline |
| `mcGuildNotFoundMessage` | Error when guild not found |
| `mcCategoryNotFoundMessage` | Error when category not found |
| `mcTicketCreatedMessage` | Success message in Minecraft |
| `mcDiscordNotFoundMessage` | Error when player isn't linked |

### Transcripts

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enableTranscript` | boolean | `true` | Save conversation on close |
| `transcriptFormat` | string | `"TXT"` | `TXT` or `JSON` |
| `transcriptFilename` | string | `"transcript-%user%-%date%"` | Filename. Placeholders: `%user%`, `%date%`, `%reason%` |

## 📄 Transcripts {#transcripts}

When a ticket is closed with transcripts enabled:

1. Voidium fetches the last ~100 messages from the ticket channel
2. Formats them as TXT (plain text log) or JSON (structured data)
3. Uploads the transcript file to the ticket channel
4. Waits a few seconds, then deletes the channel

The transcript file remains accessible in Discord's message history (uploaded before deletion).

## Related

- <a href="Commands_EN.html">Commands</a> — <code>/ticket</code> and <code>/reply</code>
- <a href="Discord_EN.html">Discord</a> — bot setup, linking
