---
layout: default
title: Web panel (CZ)
---

# 🌐 Web panel

<div class="hero">
	<p><strong>Voidium Web Control Panel</strong> je lehký HTTP dashboard pro stav serveru, rychlé akce a editaci konfigurace.</p>

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

**Dashboard**

- <code>GET /</code> — HTML dashboard (vyžaduje auth)
- <code>GET /css/style.css</code> — styly panelu

**Akce (POST)**

- <code>/api/action</code> (form payload):
	- <code>action=restart</code>
	- <code>action=announce</code> + <code>message</code>
	- <code>action=kick</code> + <code>player</code>
	- <code>action=ban</code> + <code>uuid</code> + <code>name</code>
	- <code>action=unban</code> + <code>uuid</code>
	- <code>action=unlink</code> + <code>uuid</code>

**Config API**

- <code>GET /api/config</code> — vrátí aktuální konfiguraci
- <code>POST /api/config</code> — uloží změny konfigurace

POST podporuje oba formáty:

- <code>{"section":"discord","data":{...}}</code>
- starší formát <code>{"discord":{...},"general":{...}}</code>

**Reset lokalizace**

- <code>POST /api/locale</code> s JSON: <code>{"locale":"en"}</code> nebo <code>{"locale":"cz"}</code>

**Historie statistik**

- <code>GET /api/stats/history</code> — datapointy (pokud je Stats modul aktivní)

**Discord role**

- <code>GET /api/discord/roles</code> — seznam rolí (vyžaduje běžící Discord bota)

## 🔒 Zabezpečení {#security}

- Panel běží přes HTTP; používej interní síť nebo reverse proxy.
- Nesdílej token URL.
- Restart serveru vždy invaliduje staré tokeny.

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
