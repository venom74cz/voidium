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
    
    // Customizable messages for stats report
    private String reportTitle = "📊 Daily Statistics - %date%";
    private String reportPeakLabel = "Peak Players";
    private String reportAverageLabel = "Average Players";
    private String reportFooter = "Voidium Stats";

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
    
    // Message getters
    public String getReportTitle() { return reportTitle; }
    public String getReportPeakLabel() { return reportPeakLabel; }
    public String getReportAverageLabel() { return reportAverageLabel; }
    public String getReportFooter() { return reportFooter; }
    
    // Apply locale preset
    public void applyLocale(String locale) {
        java.util.Map<String, String> messages = LocalePresets.getStatsMessages(locale);
        this.reportTitle = messages.get("reportTitle");
        this.reportPeakLabel = messages.get("reportPeakLabel");
        this.reportAverageLabel = messages.get("reportAverageLabel");
        this.reportFooter = messages.get("reportFooter");
        save();
    }
}
