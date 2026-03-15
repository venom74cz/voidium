---
layout: default
title: Changelog (CZ)
---

# 🧾 Changelog

<div class="hero">
	<p>Historie vydání Voidium. Na stránce jsou zvýrazněné poslední změny, plná historie je v repozitáři.</p>

	<div class="card-grid">
		<a class="card" href="#latest">
			<div class="card-title"><span class="card-icon">🆕</span>Nejnovější</div>
			<div class="card-desc">Poslední release notes</div>
		</a>
		<a class="card" href="#history">
			<div class="card-title"><span class="card-icon">📚</span>Historie</div>
			<div class="card-desc">Starší verze</div>
		</a>
	</div>
</div>

## 🆕 Nejnovější verze {#latest}

### 2.5.1 — 2026-03-15

**Opravy chatu a ranků**

- Odkazy z Discordu přeposlané do Minecraft chatu jsou znovu klikatelné.
- Rank prefixy a suffixy v TABu znovu ukazují tooltip s odehranými a požadovanými hodinami, i když je zapnutý PlayerList.
- Metadata verze byla sjednocena na 2.5.1.

### 2.5 — 2026‑03‑14

**Web Control Panel — přepsání do React + Vite**

- Kompletní přepsání admin panelu do React 19 + Vite 6 + TypeScript SPA, zabaleno v mod JARu.
- SSE real-time aktualizace (push každé 3s), dark/light téma, volba jazyka (EN/CZ).
- Config Studio: vizuální editor pro všech 12 modulů se schématem, diff, náhled, apply, rollback.
- Toast notifikace, systémové info (CPU/RAM/Disk), ikona serveru, editor server.properties.
- Živá konzole se spuštěním příkazů, live chat feed, audit trail, SVG grafy výkonu.
- Náhled MC textových barev, nápovědy u custom podmínek v editoru ranků.
- AI admin asistent s návrhy konfigurace, analýzou incidentů a historií konverzací hráčů.
- Rate limiting na AI endpointech (120 req/min na IP).

**Zabezpečení & Sessions**

- Maskované tajné hodnoty v Config Studiu, čištění session, sliding sessions, HttpOnly cookies.
- Allowlist příkazů konzole, redakce AI tajemství, nastavitelný session TTL.

**Opravy chyb**

- Config Studio diff už neukazuje falešné změny po apply.
- AnnouncementManager reload už nespad ne scheduler.
- Opravena registrace Mixinů pro synchronizaci banů.

### 2.4 — 2026‑03‑11

- Plná podpora RGB barev v chatu (<code>&#RRGGBB</code>).
- Tooltip u ranku při hoveru (odehrané hodiny / požadované hodiny).
- Discord přejmenování při propojení účtu.
- Vylepšený reload (<code>/voidium reload</code> restartuje všechny manažery).

## 📚 Historie verzí {#history}

- 2.5 — React + Vite rewrite web panelu
- 2.3.5 — Oprava Discord bridge + dokumentace Web panelu
- 2.3.4 — Zpevnění vote systému + release messaging
- 2.3.3 — Limit stáří votů + V1 kompatibilita
- 2.3.1 — Chat vylepšení + build metadata

Plná historie: viz repozitářový [CHANGELOG](../CHANGELOG.md).
