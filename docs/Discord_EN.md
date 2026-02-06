---
layout: default
title: Discord (EN)
---

# 🤖 Discord

<div class="hero">
   <p><strong>Discord module</strong> connects Voidium to your Discord server: whitelist/linking, chat bridge, console streaming, status/topic updates and tickets.</p>

   <div class="note">
      You must enable Discord intents: <code>GUILD_MEMBERS</code> and <code>MESSAGE_CONTENT</code>.
   </div>

   <h2>Jump to</h2>
   <div class="card-grid">
      <a class="card" href="#setup">
         <div class="card-title"><span class="card-icon">✅</span>Setup</div>
         <div class="card-desc">Bot, intents, permissions</div>
      </a>
      <a class="card" href="#config">
         <div class="card-title"><span class="card-icon">⚙️</span>Configuration</div>
         <div class="card-desc">discord.json + tickets.json</div>
      </a>
      <a class="card" href="#linking">
         <div class="card-title"><span class="card-icon">🔗</span>Linking</div>
         <div class="card-desc">Whitelist codes, link channel, roles</div>
      </a>
      <a class="card" href="#chat-bridge">
         <div class="card-title"><span class="card-icon">💬</span>Chat bridge</div>
         <div class="card-desc">MC ↔ Discord messages</div>
      </a>
      <a class="card" href="#tickets">
         <div class="card-title"><span class="card-icon">🎫</span>Tickets</div>
         <div class="card-desc">Discord slash + in-game /ticket</div>
      </a>
      <a class="card" href="#troubleshooting">
         <div class="card-title"><span class="card-icon">🧯</span>Troubleshooting</div>
         <div class="card-desc">Common problems and fixes</div>
      </a>
   </div>
</div>

## ✅ Setup {#setup}

You need:

- A Discord bot (Developer Portal) invited to your guild
- Enabled intents: <code>GUILD_MEMBERS</code> and <code>MESSAGE_CONTENT</code>
- Channels (recommended): <strong>chat</strong>, <strong>link</strong>, <strong>console</strong>, <strong>status</strong>, plus a <strong>ticket category</strong>

Minimal first-run checklist:

1. Edit <code>config/voidium/discord.json</code>
2. Set <code>enableDiscord: true</code>, <code>botToken</code>, <code>guildId</code>
3. Configure channel IDs:
   - <code>chatChannelId</code> (required for chat bridge)
   - <code>linkChannelId</code> (recommended; see note below)
4. Restart the server (for token changes), or try <code>/voidium reload</code> for small edits

<div class="note">
   <strong>Important:</strong> keep <code>MESSAGE_CONTENT</code> intent enabled so the bot can read Discord messages for the chat bridge.
</div>

## ⚙️ Configuration {#config}

Voidium uses two Discord-related config files:

- <code>config/voidium/discord.json</code> (bot, channels, whitelist, chat bridge, status/topic, ban sync)
- <code>config/voidium/tickets.json</code> (ticket system)

### discord.json (key fields)

**Basics**

- <code>enableDiscord</code>, <code>botToken</code>, <code>guildId</code>

**Channels**

- <code>chatChannelId</code> — Discord ↔ MC chat bridge channel
- <code>consoleChannelId</code> — console log streaming target
- <code>linkChannelId</code> — channel where users can paste verification codes (messages are deleted)
- <code>statusChannelId</code> — status messages; if empty, Voidium uses <code>chatChannelId</code>

**Whitelist & linking**

- <code>enableWhitelist</code>
- <code>kickMessage</code>, <code>verificationHintMessage</code>
- <code>linkSuccessMessage</code>, <code>alreadyLinkedMessage</code>, <code>maxAccountsPerDiscord</code>

**Chat bridge**

- <code>enableChatBridge</code>
- <code>minecraftToDiscordFormat</code>, <code>discordToMinecraftFormat</code>
- <code>chatWebhookUrl</code> (optional; MC → Discord webhook with skin avatar)

<div class="note">
   <strong>Emoji note:</strong> Discord → MC chat always maps a small set of Unicode emoji into <code>:aliases:</code> for Voidium client rendering. The <code>translateEmojis</code> toggle is currently only used for ticket-message forwarding (and may have limited effect depending on mappings).
</div>

**Status / topic**

- <code>enableStatusMessages</code> + status text fields
- <code>enableTopicUpdate</code>, <code>channelTopicFormat</code>, <code>uptimeFormat</code>

**Ban sync**

- <code>syncBansDiscordToMc</code>, <code>syncBansMcToDiscord</code>

**Bot responses**

