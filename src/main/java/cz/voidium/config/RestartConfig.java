package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class RestartConfig {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalTime.class, new VoidiumConfig.LocalTimeAdapter())
            .create();
    private static RestartConfig instance;
    private transient Path configPath;

    // === RESTART CONFIGURATION ===
    // Choose restart type: FIXED_TIME (specific times), INTERVAL (every X hours), or DELAY (restart in X minutes)
    private RestartType restartType = RestartType.FIXED_TIME;
    
    // Available restart types:
    // FIXED_TIME - Restart at specific times of day
    // INTERVAL - Restart every X hours
    // DELAY - Restart in X minutes from server start
    public enum RestartType {
        FIXED_TIME,
        INTERVAL,
        DELAY
    }
    
    // Fixed restart times (only used when restartType = FIXED_TIME)
    // Format: "HH:MM" (24-hour format). Example: "06:00" for 6 AM
    private List<LocalTime> fixedRestartTimes = new ArrayList<>();
    
    // Restart interval in hours (only used when restartType = INTERVAL)
    // Server will restart every X hours after the last restart
    private int intervalHours = 6;
    
    // Restart delay in minutes (only used when restartType = DELAY)
    // Server will restart X minutes after startup
    private int delayMinutes = 60;

    public RestartConfig(Path configPath) {
        this.configPath = configPath;
        fixedRestartTimes.add(LocalTime.of(6, 0));
        fixedRestartTimes.add(LocalTime.of(18, 0));
    }

    public static RestartConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        try {
            Path configPath = configDir.resolve("restart.json");
            System.out.println("Loading restart config from: " + configPath);
            instance = load(configPath);
            System.out.println("Restart config loaded successfully. Type: " + instance.getRestartType() + ", Times: " + instance.getFixedRestartTimes().size());
        } catch (Exception e) {
            System.err.println("Failed to initialize restart config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static RestartConfig load(Path configPath) {
        RestartConfig config = new RestartConfig(configPath);
        
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                RestartConfig loaded = GSON.fromJson(reader, RestartConfig.class);
                if (loaded != null) {
                    loaded.configPath = configPath;
                    return loaded;
                }
            } catch (Exception e) {
                System.err.println("Failed to load restart configuration: " + e.getMessage());
            }
        }
        
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                writer.write("// === RESTART CONFIGURATION ===\n");
                writer.write("// Choose restart type: FIXED_TIME (specific times), INTERVAL (every X hours), or DELAY (restart in X minutes)\n");
                writer.write("// Available restart types:\n");
                writer.write("// FIXED_TIME - Restart at specific times of day\n");
                writer.write("// INTERVAL - Restart every X hours\n");
                writer.write("// DELAY - Restart in X minutes from server start\n");
                writer.write("// Fixed restart times (only used when restartType = FIXED_TIME)\n");
                writer.write("// Format: \"HH:MM\" (24-hour format). Example: \"06:00\" for 6 AM\n");
                writer.write("// Restart interval in hours (only used when restartType = INTERVAL)\n");
                writer.write("// Server will restart every X hours after the last restart\n");
                writer.write("// Restart delay in minutes (only used when restartType = DELAY)\n");
                writer.write("// Server will restart X minutes after startup\n\n");
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save restart configuration: " + e.getMessage());
        }
    }

    // Gettery
    public RestartType getRestartType() { return restartType; }
    public List<LocalTime> getFixedRestartTimes() { return fixedRestartTimes; }
    public int getIntervalHours() { return intervalHours; }
    public int getDelayMinutes() { return delayMinutes; }
    
    // Settery (potřebné pro WebManager)
    public void setRestartType(RestartType restartType) { this.restartType = restartType; }
    public void setFixedRestartTimes(List<LocalTime> fixedRestartTimes) { this.fixedRestartTimes = fixedRestartTimes; }
    public void setIntervalHours(int intervalHours) { this.intervalHours = intervalHours; }
    public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }
}