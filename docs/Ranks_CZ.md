---
layout: default
title: Auto‑Rank (CZ)
---

# 🏅 Auto‑Rank

<div class="hero">
   <p><strong>Auto‑Rank</strong> povyšuje hráče na základě odehraného času a volitelných vlastních podmínek (zabití mobů, návštěvy biomů, těžení/pokládání bloků). Každý rank může přidat prefix nebo suffix s plnou podporou barevných kódů &amp; RGB.</p>

   <div class="note">
      Pro zapnutí: nastavte <code>enableRanks: true</code> v <code>general.json</code>. Pak upravte ranky v <code>ranks.json</code>.
   </div>

   <h2>Rychlá navigace</h2>
   <div class="card-grid">
      <a class="card" href="#jak-to-funguje">
         <div class="card-title"><span class="card-icon">⚙️</span>Jak to funguje</div>
         <div class="card-desc">Odehraný čas, podmínky, povýšení</div>
      </a>
      <a class="card" href="#konfigurace">
         <div class="card-title"><span class="card-icon">📝</span>Konfigurace</div>
         <div class="card-desc">ranks.json pole</div>
      </a>
      <a class="card" href="#vlastni-podminky">
         <div class="card-title"><span class="card-icon">🎯</span>Vlastní podmínky</div>
         <div class="card-desc">KILL, VISIT, BREAK, PLACE</div>
      </a>
      <a class="card" href="#tooltipy">
         <div class="card-title"><span class="card-icon">💬</span>Hover tooltipy</div>
         <div class="card-desc">Info o odehraném čase při najetí</div>
      </a>
   </div>
</div>

## ⚙️ Jak to funguje {#jak-to-funguje}

1. **Kontrola odehraného času** — Voidium čte statistiku `PLAY_TIME` hráče (ticky → hodiny) každých `checkIntervalMinutes` (výchozí 5).
2. **Vlastní podmínky** — Pokud definice ranku obsahuje custom conditions, hráč je musí splnit (sleduje `ProgressTracker`).
3. **Nejvyšší vyhrává** — Aplikuje se rank s nejvyšším požadavkem `hours`, který hráč splňuje.
4. **Zpráva o povýšení** — Když hráč poprvé dosáhne nového ranku, vidí `promotionMessage` v chatu.
5. **Zobrazení** — Rank prefix/suffix se zobrazuje v TAB listu (pokud je Player List modul zapnutý) nebo v display name v chatu.

## 📝 Konfigurace {#konfigurace}

Soubor: <code>config/voidium/ranks.json</code>

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `enableAutoRanks` | boolean | `true` | Hlavní přepínač |
| `checkIntervalMinutes` | int | `5` | Jak často kontrolovat odehraný čas (min 1) |
| `promotionMessage` | string | `&aCongratulations! You have earned the rank &b%rank%&a!` | Zpráva při povýšení. Placeholdery: `%rank%`, `{player}`, `{hours}` |
| `tooltipPlayed` | string | `§7Played: §f%hours%h` | Hover text s odehranými hodinami |
| `tooltipRequired` | string | `§7Required: §f%hours%h` | Hover text s požadovanými hodinami |
| `ranks` | pole | _(viz níže)_ | Seznam definic ranků |

### Definice ranku

Každý záznam v poli `ranks`:

```json
{
  "type": "PREFIX",
  "value": "&6[Veteran] ",
  "hours": 100,
  "customConditions": []
}
```

| Pole | Popis |
|------|-------|
| `type` | `"PREFIX"` nebo `"SUFFIX"` |
| `value` | Zobrazený text (podporuje `&` kódy, `&#RRGGBB` hex) |
| `hours` | Minimální odehraný čas v hodinách |
| `customConditions` | Volitelné pole dalších požadavků |

### Výchozí ranky

| Rank | Typ | Hodiny | Zobrazení |
|------|-----|--------|-----------|
| Member | PREFIX | 10 | `&7[Member] ` |
| Veteran | PREFIX | 100 | `&6[Veteran] ` |
| Star | SUFFIX | 500 | ` &e★` |

## 🎯 Vlastní podmínky {#vlastni-podminky}

Vlastní podmínky umožňují požadovat úspěchy navíc k odehranému času. Každá podmínka má `type` a `count`:

```json
{
  "type": "PREFIX",
  "value": "&c[Hunter] ",
  "hours": 50,
  "customConditions": [
    { "type": "KILL", "count": 500 }
  ]
}
```

### Typy podmínek

| Typ | Sleduje | Příklad |
|-----|---------|---------|
| `KILL` | Zabité moby (jakýkoliv typ) | 500 zabití → "Hunter" |
| `VISIT` | Navštívené unikátní biomy | 10 biomů → "Explorer" |
| `BREAK` | Zničené bloky (jakýkoliv typ) | 1000 bloků → "Miner" |
| `PLACE` | Položené bloky (jakýkoliv typ) | 500 bloků → "Builder" |

Postup se sleduje per-player v <code>config/voidium/storage/player_progress.json</code> a přetrvává přes restarty.

Můžete kombinovat více podmínek na jednom ranku:

```json
{
  "type": "SUFFIX",
  "value": " &d♦",
  "hours": 200,
  "customConditions": [
    { "type": "KILL", "count": 1000 },
    { "type": "VISIT", "count": 15 }
  ]
}
```

Hráč musí splnit **všechny** podmínky (AND logika).

## 💬 Hover tooltipy {#tooltipy}

Když se zobrazuje rank prefix nebo suffix hráče, najetí myší ukáže:

- **Odehrané hodiny** — formátované přes `tooltipPlayed` (např. `Played: 142.5h`)
- **Požadované hodiny** — formátované přes `tooltipRequired` (např. `Required: 100h`)

Funguje v chatu i v TAB player listu.

## Integrace s Player Listem

Když je modul Player List zapnutý (`enablePlayerList: true` v `general.json`), rank prefixy a suffixy se aplikují přes scoreboard týmy v TABu. `RankManager` přenechá zobrazení `PlayerListManageru` aby nedocházelo k duplicitě.

Když je Player List vypnutý, ranky se aplikují přímo na display name hráče v chatu.

## Další

- <a href="Commands_CZ.html">Příkazy</a> — <code>/voidium status</code> zobrazí info o rankách
- <a href="PlayerList_CZ.html">Player List</a> — TAB integrace
- <a href="Config_CZ.html">Konfigurace</a> — všechny konfigurační soubory
