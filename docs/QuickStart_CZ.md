---
layout: default
title: Rychlý start (CZ)
---

# ⚡ Rychlý start

<div class="hero">
	<p>Cíl: mít Voidium spuštěné bez šumu v logu a zapnout jen to, co opravdu používáte.</p>

	<h2>Rychlé cesty</h2>
	<div class="section-grid">
		<a class="section-card" href="#minimal">
			<div class="title">Minimum (bez Discordu)</div>
			<div class="card-desc">Server nástroje + volitelný Web Panel</div>
		</a>
		<a class="section-card" href="#web">
			<div class="title">Zapnout Web Panel</div>
			<div class="card-desc">Odkaz získáte přes <code>/voidium web</code></div>
		</a>
		<a class="section-card" href="#discord">
			<div class="title">Zapnout Discord</div>
			<div class="card-desc">Token + guild id + enable přepínač</div>
		</a>
		<a class="section-card" href="#overeni">
			<div class="title">Ověření</div>
			<div class="card-desc"><code>/voidium status</code> + logy</div>
		</a>
	</div>
</div>

## 0) Instalace + první start

Pokud ještě nemáte Voidium nainstalované:

- [Instalace](Install_CZ.html)

Po prvním startu serveru by měla existovat složka:

```
config/voidium/
```

## ✅ Minimální setup (doporučeno) {#minimal}

Otevřete:

- <code>config/voidium/general.json</code>

Tohle je „hlavní rozvaděč“ modulů (Web, Discord, Stats, Ranks, Vote, …).

<div class="note">
	Tip: Pokud nechcete hned nastavovat Discord, dejte v <code>general.json</code> <code>enableDiscord</code> na <code>false</code>.
	Vyhnete se zbytečnému šumu kolem bota při startu.
</div>

Pak server restartujte.

## 🌐 Web Panel (volitelné) {#web}

1. Zkontrolujte, že v <code>general.json</code> je <code>enableWeb</code> zapnuté
2. V <code>config/voidium/web.json</code> nastavte port (default <code>8081</code>)
3. Ve hře (OP) nebo v konzoli serveru spusťte:

```
/voidium web
```

Vypíše URL s tokenem (otevírejte ideálně odkaz z konzole). Token je potřeba při prvním otevření a potom se uloží jako HTTP-only cookie.

## 🤖 Discord (volitelné) {#discord}

Voidium má dva přepínače:

- Globální: <code>config/voidium/general.json</code> → <code>enableDiscord</code>
- Modul Discord: <code>config/voidium/discord.json</code> → <code>enableDiscord</code>

Minimální kroky:

1. V <code>discord.json</code> nastavte <code>enableDiscord</code> na <code>true</code>
2. Vyplňte <code>botToken</code> a <code>guildId</code>
3. Pro první setup doporučuju restart serveru

Když měníte config za běhu, můžete reload:

```
/voidium reload
```

## 🔎 Ověření funkčnosti {#overeni}

### 1) Logy

Na startu dedikovaného serveru by mělo být něco jako:

- <code>VOIDIUM - INTELLIGENT SERVER CONTROL is loading...</code>

### 2) In-game příkaz

Spusťte:

```
/voidium status
```

Měli byste vidět metriky jako TPS/MSPT a další info.

## Další kroky

- [Konfigurace](Config_CZ.html)
- [Příkazy](Commands_CZ.html)
- [Discord modul](Discord_CZ.html)
