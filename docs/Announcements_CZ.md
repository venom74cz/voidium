---
layout: default
title: Announcements (CZ)
---

# 📢 Announcements

<div class="hero">
   <p><strong>Announcements</strong> broadcastují rotující zprávy všem online hráčům v konfigurovatelném intervalu. Podporují barevné kódy a vlastní prefix.</p>

   <div class="note">
      Pro zapnutí: nastavte <code>enableAnnouncements: true</code> v <code>general.json</code>. Zprávy nastavte v <code>announcements.json</code>.
   </div>

   <h2>Rychlá navigace</h2>
   <div class="card-grid">
      <a class="card" href="#jak-to-funguje">
         <div class="card-title"><span class="card-icon">⚙️</span>Jak to funguje</div>
         <div class="card-desc">Rotace, intervaly, prefix</div>
      </a>
      <a class="card" href="#konfigurace">
         <div class="card-title"><span class="card-icon">📝</span>Konfigurace</div>
         <div class="card-desc">announcements.json pole</div>
      </a>
      <a class="card" href="#prikazy">
         <div class="card-title"><span class="card-icon">⌨️</span>Příkazy</div>
         <div class="card-desc">Manuální broadcast</div>
      </a>
   </div>
</div>

## ⚙️ Jak to funguje {#jak-to-funguje}

1. Voidium načte seznam zpráv z configu
2. Každých `announcementIntervalMinutes` (výchozí 30) se další zpráva v seznamu broadcastuje všem online hráčům
3. Zprávy rotují v pořadí — po poslední zprávě se vrátí na první
4. Ke každé zprávě se přidá konfigurovatelný `prefix`

## 📝 Konfigurace {#konfigurace}

Soubor: <code>config/voidium/announcements.json</code>

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `announcements` | string[] | `["&bWelcome to the server!", "&eDon't forget..."]` | Seznam zpráv k rotaci |
| `announcementIntervalMinutes` | int | `30` | Minuty mezi broadcasty (0 = vypnuto) |
| `prefix` | string | `&8[&bVoidium&8]&r ` | Prefix přidaný ke každé zprávě |

### Příklad configu

```json
{
  "announcements": [
    "&bVítejte na serveru!",
    "&eNavštivte naše webové stránky!",
    "&aPro kontakt s adminy použijte /ticket!"
  ],
  "announcementIntervalMinutes": 15,
  "prefix": "&8[&bServer&8]&r "
}
```

### Barevné kódy

Použijte `&` pro barevné kódy:

| Kód | Barva | Kód | Styl |
|-----|-------|-----|------|
| `&0`–`&9` | Černá až Modrá | `&l` | **Tučné** |
| `&a`–`&f` | Zelená až Bílá | `&o` | *Kurzíva* |
| | | `&n` | Podtržení |
| | | `&r` | Reset |

## ⌨️ Příkazy {#prikazy}

| Příkaz | Oprávnění | Popis |
|--------|-----------|-------|
| `/voidium announce <zpráva>` | OP | Odeslat jednorázový broadcast všem hráčům |

Manuální broadcast používá nastavený `prefix`.

## Další

- <a href="Commands_CZ.html">Příkazy</a>
- <a href="Config_CZ.html">Konfigurace</a>
