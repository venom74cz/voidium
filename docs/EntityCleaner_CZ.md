---
layout: default
title: Entity Cleaner (CZ)
---

# 🧹 Entity Cleaner

<div class="hero">
   <p><strong>Entity Cleaner</strong> je ClearLag alternativa zabudovaná přímo ve Voidium. Automaticky odstraňuje hozené itemy, XP orby, šípy a volitelně moby — s plnou ochranou pojmenovaných entit, ochočených zvířat, bossů a položek na whitelistu.</p>

   <div class="note">
      Zapnutý ve výchozím stavu. Konfigurace v <code>entitycleaner.json</code>.
   </div>

   <h2>Rychlá navigace</h2>
   <div class="card-grid">
      <a class="card" href="#jak-to-funguje">
         <div class="card-title"><span class="card-icon">⚙️</span>Jak to funguje</div>
         <div class="card-desc">Cyklus čištění, varování, typy</div>
      </a>
      <a class="card" href="#konfigurace">
         <div class="card-title"><span class="card-icon">📝</span>Konfigurace</div>
         <div class="card-desc">entitycleaner.json pole</div>
      </a>
      <a class="card" href="#ochrana">
         <div class="card-title"><span class="card-icon">🛡️</span>Ochrana</div>
         <div class="card-desc">Whitelisty, pojmenované, ochočené, bossové</div>
      </a>
      <a class="card" href="#prikazy">
         <div class="card-title"><span class="card-icon">⌨️</span>Příkazy</div>
         <div class="card-desc">Manuální clear a preview</div>
      </a>
   </div>
</div>

## ⚙️ Jak to funguje {#jak-to-funguje}

1. Každých `cleanupIntervalSeconds` (výchozí 300 = 5 minut) se spustí cyklus čištění.
2. Varovné zprávy se broadcastují v `warningTimes` (výchozí: 30s, 10s, 5s před čištěním).
3. V čas čištění se nakonfigurované typy entit odstraní ze všech načtených dimenzí.
4. Souhrnná zpráva ukáže počty odstraněných entit po kategoriích.

### Typy entit

| Přepínač | Výchozí | Odstraňuje |
|----------|---------|------------|
| `removeDroppedItems` | `true` | Hozené itemy |
| `removePassiveMobs` | `false` | Zvířata (krávy, prasata, ovce, atd.) |
| `removeHostileMobs` | `false` | Monstra (zombíci, kostlivci, creepeři, atd.) |
| `removeXpOrbs` | `true` | XP orby |
| `removeArrows` | `true` | Šípy zaseknuté v zemi/zdech |

<div class="note">
   <strong>Farm-friendly výchozí nastavení:</strong> Odstraňují se pouze itemy, XP a šípy. Moby jsou chráněné — vaše farmy a výběhy zůstanou v bezpečí.
</div>

## 📝 Konfigurace {#konfigurace}

Soubor: <code>config/voidium/entitycleaner.json</code>

| Pole | Typ | Výchozí | Popis |
|------|-----|---------|-------|
| `enabled` | boolean | `true` | Hlavní přepínač |
| `cleanupIntervalSeconds` | int | `300` | Sekundy mezi čištěními (min 10) |
| `warningTimes` | int[] | `[30, 10, 5]` | Sekundy před čištěním pro odeslání varování |
| `warningMessage` | string | `&e[EntityCleaner] &fClearing entities in &c%seconds% &fseconds!` | Varovný broadcast. Placeholder: `%seconds%` |
| `cleanupMessage` | string | `&a[EntityCleaner] &fRemoved &e%items% items...` | Souhrnná zpráva. Placeholdery: `%items%`, `%mobs%`, `%xp%`, `%arrows%` |

## 🛡️ Ochrana {#ochrana}

### Přepínače ochrany

| Pole | Výchozí | Popis |
|------|---------|-------|
| `removeNamedEntities` | `false` | Pokud `false`, entity s name tagem jsou chráněné |
| `removeTamedAnimals` | `false` | Pokud `false`, ochočená zvířata (vlci, kočky, koně) jsou chráněná |
| `protectBosses` | `true` | Pokud `true`, bossové (Ender Dragon, Wither, moddovaní) se nikdy neodstraní |

### Entity whitelist

Entity v `entityWhitelist` se **nikdy** neodstraní bez ohledu na ostatní nastavení:

```json
"entityWhitelist": [
  "minecraft:villager",
  "minecraft:iron_golem",
  "minecraft:snow_golem",
  "minecraft:wandering_trader",
  "minecraft:trader_llama"
]
```

### Item whitelist

Hozené itemy v `itemWhitelist` se **nikdy** neodstraní:

```json
"itemWhitelist": [
  "minecraft:diamond",
  "minecraft:netherite_ingot",
  "minecraft:netherite_scrap",
  "minecraft:elytra",
  "minecraft:nether_star",
  "minecraft:totem_of_undying",
  "minecraft:enchanted_golden_apple"
]
```

## ⌨️ Příkazy {#prikazy}

| Příkaz | Oprávnění | Popis |
|--------|-----------|-------|
| `/voidium clear` | OP | Vynutí čištění všech konfigurovaných typů |
| `/voidium clear items` | OP | Smaže pouze hozené itemy |
| `/voidium clear mobs` | OP | Smaže pouze moby |
| `/voidium clear xp` | OP | Smaže pouze XP orby |
| `/voidium clear arrows` | OP | Smaže pouze šípy |
| `/voidium clear preview` | OP | Náhled co by se smazalo (bez mazání) |

## Další

- <a href="Commands_CZ.html">Příkazy</a>
- <a href="Config_CZ.html">Konfigurace</a>
