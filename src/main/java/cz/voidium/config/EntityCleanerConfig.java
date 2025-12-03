package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class EntityCleanerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static EntityCleanerConfig instance;
    private transient Path configPath;

    // === ENTITY CLEANER CONFIGURATION ===
    
    // Enable/disable automatic entity cleanup
    private boolean enabled = true;
    
    // Interval between automatic cleanups (in seconds)
    private int cleanupIntervalSeconds = 300;
    
    // Warning messages before cleanup (seconds before cleanup)
    private List<Integer> warningTimes = new ArrayList<>();
    
    // === Entity Types to Remove ===
    
    // Remove dropped items (ItemEntity)
    private boolean removeDroppedItems = true;
    
    // Remove passive mobs (animals like cows, pigs, sheep)
    private boolean removePassiveMobs = false;
    
    // Remove hostile mobs (zombies, skeletons, creepers)
    private boolean removeHostileMobs = false;
    
    // Remove experience orbs
    private boolean removeXpOrbs = true;
    
    // Remove arrows stuck in ground/walls
    private boolean removeArrows = true;
    
    // === Protection Settings ===
    
    // If false, entities with custom names (name tags) will be protected
    private boolean removeNamedEntities = false;
    
    // If false, tamed animals (wolves, cats, horses) will be protected
    private boolean removeTamedAnimals = false;
    
    // === Whitelists ===
    
    // Entity types that will NEVER be removed (use minecraft:entity_id format)
    private List<String> entityWhitelist = new ArrayList<>();
    
    // Dropped items that will NEVER be removed (use minecraft:item_id format)
    private List<String> itemWhitelist = new ArrayList<>();
    
    // === Messages ===
    
    // Warning message sent before cleanup. Use %seconds% for remaining time.
    private String warningMessage = "&e[EntityCleaner] &fClearing entities in &c%seconds% &fseconds!";
    
    // Message sent after cleanup. Use %items%, %mobs%, %xp%, %arrows% for counts.
    private String cleanupMessage = "&a[EntityCleaner] &fRemoved &e%items% items&f, &e%mobs% mobs&f, &e%xp% XP orbs&f, &e%arrows% arrows&f.";

    public EntityCleanerConfig(Path configPath) {
        this.configPath = configPath;
        // Default warning times
        warningTimes.add(30);
        warningTimes.add(10);
        warningTimes.add(5);
        // Default entity whitelist
        entityWhitelist.add("minecraft:villager");
        entityWhitelist.add("minecraft:iron_golem");
        entityWhitelist.add("minecraft:snow_golem");
        entityWhitelist.add("minecraft:wandering_trader");
        entityWhitelist.add("minecraft:trader_llama");
        // Default item whitelist
        itemWhitelist.add("minecraft:diamond");
        itemWhitelist.add("minecraft:netherite_ingot");
        itemWhitelist.add("minecraft:netherite_scrap");
        itemWhitelist.add("minecraft:elytra");
        itemWhitelist.add("minecraft:nether_star");
        itemWhitelist.add("minecraft:totem_of_undying");
        itemWhitelist.add("minecraft:enchanted_golden_apple");
    }

    public static EntityCleanerConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        Path configPath = configDir.resolve("entitycleaner.json");
        instance = load(configPath);
    }

    private static EntityCleanerConfig load(Path configPath) {
        EntityCleanerConfig config = new EntityCleanerConfig(configPath);
        
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                EntityCleanerConfig loaded = GSON.fromJson(reader, EntityCleanerConfig.class);
                if (loaded != null) {
                    loaded.configPath = configPath;
                    return loaded;
                }
            } catch (Exception e) {
                System.err.println("Failed to load entitycleaner configuration: " + e.getMessage());
            }
        }
        
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                writer.write("// === ENTITY CLEANER CONFIGURATION ===\n");
                writer.write("// Enable/disable automatic entity cleanup\n");
                writer.write("// cleanupIntervalSeconds - time between cleanups in seconds\n");
                writer.write("// warningTimes - seconds before cleanup to show warning (e.g. [30, 10, 5])\n");
                writer.write("// \n");
                writer.write("// === Entity Types to Remove ===\n");
                writer.write("// removeDroppedItems - dropped items (ItemEntity)\n");
                writer.write("// removePassiveMobs - animals (cows, pigs, sheep, etc.)\n");
                writer.write("// removeHostileMobs - monsters (zombies, skeletons, creepers, etc.)\n");
                writer.write("// removeXpOrbs - experience orbs\n");
                writer.write("// removeArrows - arrows stuck in ground/walls\n");
                writer.write("// \n");
                writer.write("// === Protection Settings ===\n");
                writer.write("// removeNamedEntities - if false, named entities (name tags) are protected\n");
                writer.write("// removeTamedAnimals - if false, tamed animals are protected\n");
                writer.write("// \n");
                writer.write("// === Whitelists ===\n");
                writer.write("// entityWhitelist - entity types that will NEVER be removed\n");
                writer.write("// itemWhitelist - dropped items that will NEVER be removed\n");
                writer.write("// \n");
                writer.write("// === Messages ===\n");
                writer.write("// Use & for color codes, %seconds% for time, %items%/%mobs%/%xp%/%arrows% for counts\n\n");
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save entitycleaner configuration: " + e.getMessage());
        }
    }

    // === Getters ===
    public boolean isEnabled() { return enabled; }
    public int getCleanupIntervalSeconds() { return cleanupIntervalSeconds; }
    public List<Integer> getWarningTimes() { return warningTimes; }
    public boolean isRemoveDroppedItems() { return removeDroppedItems; }
    public boolean isRemovePassiveMobs() { return removePassiveMobs; }
    public boolean isRemoveHostileMobs() { return removeHostileMobs; }
    public boolean isRemoveXpOrbs() { return removeXpOrbs; }
    public boolean isRemoveArrows() { return removeArrows; }
    public boolean isRemoveNamedEntities() { return removeNamedEntities; }
    public boolean isRemoveTamedAnimals() { return removeTamedAnimals; }
    public List<String> getEntityWhitelist() { return entityWhitelist; }
    public List<String> getItemWhitelist() { return itemWhitelist; }
    public String getWarningMessage() { return warningMessage; }
    public String getCleanupMessage() { return cleanupMessage; }
    
    // === Setters ===
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setCleanupIntervalSeconds(int cleanupIntervalSeconds) { this.cleanupIntervalSeconds = cleanupIntervalSeconds; }
    public void setWarningTimes(List<Integer> warningTimes) { this.warningTimes = warningTimes; }
    public void setRemoveDroppedItems(boolean removeDroppedItems) { this.removeDroppedItems = removeDroppedItems; }
    public void setRemovePassiveMobs(boolean removePassiveMobs) { this.removePassiveMobs = removePassiveMobs; }
    public void setRemoveHostileMobs(boolean removeHostileMobs) { this.removeHostileMobs = removeHostileMobs; }
    public void setRemoveXpOrbs(boolean removeXpOrbs) { this.removeXpOrbs = removeXpOrbs; }
    public void setRemoveArrows(boolean removeArrows) { this.removeArrows = removeArrows; }
    public void setRemoveNamedEntities(boolean removeNamedEntities) { this.removeNamedEntities = removeNamedEntities; }
    public void setRemoveTamedAnimals(boolean removeTamedAnimals) { this.removeTamedAnimals = removeTamedAnimals; }
    public void setEntityWhitelist(List<String> entityWhitelist) { this.entityWhitelist = entityWhitelist; }
    public void setItemWhitelist(List<String> itemWhitelist) { this.itemWhitelist = itemWhitelist; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
    public void setCleanupMessage(String cleanupMessage) { this.cleanupMessage = cleanupMessage; }
    
    public void applyLocale(String locale) {
        java.util.Map<String, String> messages = LocalePresets.getEntityCleanerMessages(locale);
        this.warningMessage = messages.get("warningMessage");
        this.cleanupMessage = messages.get("cleanupMessage");
        save();
    }
}
