---
layout: default
title: Quick Start (EN)
---

# ⚡ Quick Start

<div class="hero">
	<p>Goal: get Voidium running cleanly, then enable only what you actually want.</p>

	<h2>Quick paths</h2>
	<div class="section-grid">
		<a class="section-card" href="#minimal">
			<div class="title">Minimal (no Discord)</div>
			<div class="card-desc">Server tools + optional Web Panel</div>
		</a>
		<a class="section-card" href="#web">
			<div class="title">Enable Web Panel</div>
			<div class="card-desc">Get your URL via <code>/voidium web</code></div>
		</a>
		<a class="section-card" href="#discord">
			<div class="title">Enable Discord</div>
			<div class="card-desc">Token + guild id + enable flag</div>
		</a>
		<a class="section-card" href="#verify">
			<div class="title">Verify</div>
			<div class="card-desc"><code>/voidium status</code> and logs</div>
		</a>
	</div>
</div>

## 0) Install + first boot

If you haven’t installed Voidium yet, start here:

- [Installation](Install_EN.html)

After the first boot you should have:

```
config/voidium/
```

## ✅ Minimal setup (recommended) {#minimal}

Open:

- <code>config/voidium/general.json</code>

This file is the “master switchboard” for modules (Web, Discord, Stats, Ranks, Vote, …).

<div class="note">
	Tip: If you don’t plan to configure Discord right away, set <code>enableDiscord</code> to <code>false</code> to avoid bot-related startup noise.
</div>

Then restart the server.

## 🌐 Web Panel (optional) {#web}

1. Ensure <code>enableWeb</code> is enabled in <code>general.json</code>
2. Check <code>config/voidium/web.json</code> (default port is <code>8081</code>)
3. In-game (OP) or in the server console run:

```
/voidium web
```

It prints a URL with a token (use the console link). The token is required on first open and then stored as an HTTP-only cookie.

## 🤖 Discord (optional) {#discord}

Voidium uses two layers of toggles:

- Global toggle: <code>config/voidium/general.json</code> → <code>enableDiscord</code>
- Discord module toggle: <code>config/voidium/discord.json</code> → <code>enableDiscord</code>

Minimal steps:

1. In <code>discord.json</code> set <code>enableDiscord</code> to <code>true</code>
2. Fill in <code>botToken</code> and <code>guildId</code>
3. Restart the server (recommended for first setup)

If you change config while the server is running, you can reload:

```
/voidium reload
```

## 🔎 Verify it works {#verify}

### 1) Logs

On dedicated server startup you should see a line like:

- <code>VOIDIUM - INTELLIGENT SERVER CONTROL is loading...</code>

### 2) In-game command

Run:

```
/voidium status
```

You should see server metrics like TPS/MSPT and module-related info.

## Next

- [Configuration](Config_EN.html)
- [Commands](Commands_EN.html)
- [Discord module](Discord_EN.html)
