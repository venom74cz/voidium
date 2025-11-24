package cz.voidium.ranks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player progress for custom rank conditions (mob kills, biome visits, etc.)
 */
public class ProgressTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Progress");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ProgressTracker instance;
    
    private final Path dataPath;
    
    // UUID -> condition type -> target -> count
    private Map<String, Map<String, Map<String, Integer>>> playerProgress = new ConcurrentHashMap<>();
    
    public static class ProgressData {
        public Map<String, Map<String, Integer>> progress = new HashMap<>();
        
        public ProgressData() {}
    }
    
    private ProgressTracker() {
        this.dataPath = FMLPaths.CONFIGDIR.get().resolve("voidium").resolve("player_progress.json");
        load();
    }
    
    public static synchronized ProgressTracker getInstance() {
        if (instance == null) {
            instance = new ProgressTracker();
        }
        return instance;
    }
    
    /**
     * Increment progress for a player
     * @param uuid Player UUID
     * @param type Condition type (KILL_MOBS, VISIT_BIOMES, etc.)
     * @param target Target identifier (e.g., "minecraft:zombie", "minecraft:plains")
     * @param amount Amount to increment (default 1)
     */
    public void incrementProgress(String uuid, String type, String target, int amount) {
        playerProgress
            .computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(type, k -> new ConcurrentHashMap<>())
            .merge(target, amount, Integer::sum);
    }
    
    /**
     * Get progress for a player
     * @param uuid Player UUID
     * @param type Condition type
     * @param target Target identifier
     * @return Current progress count
     */
    public int getProgress(String uuid, String type, String target) {
        return playerProgress
            .getOrDefault(uuid, Collections.emptyMap())
            .getOrDefault(type, Collections.emptyMap())
            .getOrDefault(target, 0);
    }
    
    /**
     * Check if player meets a specific condition
     */
    public boolean meetsCondition(String uuid, String type, String target, int required) {
        return getProgress(uuid, type, target) >= required;
    }
    
    /**
     * Get all progress for a player
     */
    public Map<String, Map<String, Integer>> getPlayerProgress(String uuid) {
        return playerProgress.getOrDefault(uuid, Collections.emptyMap());
    }
    
    /**
     * Save progress to disk
     */
    public void save() {
        try {
            Files.createDirectories(dataPath.getParent());
            try (Writer writer = Files.newBufferedWriter(dataPath)) {
                GSON.toJson(playerProgress, writer);
            }
            LOGGER.info("Player progress saved.");
        } catch (IOException e) {
            LOGGER.error("Failed to save player progress", e);
        }
    }
    
    /**
     * Load progress from disk
     */
    private void load() {
        if (!Files.exists(dataPath)) {
            LOGGER.info("No player progress data found, starting fresh.");
            return;
        }
        
        try (Reader reader = Files.newBufferedReader(dataPath)) {
            Map<String, Map<String, Map<String, Integer>>> loaded = GSON.fromJson(
                reader, 
                new TypeToken<Map<String, Map<String, Map<String, Integer>>>>(){}.getType()
            );
            if (loaded != null) {
                playerProgress = new ConcurrentHashMap<>(loaded);
                LOGGER.info("Player progress loaded.");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load player progress", e);
        }
    }
    
    /**
     * Reset all progress for a player
     */
    public void resetPlayer(String uuid) {
        playerProgress.remove(uuid);
        save();
    }
}
