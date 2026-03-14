---
layout: default
title: Web panel (CZ)
---

# 🌐 Web panel

<div class="hero">
	<p><strong>Voidium Web Control Panel</strong> je React 19 + Vite 6 + TypeScript SPA zabalená přímo v mod JARu. Poskytuje kompletní admin dashboard pro stav serveru, rychlé akce, AI asistenci a vizuální editaci konfigurace — bez externích závislostí.</p>

	<div class="note">
		Přístup chrání jednorázový token v URL. Token se mění při každém startu serveru a po prvním otevření se uloží do session cookie.
	</div>

	<h2>Rychlá navigace</h2>
	<div class="card-grid">
		<a class="card" href="#setup">
			<div class="card-title"><span class="card-icon">✅</span>Setup</div>
			<div class="card-desc">Zapnutí modulu + základ</div>
		</a>
		<a class="card" href="#config">
			<div class="card-title"><span class="card-icon">⚙️</span>Konfigurace</div>
			<div class="card-desc">Klíče v web.json</div>
		</a>
		<a class="card" href="#access">
			<div class="card-title"><span class="card-icon">🔗</span>Přístup</div>
			<div class="card-desc">Token URL + cookie</div>
		</a>
		<a class="card" href="#api">
			<div class="card-title"><span class="card-icon">🧩</span>API</div>
			<div class="card-desc">Endpointy a akce</div>
		</a>
		<a class="card" href="#security">
			<div class="card-title"><span class="card-icon">🔒</span>Zabezpečení</div>
			<div class="card-desc">Doporučení</div>
		</a>
		<a class="card" href="#troubleshooting">
			<div class="card-title"><span class="card-icon">🧯</span>Problémy</div>
			<div class="card-desc">Nejčastější chyby</div>
		</a>
	</div>
</div>

## ✅ Setup {#setup}

1. Zapni modul v <code>config/voidium/general.json</code>:
	- <code>enableWeb: true</code>
2. Uprav <code>config/voidium/web.json</code> (port, jazyk, hostname)
3. Restartuj server

Odkaz na panel uvidíš v logu, případně použij <code>/voidium web</code> in‑game.

## ⚙️ Konfigurace {#config}

Soubor: <code>config/voidium/web.json</code>

- <code>port</code> — HTTP port (default: <code>8081</code>)
- <code>language</code> — jazyk panelu: <code>en</code> nebo <code>cz</code>
- <code>publicHostname</code> — hostname/IP v přístupovém URL

<div class="note">
	Pokud je <code>publicHostname</code> <code>localhost</code> / <code>127.0.0.1</code>, Voidium se pokusí detekovat LAN IP při generování URL.
</div>

## 🔗 Přístup & auth {#access}

- URL obsahuje token, např.: <code>http://HOST:PORT/?token=UUID</code>
- Token se generuje při každém startu serveru
- Po prvním načtení se nastaví cookie <code>session</code>
- Bez tokenu/cookie vrací panel <strong>401 Unauthorized</strong>

## 🧩 API & akce {#api}

Panel má několik endpointů:

**Statické assety**

- <code>GET /</code> — React SPA (index.html, vyžaduje auth)
- <code>GET /assets/*</code> — hashované JS/CSS bundly (immutable cache)

**Dashboard**

- <code>GET /api/dashboard</code> — kompletní telemetrie serveru (vyžaduje auth)
- <code>GET /api/feeds</code> — snapshot chatu + konzole
- <code>GET /api/events</code> — Server-Sent Events (SSE) stream, push dashboard dat každé 3 sekundy

**Akce (POST)**

- <code>POST /api/action</code> (JSON s polem <code>action</code>):
	- <code>restart</code>, <code>reload</code>, <code>announce</code>, <code>maintenance_on/off</code>
	- <code>entitycleaner_preview/all/items/mobs/xp/arrows</code>
	- <code>vote_payout/clear/payout_all/clear_all</code>
	- <code>ticket_close/note/transcript</code>

**Config Studio API**

- <code>GET /api/config/schema</code> — definice polí pro všechny moduly
- <code>GET /api/config/values</code> — aktuální hodnoty
- <code>GET /api/config/defaults</code> — tovární výchozí hodnoty
- <code>GET /api/config/locale</code> — lokální preset (en/cz)
- <code>POST /api/config/preview</code> — náhled změn s impact flagy
- <code>POST /api/config/diff</code> — diff aktuální vs navržené hodnoty
- <code>POST /api/config/apply</code> — aplikuj změny (vytvoří zálohu)
- <code>POST /api/config/rollback</code> — vrátí poslední zálohu
- <code>POST /api/config/reload</code> — reload konfigů z disku

**AI API**

- <code>POST /api/ai/admin</code> — admin AI konverzace
- <code>POST /api/ai/admin/suggest</code> — AI návrhy konfigurace
- <code>GET /api/ai/players</code> — historie AI konverzací hráčů

**Konzole**

- <code>POST /api/console/execute</code> — spustí povolené příkazy

**Export schématu**

- <code>GET /api/config/schema/export</code> — kompletní schéma konfigurace ve formátu JSON Schema draft-07

**Správa serveru**

- <code>GET /api/server-icon</code> — ikona serveru (PNG, bez auth)
- <code>GET /api/server-properties</code> — čtení server.properties jako JSON key-value
- <code>POST /api/server-properties</code> — zápis server.properties (JSON body s key-value páry)

**Discord role**

- <code>GET /api/discord/roles</code> — seznam rolí (vyžaduje běžící Discord bota)

## 🔒 Zabezpečení {#security}

- Panel běží přes HTTP; používej interní síť nebo reverse proxy.
- Nesdílej token URL.
- Restart serveru vždy invaliduje staré tokeny.
- **Rate limiting**: AI endpointy (<code>/api/ai/*</code>) mají limit 120 požadavků/minutu na IP. Po překročení vrací HTTP 429 s hlavičkou <code>Retry-After</code>.
- **Téma & jazyk**: Dark/light téma a jazyk se ukládají do <code>localStorage</code> (pouze na straně klienta).

## 🧯 Řešení problémů {#troubleshooting}

**Panel se nespustí**

- Ověř <code>enableWeb</code> v <code>general.json</code>
- Zkontroluj, jestli port není obsazený

**401 Unauthorized**

- Použij čerstvé URL z logu nebo <code>/voidium web</code>
- Token se mění po restartu serveru

**Chybí data pro Stats/Discord**

- Stats vyžaduje <code>enableStats</code>
- Discord role vyžadují zapnutý a připojený Discord modul

## Další

- [Konfigurace](Config_CZ.html)
- [Příkazy](Commands_CZ.html)
