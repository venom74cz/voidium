package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AnnouncementConfig instance;
    private transient Path configPath;

    // === ANNOUNCEMENT CONFIGURATION ===
    // List of messages to broadcast to players
    // Use & for color codes (e.g., &b = aqua, &e = yellow, &c = red)
    // Available colors: &0-&9, &a-&f, &l (bold), &o (italic), &n (underline), &r (reset)
    private List<String> announcements = new ArrayList<>();
    
    // How often to send announcements (in minutes)
    // Set to 0 to disable automatic announcements
    private int announcementIntervalMinutes = 30;
    
    // Prefix added to all announcement messages
    // Use & for color codes
    private String prefix = "&8[&bVoidium&8]&r ";

    public AnnouncementConfig(Path configPath) {
        this.configPath = configPath;
        announcements.add("&bWelcome to the server!");
        announcements.add("&eDon't forget to visit our website!");
    }

    public static AnnouncementConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        Path configPath = configDir.resolve("announcements.json");
        instance = load(configPath);
    }

    private static AnnouncementConfig load(Path configPath) {
        AnnouncementConfig config = new AnnouncementConfig(configPath);
        boolean migrated = false;

        if (Files.exists(configPath)) {
            try {
                AnnouncementConfig loaded = ConfigFileHelper.loadJson(configPath, GSON, AnnouncementConfig.class);
                if (loaded != null) {
                    loaded.configPath = configPath;
                    migrated = loaded.normalize();
                    if (migrated) {
                        loaded.save();
                    }
                    return loaded;
                }
            } catch (Exception e) {
                System.err.println("Failed to load announcement configuration: " + e.getMessage());
            }
        }
        
        config.save();
        return config;
    }

    private boolean normalize() {
        boolean changed = false;
        if (announcements == null) {
            announcements = new ArrayList<>();
            changed = true;
        }
        if (announcementIntervalMinutes < 0) {
            announcementIntervalMinutes = 30;
            changed = true;
        }
        if (prefix == null) {
            prefix = "&8[&bVoidium&8]&r ";
            changed = true;
        }
        return changed;
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                writer.write("// === ANNOUNCEMENT CONFIGURATION ===\n");
                writer.write("// List of messages to broadcast to players\n");
                writer.write("// Use & for color codes (e.g., &b = aqua, &e = yellow, &c = red)\n");
                writer.write("// Available colors: &0-&9, &a-&f, &l (bold), &o (italic), &n (underline), &r (reset)\n");
                writer.write("// How often to send announcements (in minutes)\n");
                writer.write("// Set to 0 to disable automatic announcements\n");
                writer.write("// Prefix added to all announcement messages\n");
                writer.write("// Use & for color codes\n\n");
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save announcement configuration: " + e.getMessage());
        }
    }

    // Gettery
    public List<String> getAnnouncements() { return announcements; }
    public int getAnnouncementIntervalMinutes() { return announcementIntervalMinutes; }
    public String getPrefix() { return prefix; }

    public void setAnnouncements(List<String> announcements) {
        this.announcements = announcements == null ? new ArrayList<>() : new ArrayList<>(announcements);
    }

    public void setAnnouncementIntervalMinutes(int announcementIntervalMinutes) {
        this.announcementIntervalMinutes = Math.max(0, announcementIntervalMinutes);
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    // Apply locale preset
    public void applyLocale(String locale) {
        java.util.Map<String, Object> messages = LocalePresets.getAnnouncementMessages(locale);
        this.prefix = (String) messages.get("prefix");
        Object announcementsObj = messages.get("announcements");
        if (announcementsObj instanceof String[]) {
            this.announcements = new ArrayList<>(java.util.Arrays.asList((String[]) announcementsObj));
        }
        save();
    }
}