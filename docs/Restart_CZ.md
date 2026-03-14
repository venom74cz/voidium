---
layout: default
title: Restart systém (CZ)
---

# 🔄 Restart systém

<div class="hero">
   <p><strong>Restart systém</strong> poskytuje automatické a manuální restarty serveru s konfigurovatelným rozvrhem, varováními pro hráče a bezpečným vypnutím.</p>

   <div class="note">
      Pro zapnutí: nastavte <code>enableRestarts: true</code> v <code>general.json</code>. Rozvrh nastavte v <code>restart.json</code>.
   </div>

   <h2>Rychlá navigace</h2>
   <div class="card-grid">
      <a class="card" href="#typy-restartu">
         <div class="card-title"><span class="card-icon">⏰</span>Typy restartů</div>
         <div class="card-desc">FIXED_TIME, INTERVAL, DELAY</div>
      </a>
      <a class="card" href="#konfigurace">
         <div class="card-title"><span class="card-icon">⚙️</span>Konfigurace</div>
         <div class="card-desc">restart.json pole</div>
      </a>
      <a class="card" href="#prikazy">
         <div class="card-title"><span class="card-icon">⌨️</span>Příkazy</div>
         <div class="card-desc">Manuální restart a zrušení</div>
      </a>
      <a class="card" href="#varovani">
         <div class="card-title"><span class="card-icon">⚠️</span>Varování</div>
         <div class="card-desc">Odpočet před restartem</div>
      </a>
   </div>
</div>

## ⏰ Typy restartů {#typy-restartu}

Voidium podporuje tři režimy plánování restartů:

| Typ | Popis |
|-----|-------|
| `FIXED_TIME` | Restart v konkrétní časy dne (např. 06:00, 18:00). Výchozí režim. |
| `INTERVAL` | Restart každých X hodin od posledního restartu. |
| `DELAY` | Restart X minut po startu serveru. |

V jeden moment je aktivní pouze jeden typ — nastavuje se přes `restartType` v configu.

## ⚙️ Konfigurace {#konfigurace}

Soubor: <code>config/voidium/restart.json</code>

### Rozvrh

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `restartType` | enum | `FIXED_TIME` | `FIXED_TIME`, `INTERVAL` nebo `DELAY` |
| `fixedRestartTimes` | pole | `["06:00", "18:00"]` | Časy restartu (HH:MM, 24h formát). Pouze pro `FIXED_TIME`. |
| `intervalHours` | int | `6` | Hodiny mezi restarty. Pouze pro `INTERVAL`. |
| `delayMinutes` | int | `60` | Minuty po startu. Pouze pro `DELAY`. |

### Zprávy

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `warningMessage` | string | `&cServer restart in %minutes% minutes!` | Varovný broadcast. Placeholder: `%minutes%` |
| `restartingNowMessage` | string | `&cServer is restarting now!` | Finální zpráva před vypnutím |
| `kickMessage` | string | `&cServer is restarting. Please reconnect in a few minutes.` | Text na obrazovce odpojení |

### Příklad configu

```json
{
  "restartType": "FIXED_TIME",
  "fixedRestartTimes": ["06:00", "18:00"],
  "intervalHours": 6,
  "delayMinutes": 60,
  "warningMessage": "&cServer restart in %minutes% minutes!",
  "restartingNowMessage": "&cServer is restarting now!",
  "kickMessage": "&cServer is restarting. Please reconnect in a few minutes."
}
```

## ⌨️ Příkazy {#prikazy}

| Příkaz | Oprávnění | Popis |
|--------|-----------|-------|
| `/voidium restart <minutes>` | OP | Naplánuje manuální restart za 1–60 minut |
| `/voidium cancel` | OP | Zruší čekající manuální restart |

Manuální restarty používají stejné varovné/kick zprávy jako automatické.

## ⚠️ Varování {#varovani}

Před provedením restartu Voidium posílá varovné zprávy v nastavených intervalech. `warningMessage` se broadcastuje s placeholderem `%minutes%` nahrazeným zbývajícím časem.

Když odpočet dosáhne nuly:
1. Broadcastuje se `restartingNowMessage`
2. Všichni hráči jsou odpojeni s `kickMessage`
3. Server se vypne

<div class="note">
   Pro automatický restart po vypnutí použijte funkci vašeho hostingu pro auto-restart nebo wrapper script, který server znovu spustí.
</div>

## Další

- <a href="Commands_CZ.html">Příkazy</a>
- <a href="Config_CZ.html">Konfigurace</a>
