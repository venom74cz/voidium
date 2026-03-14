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
            try {
                instance = ConfigFileHelper.loadJson(configPath, GSON, PlayerListConfig.class);
                if (instance == null) {
                    instance = new PlayerListConfig(configPath);
                }
                instance.configPath = configPath;
                if (instance.normalize()) {
                    instance.save();
                }
            } catch (IOException e) {
                e.printStackTrace();
                instance = new PlayerListConfig(configPath);
            }
        } else {
            instance = new PlayerListConfig(configPath);
            instance.save();
        }
    }

    private boolean normalize() {
        boolean changed = false;
        if (headerLine1 == null) { headerLine1 = "§b§l✦ VOIDIUM SERVER ✦"; changed = true; }
        if (headerLine2 == null) { headerLine2 = "§7Online: §a%online%§7/§a%max%"; changed = true; }
        if (headerLine3 == null) { headerLine3 = ""; changed = true; }
        if (footerLine1 == null) { footerLine1 = "§7TPS: §a%tps%"; changed = true; }
        if (footerLine2 == null) { footerLine2 = "§7Ping: §e%ping%ms"; changed = true; }
        if (footerLine3 == null) { footerLine3 = ""; changed = true; }
        if (playerNameFormat == null || playerNameFormat.isBlank()) { playerNameFormat = "%rank_prefix%%player_name%%rank_suffix%"; changed = true; }
        if (defaultPrefix == null) { defaultPrefix = "§7"; changed = true; }
        if (defaultSuffix == null) { defaultSuffix = ""; changed = true; }
        if (updateIntervalSeconds < 3) { updateIntervalSeconds = 5; changed = true; }
        return changed;
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

    public void setEnableCustomPlayerList(boolean enableCustomPlayerList) { this.enableCustomPlayerList = enableCustomPlayerList; }
    public void setHeaderLine1(String headerLine1) { this.headerLine1 = headerLine1; }
    public void setHeaderLine2(String headerLine2) { this.headerLine2 = headerLine2; }
    public void setHeaderLine3(String headerLine3) { this.headerLine3 = headerLine3; }
    public void setFooterLine1(String footerLine1) { this.footerLine1 = footerLine1; }
    public void setFooterLine2(String footerLine2) { this.footerLine2 = footerLine2; }
    public void setFooterLine3(String footerLine3) { this.footerLine3 = footerLine3; }
    public void setEnableCustomNames(boolean enableCustomNames) { this.enableCustomNames = enableCustomNames; }
    public void setPlayerNameFormat(String playerNameFormat) { this.playerNameFormat = playerNameFormat; }
    public void setCombineMultipleRanks(boolean combineMultipleRanks) { this.combineMultipleRanks = combineMultipleRanks; }
    public void setDefaultPrefix(String defaultPrefix) { this.defaultPrefix = defaultPrefix; }
    public void setDefaultSuffix(String defaultSuffix) { this.defaultSuffix = defaultSuffix; }
    public void setUpdateIntervalSeconds(int updateIntervalSeconds) { this.updateIntervalSeconds = Math.max(3, updateIntervalSeconds); }
    
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
