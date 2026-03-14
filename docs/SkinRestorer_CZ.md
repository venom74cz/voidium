---
layout: default
title: Skin Restorer (CZ)
---

# 🧍 Skin Restorer

<div class="hero">
   <p><strong>Skin Restorer</strong> stahuje a aplikuje reálné Minecraft skiny pro hráče na offline-mode serverech. Skiny se injektují při připojení (bez nutnosti relogu) a cachují se lokálně pro snížení volání Mojang API.</p>

   <div class="note">
      Pro zapnutí: nastavte <code>enableSkinRestorer: true</code> v <code>general.json</code>. Automaticky vypnutý v online-mode.
   </div>

   <h2>Rychlá navigace</h2>
   <div class="card-grid">
      <a class="card" href="#jak-to-funguje">
         <div class="card-title"><span class="card-icon">⚙️</span>Jak to funguje</div>
         <div class="card-desc">Stažení, injekce, cache</div>
      </a>
      <a class="card" href="#konfigurace">
         <div class="card-title"><span class="card-icon">📝</span>Konfigurace</div>
         <div class="card-desc">TTL cache, manuální refresh</div>
      </a>
      <a class="card" href="#prikazy">
         <div class="card-title"><span class="card-icon">⌨️</span>Příkazy</div>
         <div class="card-desc">Manuální refresh skinu</div>
      </a>
   </div>
</div>

## ⚙️ Jak to funguje {#jak-to-funguje}

1. **Hráč se připojí** — Voidium zkontroluje, zda má hráč cachovaný skin
2. **Cache hit** — Pokud je skin cachovaný a nevypršel, aplikuje se okamžitě
3. **Cache miss** — Voidium se dotáže Mojang API:
   - Nejprve: přeloží jméno hráče → oficiální UUID přes `api.mojang.com`
   - Pak: stáhne skin texture data (value + signature) přes `sessionserver.mojang.com`
4. **Injekce** — Skin se aplikuje na game profil hráče při loginu (early join injekce — bez nutnosti relogu)
5. **Uložení cache** — Skin data se uloží do `skin-cache.json` s časovým razítkem

### Chytré funkce

- **Auto-vypnutí v online mode** — Když server běží v online mode, reálné skiny jsou již dostupné
- **Webhook integrace** — Když chat bridge používá webhooky, správný skin avatar se rozpozná i pro offline-mode hráče

## 📝 Konfigurace {#konfigurace}

Skin Restorer se konfiguruje přes `general.json`:

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `enableSkinRestorer` | boolean | `true` | Hlavní přepínač |
| `skinCacheHours` | int | `24` | Jak dlouho uchovávat cachované skiny před opětovným stažením (hodiny, min 1) |

### Cache soubor

Skiny se cachují v <code>config/voidium/storage/skin-cache.json</code>. Každý záznam obsahuje:

- Jméno hráče → skin texture value + signature + timestamp
- Záznamy starší než `skinCacheHours` se znovu stáhnou při dalším připojení

<div class="note">
   <strong>Tip:</strong> Nastavte <code>skinCacheHours</code> výš (např. 48) pro snížení volání Mojang API, nebo níž (např. 6) pokud hráči často mění skiny.
</div>

## ⌨️ Příkazy {#prikazy}

| Příkaz | Oprávnění | Popis |
|--------|-----------|-------|
| `/voidium skin <player>` | OP | Vynutí refresh skinu online hráče |

Příkaz ihned znovu stáhne skin z Mojang API a aktualizuje jak cache, tak vzhled hráče ve hře.

## Další

- <a href="Commands_CZ.html">Příkazy</a>
- <a href="Config_CZ.html">Konfigurace</a>
