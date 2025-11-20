package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;
import java.time.LocalTime;

public class StatsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static StatsConfig instance;
    private transient Path configPath;

    private boolean enableStats = true;
    private String reportChannelId = "";
    private String reportTime = "09:00"; // HH:mm

    public StatsConfig(Path configPath) {
        this.configPath = configPath;
    }

    public static StatsConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        Path configPath = configDir.resolve("stats.json");
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                instance = GSON.fromJson(reader, StatsConfig.class);
                instance.configPath = configPath;
            } catch (IOException e) {
                e.printStackTrace();
                instance = new StatsConfig(configPath);
            }
        } else {
            instance = new StatsConfig(configPath);
            instance.save();
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isEnableStats() { return enableStats; }
    public String getReportChannelId() { return reportChannelId; }
    public LocalTime getReportTime() { 
        try {
            return LocalTime.parse(reportTime);
        } catch (Exception e) {
            return LocalTime.of(9, 0);
        }
    }
}
