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
    
    // Bot Activity
    private String botActivityType = "PLAYING"; // PLAYING, WATCHING, LISTENING, COMPETING
    private String botActivityText = "Minecraft";
    
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
    private java.util.Map<String, RoleStyle> rolePrefixes = new java.util.HashMap<>(); // Discord Role ID -> Prefix/Suffix/Color
    
    public static class RoleStyle {
        public String prefix = "";
        public String suffix = "";
        public String color = "";
        public int priority = 0;
        
        public RoleStyle() {}
        public RoleStyle(String prefix, String suffix, String color, int priority) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.color = color;
            this.priority = priority;
        }
    }
    
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

    // Bot Messages (configurable responses)
    private String invalidCodeMessage = "Invalid or expired code.";
    private String notLinkedMessage = "You are not linked! Enter a valid code from the game.";
    private String alreadyLinkedSingleMessage = ", you are already linked! UUID: `%uuid%`";
    private String alreadyLinkedMultipleMessage = ", you are already linked to %count% accounts!";
    private String unlinkSuccessMessage = "All linked accounts have been successfully unlinked.";
    private String wrongGuildMessage = "This command can only be used on the official Discord server.";
    private String ticketCreatedMessage = "Ticket created!";
    private String ticketClosingMessage = "Closing ticket...";
    private String textChannelOnlyMessage = "This command can only be used in a text channel.";

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
    public String getBotActivityType() { return botActivityType; }
    public String getBotActivityText() { return botActivityText; }
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
    public java.util.Map<String, RoleStyle> getRolePrefixes() { return rolePrefixes; }
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
    
    // Bot Messages getters (null-safe)
    public String getInvalidCodeMessage() { return invalidCodeMessage != null ? invalidCodeMessage : "Invalid or expired code."; }
    public String getNotLinkedMessage() { return notLinkedMessage != null ? notLinkedMessage : "You are not linked to any account."; }
    public String getAlreadyLinkedSingleMessage() { return alreadyLinkedSingleMessage != null ? alreadyLinkedSingleMessage : " you are already linked!"; }
    public String getAlreadyLinkedMultipleMessage() { return alreadyLinkedMultipleMessage != null ? alreadyLinkedMultipleMessage : " you are already linked to multiple accounts."; }
    public String getUnlinkSuccessMessage() { return unlinkSuccessMessage != null ? unlinkSuccessMessage : "Successfully unlinked all accounts."; }
    public String getWrongGuildMessage() { return wrongGuildMessage != null ? wrongGuildMessage : "This command can only be used in the configured server."; }
    public String getTicketCreatedMessage() { return ticketCreatedMessage != null ? ticketCreatedMessage : "Ticket created!"; }
    public String getTicketClosingMessage() { return ticketClosingMessage != null ? ticketClosingMessage : "Closing ticket..."; }
    public String getTextChannelOnlyMessage() { return textChannelOnlyMessage != null ? textChannelOnlyMessage : "This command can only be used in a text channel."; }
    
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
        
        // Bot messages
        this.invalidCodeMessage = messages.get("invalidCodeMessage");
        this.notLinkedMessage = messages.get("notLinkedMessage");
        this.alreadyLinkedSingleMessage = messages.get("alreadyLinkedSingleMessage");
        this.alreadyLinkedMultipleMessage = messages.get("alreadyLinkedMultipleMessage");
        this.unlinkSuccessMessage = messages.get("unlinkSuccessMessage");
        this.wrongGuildMessage = messages.get("wrongGuildMessage");
        this.ticketCreatedMessage = messages.get("ticketCreatedMessage");
        this.ticketClosingMessage = messages.get("ticketClosingMessage");
        this.textChannelOnlyMessage = messages.get("textChannelOnlyMessage");
        
        save();
    }
}
