---
layout: default
title: Stats & reporty (CZ)
---

# 📊 Stats & reporty

<div class="hero">
   <p><strong>Stats modul</strong> sleduje výkon serveru (TPS, počet hráčů) a posílá automatické denní reporty do Discord kanálu.</p>

   <div class="note">
      Pro zapnutí: nastavte <code>enableStats: true</code> v <code>general.json</code>. Kanál nastavte v <code>stats.json</code>.
   </div>

   <h2>Rychlá navigace</h2>
   <div class="card-grid">
      <a class="card" href="#prehled">
         <div class="card-title"><span class="card-icon">📈</span>Přehled</div>
         <div class="card-desc">Co se sleduje a jak</div>
      </a>
      <a class="card" href="#konfigurace">
         <div class="card-title"><span class="card-icon">⚙️</span>Konfigurace</div>
         <div class="card-desc">stats.json pole</div>
      </a>
      <a class="card" href="#denni-report">
         <div class="card-title"><span class="card-icon">📋</span>Denní report</div>
         <div class="card-desc">Formát Discord embedu</div>
      </a>
   </div>
</div>

## 📈 Přehled {#prehled}

Voidium vzorkuje počet hráčů každou minutu a sleduje:

- **Špičkový počet hráčů** — nejvyšší online počet za posledních 24 hodin
- **Průměrný počet hráčů** — průměr za dané období
- **TPS** — ticky za sekundu v reálném čase přes `TpsTracker`

Data se drží v paměti pro aktuální reportovací období a resetují se po odeslání denního reportu.

## ⚙️ Konfigurace {#konfigurace}

Soubor: <code>config/voidium/stats.json</code>

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `enableStats` | boolean | `true` | Hlavní přepínač |
| `reportChannelId` | string | `""` | ID Discord kanálu pro denní reporty |
| `reportTime` | string | `"09:00"` | Čas odeslání reportu (HH:mm, 24h) |
| `reportTitle` | string | `📊 Daily Statistics - %date%` | Titulek embedu. Placeholder: `%date%` |
| `reportPeakLabel` | string | `Peak Players` | Popisek pro špičku |
| `reportAverageLabel` | string | `Average Players` | Popisek pro průměr |
| `reportFooter` | string | `Voidium Stats` | Text patičky embedu |

### Příklad configu

```json
{
  "enableStats": true,
  "reportChannelId": "1234567890",
  "reportTime": "09:00",
  "reportTitle": "📊 Daily Statistics - %date%",
  "reportPeakLabel": "Peak Players",
  "reportAverageLabel": "Average Players",
  "reportFooter": "Voidium Stats"
}
```

## 📋 Denní report {#denni-report}

V nastavený `reportTime` Voidium odešle Discord embed do `reportChannelId`:

- **Titulek**: `reportTitle` s nahrazeným `%date%`
- **Pole**: Špičkový počet hráčů, Průměrný počet hráčů
- **Barva**: Cyan
- **Patička**: `reportFooter`

<div class="note">
   <strong>Požadavek:</strong> Discord modul musí být zapnutý a bot musí mít oprávnění posílat zprávy v kanálu pro reporty.
</div>

## Další

- <a href="Discord_CZ.html">Discord</a> — nastavení bota
- <a href="Config_CZ.html">Konfigurace</a> — všechny konfigurační soubory
