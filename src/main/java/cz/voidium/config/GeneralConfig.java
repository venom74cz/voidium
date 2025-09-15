package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;

public class GeneralConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GeneralConfig instance;
    private transient Path configPath;

    // === GENERAL CONFIGURATION ===
    // Master switch - set to false to completely disable the mod
    private boolean enableMod = true;
    
    // Enable/disable automatic server restarts
    // If false, only manual restarts via commands will work
    private boolean enableRestarts = true;
    
    // Enable/disable automatic announcements
    // If false, only manual announcements via commands will work
    private boolean enableAnnouncements = true;
    
    // Enable/disable boss bar countdown during restarts (10+ minutes)
    // Shows a red progress bar at the top of players' screens
    private boolean enableBossBar = true;
    
    // Enable/disable Skin Restorer (fetch real skins in offline mode)
    private boolean enableSkinRestorer = true;

    // Number of hours to keep a cached skin entry (value+signature) before refetch.
    // Minimum enforced internally is 1. Set higher (e.g. 48) to reduce Mojang API calls,
    // or lower (e.g. 6) if players frequently change skins. Default 24.
    private int skinCacheHours = 24;
    
    // Default prefix for mod messages (used in commands and notifications)
    // Use & for color codes
    private String modPrefix = "&8[&bVoidium&8]&r ";

    public GeneralConfig(Path configPath) {
        this.configPath = configPath;
    }

    public static GeneralConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        Path configPath = configDir.resolve("general.json");
        instance = load(configPath);
    }

    private static GeneralConfig load(Path configPath) {
        GeneralConfig config = new GeneralConfig(configPath);
        
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                GeneralConfig loaded = GSON.fromJson(reader, GeneralConfig.class);
                if (loaded != null) {
                    loaded.configPath = configPath;
                    return loaded;
                }
            } catch (Exception e) {
                System.err.println("Failed to load general configuration: " + e.getMessage());
            }
        }
        
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                writer.write("// === GENERAL CONFIGURATION ===\n");
                writer.write("// Master switch - set to false to completely disable the mod\n");
                writer.write("// Enable/disable automatic server restarts\n");
                writer.write("// If false, only manual restarts via commands will work\n");
                writer.write("// Enable/disable automatic announcements\n");
                writer.write("// If false, only manual announcements via commands will work\n");
                writer.write("// Enable/disable boss bar countdown during restarts (10+ minutes)\n");
                writer.write("// Shows a red progress bar at the top of players' screens\n");
                writer.write("// Enable/disable Skin Restorer (real skins in offline mode)\n");
                writer.write("// skinCacheHours - retention time for cached skins (hours) – min 1\n");
                writer.write("// Default prefix for mod messages (used in commands and notifications)\n");
                writer.write("// Use & for color codes\n\n");
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save general configuration: " + e.getMessage());
        }
    }

    // Gettery
    public boolean isEnableMod() { return enableMod; }
    public boolean isEnableRestarts() { return enableRestarts; }
    public boolean isEnableAnnouncements() { return enableAnnouncements; }
    public boolean isEnableBossBar() { return enableBossBar; }
    public String getModPrefix() { return modPrefix; }
    public boolean isEnableSkinRestorer() { return enableSkinRestorer; }
    public int getSkinCacheHours() { return skinCacheHours; }
}