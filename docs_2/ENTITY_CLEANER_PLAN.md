# Plán modulu: Entity Cleaner

## 📌 Původní požadavek uživatele

> Can you? and also add so you can config it obviously, and maybe add so there is a true or false config for removing named mobs animals and shit. So it would delete all Dropped items mob and animals. But would allow whitelist on what to not delete and also a true or false for removing named entities.
>
> I'm on 1.21.1 neoforge so would appreciate it if you could update on this version
>
> also is this a server side mod?

---

## 📋 Specifikace modulu

### Název modulu
`EntityCleaner` (balíček: `cz.voidium.entitycleaner`)

### Účel
Automatické odstraňování entit ze světa pro snížení lag a zlepšení výkonu serveru.

### Hlavní funkce

1. **Automatické čištění entit v intervalech**
   - Konfigurovatelný interval (např. každých 5 minut)
   - Varování před čištěním (např. 30s, 10s, 5s před)

2. **Typy entit k odstranění**
   - Dropped items (ItemEntity)
   - Pasivní moby (zvířata)
   - Hostilní moby
   - XP orbs
   - Šípy (arrows)

3. **Whitelist systém**
   - Whitelist konkrétních typů entit (např. `minecraft:cow`)
   - Whitelist konkrétních itemů (např. `minecraft:diamond`)

4. **Ochrana pojmenovaných entit**
   - Toggle: `removeNamedEntities: true/false`
   - Pokud false, entity s custom name (name tag) nebudou odstraněny

5. **Ruční příkazy** (subpříkazy pod `/voidium`)
   - `/voidium clear` - okamžité vyčištění
   - `/voidium clear items` - pouze itemy
   - `/voidium clear mobs` - pouze moby
   - `/voidium clear preview` - zobrazí kolik entit by bylo odstraněno

---

## 🔗 Integrace s existujícím modem

### Existující struktura (nutno dodržet)
- **Hlavní třída:** `Voidium.java` - registrace event listenerů a inicializace managerů
- **Konfigurace:** `cz.voidium.config.*` - JSON formát s komentáři
- **Příkazy:** `cz.voidium.commands.VoidiumCommand.java` - všechny příkazy pod `/voidium`
- **Reload:** Existující `/voidium reload` automaticky reloadne všechny configy

### Co již existuje a lze využít
- `GeneralConfig.java` - přidat toggle `enableEntityCleaner`
- `VoidiumCommand.java` - přidat subpříkazy `/voidium clear ...`
- `AnnouncementManager.broadcastMessage()` - pro varování před čištěním
- Event bus registrace v `Voidium.java`
- `LocalePresets.java` - pro lokalizaci zpráv

---

## ⚙️ Návrh konfigurace

**Soubor:** `config/voidium/entitycleaner.json`

```json
{
  // Enable/disable automatic entity cleanup
  "enabled": true,
  
  // Interval between automatic cleanups (in seconds)
  "cleanupIntervalSeconds": 300,
  
  // Warning messages before cleanup (in seconds before cleanup)
  "warningTimes": [30, 10, 5],
  
  // === Entity Types to Remove ===
  // Remove dropped items (ItemEntity)
  "removeDroppedItems": true,
  
  // Remove passive mobs (animals like cows, pigs, sheep)
  "removePassiveMobs": false,
  
  // Remove hostile mobs (zombies, skeletons, creepers)
  "removeHostileMobs": false,
  
  // Remove experience orbs
  "removeXpOrbs": true,
  
  // Remove arrows stuck in ground/walls
  "removeArrows": true,
  
  // === Protection Settings ===
  // If false, entities with custom names (name tags) will be protected
  "removeNamedEntities": false,
  
  // If false, tamed animals (wolves, cats, horses) will be protected
  "removeTamedAnimals": false,
  
  // === Whitelists ===
  // Entity types that will NEVER be removed (use minecraft:entity_id format)
  "entityWhitelist": [
    "minecraft:villager",
    "minecraft:iron_golem",
    "minecraft:snow_golem",
    "minecraft:wandering_trader"
  ],
  
  // Dropped items that will NEVER be removed (use minecraft:item_id format)
  "itemWhitelist": [
    "minecraft:diamond",
    "minecraft:netherite_ingot",
    "minecraft:netherite_scrap",
    "minecraft:elytra",
    "minecraft:nether_star",
    "minecraft:totem_of_undying",
    "minecraft:enchanted_golden_apple"
  ],
  
  // === Messages (use & for color codes) ===
  "warningMessage": "&e[EntityCleaner] &fClearing entities in &c%seconds% &fseconds!",
  "cleanupMessage": "&a[EntityCleaner] &fRemoved &e%items% items &fand &e%mobs% mobs&f."
}
```

### Přidat do GeneralConfig.java
```java
// Enable/disable Entity Cleaner (automatic entity cleanup)
private boolean enableEntityCleaner = true;

public boolean isEnableEntityCleaner() { return enableEntityCleaner; }
```

---

## 🔧 Technická implementace

### Nové soubory

| Soubor | Umístění | Popis |
|--------|----------|-------|
| `EntityCleanerConfig.java` | `cz.voidium.config` | Konfigurace modulu (JSON) |
| `EntityCleanerManager.java` | `cz.voidium.entitycleaner` | Hlavní logika a scheduler |

### Úpravy existujících souborů

