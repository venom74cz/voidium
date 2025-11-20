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
    
    // Rank definitions: Rank Name -> Hours required
    private Map<String, Integer> playtimeRanks = new LinkedHashMap<>();
    
    // Commands to execute when player reaches a rank
    // %player% = player name, %rank% = rank name
    private String promotionCommand = "ftbranks add %player% %rank%";
    
    // Message to player
    private String promotionMessage = "&aGratulujeme! Za nahrany cas jsi ziskal rank &b%rank%&a!";

    public RanksConfig(Path configPath) {
        this.configPath = configPath;
        // Default examples
        playtimeRanks.put("Member", 10);
        playtimeRanks.put("Regular", 50);
        playtimeRanks.put("Veteran", 100);
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
    public Map<String, Integer> getPlaytimeRanks() { return playtimeRanks; }
    public String getPromotionCommand() { return promotionCommand; }
    public String getPromotionMessage() { return promotionMessage; }
}
