---
layout: default
title: Web panel (EN)
---

# 🌐 Web panel

<div class="hero">
	<p><strong>Voidium Web Control Panel</strong> is a lightweight HTTP dashboard for server status, quick actions, and live config editing.</p>

	<div class="note">
		Access is protected by a one‑time token in the URL. The token changes on every server start and is stored in a session cookie after first load.
	</div>

	<h2>Jump to</h2>
	<div class="card-grid">
		<a class="card" href="#setup">
			<div class="card-title"><span class="card-icon">✅</span>Setup</div>
			<div class="card-desc">Enable module + basic config</div>
		</a>
		<a class="card" href="#config">
			<div class="card-title"><span class="card-icon">⚙️</span>Configuration</div>
			<div class="card-desc">web.json keys</div>
		</a>
		<a class="card" href="#access">
			<div class="card-title"><span class="card-icon">🔗</span>Access</div>
			<div class="card-desc">Token URL + cookie</div>
		</a>
		<a class="card" href="#api">
			<div class="card-title"><span class="card-icon">🧩</span>API</div>
			<div class="card-desc">Endpoints and actions</div>
		</a>
		<a class="card" href="#security">
			<div class="card-title"><span class="card-icon">🔒</span>Security</div>
			<div class="card-desc">Keep it safe</div>
		</a>
		<a class="card" href="#troubleshooting">
			<div class="card-title"><span class="card-icon">🧯</span>Troubleshooting</div>
			<div class="card-desc">Common problems</div>
		</a>
	</div>
</div>

## ✅ Setup {#setup}

1. Enable the module in <code>config/voidium/general.json</code>:
	- <code>enableWeb: true</code>
2. Configure <code>config/voidium/web.json</code> (port, language, hostname)
3. Restart the server

You’ll see an access link in logs, or you can use <code>/voidium web</code> in‑game.

## ⚙️ Configuration {#config}

File: <code>config/voidium/web.json</code>

- <code>port</code> — HTTP port (default: <code>8081</code>)
- <code>language</code> — panel language: <code>en</code> or <code>cz</code>
- <code>publicHostname</code> — hostname/IP used in the access URL

<div class="note">
	If <code>publicHostname</code> is <code>localhost</code> / <code>127.0.0.1</code>, Voidium tries to detect a LAN IP when generating the URL.
</div>

## 🔗 Access & auth {#access}

- The Web panel URL includes a token, for example:
	<code>http://HOST:PORT/?token=UUID</code>
- The token is generated on every server start.
- After the first successful load, a <code>session</code> cookie is set and used for future requests.
- Without a valid token or cookie, the panel returns <strong>401 Unauthorized</strong>.

## 🧩 API & actions {#api}

The panel exposes a few HTTP endpoints:

**Dashboard**

- <code>GET /</code> — HTML dashboard (requires auth)
- <code>GET /css/style.css</code> — panel styles

**Actions (POST)**

- <code>/api/action</code> (form payload):
	- <code>action=restart</code>
	- <code>action=announce</code> + <code>message</code>
	- <code>action=kick</code> + <code>player</code>
	- <code>action=ban</code> + <code>uuid</code> + <code>name</code>
	- <code>action=unban</code> + <code>uuid</code>
	- <code>action=unlink</code> + <code>uuid</code>

**Config API**

- <code>GET /api/config</code> — returns current config JSON
- <code>POST /api/config</code> — saves config updates

The POST endpoint supports both:

- <code>{"section":"discord","data":{...}}</code>
- legacy format <code>{"discord":{...},"general":{...}}</code>

**Locale reset**

- <code>POST /api/locale</code> with JSON: <code>{"locale":"en"}</code> or <code>{"locale":"cz"}</code>

**Stats history**

- <code>GET /api/stats/history</code> — stats datapoints (if Stats module is enabled)

**Discord roles**

- <code>GET /api/discord/roles</code> — role list (requires Discord bot running)

## 🔒 Security notes {#security}

- The panel runs over plain HTTP; keep it on a trusted network or behind a reverse proxy.
- Don’t share the token URL.
- Restarting the server invalidates old tokens.

## 🧯 Troubleshooting {#troubleshooting}

**Panel doesn’t start**

- Ensure <code>enableWeb</code> is true in <code>general.json</code>
- Check if the port is already in use

**401 Unauthorized**

- Use a fresh URL from logs or <code>/voidium web</code>
- The token changes after every server restart

**Stats/Discord data missing**

- Stats require <code>enableStats</code>
- Discord roles require Discord module to be enabled and connected

## Next

- [Configuration](Config_EN.html)
- [Commands](Commands_EN.html)
