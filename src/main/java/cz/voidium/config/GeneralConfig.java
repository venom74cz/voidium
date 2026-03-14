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
    
    // Enable/disable Skin Restorer (fetch real skins in offline mode)
    private boolean enableSkinRestorer = true;

    // Enable/disable Discord Integration (Bot, Chat Bridge, Whitelist)
    private boolean enableDiscord = true;

    // Enable/disable Web Control Interface
    private boolean enableWeb = true;

    // Enable/disable Statistics Tracking
    private boolean enableStats = true;

    // Enable/disable Auto Ranks (Playtime based)
    private boolean enableRanks = true;

    // Enable/disable Vote Manager (Votifier)
    private boolean enableVote = true;
    
    // Enable/disable Custom Player List (TAB menu)
    private boolean enablePlayerList = true;

    // Maintenance mode – blocks player login and shows a banner on the dashboard
    private boolean maintenanceMode = false;

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
        boolean migrated = false;

        if (Files.exists(configPath)) {
            try {
                GeneralConfig loaded = ConfigFileHelper.loadJson(configPath, GSON, GeneralConfig.class);
                if (loaded != null) {
                    loaded.configPath = configPath;
                    migrated = loaded.normalize();
                    if (migrated) {
                        loaded.save();
                    }
                    return loaded;
                }
            } catch (Exception e) {
                System.err.println("Failed to load general configuration: " + e.getMessage());
            }
        }
        
        config.save();
        return config;
    }

    private boolean normalize() {
        boolean changed = false;
        if (skinCacheHours < 1) {
            skinCacheHours = 24;
            changed = true;
        }
        if (modPrefix == null || modPrefix.isBlank()) {
            modPrefix = "&8[&bVoidium&8]&r ";
            changed = true;
        }
        return changed;
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
    public boolean isEnableSkinRestorer() { return enableSkinRestorer; }
    public boolean isEnableDiscord() { return enableDiscord; }
    public boolean isEnableWeb() { return enableWeb; }
    public boolean isEnableStats() { return enableStats; }
    public boolean isEnableRanks() { return enableRanks; }
    public boolean isEnableVote() { return enableVote; }
    public boolean isEnablePlayerList() { return enablePlayerList; }
    public boolean isMaintenanceMode() { return maintenanceMode; }
    public int getSkinCacheHours() { return skinCacheHours; }
    public String getModPrefix() { return modPrefix; }

    public void setEnableMod(boolean enableMod) { this.enableMod = enableMod; }
    public void setEnableRestarts(boolean enableRestarts) { this.enableRestarts = enableRestarts; }
    public void setEnableAnnouncements(boolean enableAnnouncements) { this.enableAnnouncements = enableAnnouncements; }
    public void setEnableSkinRestorer(boolean enableSkinRestorer) { this.enableSkinRestorer = enableSkinRestorer; }
    public void setEnableDiscord(boolean enableDiscord) { this.enableDiscord = enableDiscord; }
    public void setEnableWeb(boolean enableWeb) { this.enableWeb = enableWeb; }
    public void setEnableStats(boolean enableStats) { this.enableStats = enableStats; }
    public void setEnableRanks(boolean enableRanks) { this.enableRanks = enableRanks; }
    public void setEnableVote(boolean enableVote) { this.enableVote = enableVote; }
    public void setEnablePlayerList(boolean enablePlayerList) { this.enablePlayerList = enablePlayerList; }
    public void setMaintenanceMode(boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }
    public void setSkinCacheHours(int skinCacheHours) { this.skinCacheHours = Math.max(1, skinCacheHours); }
    public void setModPrefix(String modPrefix) { this.modPrefix = modPrefix; }
    
    // Apply locale preset
    public void applyLocale(String locale) {
        java.util.Map<String, String> messages = LocalePresets.getGeneralMessages(locale);
        this.modPrefix = messages.get("modPrefix");
        save();
    }
}