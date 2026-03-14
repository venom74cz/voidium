---
layout: default
title: Lokalizace (CZ)
---

# 🌍 Lokalizace

<div class="hero">
   <p><strong>Voidium</strong> podporuje <strong>anglické</strong> a <strong>české</strong> locale presety. Každá uživatelská zpráva se dá upravit v příslušném config souboru a jedním kliknutím lze přepnout všechny zprávy najednou.</p>

   <div class="note">
      Locale presety mění <strong>hodnoty konfiguračních polí</strong> (zprávy, popisky, prefixy). Neovlivňují kód modu ani příkazy — příkazy jsou vždy v angličtině.
   </div>

   <h2>Rychlá navigace</h2>
   <div class="card-grid">
      <a class="card" href="#jak-to-funguje">
         <div class="card-title"><span class="card-icon">⚙️</span>Jak to funguje</div>
         <div class="card-desc">Locale presety, zprávy per-modul</div>
      </a>
      <a class="card" href="#aplikovani-locale">
         <div class="card-title"><span class="card-icon">🔄</span>Aplikování locale</div>
         <div class="card-desc">Web panel, příkazy, config</div>
      </a>
      <a class="card" href="#placeholdery">
         <div class="card-title"><span class="card-icon">📝</span>Placeholdery</div>
         <div class="card-desc">Dynamické hodnoty ve zprávách</div>
      </a>
      <a class="card" href="#vlastni-zpravy">
         <div class="card-title"><span class="card-icon">✏️</span>Vlastní zprávy</div>
         <div class="card-desc">Přepis jednotlivých zpráv</div>
      </a>
   </div>
</div>

---

## ⚙️ Jak to funguje {#jak-to-funguje}

Voidium nepoužívá separátní jazykový soubor. Místo toho je každá zpráva **konfiguračním polem** v příslušném config souboru (např. `discord.json`, `restart.json`, `tickets.json`).

Třída `LocalePresets` poskytuje kompletní sady přeložených hodnot pro každý modul:

| Modul | Config soubor | Přeložená pole |
|-------|---------------|----------------|
| General | `general.json` | `modPrefix` |
| Discord | `discord.json` | 20+ polí: kick zprávy, link zprávy, odpovědi bota, stavové zprávy |
| Announcements | `announcements.json` | `prefix`, `announcements[]` |
| Ranks | `ranks.json` | `promotionMessage` |
| Votes | `vote.json` | `announcementMessage` |
| Tickets | `tickets.json` | 11 polí: ticket created/welcome/close zprávy, MC zprávy |
| Restart | `restart.json` | `warningMessage`, `restartingNowMessage`, `kickMessage` |
| Entity Cleaner | `entitycleaner.json` | `warningMessage`, `cleanupMessage` |
| Stats | `stats.json` | `reportTitle`, `reportPeakLabel`, `reportAverageLabel`, `reportFooter` |
| Player List | `playerlist.json` | `headerLine1–3`, `footerLine1–3` |

### Podporované locales

| Kód | Jazyk |
|-----|-------|
| `en` | Angličtina (výchozí) |
| `cz` | Čeština |

---

## 🔄 Aplikování locale {#aplikovani-locale}

### Přes Web Panel (doporučeno)

1. Otevřete **Config Studio** ve web panelu
2. Použijte dropdown **Locale Preset** (nebo zavolejte `/api/config/locale`)
3. Vyberte `en` nebo `cz`
4. Všechny zprávy všech modulů budou aktualizovány a uloženy okamžitě

### Přes příkaz ve hře

```
/voidium locale <en|cz>
```

Tento příkaz zavolá `applyLocale()` na každou config třídu, přepíše všechna pole se zprávami a uloží každý config soubor.

### Ručně

Upravte jednotlivé config soubory v `config/voidium/` a změňte konkrétní pole se zprávami. Máte tak plnou kontrolu — můžete míchat jazyky nebo psát úplně vlastní zprávy.

<div class="note">
   <strong>Důležité:</strong> Aplikování locale presetu <strong>přepíše</strong> jakékoli vlastní úpravy zpráv. Pokud jste zprávy přizpůsobili, zálohujte je před aplikací presetu.
</div>

---

## 📝 Placeholdery {#placeholdery}

Zprávy podporují dynamické placeholdery pomocí syntaxe `%název%`. Každé pole zprávy má svou sadu dostupných placeholderů:

### Běžné placeholdery

| Placeholder | Dostupný v | Hodnota |
|-------------|-----------|---------|
| `%player%` | Discord, Ranks, Votes | Jméno Minecraft hráče |
| `%user%` | Discord chat bridge | Uživatelské jméno na Discordu |
| `%message%` | Chat bridge | Text chatové zprávy |
| `%code%` | Discord whitelist | 6místný kód pro propojení |
| `%max%` | Discord linking | Max účtů na Discord uživatele |
| `%online%` | Téma kanálu | Počet online hráčů |
| `%uptime%` | Téma kanálu | Uptime serveru |
| `%days%`, `%hours%`, `%minutes%` | Formát uptime | Složky uptime |
| `%rank%` | Ranks | Název ranku |
| `%time%` | Restart varování | Čas do restartu |
| `%count%` | Entity Cleaner | Počet odstraněných entit |
| `%voter%` | Votes | Jméno hráče který hlasoval |

Placeholdery se nahrazují za běhu pomocí `String.replace()`. Pokud placeholder není nahrazen, zobrazí se tak jak je — to pomáhá odhalit překlepy.

---

## ✏️ Vlastní zprávy {#vlastni-zpravy}

Jakoukoliv zprávu můžete upravit přímou editací config pole:

### Příklad: Vlastní restart varování

V `restart.json`:
```json
{
  "warningMessage": "&c&l⚠ RESTART SERVERU za %time%! Uložte si stavby!",
  "restartingNowMessage": "&4Server se restartuje NYNÍ!",
  "kickMessage": "Server se restartuje. Připojte se prosím za chvilku."
}
```

### Příklad: Vlastní Discord úspěšné propojení

V `discord.json`:
```json
{
  "linkSuccessMessage": "✅ Úspěšně propojeno s **%player%**! Vítej na palubě."
}
```

### Barevné kódy

Všechny herní zprávy podporují `&` barevné kódy:

| Kód | Výsledek | Kód | Výsledek |
|-----|----------|-----|----------|
| `&0`–`&9` | Barvy (černá–modrá) | `&l` | **Tučné** |
| `&a`–`&f` | Barvy (zelená–bílá) | `&o` | *Kurzíva* |
| | | `&n` | Podtržení |
| | | `&r` | Reset |

### Jazyk Web Panelu

Jazyk UI web panelu se ovládá samostatně přes `web.json`:

```json
{
  "language": "cz"
}
```

Nastavte na `"cz"` pro české popisky v Config Studiu a dashboardu.

---

## Související stránky

- <a href="Config_CZ.html">Konfigurace</a> — Reference všech konfiguračních souborů
- <a href="Web_CZ.html">Web Control Panel</a> — Dokumentace Config Studia
- <a href="Discord_CZ.html">Discord</a> — Zprávy Discord bota
