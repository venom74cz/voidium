package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RanksConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static RanksConfig instance;
    private transient Path configPath;

    private boolean enableAutoRanks = true;
    private int checkIntervalMinutes = 5;
    @SuppressWarnings("unused")
    private boolean countAfkTime = false; // Reserved for future AFK time tracking feature

    public static class CustomCondition {
        public String type; // "KILL", "VISIT", "BREAK", "PLACE"
        public int count;

        public CustomCondition() {
        }

        public CustomCondition(String type, int count) {
            this.type = type;
            this.count = count;
        }
    }

    public static class RankDefinition {
        public String type; // "PREFIX" or "SUFFIX"
        public String value; // e.g. "[Veteran] "
        public int hours; // e.g. 100
        public List<CustomCondition> customConditions; // Optional custom conditions

        public RankDefinition() {
            this.customConditions = new ArrayList<>();
        }

        public RankDefinition(String type, String value, int hours) {
            this.type = type;
            this.value = value;
            this.hours = hours;
            this.customConditions = new ArrayList<>();
        }
    }

    // List of rank definitions
    private List<RankDefinition> ranks = new ArrayList<>();

    // Message to player
    private String promotionMessage = "&aCongratulations! You have earned the rank &b%rank%&a!";

    // Tooltip texts (shown on hover over rank prefix/suffix)
    private String tooltipPlayed = "§7Played: §f%hours%h";
    private String tooltipRequired = "§7Required: §f%hours%h";

    public RanksConfig(Path configPath) {
        this.configPath = configPath;
        // Default examples
        ranks.add(new RankDefinition("PREFIX", "&7[Member] ", 10));
        ranks.add(new RankDefinition("PREFIX", "&6[Veteran] ", 100));
        ranks.add(new RankDefinition("SUFFIX", " &e★", 500));
    }

    public static RanksConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        Path configPath = configDir.resolve("ranks.json");
        if (Files.exists(configPath)) {
            try {
                instance = ConfigFileHelper.loadJson(configPath, GSON, RanksConfig.class);
                if (instance == null) {
                    instance = new RanksConfig(configPath);
                }
                instance.configPath = configPath;
                boolean migrated = instance.normalize();
                if (migrated) {
                    instance.save();
                }
            } catch (IOException e) {
                e.printStackTrace();
                instance = new RanksConfig(configPath);
            }
        } else {
            instance = new RanksConfig(configPath);
            instance.save();
        }
    }

    private boolean normalize() {
        boolean changed = false;
        if (checkIntervalMinutes < 1) {
            checkIntervalMinutes = 5;
            changed = true;
        }
        if (ranks == null) {
            ranks = new ArrayList<>();
            changed = true;
        }
        for (RankDefinition rank : ranks) {
            if (rank.customConditions == null) {
                rank.customConditions = new ArrayList<>();
                changed = true;
            }
            if (rank.type == null || rank.type.isBlank()) {
                rank.type = "PREFIX";
                changed = true;
            }
            if (rank.value == null) {
                rank.value = "";
                changed = true;
            }
            if (rank.hours < 0) {
                rank.hours = 0;
                changed = true;
            }
        }
        if (promotionMessage == null || promotionMessage.isBlank()) {
            promotionMessage = "&aCongratulations! You have earned the rank &b%rank%&a!";
            changed = true;
        }
        if (tooltipPlayed == null || tooltipPlayed.isBlank()) {
            tooltipPlayed = "§7Played: §f%hours%h";
            changed = true;
        }
        if (tooltipRequired == null || tooltipRequired.isBlank()) {
            tooltipRequired = "§7Required: §f%hours%h";
            changed = true;
        }
        return changed;
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isEnableAutoRanks() {
        return enableAutoRanks;
    }

    public int getCheckIntervalMinutes() {
        return checkIntervalMinutes;
    }

    public List<RankDefinition> getRanks() {
        return ranks;
    }

    public String getPromotionMessage() {
        return promotionMessage;
    }

    public String getTooltipPlayed() {
        return tooltipPlayed != null ? tooltipPlayed : "§7Played: §f%hours%h";
    }

    public String getTooltipRequired() {
        return tooltipRequired != null ? tooltipRequired : "§7Required: §f%hours%h";
    }

    public void setEnableAutoRanks(boolean enableAutoRanks) {
        this.enableAutoRanks = enableAutoRanks;
    }

    public void setCheckIntervalMinutes(int checkIntervalMinutes) {
        this.checkIntervalMinutes = Math.max(1, checkIntervalMinutes);
    }

    public void setRanks(List<RankDefinition> ranks) {
        this.ranks = ranks == null ? new ArrayList<>() : new ArrayList<>(ranks);
    }

    public void setPromotionMessage(String promotionMessage) {
        this.promotionMessage = promotionMessage;
    }

    public void setTooltipPlayed(String tooltipPlayed) {
        this.tooltipPlayed = tooltipPlayed;
    }

    public void setTooltipRequired(String tooltipRequired) {
        this.tooltipRequired = tooltipRequired;
    }

    // Apply locale preset
    public void applyLocale(String locale) {
        java.util.Map<String, String> messages = LocalePresets.getRankMessages(locale);
        this.promotionMessage = messages.get("promotionMessage");
        save();
    }
}
