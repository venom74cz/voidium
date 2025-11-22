package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;

public class DiscordConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DiscordConfig instance;
    private transient Path configPath;

    // === DISCORD CONFIGURATION ===
    private boolean enableDiscord = false;
    private String botToken = "YOUR_BOT_TOKEN_HERE";
    private String guildId = "YOUR_GUILD_ID_HERE";
    
    // Whitelist settings
    private boolean enableWhitelist = true;
    private String kickMessage = "&cYou are not whitelisted!\\n&7To join, you must verify on our Discord.\\n&7Your verification code: &b%code%";
    private String linkSuccessMessage = "Successfully linked account **%player%**!";
    private String alreadyLinkedMessage = "This Discord account is already linked to the maximum number of accounts (%max%).";
    private int maxAccountsPerDiscord = 1;
    
    // Channel IDs
    private String chatChannelId = "";
    private String consoleChannelId = "";
    private String linkChannelId = "";
    private String statusChannelId = ""; // If empty, uses consoleChannelId

    // Console & Status settings
    private boolean enableConsoleLog = false;
    private boolean enableStatusMessages = true;
    private String statusMessageStarting = ":yellow_circle: **Server is starting...**";
    private String statusMessageStarted = ":green_circle: **Server is online!**";
    private String statusMessageStopping = ":orange_circle: **Server is stopping...**";
    private String statusMessageStopped = ":red_circle: **Server is offline.**";

    // Role settings
    private String linkedRoleId = "";
    
    // Ban Sync settings
    private boolean syncBansDiscordToMc = true;
    private boolean syncBansMcToDiscord = false;

    // Chat Bridge settings
    private boolean enableChatBridge = true;
    private String minecraftToDiscordFormat = "**%player%** » %message%";
    private String discordToMinecraftFormat = "&9[Discord] &f%user% &8» &7%message%";
    private boolean translateEmojis = true;

    // Webhook URL
    private String chatWebhookUrl = "";

    // Channel Topic settings
    private boolean enableTopicUpdate = true;
    private String channelTopicFormat = "Online: %online%/%max% | Uptime: %uptime% | Voidium Server";
    private String uptimeFormat = "%days%d %hours%h %minutes%m";

    public DiscordConfig(Path configPath) {
        this.configPath = configPath;
    }

    public static DiscordConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        Path configPath = configDir.resolve("discord.json");
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                instance = GSON.fromJson(reader, DiscordConfig.class);
                instance.configPath = configPath;
            } catch (IOException e) {
                e.printStackTrace();
                instance = new DiscordConfig(configPath);
            }
        } else {
            instance = new DiscordConfig(configPath);
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
    public boolean isEnableDiscord() { return enableDiscord; }
    public String getBotToken() { return botToken; }
    public String getGuildId() { return guildId; }
    public boolean isEnableWhitelist() { return enableWhitelist; }
    public String getKickMessage() { return kickMessage; }
    public String getLinkSuccessMessage() { return linkSuccessMessage; }
    public String getAlreadyLinkedMessage() { return alreadyLinkedMessage; }
    public int getMaxAccountsPerDiscord() { return maxAccountsPerDiscord; }
    public String getChatChannelId() { return chatChannelId; }
    public String getConsoleChannelId() { return consoleChannelId; }
    public String getLinkChannelId() { return linkChannelId; }
    public String getStatusChannelId() { return statusChannelId != null && !statusChannelId.isEmpty() ? statusChannelId : chatChannelId; }
    public String getLinkedRoleId() { return linkedRoleId; }
    public boolean isSyncBansDiscordToMc() { return syncBansDiscordToMc; }
    public boolean isSyncBansMcToDiscord() { return syncBansMcToDiscord; }
    public boolean isEnableChatBridge() { return enableChatBridge; }
    public String getMinecraftToDiscordFormat() { return minecraftToDiscordFormat; }
    public String getDiscordToMinecraftFormat() { return discordToMinecraftFormat; }
    public boolean isTranslateEmojis() { return translateEmojis; }
    public String getChatWebhookUrl() { return chatWebhookUrl; }
    
    public boolean isEnableTopicUpdate() { return enableTopicUpdate; }
    public String getChannelTopicFormat() { return channelTopicFormat; }
    public String getUptimeFormat() { return uptimeFormat; }
    
    public boolean isEnableConsoleLog() { return enableConsoleLog; }
    public boolean isEnableStatusMessages() { return enableStatusMessages; }
    public String getStatusMessageStarting() { return statusMessageStarting; }
    public String getStatusMessageStarted() { return statusMessageStarted; }
    public String getStatusMessageStopping() { return statusMessageStopping; }
    public String getStatusMessageStopped() { return statusMessageStopped; }
    
    // Apply locale preset
    public void applyLocale(String locale) {
        java.util.Map<String, String> messages = LocalePresets.getDiscordMessages(locale);
        this.kickMessage = messages.get("kickMessage");
        this.linkSuccessMessage = messages.get("linkSuccessMessage");
        this.alreadyLinkedMessage = messages.get("alreadyLinkedMessage");
        this.minecraftToDiscordFormat = messages.get("minecraftToDiscordFormat");
        this.discordToMinecraftFormat = messages.get("discordToMinecraftFormat");
        
        this.statusMessageStarting = messages.get("statusMessageStarting");
        this.statusMessageStarted = messages.get("statusMessageStarted");
        this.statusMessageStopping = messages.get("statusMessageStopping");
        this.statusMessageStopped = messages.get("statusMessageStopped");
        this.channelTopicFormat = messages.get("channelTopicFormat");
        this.uptimeFormat = messages.get("uptimeFormat");
        
        save();
    }
}
