---
layout: default
title: Konfigurace (CZ)
---

# 🧩 Konfigurace

<div class="hero">
	<p>Veškerá konfigurace Voidium je v <code>config/voidium/</code> (server). Většinu věcí je nejlepší upravovat při vypnutém serveru.</p>

	<div class="note">
		Některé soubory se ukládají jako <strong>JSON s komentáři</strong> (řádky začínající <code>//</code>).
		To znamená, že to nemusí být „strict JSON“ pro všechny externí nástroje — editujte to jako obyčejný text.
	</div>

	<h2>Rychlá navigace</h2>
	<div class="card-grid">
		<a class="card" href="#struktura">
			<div class="card-title"><span class="card-icon">📁</span>Struktura složek</div>
			<div class="card-desc">Co se vytvoří při prvním startu</div>
		</a>
		<a class="card" href="#general">
			<div class="card-title"><span class="card-icon">🧰</span>general.json</div>
			<div class="card-desc">Hlavní přepínače modulů</div>
		</a>
		<a class="card" href="#moduly">
			<div class="card-title"><span class="card-icon">🧩</span>Konfigurace modulů</div>
			<div class="card-desc">Discord/Web/Stats/…</div>
		</a>
		<a class="card" href="#storage">
			<div class="card-title"><span class="card-icon">🗄️</span>storage/</div>
			<div class="card-desc">Persistentní data + migrace</div>
		</a>
		<a class="card" href="#reload">
			<div class="card-title"><span class="card-icon">🔄</span>Reload</div>
			<div class="card-desc">Kdy restart vs reload</div>
		</a>
	</div>
</div>

## 📁 Struktura složek {#struktura}

Po prvním startu dedikovaného serveru by mělo existovat:

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

## 🧰 general.json (hlavní přepínače) {#general}

Soubor: <code>config/voidium/general.json</code>

Nejdůležitější klíče:

- <code>enableMod</code> — globální zap/vyp
- <code>enableDiscord</code>, <code>enableWeb</code>, <code>enableStats</code>, <code>enableRanks</code>, <code>enableVote</code>
- <code>enableRestarts</code>, <code>enableAnnouncements</code>, <code>enablePlayerList</code>
- <code>enableSkinRestorer</code> + <code>skinCacheHours</code>

<div class="note">
	Discord má <strong>dva</strong> přepínače: <code>general.json → enableDiscord</code> a zároveň <code>discord.json → enableDiscord</code>.
</div>

## 🧩 Konfigurace modulů {#moduly}

<div class="section-grid">
	<a class="section-card" href="#discord">
		<div class="title">discord.json</div>
		<div class="card-desc">Token bota, guild id, kanály, chat bridge</div>
	</a>
	<a class="section-card" href="#web">
		<div class="title">web.json</div>
		<div class="card-desc">Port, jazyk, public hostname</div>
	</a>
	<a class="section-card" href="#stats">
		<div class="title">stats.json</div>
		<div class="card-desc">Denní reporty: kanál + čas</div>
	</a>
	<a class="section-card" href="#ranks">
		<div class="title">ranks.json</div>
		<div class="card-desc">Ranky podle playtime</div>
	</a>
	<a class="section-card" href="#tickets">
		<div class="title">tickets.json</div>
		<div class="card-desc">Tickety + transcript</div>
	</a>
	<a class="section-card" href="#votes">
		<div class="title">votes.json</div>
		<div class="card-desc">NuVotifier listener + odměny</div>
	</a>
	<a class="section-card" href="#playerlist">
		<div class="title">playerlist.json</div>
		<div class="card-desc">TAB header/footer + jména</div>
	</a>
	<a class="section-card" href="#entitycleaner">
		<div class="title">entitycleaner.json</div>
		<div class="card-desc">Cleanup plán + whitelisty</div>
	</a>
	<a class="section-card" href="#restart">
		<div class="title">restart.json</div>
		<div class="card-desc">Fixed / interval / delay restarty</div>
	</a>
	<a class="section-card" href="#announcements">
		<div class="title">announcements.json</div>
		<div class="card-desc">Broadcasty + interval</div>
	</a>
</div>

### 🤖 discord.json {#discord}

Soubor: <code>config/voidium/discord.json</code>

Důležité klíče:

- <code>enableDiscord</code>
- <code>botToken</code>, <code>guildId</code>
- ID kanálů: <code>chatChannelId</code>, <code>consoleChannelId</code>, <code>linkChannelId</code>, <code>statusChannelId</code>
- Chat bridge: <code>enableChatBridge</code>, formáty, <code>translateEmojis</code>
- Webhook chat: <code>chatWebhookUrl</code>

<div class="note">
	<code>botToken</code> nikdy nesdílejte. Když unikne, okamžitě ho v Developer Portalu otočte.
</div>

### 🌐 web.json {#web}

Soubor: <code>config/voidium/web.json</code>

- <code>port</code> (default: 8081)
- <code>language</code> (<code>en</code> / <code>cz</code>)
- <code>publicHostname</code> (použije se pro odkaz, který vám Voidium vypíše)

Odkaz na web panel získáte:

```
/voidium web
```

### 📊 stats.json {#stats}

Soubor: <code>config/voidium/stats.json</code>

- <code>enableStats</code>
- <code>reportChannelId</code>
- <code>reportTime</code> (HH:mm)
- Texty reportu: title/labels/footer

### 🏅 ranks.json {#ranks}

Soubor: <code>config/voidium/ranks.json</code>

- <code>enableAutoRanks</code>
- <code>checkIntervalMinutes</code>
- <code>ranks</code> list: PREFIX/SUFFIX definice s <code>hours</code>

### 🎫 tickets.json {#tickets}

Soubor: <code>config/voidium/tickets.json</code>

- <code>enableTickets</code>
- <code>ticketCategoryId</code>, <code>supportRoleId</code>
- Transcript: <code>enableTranscript</code>, <code>transcriptFormat</code>, <code>transcriptFilename</code>

### 🗳️ votes.json {#votes}

Soubor: <code>config/voidium/votes.json</code>

- <code>enabled</code>, <code>host</code>, <code>port</code>
- Klíče: <code>rsaPrivateKeyPath</code>, <code>rsaPublicKeyPath</code>
- Odměny: <code>commands</code> (používá <code>%PLAYER%</code>)
- Logging + offline queue se ukládají do <code>storage/</code>

### 📋 playerlist.json {#playerlist}

Soubor: <code>config/voidium/playerlist.json</code>

- <code>enableCustomPlayerList</code>
- Header/footer řádky + placeholdery typu <code>%online%</code>, <code>%max%</code>, <code>%tps%</code>, <code>%ping%</code>
- Custom jména: <code>enableCustomNames</code>, <code>playerNameFormat</code>

### 🧹 entitycleaner.json {#entitycleaner}

Soubor: <code>config/voidium/entitycleaner.json</code>

- <code>enabled</code>, <code>cleanupIntervalSeconds</code>, <code>warningTimes</code>
- Co mazat: itemy, moby, XP, šípy
- Ochrany + whitelisty itemů/entit

### 🔁 restart.json {#restart}

Soubor: <code>config/voidium/restart.json</code>

- <code>restartType</code>: <code>FIXED_TIME</code> / <code>INTERVAL</code> / <code>DELAY</code>
- Fixed list: <code>fixedRestartTimes</code>
- Interval: <code>intervalHours</code>
- Delay: <code>delayMinutes</code>

### 📣 announcements.json {#announcements}

Soubor: <code>config/voidium/announcements.json</code>

- <code>announcements</code> list
- <code>announcementIntervalMinutes</code> (0 vypne automatické broadcasty)
- <code>prefix</code>

## 🗄️ storage/ (persistentní data) {#storage}

Složka: <code>config/voidium/storage/</code>

Tady jsou runtime data, např.:

- <code>links.json</code>
- <code>pending-votes.json</code>
- <code>votes.log</code>, <code>votes-history.ndjson</code>
- <code>voidium_stats_data.json</code>, <code>voidium_ranks_data.json</code>
- <code>player_progress.json</code>
- <code>skin-cache.json</code>
- <code>last_restart.txt</code>

Při startu Voidium případně přesune starší soubory z <code>config/voidium/</code> do <code>storage/</code> automaticky.

## 🔄 Reload konfigurace {#reload}

- Pro první setup a větší změny (token bota, porty) je lepší <strong>restart</strong>.
- Na rychlé úpravy můžete použít:

```
/voidium reload
```

## Další

- [Rychlý start](QuickStart_CZ.html)
- [Řešení problémů](Troubleshooting_CZ.html)
