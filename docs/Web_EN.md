---
layout: default
title: Web panel (EN)
---

# 🌐 Web panel

<div class="hero">
	<p><strong>Voidium Web Control Panel</strong> is a React 19 + Vite 6 + TypeScript SPA bundled into the mod JAR. It provides a full admin dashboard for server status, quick actions, AI assistance, and live config editing — no external dependencies needed.</p>

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

**Static assets**

- <code>GET /</code> — React SPA entry (index.html, requires auth)
- <code>GET /assets/*</code> — hashed JS/CSS bundles (immutable cache)

**Dashboard**

- <code>GET /api/dashboard</code> — full server telemetry JSON (auth required)
- <code>GET /api/feeds</code> — chat + console feed snapshot
- <code>GET /api/events</code> — Server-Sent Events (SSE) stream, pushes dashboard data every 3 seconds

**Actions (POST)**

- <code>POST /api/action</code> (JSON payload with <code>action</code> field):
	- <code>restart</code>, <code>reload</code>, <code>announce</code>, <code>maintenance_on/off</code>
	- <code>entitycleaner_preview/all/items/mobs/xp/arrows</code>
	- <code>vote_payout/clear/payout_all/clear_all</code>
	- <code>ticket_close/note/transcript</code>

**Config Studio API**

- <code>GET /api/config/schema</code> — field definitions for all modules
- <code>GET /api/config/values</code> — current config values
- <code>GET /api/config/defaults</code> — factory defaults
- <code>GET /api/config/locale</code> — locale preset (en/cz)
- <code>POST /api/config/preview</code> — preview changes with impact flags
- <code>POST /api/config/diff</code> — diff current vs proposed values
- <code>POST /api/config/apply</code> — apply changes (creates backup)
- <code>POST /api/config/rollback</code> — rollback to last backup
- <code>POST /api/config/reload</code> — reload configs from disk

**AI API**

- <code>POST /api/ai/admin</code> — admin AI conversation
- <code>POST /api/ai/admin/suggest</code> — AI config suggestions
- <code>GET /api/ai/players</code> — player AI conversation history

**Console**

- <code>POST /api/console/execute</code> — execute allowlisted commands

**Schema export**

- <code>GET /api/config/schema/export</code> — full config schema in JSON Schema draft-07 format

**Server management**

- <code>GET /api/server-icon</code> — server favicon (PNG, no auth required)
- <code>GET /api/server-properties</code> — read server.properties as JSON key-value pairs
- <code>POST /api/server-properties</code> — write server.properties (JSON body with key-value pairs)

**Discord roles**

- <code>GET /api/discord/roles</code> — role list (requires Discord bot running)

## 🔒 Security notes {#security}

- The panel runs over plain HTTP; keep it on a trusted network or behind a reverse proxy.
- Don’t share the token URL.
- Restarting the server invalidates old tokens.- **Rate limiting**: AI endpoints (<code>/api/ai/*</code>) enforce 120 requests/minute per IP. Exceeding the limit returns HTTP 429 with <code>Retry-After</code> header.
- **Theme & locale**: Dark/light theme and language preference are stored in <code>localStorage</code> (client-side only).
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
