---
layout: default
title: Příkazy (CZ)
---

# ⌨️ Příkazy

<div class="hero">
	<p>Voidium má <strong>Minecraft (in-game)</strong> příkazy a <strong>Discord</strong> slash příkazy (pro bota). Tady je seznam příkazů, které se v modu reálně registrují.</p>

	<div class="note">
		Většina subpříkazů <code>/voidium</code> vyžaduje <strong>permission level 2</strong> (OP). Pro všechny hráče je dostupné jen <code>/voidium status</code>.
	</div>

	<h2>Rychlá navigace</h2>
	<div class="card-grid">
		<a class="card" href="#mc-voidium">
			<div class="card-title"><span class="card-icon">🧰</span>/voidium</div>
			<div class="card-desc">Status, reload, web link, utility</div>
		</a>
		<a class="card" href="#mc-ticket">
			<div class="card-title"><span class="card-icon">🎫</span>/ticket</div>
			<div class="card-desc">Ticket na Discord přímo z MC</div>
		</a>
		<a class="card" href="#mc-reply">
			<div class="card-title"><span class="card-icon">💬</span>/reply</div>
			<div class="card-desc">Odpověď do otevřeného ticketu</div>
		</a>
		<a class="card" href="#discord">
			<div class="card-title"><span class="card-icon">🤖</span>Discord slash</div>
			<div class="card-desc">/link, /unlink, /ticket …</div>
		</a>
	</div>
</div>

## 🧰 Minecraft: /voidium {#mc-voidium}

Když napíšete jen <code>/voidium</code>, Voidium vypíše help do chatu.

### Pro všechny

- <code>/voidium status</code>
	- Vypíše MOTD, verzi Voidium, počet modů, TPS/MSPT, info o restartech a interval oznámení.

### OP (permission level 2)

- <code>/voidium reload</code> — reload konfigurace + restart relevantních managerů (kde to jde)
- <code>/voidium web</code> — vypíše URL pro Web Control
- <code>/voidium players</code> — seznam online hráčů (vč. pingu)
- <code>/voidium memory</code> — využití paměti JVM
- <code>/voidium config</code> — připomene, že config je v <code>config/voidium/</code>

### OP: restarty & oznámení

- <code>/voidium restart &lt;minutes&gt;</code> — naplánuje manuální restart (1–60 minut)
- <code>/voidium cancel</code> — zruší manuální restarty
- <code>/voidium announce &lt;message&gt;</code> — broadcast zprávy všem hráčům

<div class="note">
	Pokud je modul vypnutý (nebo jeho manager není dostupný), příkaz existuje, ale odpoví chybou (např. restarty/announcements/skin/votes/entity-cleaner).
</div>

### OP: skin refresh

- <code>/voidium skin &lt;player&gt;</code> — pokusí se refreshnout skin online hráče (užitečné v offline-mode)

### OP: votes

- <code>/voidium votes pending</code> — celkový počet pending votů
- <code>/voidium votes pending &lt;player&gt;</code> — pending voty pro hráče
- <code>/voidium votes clear</code> — vyčistí pending queue

### OP: entity cleaner (force cleanup)

- <code>/voidium clear</code> — smaže itemy + moby + XP + šípy
- <code>/voidium clear items|mobs|xp|arrows</code> — smaže jen danou kategorii
- <code>/voidium clear preview</code> — pouze náhled (bez mazání)

## 🎫 Minecraft: /ticket {#mc-ticket}

Vytvoří Discord ticket přímo z Minecraftu.

Syntaxe:

- <code>/ticket &lt;reason&gt; &lt;message...&gt;</code>

Poznámky:

- <code>reason</code> je jedno slovo (bez mezer). <code>message</code> může mít mezery.
- Vyžaduje propojený Discord účet. Když není propojený, hra vás pošle na <code>/link</code> v Discordu.

Příklad:

```
/ticket bug Web panel vrací 404
```

## 💬 Minecraft: /reply {#mc-reply}

Odpoví do vašeho aktuálně otevřeného ticketu (vytvořeného z Minecraftu nebo Discordu).

Syntaxe:

- <code>/reply &lt;message...&gt;</code>

Pokud nemáte otevřený ticket, dostanete chybu.

## 🤖 Discord: slash příkazy {#discord}

Tyhle příkazy jsou v <strong>Discordu</strong> (ne v Minecraftu). Registruje je Voidium bot, když je Discord integrace zapnutá a bot se úspěšně připojí.

- <code>/link code:&lt;code&gt;</code> — propojí Discord účet s Minecraft účtem
- <code>/unlink</code> — odpojí Discord účet
- <code>/ticket create reason:&lt;reason&gt;</code> — vytvoří ticket (Discord)
- <code>/ticket close</code> — uzavře ticket v aktuálním ticket kanálu

## Další

- [Konfigurace](Config_CZ.html)
- [Discord setup](Discord_CZ.html)
