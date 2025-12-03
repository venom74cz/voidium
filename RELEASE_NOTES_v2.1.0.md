# Voidium v2.1.0 Release Notes

## 🆕 New Feature: Entity Cleaner (ClearLag Alternative)

A complete entity cleanup system equivalent to popular ClearLag plugins, now fully integrated into Voidium!

### Features

**Automatic Cleanup**
- Configurable cleanup interval (default: 5 minutes)
- Warning system alerts players at 30s, 10s, and 5s before cleanup
- All messages fully customizable with color code support

**Entity Types Supported**
- 📦 **Dropped Items** - Remove ground items (enabled by default)
- ✨ **XP Orbs** - Remove experience orbs (enabled by default)
- 🏹 **Arrows** - Remove stuck arrows (enabled by default)
- 🐄 **Passive Mobs** - Remove passive animals (disabled by default)
- 👹 **Hostile Mobs** - Remove hostile creatures (disabled by default)

**Protection System (Farm-Friendly)**
- Named entities (name tags) protected by default
- Tamed animals protected by default
- Entity type whitelist (default: villagers, iron golems, wandering traders, allays, snow golems)
- Item type whitelist (default: diamonds, netherite items, elytra, enchanted books, totems, nether stars)

### Commands

| Command | Description |
|---------|-------------|
| `/voidium clear` | Force immediate cleanup of all enabled entity types |
| `/voidium clear items` | Remove only dropped items |
| `/voidium clear mobs` | Remove only mobs (passive + hostile) |
| `/voidium clear xp` | Remove only XP orbs |
| `/voidium clear arrows` | Remove only stuck arrows |
| `/voidium clear preview` | Show what would be removed without actually removing |

### Configuration

New config file: `config/voidium/entitycleaner.json`

```json
{
  "enabled": true,
  "cleanupIntervalSeconds": 300,
  "warningTimes": [30, 10, 5],
  "removeDroppedItems": true,
  "removePassiveMobs": false,
  "removeHostileMobs": false,
  "removeXpOrbs": true,
  "removeArrows": true,
  "removeNamedEntities": false,
  "removeTamedAnimals": false,
  "entityWhitelist": ["minecraft:villager", "minecraft:iron_golem", ...],
  "itemWhitelist": ["minecraft:diamond", "minecraft:netherite_ingot", ...],
  "warningMessage": "&e⚠ Entities will be cleared in %time% seconds!",
  "cleanupMessage": "&a✓ Cleared %items% items, %mobs% mobs, %xp% XP orbs, %arrows% arrows"
}
```

### Web Panel Integration

- Full configuration through web interface
- All settings editable with user-friendly form fields
- Complete English and Czech translations
- Field descriptions and validation

### Comparison to ClearLag Plugins

Voidium Entity Cleaner offers everything ClearLag does and more:

| Feature | ClearLag | Voidium |
|---------|----------|---------|
| Auto cleanup | ✅ | ✅ |
| Warning system | ✅ | ✅ |
| Item whitelist | ✅ | ✅ |
| Entity whitelist | ✅ | ✅ |
| Named entity protection | ✅ | ✅ |
| Tamed animal protection | ⚠️ Varies | ✅ |
| XP orb cleanup | ✅ | ✅ |
| Arrow cleanup | ✅ | ✅ |
| Web panel config | ❌ | ✅ |
| Bilingual messages | ❌ | ✅ |
| Hot reload | ❌ | ✅ |
| Preview command | ⚠️ Varies | ✅ |

---

## 📋 Other Changes

### Version Updates
- Version number updated to 2.1.0 across all components

### Documentation
- README.md completely updated with all current features
- Added Entity Cleaner section with full documentation
- Updated command list
- Added entitycleaner.json to configuration table

---

## ⬆️ Upgrade Notes

1. **New Config File**: `entitycleaner.json` will be auto-generated with safe defaults on first run
2. **Safe by Default**: Mob removal is disabled by default - your farms are safe!
3. **Reload Support**: Use `/voidium reload` to apply config changes without restart

---

## 📦 Installation

1. Download `voidium-2.1.0.jar`
2. Place in your server's `mods/` folder
3. Start server (config files auto-generated)
4. Configure via web panel (`/voidium web`) or edit JSON files

---

**Minecraft**: 1.21.1-1.21.10  
**NeoForge**: 21.1.208+  
**Java**: 21+
