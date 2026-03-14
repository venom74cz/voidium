---
layout: default
title: Changelog (EN)
---

# 🧾 Changelog

<div class="hero">
	<p>Release history for Voidium. This page highlights the latest changes; full history is also available in the repository changelog.</p>

	<div class="card-grid">
		<a class="card" href="#latest">
			<div class="card-title"><span class="card-icon">🆕</span>Latest</div>
			<div class="card-desc">Newest release notes</div>
		</a>
		<a class="card" href="#history">
			<div class="card-title"><span class="card-icon">📚</span>History</div>
			<div class="card-desc">Previous versions</div>
		</a>
	</div>
</div>

## 🆕 Latest release {#latest}

### 2.5 — 2026‑03‑14

**Web Control Panel — React + Vite Rewrite**

- Complete React 19 + Vite 6 + TypeScript SPA rewrite of the admin panel, bundled in the mod JAR.
- SSE real-time updates (3-second push), dark/light theme, locale selector (EN/CZ).
- Config Studio: visual editor for all 12 modules with schema-driven forms, diff, preview, apply, rollback.
- Toast notifications, system info (CPU/RAM/Disk), server icon display, server.properties editor.
- Live console with command execution, live chat feed, audit trail, SVG performance graphs.
- MC text preview for color codes, custom conditions hints in rank editor.
- AI admin assistant with config suggestions, incident review, and per-player conversation history.
- API rate limiting on AI endpoints (120 req/min per IP).

**Security & Sessions**

- Secret masking in Config Studio, session cleanup, sliding sessions, HttpOnly cookies.
- Console command allowlist, AI secret redaction, configurable session TTL.

**Bug Fixes**

- Config Studio diff no longer shows phantom changes after apply.
- AnnouncementManager reload no longer crashes the scheduler.
- Mixin registration fixed for ban sync.

### 2.4 — 2026‑03‑11

- Full RGB color support in chat (<code>&#RRGGBB</code>).
- Rank tooltip on hover (played hours / required hours).
- Discord rename on link.
- Reload improvements (<code>/voidium reload</code> restarts all managers).

## 📚 Previous versions {#history}

- 2.3.5 — Discord bridge fix + Web panel docs
- 2.3.4 — Vote system hardening + release messaging improvements
- 2.3.3 — Vote age limit + V1 compatibility fixes
- 2.3.1 — Chat improvements + build metadata

Full history: see the repository [CHANGELOG](../CHANGELOG.md).
