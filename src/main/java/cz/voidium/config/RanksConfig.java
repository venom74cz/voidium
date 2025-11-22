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
    private boolean countAfkTime = false; // If false, we need custom tracking (TODO)
    
    public static class RankDefinition {
        public String type; // "PREFIX" or "SUFFIX"
        public String value; // e.g. "[Veteran] "
        public int hours; // e.g. 100

        public RankDefinition() {}
        public RankDefinition(String type, String value, int hours) {
            this.type = type;
            this.value = value;
            this.hours = hours;
        }
    }

    // List of rank definitions
    private List<RankDefinition> ranks = new ArrayList<>();
    
    // Message to player
    private String promotionMessage = "&aGratulujeme! Za nahrany cas jsi ziskal rank &b%rank%&a!";

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
            try (Reader reader = Files.newBufferedReader(configPath)) {
                instance = GSON.fromJson(reader, RanksConfig.class);
                instance.configPath = configPath;
                if (instance.ranks == null) instance.ranks = new ArrayList<>();
            } catch (IOException e) {
                e.printStackTrace();
                instance = new RanksConfig(configPath);
            }
        } else {
            instance = new RanksConfig(configPath);
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

    public boolean isEnableAutoRanks() { return enableAutoRanks; }
    public int getCheckIntervalMinutes() { return checkIntervalMinutes; }
    public List<RankDefinition> getRanks() { return ranks; }
    public String getPromotionMessage() { return promotionMessage; }
    
    // Apply locale preset
    public void applyLocale(String locale) {
        java.util.Map<String, String> messages = LocalePresets.getRankMessages(locale);
        this.promotionMessage = messages.get("promotionMessage");
        save();
    }
}
