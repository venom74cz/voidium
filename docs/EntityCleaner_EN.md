---
layout: default
title: Entity Cleaner (EN)
---

# 🧹 Entity Cleaner

<div class="hero">
   <p><strong>Entity Cleaner</strong> is a ClearLag alternative built into Voidium. It automatically removes dropped items, XP orbs, arrows, and optionally mobs — with full protection for named entities, tamed animals, bosses, and whitelisted items.</p>

   <div class="note">
      Enabled by default. Configure in <code>entitycleaner.json</code>.
   </div>

   <h2>Jump to</h2>
   <div class="card-grid">
      <a class="card" href="#how-it-works">
         <div class="card-title"><span class="card-icon">⚙️</span>How it works</div>
         <div class="card-desc">Cleanup cycle, warnings, types</div>
      </a>
      <a class="card" href="#config">
         <div class="card-title"><span class="card-icon">📝</span>Configuration</div>
         <div class="card-desc">entitycleaner.json fields</div>
      </a>
      <a class="card" href="#protection">
         <div class="card-title"><span class="card-icon">🛡️</span>Protection</div>
         <div class="card-desc">Whitelists, named, tamed, bosses</div>
      </a>
      <a class="card" href="#commands">
         <div class="card-title"><span class="card-icon">⌨️</span>Commands</div>
         <div class="card-desc">Manual clear & preview</div>
      </a>
   </div>
</div>

## ⚙️ How it works {#how-it-works}

1. Every `cleanupIntervalSeconds` (default 300 = 5 minutes), a cleanup cycle starts.
2. Warning messages are broadcast at `warningTimes` (default: 30s, 10s, 5s before cleanup).
3. At cleanup time, the configured entity types are removed from all loaded dimensions.
4. A summary message shows removed counts per category.

### Entity types

| Toggle | Default | Removes |
|--------|---------|---------|
| `removeDroppedItems` | `true` | Dropped item entities |
| `removePassiveMobs` | `false` | Animals (cows, pigs, sheep, etc.) |
| `removeHostileMobs` | `false` | Monsters (zombies, skeletons, creepers, etc.) |
| `removeXpOrbs` | `true` | Experience orbs |
| `removeArrows` | `true` | Arrows stuck in ground/walls |

<div class="note">
   <strong>Farm-friendly defaults:</strong> Only items, XP, and arrows are removed. Mobs are protected by default — your farms and animal pens stay safe.
</div>

## 📝 Configuration {#config}

File: <code>config/voidium/entitycleaner.json</code>

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | boolean | `true` | Master switch |
| `cleanupIntervalSeconds` | int | `300` | Seconds between cleanups (min 10) |
| `warningTimes` | int[] | `[30, 10, 5]` | Seconds before cleanup to send warnings |
| `warningMessage` | string | `&e[EntityCleaner] &fClearing entities in &c%seconds% &fseconds!` | Warning broadcast. Placeholder: `%seconds%` |
| `cleanupMessage` | string | `&a[EntityCleaner] &fRemoved &e%items% items&f, &e%mobs% mobs&f, &e%xp% XP orbs&f, &e%arrows% arrows&f.` | Summary message. Placeholders: `%items%`, `%mobs%`, `%xp%`, `%arrows%` |

## 🛡️ Protection {#protection}

### Protection toggles

| Field | Default | Description |
|-------|---------|-------------|
| `removeNamedEntities` | `false` | If `false`, entities with name tags are protected |
| `removeTamedAnimals` | `false` | If `false`, tamed animals (wolves, cats, horses) are protected |
| `protectBosses` | `true` | If `true`, bosses (Ender Dragon, Wither, modded bosses) are never removed |

### Entity whitelist

Entities in `entityWhitelist` are **never** removed regardless of other settings:

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

Dropped items in `itemWhitelist` are **never** removed:

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

## ⌨️ Commands {#commands}

| Command | Permission | Description |
|---------|-----------|-------------|
| `/voidium clear` | OP | Force cleanup of all configured types |
| `/voidium clear items` | OP | Clear only dropped items |
| `/voidium clear mobs` | OP | Clear only mobs |
| `/voidium clear xp` | OP | Clear only XP orbs |
| `/voidium clear arrows` | OP | Clear only arrows |
| `/voidium clear preview` | OP | Preview what would be removed (no deletion) |

## Related

- <a href="Commands_EN.html">Commands</a>
- <a href="Config_EN.html">Configuration</a>
