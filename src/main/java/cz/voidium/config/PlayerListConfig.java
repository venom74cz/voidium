package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlayerListConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PlayerListConfig instance;
    private transient Path configPath;

    // Configuration
    private boolean enableCustomPlayerList = true;
    private String headerLine1 = "§b§l✦ VOIDIUM SERVER ✦";
    private String headerLine2 = "§7Online: §a%online%§7/§a%max%";
    private String headerLine3 = "";
    private String footerLine1 = "§7TPS: §a%tps%";
    private String footerLine2 = "§7Ping: §e%ping%ms";
    private String footerLine3 = "";
    
    // Player name format / Formát jména hráče
    private boolean enableCustomNames = true;
    private String playerNameFormat = "%rank_prefix%%player_name%%rank_suffix%";
    private String defaultPrefix = "§7";
    private String defaultSuffix = "";
    
    // Multi-source ranks - combines Discord roles + time-based ranks
    // Více zdrojů ranků - kombinuje Discord role + časové ranky
    // When true, all applicable prefixes/suffixes are combined (e.g., "[OWNER] [VIP] PlayerName ★★★ ✦")
    // Když true, všechny použitelné prefixy/suffixy jsou zkombinované (např. "[OWNER] [VIP] PlayerName ★★★ ✦")
    // When false, only highest priority prefix/suffix is used
    // Když false, použije se pouze prefix/suffix s nejvyšší prioritou
    private boolean combineMultipleRanks = true;
    
    // Update interval (minimum 3 seconds to avoid performance issues)
    // Interval aktualizace (minimum 3 sekundy kvůli výkonu)
    private int updateIntervalSeconds = 5;

    private PlayerListConfig(Path configPath) {
        this.configPath = configPath;
    }

    public static PlayerListConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        Path configPath = configDir.resolve("playerlist.json");
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                instance = GSON.fromJson(reader, PlayerListConfig.class);
                instance.configPath = configPath;
            } catch (IOException e) {
                e.printStackTrace();
                instance = new PlayerListConfig(configPath);
            }
        } else {
            instance = new PlayerListConfig(configPath);
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

    // Getters
    public boolean isEnableCustomPlayerList() { return enableCustomPlayerList; }
    public String getHeaderLine1() { return headerLine1; }
    public String getHeaderLine2() { return headerLine2; }
    public String getHeaderLine3() { return headerLine3; }
    public String getFooterLine1() { return footerLine1; }
    public String getFooterLine2() { return footerLine2; }
    public String getFooterLine3() { return footerLine3; }
    public boolean isEnableCustomNames() { return enableCustomNames; }
    public String getPlayerNameFormat() { return playerNameFormat; }
    public boolean isCombineMultipleRanks() { return combineMultipleRanks; }
    public String getDefaultPrefix() { return defaultPrefix; }
    public String getDefaultSuffix() { return defaultSuffix; }
    public int getUpdateIntervalSeconds() { return updateIntervalSeconds; }
    
    // Apply locale preset
    public void applyLocale(String locale) {
        java.util.Map<String, String> messages = LocalePresets.getPlayerListMessages(locale);
        this.headerLine1 = messages.get("headerLine1");
        this.headerLine2 = messages.get("headerLine2");
        this.headerLine3 = messages.get("headerLine3");
        this.footerLine1 = messages.get("footerLine1");
        this.footerLine2 = messages.get("footerLine2");
        this.footerLine3 = messages.get("footerLine3");
        save();
    }
}