- <code>invalidCodeMessage</code>, <code>notLinkedMessage</code>
- <code>alreadyLinkedSingleMessage</code> (<code>%uuid%</code>), <code>alreadyLinkedMultipleMessage</code> (<code>%count%</code>)
- <code>unlinkSuccessMessage</code>, <code>wrongGuildMessage</code>
- <code>ticketCreatedMessage</code>, <code>ticketClosingMessage</code>, <code>textChannelOnlyMessage</code>

**Roles & prefixes**

- <code>linkedRoleId</code> — role assigned after successful linking (link-channel flow)
- <code>rolePrefixes</code> + <code>useHexColors</code>

### tickets.json (TicketConfig)

File: <code>config/voidium/tickets.json</code>

- <code>enableTickets</code>
- <code>ticketCategoryId</code>, <code>supportRoleId</code>
- <code>ticketChannelTopic</code> (placeholders: <code>%user%</code>, <code>%reason%</code>)
- <code>maxTicketsPerUser</code>
- Messages: <code>ticketCreatedMessage</code>, <code>ticketWelcomeMessage</code>, <code>ticketCloseMessage</code>, <code>noPermissionMessage</code>, <code>ticketLimitReachedMessage</code>, <code>ticketAlreadyClosedMessage</code>
- Transcript: <code>enableTranscript</code>, <code>transcriptFormat</code> (<code>TXT</code>/<code>JSON</code>), <code>transcriptFilename</code> (supports <code>%user%</code>, <code>%date%</code>, <code>%reason%</code>)
- In-game messages (color codes via <code>&</code>): <code>mcBotNotConnectedMessage</code>, <code>mcGuildNotFoundMessage</code>, <code>mcCategoryNotFoundMessage</code>, <code>mcTicketCreatedMessage</code>, <code>mcDiscordNotFoundMessage</code>

## 🔗 Whitelist & Linking {#linking}

### In-game (whitelist flow)

When whitelist is enabled and a player is not linked:

1. The player joins and is <strong>frozen</strong> in place (not kicked).
2. They receive a 6-digit code in chat (<code>kickMessage</code> + <code>verificationHintMessage</code>).
3. The code expires after <strong>10 minutes</strong>.
4. After linking, the player is unfrozen.

### On Discord

- Slash commands:
   - <code>/link code:&lt;code&gt;</code>
   - <code>/unlink</code>
- Link channel:
   - User posts the code into <code>linkChannelId</code>
   - Bot deletes the message and replies (auto-deletes the reply after a few seconds)
   - Optional: assigns <code>linkedRoleId</code>

Data is stored in <code>config/voidium/storage/links.json</code>.

## 💬 Chat bridge {#chat-bridge}

### MC → Discord

- Triggered by normal chat + <code>/say</code>
- Sent into <code>chatChannelId</code>
- Uses <code>minecraftToDiscordFormat</code>
- If <code>chatWebhookUrl</code> is set, messages are sent via webhook (skin-based avatar)

### Discord → MC

- Messages from <code>chatChannelId</code> are forwarded into Minecraft when chat bridge is enabled
- Basic Markdown is converted to MC formatting (bold/italic/underline/strike)
- Some Unicode emoji are converted to <code>:aliases:</code> for Voidium client rendering

### Join / leave / death

When chat bridge is enabled, join/leave/death messages are posted to Discord.

## 🎫 Tickets {#tickets}

### Discord slash commands

- <code>/ticket create reason:&lt;reason&gt;</code>
- <code>/ticket close</code>

Ticket creation is rate-limited (global cooldown ~60 seconds) to avoid Discord 429 errors.

### In-game

- <code>/ticket &lt;reason&gt; &lt;message...&gt;</code>
- <code>/reply &lt;message...&gt;</code>

See the full command reference here: <a href="Commands_EN.html">Commands</a>.

### Transcripts

If transcripts are enabled:

- Voidium fetches up to the last ~100 messages
- Uploads a TXT/JSON transcript file into the ticket channel
- Then deletes the channel a few seconds after closing

## 🧯 Troubleshooting {#troubleshooting}

**Bot won’t start**

- Verify <code>botToken</code> and <code>guildId</code>
- Check that the intents are enabled in Developer Portal

**Discord → MC messages do not arrive**

- Ensure <code>enableDiscord</code> and <code>enableChatBridge</code> are true
- Check <code>chatChannelId</code>
- Ensure <code>MESSAGE_CONTENT</code> intent is enabled

**Linking doesn’t work**

- Code is valid for ~10 minutes
- Check <code>linkChannelId</code> permissions (send + delete)

**Ticket not created**

- Check <code>ticketCategoryId</code> and bot permissions
- Remember the global ~60s cooldown between ticket creations

## Related

- <a href="Config_EN.html">Configuration</a>
- <a href="Commands_EN.html">Commands</a>
