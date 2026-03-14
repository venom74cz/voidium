---
layout: default
title: Playerlist (CZ)
---

# 🧍 Playerlist (TAB)

<div class="hero">
   <p><strong>Player List</strong> přizpůsobuje TAB menu s konfigurovatelným headerem, footerem a formátováním jmen hráčů. Podporuje živé placeholdery, rank prefixy/suffixy z Discord rolí i Auto‑Rank systému.</p>

   <div class="note">
      Pro zapnutí: nastavte <code>enablePlayerList: true</code> v <code>general.json</code>. Konfigurace v <code>playerlist.json</code>.
   </div>

   <h2>Rychlá navigace</h2>
   <div class="card-grid">
      <a class="card" href="#header-footer">
         <div class="card-title"><span class="card-icon">📋</span>Header & Footer</div>
         <div class="card-desc">3 řádky s placeholdery</div>
      </a>
      <a class="card" href="#jmena-hracu">
         <div class="card-title"><span class="card-icon">👤</span>Jména hráčů</div>
         <div class="card-desc">Prefixy, suffixy, formátování</div>
      </a>
      <a class="card" href="#konfigurace">
         <div class="card-title"><span class="card-icon">⚙️</span>Konfigurace</div>
         <div class="card-desc">playerlist.json pole</div>
      </a>
   </div>
</div>

## 📋 Header & Footer {#header-footer}

Header a footer TAB listu mají každý 3 konfigurovatelné řádky. Použijte `§` barevné kódy a placeholdery:

### Placeholdery

| Placeholder | Popis |
|-------------|-------|
| `%online%` | Aktuální počet online hráčů |
| `%max%` | Maximální počet slotů |
| `%tps%` | TPS serveru |
| `%ping%` | Ping hráče v ms |
| `%playtime%` | Odehraný čas hráče v hodinách |
| `%time%` | Aktuální čas serveru |
| `%memory%` | Využití paměti JVM |

### Výchozí header

```
§b§l✦ VOIDIUM SERVER ✦
§7Online: §a%online%§7/§a%max%
```

### Výchozí footer

```
§7TPS: §a%tps%
§7Ping: §e%ping%ms
```

## 👤 Jména hráčů {#jmena-hracu}

### Formát jména

Jména hráčů v TABu se formátují přes `playerNameFormat`:

```
%rank_prefix%%player_name%%rank_suffix%
```

### Zdroje ranků

Voidium kombinuje ranky z více zdrojů:

1. **Discord role** — prefix/suffix z `rolePrefixes` v `discord.json`
2. **Časové ranky** — z Auto-Rank systému (`ranks.json`)

Když je `combineMultipleRanks` `true`, všechny aplikovatelné prefixy/suffixy se zkombinují. Když `false`, použije se pouze ten s nejvyšší prioritou.

### Výchozí prefix/suffix

Hráči bez ranku dostanou `defaultPrefix` a `defaultSuffix` (výchozí: `§7` šedá a prázdný).

## ⚙️ Konfigurace {#konfigurace}

Soubor: <code>config/voidium/playerlist.json</code>

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `enableCustomPlayerList` | boolean | `true` | Hlavní přepínač |
| `headerLine1` | string | `§b§l✦ VOIDIUM SERVER ✦` | Header řádek 1 |
| `headerLine2` | string | `§7Online: §a%online%§7/§a%max%` | Header řádek 2 |
| `headerLine3` | string | `""` | Header řádek 3 |
| `footerLine1` | string | `§7TPS: §a%tps%` | Footer řádek 1 |
| `footerLine2` | string | `§7Ping: §e%ping%ms` | Footer řádek 2 |
| `footerLine3` | string | `""` | Footer řádek 3 |
| `enableCustomNames` | boolean | `true` | Zapnout formátování jmen |
| `playerNameFormat` | string | `%rank_prefix%%player_name%%rank_suffix%` | Šablona formátu jména |
| `defaultPrefix` | string | `§7` | Záložní prefix |
| `defaultSuffix` | string | `""` | Záložní suffix |
| `combineMultipleRanks` | boolean | `true` | Kombinovat všechny ranky nebo jen nejvyšší |
| `updateIntervalSeconds` | int | `5` | Interval aktualizace (min 3) |

## Další

- <a href="Ranks_CZ.html">Auto-Rank</a> — systém časových ranků
- <a href="Discord_CZ.html">Discord</a> — role prefixy
- <a href="Config_CZ.html">Konfigurace</a>