| Soubor | Změna |
|--------|-------|
| `Voidium.java` | Inicializace EntityCleanerManager v `onServerStarted` |
| `GeneralConfig.java` | Přidat `enableEntityCleaner` toggle |
| `VoidiumCommand.java` | Přidat subpříkazy `/voidium clear ...` |
| `LocalePresets.java` | Přidat zprávy pro EntityCleaner |
| `MODULES.md` | Dokumentace nového modulu |

### Klíčové třídy NeoForge/Minecraft
- `ServerLevel.getAllEntities()` - získání všech entit
- `Entity.discard()` - odstranění entity
- `ItemEntity` - dropped items
- `ExperienceOrb` - XP orbs
- `Arrow`, `SpectralArrow` - šípy
- `LivingEntity.hasCustomName()` - kontrola pojmenování
- `TamableAnimal.isTame()` - kontrola ochočení
- `Monster` - hostilní moby
- `Animal` - pasivní moby

### Timer implementace
Použít `ScheduledExecutorService` (jako `RestartManager`) nebo počítat ticky v `ServerTickEvent`.

---

## 💬 Zprávy hráčům

```
[EntityCleaner] Clearing entities in 30 seconds!
[EntityCleaner] Clearing entities in 10 seconds!
[EntityCleaner] Clearing entities in 5 seconds!
[EntityCleaner] Removed 156 items and 23 mobs.
```

---

## ✅ Odpovědi na otázky uživatele

| Otázka | Odpověď |
|--------|---------|
| Config support? | ✅ Ano, `entitycleaner.json` - plně konfigurovatelné |
| Named entities toggle? | ✅ `removeNamedEntities = true/false` |
| Whitelist? | ✅ Pro entity i itemy (2 separátní seznamy) |
| Server-side only? | ✅ Ano, čistě serverový modul (jako celý Voidium) |
| 1.21.1 NeoForge? | ✅ Voidium podporuje 1.21.1 - 1.21.10, NeoForge 21.1.208+ |

---

## 📝 Priorita funkcí

### Fáze 1 - MVP (Minimum Viable Product)
- Konfigurace (`EntityCleanerConfig.java`)
- Toggle v `GeneralConfig` (`enableEntityCleaner`)
- Automatické čištění dropped items a XP orbs
- Příkaz `/voidium clear`
- Inicializace v `Voidium.java`

### Fáze 2
- Whitelist pro itemy a entity
- Ochrana pojmenovaných entit
- Varování před čištěním
- Příkaz `/voidium clear preview`

### Fáze 3
- Čištění mobů (pasivní/hostilní)
- Ochrana tamed animals
- Příkazy `/voidium clear items`, `/voidium clear mobs`
- Lokalizace (en_us, cs_cz)

---

## ☑️ TODO Checklist

### Příprava
- [ ] Vytvořit package `cz.voidium.entitycleaner`
- [ ] Přidat modul do `MODULES.md`

### Konfigurace
- [ ] Vytvořit `EntityCleanerConfig.java` v `cz.voidium.config`
- [ ] Přidat `enableEntityCleaner` do `GeneralConfig.java`
- [ ] Přidat `EntityCleanerConfig.init()` do `Voidium.java` konstruktoru
- [ ] Přidat reload do `VoidiumCommand.reload()`

### Hlavní logika
- [ ] Vytvořit `EntityCleanerManager.java`
- [ ] Implementovat `start(MinecraftServer)` metodu
- [ ] Implementovat `stop()` metodu (shutdown scheduler)
- [ ] Implementovat scheduler pro automatické čištění
- [ ] Implementovat `cleanEntities()` metodu
- [ ] Implementovat filtrování podle typu entity
- [ ] Implementovat whitelist kontrolu
- [ ] Implementovat kontrolu pojmenovaných entit
- [ ] Implementovat kontrolu ochočených zvířat
- [ ] Implementovat varování před čištěním

### Integrace do Voidium.java
- [ ] Přidat `private EntityCleanerManager entityCleanerManager;`
- [ ] Spustit manager v `onServerStarted` (pokud `gc.isEnableEntityCleaner()`)
- [ ] Zastavit manager v `onServerStopping`

### Příkazy (VoidiumCommand.java)
- [ ] Přidat `/voidium clear` - okamžité vyčištění
- [ ] Přidat `/voidium clear items` - pouze itemy
- [ ] Přidat `/voidium clear mobs` - pouze moby
- [ ] Přidat `/voidium clear preview` - preview počtu entit
- [ ] Přidat help text do `showHelp()`

### Zprávy a lokalizace
- [ ] Přidat zprávy do `EntityCleanerConfig`
- [ ] Přidat lokalizaci do `LocalePresets.java` (en_us, cs_cz)

### Testování
- [ ] Otestovat automatické čištění v intervalu
- [ ] Otestovat whitelist pro entity
- [ ] Otestovat whitelist pro itemy
- [ ] Otestovat ochranu pojmenovaných entit
- [ ] Otestovat ochranu tamed animals
- [ ] Otestovat všechny příkazy
- [ ] Otestovat reload konfigurace
- [ ] Otestovat na serveru (server-side only)

### Dokumentace
- [ ] Aktualizovat `MODULES.md` - přidat EntityCleaner sekci
- [ ] Aktualizovat `README.md` - přidat EntityCleaner sekci
- [ ] Vytvořit release notes

---

## 📅 Časový odhad

| Fáze | Odhad času |
|------|------------|
| Fáze 1 (MVP) | 2-3 hodiny |
| Fáze 2 | 2-3 hodiny |
| Fáze 3 | 1-2 hodiny |
| Testování | 1-2 hodiny |
| **Celkem** | **6-10 hodin** |
