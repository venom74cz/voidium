---
layout: default
title: Discord Integration (EN)
---

# Discord Integration – Voidium WIKI (EN)

## 1) Feature overview
- **Whitelist + Account Linking** (Discord ↔ Minecraft)
- **Chat Bridge** (MC → Discord and Discord → MC)
- **Webhook messages** with skin-based avatars
- **Console streaming** (server logs to Discord)
- **Status messages** (Starting/Online/Stopping/Offline)
- **Channel topic updater** (online players, uptime, TPS)
- **Ban Sync** (Discord → MC and MC → Discord)
- **Ticket system** (Discord slash commands + in-game /ticket)

---

## 2) Requirements
1. **Discord bot account** (Discord Developer Portal)
2. **Bot token** (keep private)
3. **Enabled intents**:
   - `GUILD_MEMBERS`
   - `MESSAGE_CONTENT`
4. Bot must have access to configured channels (chat, link, console, status, ticket category).

---

## 3) Installation & first run
1. Create a bot and invite it to your server (OAuth2 URL Generator).
2. Enable intents in the Developer Portal.
3. Open configuration: `config/voidium/discord.json`.
4. Set required values:
   - `enableDiscord: true`
   - `botToken: ...`
   - `guildId: ...`
   - `chatChannelId / linkChannelId / consoleChannelId / statusChannelId`
5. Restart the server or use `/voidium reload`.

Tip: Leave an ID empty if you don’t want to use that feature.

---

## 4) Configuration: `discord.json`
Below are all key fields with explanations.

### 4.1 Basics
- `enableDiscord` – enables the integration
- `botToken` – bot token
- `guildId` – Discord server ID

### 4.2 Bot activity
- `botActivityType` – `PLAYING | WATCHING | LISTENING | COMPETING`
- `botActivityText` – activity text, supports placeholders (see below)

### 4.3 Whitelist / linking
- `enableWhitelist` – enables whitelist flow
- `kickMessage` – message shown to player with code
- `verificationHintMessage` – extra hint
- `linkSuccessMessage` – message after linking
- `alreadyLinkedMessage` – when limit is reached
- `maxAccountsPerDiscord` – max accounts per Discord

### 4.4 Channels
- `chatChannelId` – chat bridge channel
- `consoleChannelId` – console logs channel
- `linkChannelId` – link channel (messages are deleted)
- `statusChannelId` – status channel (falls back to `chatChannelId` if empty)

### 4.5 Status messages
- `enableStatusMessages`
- `statusMessageStarting`
- `statusMessageStarted`
- `statusMessageStopping`
- `statusMessageStopped`

### 4.6 Roles & prefixes
- `linkedRoleId` – role assigned when linking via link channel (`linkChannelId`)
- `rolePrefixes` – Discord role → prefix/suffix/color map
- `useHexColors` – true = hex colors (Voidium client), false = MC colors

### 4.7 Ban Sync
- `syncBansDiscordToMc` – Discord ban bans MC accounts
- `syncBansMcToDiscord` – MC ban bans Discord account

### 4.8 Chat Bridge
- `enableChatBridge`
- `minecraftToDiscordFormat`
- `discordToMinecraftFormat`
- `translateEmojis` – currently affects only ticket messages (chat uses Unicode → alias mapping)

### 4.9 Webhook
- `chatWebhookUrl` – if set, MC → Discord uses webhook with skin avatar

### 4.10 Bot Messages (responses)
- `invalidCodeMessage`
- `notLinkedMessage`
- `alreadyLinkedSingleMessage` (variable: `%uuid%`)
- `alreadyLinkedMultipleMessage` (variable: `%count%`)
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

## 5) Placeholders
### 5.1 Whitelist / Link
- `%code%` – verification code
- `%player%` – player name (if available) / otherwise UUID
- `%max%` – max accounts per Discord

### 5.2 Chat formats
- `%player%` – player name (MC → Discord)
- `%message%` – message content
- `%user%` – Discord user (Discord → MC)

