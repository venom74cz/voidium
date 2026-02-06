---
layout: default
title: Commands (EN)
---

# ⌨️ Commands

<div class="hero">
	<p>Voidium has <strong>Minecraft (in-game)</strong> commands and <strong>Discord</strong> slash commands (for the bot). This page lists the real commands currently registered in the mod.</p>

	<div class="note">
		Most <code>/voidium</code> subcommands require <strong>permission level 2</strong> (OP). Only <code>/voidium status</code> is available to everyone.
	</div>

	<h2>Jump to</h2>
	<div class="card-grid">
		<a class="card" href="#mc-voidium">
			<div class="card-title"><span class="card-icon">🧰</span>/voidium</div>
			<div class="card-desc">Status, reload, web link, utilities</div>
		</a>
		<a class="card" href="#mc-ticket">
			<div class="card-title"><span class="card-icon">🎫</span>/ticket</div>
			<div class="card-desc">Create a Discord ticket from Minecraft</div>
		</a>
		<a class="card" href="#mc-reply">
			<div class="card-title"><span class="card-icon">💬</span>/reply</div>
			<div class="card-desc">Reply to your open ticket</div>
		</a>
		<a class="card" href="#discord">
			<div class="card-title"><span class="card-icon">🤖</span>Discord slash</div>
			<div class="card-desc">/link, /unlink, /ticket …</div>
		</a>
	</div>
</div>

## 🧰 Minecraft: /voidium {#mc-voidium}

Typing <code>/voidium</code> without subcommand shows a help list in chat.

### Everyone

- <code>/voidium status</code>
  - Shows server MOTD, Voidium version, mod count, TPS/MSPT, restart schedule info, announcement interval.

### OP (permission level 2)

- <code>/voidium reload</code> — reloads configs and restarts relevant managers where possible
- <code>/voidium web</code> — prints the Web Control URL
- <code>/voidium players</code> — lists online players (incl. ping)
- <code>/voidium memory</code> — prints JVM memory usage
- <code>/voidium config</code> — points you to <code>config/voidium/</code> and key files

### OP: restarts & announcements

- <code>/voidium restart &lt;minutes&gt;</code> — schedules a manual restart (1–60 minutes)
- <code>/voidium cancel</code> — cancels manual restarts
- <code>/voidium announce &lt;message&gt;</code> — broadcasts a message to all players

<div class="note">
	If a module is disabled (or its manager is not available), the command still exists but will respond with an error (e.g., restarts/announcements/skin/votes/entity-cleaner).
</div>

### OP: skin refresh

- <code>/voidium skin &lt;player&gt;</code> — tries to refresh an online player’s skin (useful for offline-mode setups)

### OP: votes

- <code>/voidium votes pending</code> — shows total pending votes
- <code>/voidium votes pending &lt;player&gt;</code> — shows pending votes for a player
- <code>/voidium votes clear</code> — clears the pending vote queue

### OP: entity cleaner (force cleanup)

- <code>/voidium clear</code> — clears items + mobs + XP + arrows
- <code>/voidium clear items|mobs|xp|arrows</code> — clears only that category
- <code>/voidium clear preview</code> — preview only (no deletion)

## 🎫 Minecraft: /ticket {#mc-ticket}

Creates a Discord ticket directly from Minecraft.

Syntax:

- <code>/ticket &lt;reason&gt; &lt;message...&gt;</code>

Notes:

- <code>reason</code> is a single word (no spaces). The <code>message</code> can contain spaces.
- Requires your Discord account to be linked. If it isn’t, the game will tell you to use <code>/link</code> on Discord.

Example:

```
/ticket bug The web panel returns 404
```

## 💬 Minecraft: /reply {#mc-reply}

Replies to your currently open ticket (created from Minecraft or Discord).

Syntax:

- <code>/reply &lt;message...&gt;</code>

If you don’t have an open ticket, you’ll get an error.

## 🤖 Discord: slash commands {#discord}

These commands exist in <strong>Discord</strong> (not in Minecraft). They are registered by the Voidium bot when Discord integration is enabled and the bot successfully connects.

- <code>/link code:&lt;code&gt;</code> — links your Discord account to your Minecraft account
- <code>/unlink</code> — unlinks your Discord account
- <code>/ticket create reason:&lt;reason&gt;</code> — creates a new ticket (Discord side)
- <code>/ticket close</code> — closes the current ticket channel

## Next

- [Configuration](Config_EN.html)
- [Discord setup](Discord_EN.html)
