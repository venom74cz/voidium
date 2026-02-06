---
layout: default
title: Installation (EN)
---

# 🧰 Installation

<div class="hero">
	<p>Voidium runs on a <strong>NeoForge dedicated server</strong>. A client install is optional (it only adds UI/chat features).</p>

	<h2>Jump to</h2>
	<div class="card-grid">
		<a class="card" href="#requirements">
			<div class="card-title"><span class="card-icon">✅</span>Requirements</div>
			<div class="card-desc">Java, NeoForge, dedicated server</div>
		</a>
		<a class="card" href="#server-install">
			<div class="card-title"><span class="card-icon">🖥️</span>Server install</div>
			<div class="card-desc">Download, put into mods/, first boot</div>
		</a>
		<a class="card" href="#client-install">
			<div class="card-title"><span class="card-icon">💻</span>Client (optional)</div>
			<div class="card-desc">Modern chat + emoji + history</div>
		</a>
		<a class="card" href="#first-run">
			<div class="card-title"><span class="card-icon">⚙️</span>First run</div>
			<div class="card-desc">Generated config files & storage</div>
		</a>
		<a class="card" href="#update">
			<div class="card-title"><span class="card-icon">🔁</span>Updating</div>
			<div class="card-desc">Safe jar replacement + migration</div>
		</a>
		<a class="card" href="#troubleshooting">
			<div class="card-title"><span class="card-icon">🧯</span>Troubleshooting</div>
			<div class="card-desc">Common install mistakes</div>
		</a>
	</div>
</div>

## ✅ Requirements {#requirements}

<div class="note">
	<strong>VERSION: 1.21.1+</strong>
</div>

### Server

- A NeoForge <strong>dedicated server</strong>
- Java <strong>21+</strong> (recommended)
- A standard server folder with a <code>mods/</code> directory

### Client (optional)

- NeoForge client matching your game version
- Java 21+

## 🖥️ Server installation {#server-install}

### 1) Download

- Download the latest release jar from:
	- https://github.com/venom74cz/voidium/releases

### 2) Put it into <code>mods/</code>

Place the jar directly into your server’s <code>mods/</code> directory.

```bash
# Linux/macOS
cd /path/to/your/server
mkdir -p mods
cp voidium-*.jar mods/
```

### 3) Start the server once

On the first boot, Voidium creates its config directory and default config files.

Expected folder after first start:

```
config/voidium/
```

<div class="note">
	If you don’t see <code>config/voidium/</code>, double-check that you started a <strong>dedicated server</strong>.
	Voidium’s server-side managers are initialized only on dedicated servers.
</div>

## 💻 Client installation (optional) {#client-install}

If you want the client UI features (modern chat, emoji rendering, chat history), install the same jar on the client too:

1. Install NeoForge for your client profile
2. Put <code>voidium-*.jar</code> into:
	 - Linux: <code>~/.minecraft/mods/</code>
	 - Windows: <code>%APPDATA%\.minecraft\mods\</code>
3. Start the game with NeoForge

## ⚙️ First run: what gets generated {#first-run}

After the first server start, Voidium creates:

- <code>config/voidium/general.json</code> (master switches for modules like Discord/Web/Stats/etc.)
- Module configs (examples): <code>discord.json</code>, <code>web.json</code>, <code>stats.json</code>, <code>ranks.json</code>, …
- <code>config/voidium/storage/</code> for persistent data (links, vote queues, cached skins, history files)

Voidium also auto-migrates older storage files (if present) into <code>config/voidium/storage/</code> on startup.

## 🔁 Updating {#update}

Recommended safe update process:

1. Stop the server
2. Backup <code>config/voidium/</code>
3. Replace the old jar in <code>mods/</code> with the new one
4. Start the server

Your configuration and persistent data stay in <code>config/voidium/</code>. If an internal storage layout changes, Voidium migrates files automatically into <code>config/voidium/storage/</code>.

## 🧯 Troubleshooting {#troubleshooting}

### Voidium doesn’t load

- Verify you’re using <strong>NeoForge</strong> (not Forge/Fabric)
- Verify Java 21+: run <code>java -version</code>
- Ensure the jar is directly inside <code>mods/</code> (not in a subfolder)

### Config folder not created

- Make sure you started a <strong>dedicated server</strong>
- Check server logs for <code>VOIDIUM - INTELLIGENT SERVER CONTROL is loading...</code>

## Next steps

- [Quick Start](QuickStart_EN.html)
- [Configuration](Config_EN.html)
- [Discord Setup](Discord_EN.html)