### 5.3 Status / Activity / Topic
- `%online%` – current online players
- `%max%` – max players
- `%tps%` – TPS (placeholder for now)
- `%uptime%` – server uptime

### 5.4 Bot messages
- `%uuid%` – linked account UUID
- `%count%` – number of linked accounts

---

## 6) Account linking – how it works
### 6.1 In-game workflow (Whitelist)
1. Player joins.
2. If not linked, they **remain frozen** in place.
3. They receive a code (`kickMessage`) + hint (`verificationHintMessage`).
4. After successful link, they are unfrozen.

Note: Code expires after **10 minutes**.

### 6.2 Link via Discord slash command
- `/link <code>` – links Discord account to MC account.
- `/unlink` – unlinks all accounts from Discord.

### 6.3 Link via link channel
- Send the code into `linkChannelId`.
- Bot deletes the message (privacy) and replies.
- If already linked, bot uses `alreadyLinked*` response.

### 6.4 Data storage
- Links are stored in `config/voidium/storage/links.json`.

---

## 7) Chat Bridge
### 7.1 MC → Discord
- Sends MC chat to `chatChannelId`.
- Uses `minecraftToDiscordFormat`.
- If `chatWebhookUrl` is set, uses webhook with skin avatar.

### 7.2 Discord → MC
- Messages from `chatChannelId` go to MC.
- Markdown to MC formatting:
  - `**bold**` → bold
  - `*italic*` → italic
  - `__underline__` → underline
  - `~~strike~~` → strikethrough
- Unicode emoji are mapped to aliases (`:smile:`) for client rendering.

### 7.3 Join/Leave/Death
- Join/Leave/Death are sent to Discord as messages.

---

## 8) Console streaming (logs)
- Auto-enabled when `consoleChannelId` is set.
- Logs are batched every ~3 seconds.
- Uses ANSI colors by log level.

---

## 9) Status messages
- Messages are sent to `statusChannelId`.
- Typically used on server start/stop.

---

## 10) Channel Topic Updater
- If `enableTopicUpdate = true`, bot updates `chatChannelId` topic every ~10 minutes.
- Uses `channelTopicFormat` and `uptimeFormat`.

---

## 11) Ban Sync
### 11.1 Discord → MC
- A Discord ban bans all linked MC accounts.

### 11.2 MC → Discord
- If `syncBansMcToDiscord = true`, MC ban bans the Discord account.

---

## 12) Ticket system (Discord + MC)
### 12.1 Discord slash commands
- `/ticket create <reason>` – creates a ticket
- `/ticket close` – closes the ticket

### 12.2 In-game command
- `/ticket <reason> <message>` – creates a ticket from MC

### 12.3 Configuration: `tickets.json`
- `enableTickets`
- `ticketCategoryId`
- `supportRoleId`
- `ticketChannelTopic`
- `maxTicketsPerUser`
- `ticketWelcomeMessage`
- `ticketCloseMessage`
- `ticketLimitReachedMessage`
- `enableTranscript` + `transcriptFormat` + `transcriptFilename`

### 12.4 Transcripts
- TXT and JSON are supported.
- Transcript is uploaded after ticket close.

---

## 13) Troubleshooting
**Bot won’t start:**
- Check `botToken`.
- Ensure intents are enabled.

**Chat Bridge not working:**
- Check `enableChatBridge`.
- Verify `chatChannelId`.

**Linking not working:**
- Check `linkChannelId`.
- Ensure the code isn’t expired.

**Logs not sent:**
- Check `consoleChannelId`.
- Bot needs permission to send messages.

**Ticket not created:**
- Check `ticketCategoryId`.
- Discord rate-limit: 60s cooldown between tickets.

---

## 14) Security recommendations
- Never share the token.
- Keep the link channel private.
- Grant the bot minimal permissions.

---

## 15) Related files
- `config/voidium/discord.json`
- `config/voidium/tickets.json`
- `config/voidium/storage/links.json`
