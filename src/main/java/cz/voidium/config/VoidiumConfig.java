package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import java.io.*;
import java.nio.file.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class VoidiumConfig {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
            .create();
    
    public static class LocalTimeAdapter implements com.google.gson.JsonSerializer<LocalTime>, com.google.gson.JsonDeserializer<LocalTime> {
        @Override
        public com.google.gson.JsonElement serialize(LocalTime src, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }
        
        @Override
        public LocalTime deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) {
            return LocalTime.parse(json.getAsString());
        }
    }
    private static VoidiumConfig instance;
    private transient Path configPath;

    // Restart konfigurace
    private RestartType restartType = RestartType.FIXED_TIME;
    private List<LocalTime> fixedRestartTimes = new ArrayList<>();
    private int intervalHours = 6;

    // Oznámení
    private List<String> announcements = new ArrayList<>();
    private int announcementIntervalMinutes = 30;
    private String prefix = "&8[&bVoidium&8]&r ";

    public enum RestartType {
        FIXED_TIME,
        INTERVAL
    }

    public VoidiumConfig(Path configPath) {
        this.configPath = configPath;
        // Výchozí hodnoty pro restart
        fixedRestartTimes.add(LocalTime.of(6, 0)); // 6:00
        fixedRestartTimes.add(LocalTime.of(18, 0)); // 18:00
        
        // Default announcements
        announcements.add("&bWelcome to the server!");
        announcements.add("&eDon't forget to visit our website!");
    }

    public static VoidiumConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        // configDir is now the voidium directory
        RestartConfig.init(configDir);
        AnnouncementConfig.init(configDir);
        GeneralConfig.init(configDir);
        VoteConfig.init(configDir);
        
        // Zachování kompatibility
        Path oldConfigPath = configDir.resolve("voidium.json");
        instance = new VoidiumConfig(oldConfigPath);
    }

    private static VoidiumConfig load(Path configPath) {
        VoidiumConfig config = new VoidiumConfig(configPath);
        
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                VoidiumConfig loaded = GSON.fromJson(reader, VoidiumConfig.class);
                if (loaded != null) {
                    loaded.configPath = configPath;
                    return loaded;
                }
            } catch (Exception e) {
                System.err.println("Failed to load configuration: " + e.getMessage());
            }
        }
        
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save configuration: " + e.getMessage());
        }
    }

    // Gettery a settery
    public RestartType getRestartType() { return restartType; }
    public List<LocalTime> getFixedRestartTimes() { return fixedRestartTimes; }
    public int getIntervalHours() { return intervalHours; }
    public List<String> getAnnouncements() { return announcements; }
    public int getAnnouncementIntervalMinutes() { return announcementIntervalMinutes; }
    public String getPrefix() { return prefix; }

    public static Component formatMessage(String message) {
        return Component.literal(
            message.replace("&", "§")
        );
    }
}
