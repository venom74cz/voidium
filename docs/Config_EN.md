---
layout: default
title: Configuration (EN)
---

# 🧩 Configuration

<div class="hero">
	<p>All Voidium configuration lives in <code>config/voidium/</code> (server side). Most settings are safe to edit while the server is stopped.</p>

	<div class="note">
		Some config files are written as <strong>JSON with comments</strong> (lines starting with <code>//</code>).
		That means they are not strict JSON for every external tool — edit them as plain text.
	</div>

	<h2>Jump to</h2>
	<div class="card-grid">
		<a class="card" href="#folder">
			<div class="card-title"><span class="card-icon">📁</span>Folder structure</div>
			<div class="card-desc">What gets created on first start</div>
		</a>
		<a class="card" href="#general">
			<div class="card-title"><span class="card-icon">🧰</span>general.json</div>
			<div class="card-desc">Master switches for modules</div>
		</a>
		<a class="card" href="#modules">
			<div class="card-title"><span class="card-icon">🧩</span>Module configs</div>
			<div class="card-desc">Discord/Web/Stats/Ranks/…</div>
		</a>
		<a class="card" href="#storage">
			<div class="card-title"><span class="card-icon">🗄️</span>storage/</div>
			<div class="card-desc">Persistent data + migration</div>
		</a>
		<a class="card" href="#reload">
			<div class="card-title"><span class="card-icon">🔄</span>Reloading</div>
			<div class="card-desc">When you need restart vs reload</div>
		</a>
	</div>
</div>

## 📁 Folder structure {#folder}

After the first dedicated server start, you should have:

```
config/voidium/
	general.json
	discord.json
	web.json
	stats.json
	ranks.json
	tickets.json
	votes.json
	playerlist.json
	entitycleaner.json
	restart.json
	announcements.json
	storage/
```

## 🧰 general.json (master switches) {#general}

File: <code>config/voidium/general.json</code>

Key toggles:

- <code>enableMod</code> — global on/off
- <code>enableDiscord</code>, <code>enableWeb</code>, <code>enableStats</code>, <code>enableRanks</code>, <code>enableVote</code>
- <code>enableRestarts</code>, <code>enableAnnouncements</code>, <code>enablePlayerList</code>
- <code>enableSkinRestorer</code> + <code>skinCacheHours</code>

<div class="note">
	Discord has <strong>two</strong> toggles: <code>general.json → enableDiscord</code> AND <code>discord.json → enableDiscord</code>.
</div>

## 🧩 Module configs {#modules}

<div class="section-grid">
	<a class="section-card" href="#discord">
		<div class="title">discord.json</div>
		<div class="card-desc">Bot token, guild id, channels, chat bridge</div>
	</a>
	<a class="section-card" href="#web">
		<div class="title">web.json</div>
		<div class="card-desc">Port, language, public hostname</div>
	</a>
	<a class="section-card" href="#stats">
		<div class="title">stats.json</div>
		<div class="card-desc">Daily report channel + time</div>
	</a>
	<a class="section-card" href="#ranks">
		<div class="title">ranks.json</div>
		<div class="card-desc">Playtime-based ranks</div>
	</a>
	<a class="section-card" href="#tickets">
		<div class="title">tickets.json</div>
		<div class="card-desc">Discord tickets + transcript</div>
	</a>
	<a class="section-card" href="#votes">
		<div class="title">votes.json</div>
		<div class="card-desc">NuVotifier listener + rewards</div>
	</a>
	<a class="section-card" href="#playerlist">
		<div class="title">playerlist.json</div>
		<div class="card-desc">TAB header/footer + names</div>
	</a>
	<a class="section-card" href="#entitycleaner">
		<div class="title">entitycleaner.json</div>
		<div class="card-desc">Cleanup schedule + whitelists</div>
	</a>
	<a class="section-card" href="#restart">
		<div class="title">restart.json</div>
		<div class="card-desc">Fixed / interval / delay restarts</div>
	</a>
	<a class="section-card" href="#announcements">
		<div class="title">announcements.json</div>
		<div class="card-desc">Broadcast messages + interval</div>
	</a>
</div>

### 🤖 discord.json {#discord}

File: <code>config/voidium/discord.json</code>

Important keys:

- <code>enableDiscord</code>
- <code>botToken</code>, <code>guildId</code>
- Channel IDs: <code>chatChannelId</code>, <code>consoleChannelId</code>, <code>linkChannelId</code>, <code>statusChannelId</code>
- Chat bridge: <code>enableChatBridge</code>, formats, <code>translateEmojis</code>
- Webhook chat: <code>chatWebhookUrl</code>

<div class="note">
	Keep <code>botToken</code> secret. If it leaks, rotate it in Discord Developer Portal.
</div>

### 🌐 web.json {#web}

File: <code>config/voidium/web.json</code>

- <code>port</code> (default: 8081)
- <code>language</code> (<code>en</code> / <code>cz</code>)
- <code>publicHostname</code> (used for the link shown to you)

To get your access link:

```
/voidium web
```

### 📊 stats.json {#stats}

File: <code>config/voidium/stats.json</code>

- <code>enableStats</code>
- <code>reportChannelId</code>
- <code>reportTime</code> (HH:mm)
- Custom report strings: title/labels/footer

### 🏅 ranks.json {#ranks}

File: <code>config/voidium/ranks.json</code>

- <code>enableAutoRanks</code>
- <code>checkIntervalMinutes</code>
- <code>ranks</code> list: PREFIX/SUFFIX definitions with <code>hours</code>

### 🎫 tickets.json {#tickets}

File: <code>config/voidium/tickets.json</code>

- <code>enableTickets</code>
- <code>ticketCategoryId</code>, <code>supportRoleId</code>
- Transcript: <code>enableTranscript</code>, <code>transcriptFormat</code>, <code>transcriptFilename</code>

### 🗳️ votes.json {#votes}

File: <code>config/voidium/votes.json</code>

- <code>enabled</code>, <code>host</code>, <code>port</code>
- Keys: <code>rsaPrivateKeyPath</code>, <code>rsaPublicKeyPath</code>
- Rewards: <code>commands</code> list (uses <code>%PLAYER%</code>)
- Logging + offline queue are stored in <code>storage/</code>

### 📋 playerlist.json {#playerlist}

File: <code>config/voidium/playerlist.json</code>

- <code>enableCustomPlayerList</code>
- Header/footer lines + placeholders like <code>%online%</code>, <code>%max%</code>, <code>%tps%</code>, <code>%ping%</code>
- Custom names: <code>enableCustomNames</code>, <code>playerNameFormat</code>

### 🧹 entitycleaner.json {#entitycleaner}

File: <code>config/voidium/entitycleaner.json</code>

- <code>enabled</code>, <code>cleanupIntervalSeconds</code>, <code>warningTimes</code>
- Removal toggles: dropped items, mobs, XP, arrows
- Protection toggles + item/entity whitelists

### 🔁 restart.json {#restart}

File: <code>config/voidium/restart.json</code>

- <code>restartType</code>: <code>FIXED_TIME</code> / <code>INTERVAL</code> / <code>DELAY</code>
- Fixed list: <code>fixedRestartTimes</code>
- Interval: <code>intervalHours</code>
- Delay: <code>delayMinutes</code>

### 📣 announcements.json {#announcements}

File: <code>config/voidium/announcements.json</code>

- <code>announcements</code> list
- <code>announcementIntervalMinutes</code> (0 disables auto announcements)
- <code>prefix</code>

## 🗄️ storage/ (persistent data) {#storage}

Folder: <code>config/voidium/storage/</code>

This folder stores persistent runtime data, for example:

- <code>links.json</code>
- <code>pending-votes.json</code>
- <code>votes.log</code>, <code>votes-history.ndjson</code>
- <code>voidium_stats_data.json</code>, <code>voidium_ranks_data.json</code>
- <code>player_progress.json</code>
- <code>skin-cache.json</code>
- <code>last_restart.txt</code>

On startup, Voidium migrates older files from <code>config/voidium/</code> into <code>storage/</code> automatically (if needed).

## 🔄 Reloading config {#reload}

- Prefer <strong>restart</strong> for first-time setup and bigger changes (Discord bot token, ports).
- For quick tweaks you can use:

```
/voidium reload
```

## Next

- [Quick Start](QuickStart_EN.html)
- [Troubleshooting](Troubleshooting_EN.html)
