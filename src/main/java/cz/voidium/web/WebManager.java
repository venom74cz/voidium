package cz.voidium.web;

// ============================================================================
//                          VOIDIUM WEB MANAGER
// ============================================================================
// Web Control Panel pro vzdálenou správu Minecraft serveru
// Autor: Voidium Team
// ============================================================================

// =========================== IMPORTS ========================================

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import cz.voidium.config.*;
import cz.voidium.discord.DiscordManager;
import cz.voidium.stats.StatsManager;
import net.dv8tion.jda.api.entities.Role;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// ============================================================================
//                          HLAVNÍ TŘÍDA
// ============================================================================

public class WebManager {
    
    // ========================= KONSTANTY ====================================
    
    /** Logger pro výpisy do konzole */
    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Web");
    
    /** Gson instance pro JSON serializaci s podporou LocalTime */
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalTime.class, new VoidiumConfig.LocalTimeAdapter())
            .setLongSerializationPolicy(com.google.gson.LongSerializationPolicy.STRING)
            .create();
    
    // ========================= INSTANCE PROMĚNNÉ ============================
    
    /** Singleton instance */
    private static WebManager instance;
    
    /** HTTP server instance */
    private HttpServer server;
    
    /** Reference na Minecraft server */
    private MinecraftServer mcServer;
    
    /** Autentizační token (generován při startu) */
    private String authToken;
    
    // ========================================================================
    //                          PŘEKLADY (LANG)
    // ========================================================================
    // Mapa obsahuje všechny texty UI v EN a CZ
    // Klíče: 
    //   - prosté texty: "title", "dashboard", "save"
    //   - názvy polí: "section.fieldName" (např. "discord.botToken")
    //   - popisy polí: "desc.section.fieldName" (např. "desc.discord.botToken")
    // ========================================================================
    
    private static final Map<String, Map<String, String>> LANG = new HashMap<>();
    
    static {
        // ==================== ENGLISH (DEFAULT) =============================
        Map<String, String> en = new HashMap<>();
        
        // --- Základní UI texty ---
        en.put("title", "Voidium Control Panel");
        en.put("unauthorized", "Unauthorized. Please use the link from server console.");
        en.put("dashboard", "Dashboard");
        en.put("config", "Configuration");
        en.put("status", "Server Status");
        en.put("online", "Online");
        en.put("offline", "Offline");
        
        // --- Statistiky serveru ---
        en.put("players", "Players");
        en.put("active_players", "Active Players");
        en.put("no_players", "No players online");
        en.put("player_list", "Online Players");
        en.put("memory", "Memory");
        en.put("max_memory", "Max Memory");
        en.put("used_memory", "Used Memory");
        en.put("free_memory", "Free Memory");
        en.put("tps", "TPS");
        en.put("uptime", "Uptime");
        en.put("cpu", "CPU Usage");
        en.put("server_info", "Server Information");
        
        // --- Akce ---
        en.put("actions", "Quick Actions");
        en.put("restart", "Restart Server");
        en.put("announce", "Broadcast Announcement");
        en.put("send", "Send");
        en.put("kick", "Kick");
        en.put("ban", "Ban");
        en.put("refresh", "Refresh");
        en.put("save", "Save Configuration");
        en.put("saved", "Saved!");
        en.put("saving", "Saving...");
        en.put("add", "Add");
        en.put("remove", "Remove");
        en.put("cancel", "Cancel");
        
        // --- Potvrzení a zprávy ---
        en.put("confirm_action", "Are you sure?");
        en.put("kicked_by_admin", "Kicked by admin");
        en.put("message_placeholder", "Type your message here...");
        
        // --- Sekce konfigurace ---
        en.put("general", "General");
        en.put("web", "Web");
        en.put("announcements", "Announcements");
        en.put("discord", "Discord");
        en.put("stats", "Statistics");
        en.put("vote", "Voting");
        en.put("ranks", "Ranks");
        en.put("tickets", "Tickets");
        en.put("playerlist", "Player List (TAB)");
        
        // --- Reset locale ---
        en.put("locale_reset_title", "Message Language Reset");
        en.put("locale_reset_description", "Reset all player-facing messages to English or Czech. This does NOT change ports, IDs, or technical settings.");
        en.put("reset_to_english", "Reset Messages to English");
        en.put("reset_to_czech", "Reset Messages to Czech");
        en.put("locale_reset_success", "Messages successfully reset to {locale}!");
        en.put("locale_reset_confirm_en", "Are you sure you want to reset all messages to English?");
        en.put("locale_reset_confirm_cz", "Are you sure you want to reset all messages to Czech?");
        
        // --- Popisy sekcí ---
        en.put("desc.server_info", "Real-time server statistics and resource usage");
        en.put("desc.actions", "Quick actions to manage your server");
        en.put("desc.player_list", "List of currently connected players with management options");
        en.put("desc.restart", "Restart the Minecraft server. All players will be disconnected.");
        en.put("desc.announce", "Send a broadcast message to all online players");
        en.put("desc.kick", "Disconnect this player from the server");
        
        // --- Role prefix editor ---
        en.put("label.prefix", "Prefix");
        en.put("label.suffix", "Suffix");
        en.put("label.color", "Color");
        en.put("label.priority", "Priority");
        en.put("placeholder.prefix", "Text before name (e.g., &4[ADMIN] )");
        en.put("placeholder.suffix", "Text after name (e.g.,  &4★)");
        en.put("placeholder.color", "Name color (e.g., &4 for red)");
        en.put("placeholder.priority", "Higher = applied first");
        
        // =====================================================================
        // KONFIGURACE - NÁZVY POLÍ (section.fieldName)
        // =====================================================================
        
        // --- Web ---
        en.put("web.language", "Language");
        en.put("desc.web.language", "Choose the interface language for the control panel");
        
        // --- General ---
        en.put("general.enableMod", "Enable Mod");
        en.put("general.enableRestarts", "Enable Restarts");
        en.put("general.enableAnnouncements", "Enable Announcements");
        en.put("general.enableSkinRestorer", "Enable Skin Restorer");
        en.put("general.enableDiscord", "Enable Discord");
        en.put("general.enableWeb", "Enable Web Panel");
        en.put("general.enableStats", "Enable Statistics");
        en.put("general.enableRanks", "Enable Ranks");
        en.put("general.enableVote", "Enable Voting");
        en.put("general.enablePlayerList", "Enable Player List");
        en.put("general.skinCacheHours", "Skin Cache Duration (Hours)");
        en.put("general.modPrefix", "Mod Prefix");
        
        // --- Restart ---
        en.put("restart.restartType", "Restart Type");
        en.put("restart.fixedRestartTimes", "Fixed Restart Times");
        en.put("restart.intervalHours", "Interval (Hours)");
        en.put("restart.delayMinutes", "Delay (Minutes)");
        en.put("desc.restart.restartType", "FIXED_TIME: Restart at specific times | INTERVAL: Restart every X hours | DELAY: Restart after X minutes from start");
        en.put("desc.restart.fixedRestartTimes", "Set exact times when the server should restart (format: HH:MM)");
        en.put("desc.restart.intervalHours", "Number of hours between automatic restarts");
        en.put("desc.restart.delayMinutes", "Minutes to wait before the first restart after server start");
        
        // --- Announcements ---
        en.put("announcement.prefix", "Prefix");
        en.put("announcement.announcementIntervalMinutes", "Interval (Minutes)");
        en.put("announcement.announcements", "Messages");
        en.put("desc.announcement.prefix", "Text prefix added before each announcement message");
        en.put("desc.announcement.announcementIntervalMinutes", "Time interval in minutes between automatic announcements");
        en.put("desc.announcement.announcements", "List of messages that will be broadcasted automatically in rotation");
        
        // --- Ranks ---
        en.put("ranks.enableAutoRanks", "Enable Auto Ranks");
        en.put("ranks.checkIntervalMinutes", "Check Interval (Minutes)");
        en.put("ranks.promotionMessage", "Promotion Message");
        en.put("ranks.ranks", "Rank Definitions");
        en.put("ranks.type", "Type");
        en.put("ranks.value", "Value");
        en.put("ranks.hours", "Hours Needed");
        en.put("desc.ranks.enableAutoRanks", "Automatically promote players to higher ranks based on playtime");
        en.put("desc.ranks.checkIntervalMinutes", "How often to check if players should be promoted (in minutes)");
        en.put("desc.ranks.promotionMessage", "Message sent to player when promoted. Variables: {player}, {rank}, {hours}");
        en.put("desc.ranks.ranks", "Define ranks with required playtime hours. PREFIX adds text before name, SUFFIX after name");
        
        // --- Discord ---
        en.put("discord.enableDiscord", "Enable Discord Integration");
        en.put("discord.botToken", "Bot Token");
        en.put("discord.guildId", "Guild ID");
        en.put("discord.botActivityType", "Bot Activity Type");
        en.put("discord.botActivityText", "Bot Activity Text");
        en.put("discord.enableWhitelist", "Enable Discord Whitelist");
        en.put("discord.kickMessage", "Kick Message");
        en.put("discord.linkSuccessMessage", "Link Success Message");
        en.put("discord.alreadyLinkedMessage", "Already Linked Message");
        en.put("discord.maxAccountsPerDiscord", "Max Accounts Per Discord");
        en.put("discord.chatChannelId", "Chat Channel ID");
        en.put("discord.consoleChannelId", "Console Channel ID");
        en.put("discord.statusChannelId", "Status Channel ID");
        en.put("discord.linkChannelId", "Link Channel ID");
        en.put("discord.linkedRoleId", "Linked Role ID");
        en.put("discord.rolePrefixes", "Role Prefixes");
        en.put("discord.syncBansDiscordToMc", "Sync Bans: Discord → Minecraft");
        en.put("discord.syncBansMcToDiscord", "Sync Bans: Minecraft → Discord");
        en.put("discord.enableChatBridge", "Enable Chat Bridge");
        en.put("discord.minecraftToDiscordFormat", "Minecraft → Discord Format");
        en.put("discord.discordToMinecraftFormat", "Discord → Minecraft Format");
        en.put("discord.translateEmojis", "Translate Emojis");
        en.put("discord.chatWebhookUrl", "Chat Webhook URL");
        en.put("discord.enableConsoleLog", "Enable Console Log");
        en.put("discord.enableStatusMessages", "Enable Status Messages");
        en.put("discord.statusMessageStarting", "Status: Starting");
        en.put("discord.statusMessageStarted", "Status: Started");
        en.put("discord.statusMessageStopping", "Status: Stopping");
        en.put("discord.statusMessageStopped", "Status: Stopped");
        en.put("discord.enableTopicUpdate", "Enable Topic Update");
        en.put("discord.channelTopicFormat", "Channel Topic Format");
        en.put("discord.uptimeFormat", "Uptime Format");
        // Bot Messages
        en.put("discord.invalidCodeMessage", "Invalid Code Message");
        en.put("discord.notLinkedMessage", "Not Linked Message");
        en.put("discord.alreadyLinkedSingleMessage", "Already Linked (Single) Message");
        en.put("discord.alreadyLinkedMultipleMessage", "Already Linked (Multiple) Message");
        en.put("discord.unlinkSuccessMessage", "Unlink Success Message");
        en.put("discord.wrongGuildMessage", "Wrong Guild Message");
        en.put("discord.ticketCreatedMessage", "Ticket Created Message");
        en.put("discord.ticketClosingMessage", "Ticket Closing Message");
        en.put("discord.textChannelOnlyMessage", "Text Channel Only Message");
        
        en.put("desc.discord.botToken", "Your Discord bot token from Discord Developer Portal");
        en.put("desc.discord.guildId", "Your Discord server (guild) ID");
        en.put("desc.discord.botActivityType", "Activity Type: PLAYING, WATCHING, LISTENING, COMPETING");
        en.put("desc.discord.botActivityText", "Text displayed in bot status");
        en.put("desc.discord.kickMessage", "Message shown to non-whitelisted players. Use %code% for verification code");
        en.put("desc.discord.linkSuccessMessage", "Message sent when account is linked. Use %player% for player name");
        en.put("desc.discord.alreadyLinkedMessage", "Message when max accounts reached. Use %max% for limit");
        en.put("desc.discord.chatWebhookUrl", "Webhook URL for sending Minecraft chat to Discord");
        en.put("desc.discord.rolePrefixes", "Configure player name prefixes/suffixes for Discord roles");
        en.put("desc.discord.minecraftToDiscordFormat", "Format for MC→Discord. Variables: %player%, %message%");
        en.put("desc.discord.discordToMinecraftFormat", "Format for Discord→MC. Variables: %user%, %message%");
        en.put("desc.discord.invalidCodeMessage", "Message when invalid or expired code is entered");
        en.put("desc.discord.notLinkedMessage", "Message when user is not linked and enters invalid code");
        en.put("desc.discord.alreadyLinkedSingleMessage", "Message when user is already linked (1 account). Use %uuid%");
        en.put("desc.discord.alreadyLinkedMultipleMessage", "Message when user is linked to multiple accounts. Use %count%");
        en.put("desc.discord.unlinkSuccessMessage", "Message when accounts are successfully unlinked");
        en.put("desc.discord.wrongGuildMessage", "Message when command is used in wrong Discord server");
        en.put("desc.discord.ticketCreatedMessage", "Message when a ticket is created");
        en.put("desc.discord.ticketClosingMessage", "Message when a ticket is being closed");
        en.put("desc.discord.textChannelOnlyMessage", "Message when command requires text channel");
        
        // --- Stats ---
        en.put("stats.enableStats", "Enable Statistics");
        en.put("stats.reportChannelId", "Report Channel ID");
        en.put("stats.reportTime", "Report Time (HH:MM)");
        
        // --- Vote ---
        en.put("vote.enabled", "Enable Voting System");
        en.put("vote.host", "Host Address");
        en.put("vote.port", "Port");
        en.put("vote.rsaPrivateKeyPath", "RSA Private Key Path");
        en.put("vote.rsaPublicKeyPath", "RSA Public Key Path");
        en.put("vote.sharedSecret", "Shared Secret");
        en.put("vote.announceVotes", "Announce Votes");
        en.put("vote.announcementMessage", "Announcement Message");
        en.put("vote.announcementCooldown", "Announcement Cooldown (seconds)");
        en.put("vote.commands", "Commands to Execute");
        en.put("desc.vote.announcementMessage", "Vote announcement. Use %PLAYER% for player name");
        en.put("desc.vote.commands", "Commands executed when player votes. Use %PLAYER% for player name");
        
        // --- PlayerList ---
        en.put("playerlist.enableCustomPlayerList", "Enable Custom Player List");
        en.put("playerlist.headerLine1", "Header Line 1");
        en.put("playerlist.headerLine2", "Header Line 2");
        en.put("playerlist.headerLine3", "Header Line 3");
        en.put("playerlist.footerLine1", "Footer Line 1");
        en.put("playerlist.footerLine2", "Footer Line 2");
        en.put("playerlist.footerLine3", "Footer Line 3");
        en.put("playerlist.enableCustomNames", "Enable Custom Names");
        en.put("playerlist.playerNameFormat", "Player Name Format");
        en.put("playerlist.defaultPrefix", "Default Prefix");
        en.put("playerlist.defaultSuffix", "Default Suffix");
        en.put("playerlist.combineMultipleRanks", "Combine Multiple Ranks");
        en.put("playerlist.updateIntervalSeconds", "Update Interval (seconds)");
        en.put("desc.playerlist.enableCustomPlayerList", "Enable custom TAB list header/footer");
        en.put("desc.playerlist.headerLine1", "First line of TAB header. Variables: %online%, %max%, %tps%");
        en.put("desc.playerlist.playerNameFormat", "Format: %rank_prefix%, %player_name%, %rank_suffix%");
        
        // --- Tickets ---
        en.put("tickets.enableTickets", "Enable Ticket System");
        en.put("tickets.ticketCategoryId", "Ticket Category ID");
        en.put("tickets.supportRoleId", "Support Role ID");
        en.put("tickets.ticketChannelTopic", "Ticket Channel Topic");
        en.put("tickets.maxTicketsPerUser", "Max Tickets Per User");
        en.put("tickets.ticketCreatedMessage", "Ticket Created Message");
        en.put("tickets.ticketWelcomeMessage", "Ticket Welcome Message");
        en.put("tickets.ticketCloseMessage", "Ticket Close Message");
        en.put("tickets.noPermissionMessage", "No Permission Message");
        en.put("tickets.ticketLimitReachedMessage", "Limit Reached Message");
        en.put("tickets.ticketAlreadyClosedMessage", "Already Closed Message");
        en.put("tickets.enableTranscript", "Enable Transcript");
        en.put("tickets.transcriptFormat", "Transcript Format");
        en.put("tickets.transcriptFilename", "Transcript Filename");
        en.put("desc.tickets.enableTickets", "Enable the Discord ticket system");
        en.put("desc.tickets.transcriptFormat", "Format: TXT (readable) or JSON (structured)");
        en.put("desc.tickets.transcriptFilename", "Filename template. Variables: %user%, %date%, %reason%");
        
        // --- Tlačítka a UI elementy ---
        en.put("btn.save", "Save");
        en.put("btn.cancel", "Cancel");
        en.put("btn.add", "Add");
        en.put("btn.remove", "Remove");
        en.put("label.enabled", "Enabled");
        en.put("label.disabled", "Disabled");
        en.put("loading", "Loading...");
        
        // --- Config sekce pro JavaScript ---
        en.put("config.save.success", "Configuration saved successfully!");
        en.put("config.save.error", "Failed to save configuration");
        en.put("config.load.error", "Failed to load configuration");
        en.put("config.section.web", "Web Panel");
        en.put("config.section.general", "General Settings");
        en.put("config.section.restart", "Restart Settings");
        en.put("config.section.announcement", "Announcements");
        en.put("config.section.discord", "Discord Integration");
        en.put("config.section.stats", "Statistics");
        en.put("config.section.vote", "Voting System");
        en.put("config.section.ranks", "Ranks");
        en.put("config.section.tickets", "Ticket System");
        en.put("config.section.playerlist", "Player List (TAB)");
        
        // --- Role prefix editor ---
        en.put("role.prefix.editor", "Role Prefix Editor");
        en.put("role.prefix.add", "Add Role Prefix");
        en.put("role.prefix.remove", "Remove");
        en.put("color.picker.title", "Select Color");
        
        // --- Ranks placeholders ---
        en.put("ranks.value.placeholder", "Prefix/Suffix text (e.g. &a[VIP])");
        en.put("ranks.hours.placeholder", "Required hours");
        
        // --- Language and locale ---
        en.put("language.changed", "Language changed! Reloading...");
        en.put("locale.reset.confirm", "Are you sure you want to reset all locale messages?");
        en.put("locale.reset.success", "Locale messages reset successfully!");
        en.put("locale.reset.error", "Failed to reset locale messages");
        
        // --- Web config fields ---
        en.put("config.web.language", "Interface Language");
        en.put("config.web.language.desc", "Choose language for the control panel interface");
        
        // --- More field descriptions ---
        en.put("config.general.enableMod.desc", "Master switch for enabling/disabling the entire Voidium mod");
        en.put("config.general.enableRestarts.desc", "Enable automatic server restarts");
        en.put("config.general.enableAnnouncements.desc", "Enable automated broadcast announcements");
        en.put("config.general.enableSkinRestorer.desc", "Enable skin restoration for cracked players");
        en.put("config.general.enableDiscord.desc", "Enable Discord bot integration");
        en.put("config.general.enableWeb.desc", "Enable this web control panel");
        en.put("config.general.enableStats.desc", "Enable server statistics tracking");
        en.put("config.general.enableRanks.desc", "Enable playtime-based rank system");
        en.put("config.general.enableVote.desc", "Enable Votifier voting system");
        en.put("config.general.enablePlayerList.desc", "Enable custom TAB list");
        en.put("config.general.skinCacheHours.desc", "How long to cache player skins (hours)");
        en.put("config.general.modPrefix.desc", "Prefix for all mod messages in chat");
        
        // --- Vote descriptions ---
        en.put("config.vote.enabled.desc", "Enable the Votifier voting system");
        en.put("config.vote.host.desc", "Host address for Votifier listener (usually 0.0.0.0)");
        en.put("config.vote.port.desc", "Port for Votifier listener (default: 8192)");
        en.put("config.vote.rsaPrivateKeyPath.desc", "Path to RSA private key file");
        en.put("config.vote.rsaPublicKeyPath.desc", "Path to RSA public key file");
        en.put("config.vote.sharedSecret.desc", "Shared secret for V2 protocol");
        en.put("config.vote.announceVotes.desc", "Broadcast vote announcements to all players");
        en.put("config.vote.announcementCooldown.desc", "Cooldown between vote announcements (seconds)");
        
        // --- Ranks descriptions ---
        en.put("config.ranks.enableAutoRanks.desc", "Automatically assign ranks based on playtime");
        en.put("config.ranks.checkIntervalMinutes.desc", "How often to check playtime for promotions (minutes)");
        en.put("config.ranks.promotionMessage.desc", "Message sent on promotion. Variables: {player}, {rank}, {hours}");
        en.put("config.ranks.ranks.desc", "List of playtime ranks. Type: PREFIX (before name) or SUFFIX (after name)");
        
        // --- PlayerList descriptions ---
        en.put("config.playerlist.enableCustomPlayerList.desc", "Enable custom TAB list header and footer");
        en.put("config.playerlist.headerLine1.desc", "Variables: %online%, %max%, %tps%, %playtime%, %time%, %memory%");
        en.put("config.playerlist.headerLine2.desc", "Variables: %online%, %max%, %tps%, %playtime%, %time%, %memory%");
        en.put("config.playerlist.headerLine3.desc", "Variables: %online%, %max%, %tps%, %playtime%, %time%, %memory%");
        en.put("config.playerlist.footerLine1.desc", "Variables: %online%, %max%, %tps%, %playtime%, %time%, %memory%");
        en.put("config.playerlist.footerLine2.desc", "Variables: %online%, %max%, %tps%, %playtime%, %time%, %memory%");
        en.put("config.playerlist.footerLine3.desc", "Variables: %online%, %max%, %tps%, %playtime%, %time%, %memory%");
        en.put("config.playerlist.enableCustomNames.desc", "Enable custom player name formatting in TAB");
        en.put("config.playerlist.playerNameFormat.desc", "Variables: %rank_prefix%, %player_name%, %rank_suffix%");
        en.put("config.playerlist.defaultPrefix.desc", "Default prefix when player has no rank. Color codes: &0-f, &#RRGGBB");
        en.put("config.playerlist.defaultSuffix.desc", "Default suffix when player has no rank. Color codes: &0-f, &#RRGGBB");
        en.put("config.playerlist.combineMultipleRanks.desc", "Combine all rank prefixes/suffixes instead of using highest priority only");
        en.put("config.playerlist.updateIntervalSeconds.desc", "How often to refresh the TAB list (in seconds)");
        
        // --- Stats descriptions ---
        en.put("config.stats.enableStats.desc", "Enable daily server statistics reports");
        en.put("config.stats.reportChannelId.desc", "Discord channel ID for daily statistics reports");
        en.put("config.stats.reportTime.desc", "Time to send daily report (format: HH:MM)");
        
        // --- Restart descriptions ---
        en.put("config.restart.restartType.desc", "FIXED_TIME: At specific times | INTERVAL: Every X hours | DELAY: X minutes from start");
        en.put("config.restart.fixedRestartTimes.desc", "Exact times for server restart (format: HH:MM)");
        en.put("config.restart.intervalHours.desc", "Hours between automatic restarts");
        en.put("config.restart.delayMinutes.desc", "Minutes to wait before first restart after server start");
        
        // --- Announcement descriptions ---
        en.put("config.announcement.prefix.desc", "Text prefix before each announcement. Color codes: &0-f, &#RRGGBB");
        en.put("config.announcement.announcementIntervalMinutes.desc", "Minutes between automatic announcements");
        en.put("config.announcement.announcements.desc", "Messages to broadcast. Color codes: &0-f (legacy), &#RRGGBB (hex)");
        
        // --- Tickets descriptions ---
        en.put("config.tickets.enableTickets.desc", "Enable Discord ticket support system");
        en.put("config.tickets.ticketCategoryId.desc", "Discord category ID where ticket channels will be created");
        en.put("config.tickets.supportRoleId.desc", "Discord role ID that can see and manage tickets");
        en.put("config.tickets.ticketChannelTopic.desc", "Topic for ticket channels. Variables: %player%, %reason%");
        en.put("config.tickets.maxTicketsPerUser.desc", "Maximum open tickets per player");
        en.put("config.tickets.ticketCreatedMessage.desc", "Message in MC when ticket created. Variables: %channel%");
        en.put("config.tickets.ticketWelcomeMessage.desc", "Welcome message in ticket channel. Variables: %user%, %reason%");
        en.put("config.tickets.ticketCloseMessage.desc", "Message when ticket is closed");
        en.put("config.tickets.noPermissionMessage.desc", "Message when user lacks permission");
        en.put("config.tickets.ticketLimitReachedMessage.desc", "Message when user reaches ticket limit. Variables: %max%");
        en.put("config.tickets.ticketAlreadyClosedMessage.desc", "Message when ticket is already closed");
        en.put("config.tickets.enableTranscript.desc", "Save ticket conversation when closing");
        en.put("config.tickets.transcriptFormat.desc", "TXT = readable text, JSON = structured data");
        en.put("config.tickets.transcriptFilename.desc", "Filename template. Variables: %user%, %date%, %reason%");
        
        // --- Discord descriptions ---
        en.put("config.discord.enableDiscord.desc", "Master switch for Discord bot integration");
        en.put("config.discord.botToken.desc", "Your bot token from Discord Developer Portal (keep secret!)");
        en.put("config.discord.guildId.desc", "Your Discord server ID (right-click server → Copy ID)");
        en.put("config.discord.botActivityType.desc", "PLAYING, WATCHING, LISTENING, or COMPETING");
        en.put("config.discord.botActivityText.desc", "Text shown in bot status. Variables: %online%, %max%");
        en.put("config.discord.enableWhitelist.desc", "Only allow linked Discord accounts to join");
        en.put("config.discord.kickMessage.desc", "Message for unlinked players. Variables: %code%");
        en.put("config.discord.linkSuccessMessage.desc", "Message on successful link. Variables: %player%");
        en.put("config.discord.alreadyLinkedMessage.desc", "Message when max accounts reached. Variables: %max%");
        en.put("config.discord.maxAccountsPerDiscord.desc", "Maximum MC accounts per Discord user");
        en.put("config.discord.chatChannelId.desc", "Channel ID for MC ↔ Discord chat bridge");
        en.put("config.discord.consoleChannelId.desc", "Channel ID for server console output");
        en.put("config.discord.statusChannelId.desc", "Channel ID for server status messages");
        en.put("config.discord.linkChannelId.desc", "Channel ID where players enter link codes");
        en.put("config.discord.linkedRoleId.desc", "Role ID assigned to linked users");
        en.put("config.discord.rolePrefixes.desc", "Configure name prefixes/suffixes for Discord roles");
        en.put("config.discord.syncBansDiscordToMc.desc", "Ban MC player when banned from Discord");
        en.put("config.discord.syncBansMcToDiscord.desc", "Ban Discord user when banned in MC");
        en.put("config.discord.enableChatBridge.desc", "Enable two-way chat between MC and Discord");
        en.put("config.discord.minecraftToDiscordFormat.desc", "Format for MC→Discord. Variables: %player%, %message%");
        en.put("config.discord.discordToMinecraftFormat.desc", "Format for Discord→MC. Variables: %user%, %message%. Color codes: &0-f");
        en.put("config.discord.translateEmojis.desc", "Convert Discord emoji names to Unicode characters");
        en.put("config.discord.chatWebhookUrl.desc", "Webhook URL for chat (shows player heads as avatar)");
        en.put("config.discord.enableConsoleLog.desc", "Send server console output to Discord");
        en.put("config.discord.enableStatusMessages.desc", "Send server start/stop messages to Discord");
        en.put("config.discord.statusMessageStarting.desc", "Message when server is starting");
        en.put("config.discord.statusMessageStarted.desc", "Message when server is online");
        en.put("config.discord.statusMessageStopping.desc", "Message when server is stopping");
        en.put("config.discord.statusMessageStopped.desc", "Message when server is offline");
        en.put("config.discord.enableTopicUpdate.desc", "Update channel topic with server info");
        en.put("config.discord.channelTopicFormat.desc", "Topic format. Variables: %online%, %max%, %tps%, %uptime%");
        en.put("config.discord.uptimeFormat.desc", "Uptime display format. Variables: %d% (days), %h% (hours), %m% (minutes)");
        en.put("config.discord.invalidCodeMessage.desc", "Message when invalid/expired code is entered");
        en.put("config.discord.notLinkedMessage.desc", "Message when user is not linked");
        en.put("config.discord.alreadyLinkedSingleMessage.desc", "Message when already linked (1 account). Variables: %uuid%");
        en.put("config.discord.alreadyLinkedMultipleMessage.desc", "Message when linked to multiple accounts. Variables: %count%");
        en.put("config.discord.unlinkSuccessMessage.desc", "Message when accounts are unlinked");
        en.put("config.discord.wrongGuildMessage.desc", "Message when command used in wrong server");
        en.put("config.discord.ticketCreatedMessage.desc", "Message when ticket is created");
        en.put("config.discord.ticketClosingMessage.desc", "Message when ticket is closing");
        en.put("config.discord.textChannelOnlyMessage.desc", "Message when command requires text channel");
        
        // --- Help text for color codes ---
        en.put("help.colorcodes", "Color codes: &0-9, &a-f (legacy), &#RRGGBB (hex). Formatting: &l (bold), &o (italic), &n (underline), &m (strike), &r (reset)");
        
        LANG.put("en", en);
        
        // ==================== ČEŠTINA =======================================
        Map<String, String> cz = new HashMap<>();
        
        // --- Základní UI texty ---
        cz.put("title", "Ovládací panel Voidium");
        cz.put("unauthorized", "Neautorizováno. Použijte prosím odkaz z konzole serveru.");
        cz.put("dashboard", "Nástěnka");
        cz.put("config", "Konfigurace");
        cz.put("status", "Stav serveru");
        cz.put("online", "Online");
        cz.put("offline", "Offline");
        
        // --- Statistiky serveru ---
        cz.put("players", "Hráči");
        cz.put("active_players", "Aktivní hráči");
        cz.put("no_players", "Žádní hráči online");
        cz.put("player_list", "Online hráči");
        cz.put("memory", "Paměť");
        cz.put("max_memory", "Max. paměť");
        cz.put("used_memory", "Použitá paměť");
        cz.put("free_memory", "Volná paměť");
        cz.put("tps", "TPS");
        cz.put("uptime", "Doba běhu");
        cz.put("cpu", "Vytížení CPU");
        cz.put("server_info", "Informace o serveru");
        
        // --- Akce ---
        cz.put("actions", "Rychlé akce");
        cz.put("restart", "Restartovat server");
        cz.put("announce", "Odeslat oznámení");
        cz.put("send", "Odeslat");
        cz.put("kick", "Vyhodit");
        cz.put("ban", "Zabanovat");
        cz.put("refresh", "Obnovit");
        cz.put("save", "Uložit konfiguraci");
        cz.put("saved", "Uloženo!");
        cz.put("saving", "Ukládám...");
        cz.put("add", "Přidat");
        cz.put("remove", "Odebrat");
        cz.put("cancel", "Zrušit");
        
        // --- Potvrzení a zprávy ---
        cz.put("confirm_action", "Jste si jistí?");
        cz.put("kicked_by_admin", "Vyhozen administrátorem");
        cz.put("message_placeholder", "Napište zprávu zde...");
        
        // --- Sekce konfigurace ---
        cz.put("general", "Obecné");
        cz.put("web", "Web");
        cz.put("announcements", "Oznámení");
        cz.put("discord", "Discord");
        cz.put("stats", "Statistiky");
        cz.put("vote", "Hlasování");
        cz.put("ranks", "Ranky");
        cz.put("tickets", "Tickety");
        cz.put("playerlist", "Seznam hráčů (TAB)");
        
        // --- Reset locale ---
        cz.put("locale_reset_title", "Reset jazyka zpráv");
        cz.put("locale_reset_description", "Resetuje všechny zprávy do angličtiny nebo češtiny. Nezmění porty, ID ani technická nastavení.");
        cz.put("reset_to_english", "Resetovat zprávy do angličtiny");
        cz.put("reset_to_czech", "Resetovat zprávy do češtiny");
        cz.put("locale_reset_success", "Zprávy úspěšně resetovány do {locale}!");
        cz.put("locale_reset_confirm_en", "Opravdu chcete resetovat všechny zprávy do angličtiny?");
        cz.put("locale_reset_confirm_cz", "Opravdu chcete resetovat všechny zprávy do češtiny?");
        
        // --- Popisy sekcí ---
        cz.put("desc.server_info", "Statistiky serveru a využití zdrojů v reálném čase");
        cz.put("desc.actions", "Rychlé akce pro správu serveru");
        cz.put("desc.player_list", "Seznam připojených hráčů s možnostmi správy");
        cz.put("desc.restart", "Restartuje server. Všichni hráči budou odpojeni.");
        cz.put("desc.announce", "Odešle zprávu všem online hráčům");
        cz.put("desc.kick", "Odpojí tohoto hráče ze serveru");
        
        // --- Role prefix editor ---
        cz.put("label.prefix", "Prefix");
        cz.put("label.suffix", "Suffix");
        cz.put("label.color", "Barva");
        cz.put("label.priority", "Priorita");
        cz.put("placeholder.prefix", "Text před jménem (např. &4[ADMIN] )");
        cz.put("placeholder.suffix", "Text za jménem (např.  &4★)");
        cz.put("placeholder.color", "Barva jména (např. &4 pro červenou)");
        cz.put("placeholder.priority", "Vyšší číslo = vyšší priorita");
        
        // =====================================================================
        // KONFIGURACE - NÁZVY POLÍ (section.fieldName) - ČEŠTINA
        // =====================================================================
        
        // --- Web ---
        cz.put("web.language", "Jazyk");
        cz.put("desc.web.language", "Vyberte jazyk rozhraní ovládacího panelu");
        
        // --- General ---
        cz.put("general.enableMod", "Zapnout mod");
        cz.put("general.enableRestarts", "Zapnout restarty");
        cz.put("general.enableAnnouncements", "Zapnout oznámení");
        cz.put("general.enableSkinRestorer", "Zapnout Skin Restorer");
        cz.put("general.enableDiscord", "Zapnout Discord");
        cz.put("general.enableWeb", "Zapnout webový panel");
        cz.put("general.enableStats", "Zapnout statistiky");
        cz.put("general.enableRanks", "Zapnout ranky");
        cz.put("general.enableVote", "Zapnout hlasování");
        cz.put("general.enablePlayerList", "Zapnout seznam hráčů");
        cz.put("general.skinCacheHours", "Doba cachování skinů (hodiny)");
        cz.put("general.modPrefix", "Prefix modu");
        
        // --- Restart ---
        cz.put("restart.restartType", "Typ restartu");
        cz.put("restart.fixedRestartTimes", "Pevné časy restartu");
        cz.put("restart.intervalHours", "Interval (hodiny)");
        cz.put("restart.delayMinutes", "Zpoždění (minuty)");
        cz.put("desc.restart.restartType", "FIXED_TIME: V konkrétní časy | INTERVAL: Každých X hodin | DELAY: Po X minutách od startu");
        cz.put("desc.restart.fixedRestartTimes", "Nastavte přesné časy restartů (formát: HH:MM)");
        cz.put("desc.restart.intervalHours", "Počet hodin mezi automatickými restarty");
        cz.put("desc.restart.delayMinutes", "Minuty čekání před prvním restartem");
        
        // --- Announcements ---
        cz.put("announcement.prefix", "Prefix");
        cz.put("announcement.announcementIntervalMinutes", "Interval (minuty)");
        cz.put("announcement.announcements", "Zprávy");
        cz.put("desc.announcement.prefix", "Textový prefix přidaný před každou zprávu");
        cz.put("desc.announcement.announcementIntervalMinutes", "Časový interval mezi oznámeními");
        cz.put("desc.announcement.announcements", "Seznam zpráv vysílaných v rotaci");
        
        // --- Ranks ---
        cz.put("ranks.enableAutoRanks", "Zapnout auto ranky");
        cz.put("ranks.checkIntervalMinutes", "Interval kontroly (minuty)");
        cz.put("ranks.promotionMessage", "Zpráva o povýšení");
        cz.put("ranks.ranks", "Definice ranků");
        cz.put("ranks.type", "Typ");
        cz.put("ranks.value", "Hodnota");
        cz.put("ranks.hours", "Potřebné hodiny");
        cz.put("desc.ranks.enableAutoRanks", "Automaticky povyšovat hráče podle odehraného času");
        cz.put("desc.ranks.checkIntervalMinutes", "Jak často kontrolovat povýšení (v minutách)");
        cz.put("desc.ranks.promotionMessage", "Zpráva při povýšení. Proměnné: {player}, {rank}, {hours}");
        cz.put("desc.ranks.ranks", "Definice ranků s požadovanými hodinami. PREFIX/SUFFIX");
        
        // --- Discord ---
        cz.put("discord.enableDiscord", "Zapnout Discord integraci");
        cz.put("discord.botToken", "Token bota");
        cz.put("discord.guildId", "ID serveru");
        cz.put("discord.botActivityType", "Typ aktivity bota");
        cz.put("discord.botActivityText", "Text aktivity bota");
        cz.put("discord.enableWhitelist", "Zapnout Discord whitelist");
        cz.put("discord.kickMessage", "Zpráva při vyhození");
        cz.put("discord.linkSuccessMessage", "Zpráva při úspěšném propojení");
        cz.put("discord.alreadyLinkedMessage", "Zpráva při již propojeném účtu");
        cz.put("discord.maxAccountsPerDiscord", "Max. účtů na Discord");
        cz.put("discord.chatChannelId", "ID chat kanálu");
        cz.put("discord.consoleChannelId", "ID konzole kanálu");
        cz.put("discord.statusChannelId", "ID status kanálu");
        cz.put("discord.linkChannelId", "ID propojovacího kanálu");
        cz.put("discord.linkedRoleId", "ID role pro propojené");
        cz.put("discord.rolePrefixes", "Prefixy rolí");
        cz.put("discord.syncBansDiscordToMc", "Sync banů: Discord → Minecraft");
        cz.put("discord.syncBansMcToDiscord", "Sync banů: Minecraft → Discord");
        cz.put("discord.enableChatBridge", "Zapnout chat most");
        cz.put("discord.minecraftToDiscordFormat", "Formát Minecraft → Discord");
        cz.put("discord.discordToMinecraftFormat", "Formát Discord → Minecraft");
        cz.put("discord.translateEmojis", "Překládat emoji");
        cz.put("discord.chatWebhookUrl", "URL chat webhooku");
        cz.put("discord.enableConsoleLog", "Zapnout logování konzole");
        cz.put("discord.enableStatusMessages", "Zapnout status zprávy");
        cz.put("discord.statusMessageStarting", "Status: Startuje");
        cz.put("discord.statusMessageStarted", "Status: Online");
        cz.put("discord.statusMessageStopping", "Status: Vypíná se");
        cz.put("discord.statusMessageStopped", "Status: Offline");
        cz.put("discord.enableTopicUpdate", "Aktualizovat topic kanálu");
        cz.put("discord.channelTopicFormat", "Formát topicu kanálu");
        cz.put("discord.uptimeFormat", "Formát uptime");
        // Bot Messages
        cz.put("discord.invalidCodeMessage", "Zpráva pro neplatný kód");
        cz.put("discord.notLinkedMessage", "Zpráva pro nepropojeného");
        cz.put("discord.alreadyLinkedSingleMessage", "Zpráva pro již propojeného (1 účet)");
        cz.put("discord.alreadyLinkedMultipleMessage", "Zpráva pro již propojeného (více účtů)");
        cz.put("discord.unlinkSuccessMessage", "Zpráva o úspěšném odpojení");
        cz.put("discord.wrongGuildMessage", "Zpráva pro špatný server");
        cz.put("discord.ticketCreatedMessage", "Zpráva o vytvoření ticketu");
        cz.put("discord.ticketClosingMessage", "Zpráva o zavírání ticketu");
        cz.put("discord.textChannelOnlyMessage", "Zpráva pouze pro textový kanál");
        
        cz.put("desc.discord.botToken", "Token Discord bota z Developer Portal");
        cz.put("desc.discord.guildId", "ID vašeho Discord serveru");
        cz.put("desc.discord.botActivityType", "Typ aktivity: PLAYING, WATCHING, LISTENING, COMPETING");
        cz.put("desc.discord.botActivityText", "Text zobrazený ve statusu bota");
        cz.put("desc.discord.kickMessage", "Zpráva pro hráče mimo whitelist. Použijte %code%");
        cz.put("desc.discord.linkSuccessMessage", "Zpráva při propojení účtu. Použijte %player%");
        cz.put("desc.discord.alreadyLinkedMessage", "Zpráva při dosažení limitu. Použijte %max%");
        cz.put("desc.discord.chatWebhookUrl", "Webhook URL pro odesílání chatu na Discord");
        cz.put("desc.discord.rolePrefixes", "Nastavení prefixů/suffixů pro Discord role");
        cz.put("desc.discord.minecraftToDiscordFormat", "Formát MC→Discord. Proměnné: %player%, %message%");
        cz.put("desc.discord.discordToMinecraftFormat", "Formát Discord→MC. Proměnné: %user%, %message%");
        cz.put("desc.discord.invalidCodeMessage", "Zpráva při zadání neplatného nebo expirovaného kódu");
        cz.put("desc.discord.notLinkedMessage", "Zpráva když uživatel není propojen a zadá neplatný kód");
        cz.put("desc.discord.alreadyLinkedSingleMessage", "Zpráva když je uživatel již propojen (1 účet). Použijte %uuid%");
        cz.put("desc.discord.alreadyLinkedMultipleMessage", "Zpráva když je uživatel propojen k více účtům. Použijte %count%");
        cz.put("desc.discord.unlinkSuccessMessage", "Zpráva při úspěšném odpojení účtů");
        cz.put("desc.discord.wrongGuildMessage", "Zpráva když je příkaz použit na špatném Discord serveru");
        cz.put("desc.discord.ticketCreatedMessage", "Zpráva při vytvoření ticketu");
        cz.put("desc.discord.ticketClosingMessage", "Zpráva při zavírání ticketu");
        cz.put("desc.discord.textChannelOnlyMessage", "Zpráva když příkaz vyžaduje textový kanál");
        
        // --- Stats ---
        cz.put("stats.enableStats", "Zapnout statistiky");
        cz.put("stats.reportChannelId", "ID kanálu pro reporty");
        cz.put("stats.reportTime", "Čas reportu (HH:MM)");
        
        // --- Vote ---
        cz.put("vote.enabled", "Zapnout hlasovací systém");
        cz.put("vote.host", "Adresa hostitele");
        cz.put("vote.port", "Port");
        cz.put("vote.rsaPrivateKeyPath", "Cesta k RSA privátnímu klíči");
        cz.put("vote.rsaPublicKeyPath", "Cesta k RSA veřejnému klíči");
        cz.put("vote.sharedSecret", "Sdílené tajemství");
        cz.put("vote.announceVotes", "Oznamovat hlasy");
        cz.put("vote.announcementMessage", "Zpráva oznámení");
        cz.put("vote.announcementCooldown", "Cooldown oznámení (sekundy)");
        cz.put("vote.commands", "Příkazy k vykonání");
        cz.put("desc.vote.announcementMessage", "Oznámení o hlasování. Použijte %PLAYER%");
        cz.put("desc.vote.commands", "Příkazy při hlasování. Použijte %PLAYER%");
        
        // --- PlayerList ---
        cz.put("playerlist.enableCustomPlayerList", "Zapnout vlastní seznam hráčů");
        cz.put("playerlist.headerLine1", "Hlavička řádek 1");
        cz.put("playerlist.headerLine2", "Hlavička řádek 2");
        cz.put("playerlist.headerLine3", "Hlavička řádek 3");
        cz.put("playerlist.footerLine1", "Patička řádek 1");
        cz.put("playerlist.footerLine2", "Patička řádek 2");
        cz.put("playerlist.footerLine3", "Patička řádek 3");
        cz.put("playerlist.enableCustomNames", "Zapnout vlastní jména");
        cz.put("playerlist.playerNameFormat", "Formát jména hráče");
        cz.put("playerlist.defaultPrefix", "Výchozí prefix");
        cz.put("playerlist.defaultSuffix", "Výchozí suffix");
        cz.put("playerlist.combineMultipleRanks", "Kombinovat více ranků");
        cz.put("playerlist.updateIntervalSeconds", "Interval aktualizace (sekundy)");
        cz.put("desc.playerlist.enableCustomPlayerList", "Zapnout vlastní TAB header/footer");
        cz.put("desc.playerlist.headerLine1", "První řádek hlavičky. Proměnné: %online%, %max%, %tps%");
        cz.put("desc.playerlist.playerNameFormat", "Formát: %rank_prefix%, %player_name%, %rank_suffix%");
        
        // --- Tickets ---
        cz.put("tickets.enableTickets", "Zapnout systém ticketů");
        cz.put("tickets.ticketCategoryId", "ID kategorie ticketů");
        cz.put("tickets.supportRoleId", "ID role pro podporu");
        cz.put("tickets.ticketChannelTopic", "Téma kanálu ticketu");
        cz.put("tickets.maxTicketsPerUser", "Max. ticketů na uživatele");
        cz.put("tickets.ticketCreatedMessage", "Zpráva o vytvoření ticketu");
        cz.put("tickets.ticketWelcomeMessage", "Uvítací zpráva ticketu");
        cz.put("tickets.ticketCloseMessage", "Zpráva o uzavření ticketu");
        cz.put("tickets.noPermissionMessage", "Zpráva bez oprávnění");
        cz.put("tickets.ticketLimitReachedMessage", "Zpráva při dosažení limitu");
        cz.put("tickets.ticketAlreadyClosedMessage", "Zpráva o již zavřeném ticketu");
        cz.put("tickets.enableTranscript", "Zapnout přepis");
        cz.put("tickets.transcriptFormat", "Formát přepisu");
        cz.put("tickets.transcriptFilename", "Název souboru přepisu");
        cz.put("desc.tickets.enableTickets", "Povolit systém ticketů na Discordu");
        cz.put("desc.tickets.transcriptFormat", "Formát: TXT (čitelný) nebo JSON (strukturovaný)");
        cz.put("desc.tickets.transcriptFilename", "Šablona názvu. Proměnné: %user%, %date%, %reason%");
        
        // --- Tlačítka a UI elementy ---
        cz.put("btn.save", "Uložit");
        cz.put("btn.cancel", "Zrušit");
        cz.put("btn.add", "Přidat");
        cz.put("btn.remove", "Odebrat");
        cz.put("label.enabled", "Zapnuto");
        cz.put("label.disabled", "Vypnuto");
        cz.put("loading", "Načítání...");
        
        // --- Config sekce pro JavaScript ---
        cz.put("config.save.success", "Konfigurace byla úspěšně uložena!");
        cz.put("config.save.error", "Nepodařilo se uložit konfiguraci");
        cz.put("config.load.error", "Nepodařilo se načíst konfiguraci");
        cz.put("config.section.web", "Webový panel");
        cz.put("config.section.general", "Obecná nastavení");
        cz.put("config.section.restart", "Nastavení restartů");
        cz.put("config.section.announcement", "Oznámení");
        cz.put("config.section.discord", "Discord integrace");
        cz.put("config.section.stats", "Statistiky");
        cz.put("config.section.vote", "Hlasovací systém");
        cz.put("config.section.ranks", "Ranky");
        cz.put("config.section.tickets", "Systém ticketů");
        cz.put("config.section.playerlist", "Seznam hráčů (TAB)");
        
        // --- Role prefix editor ---
        cz.put("role.prefix.editor", "Editor prefixů rolí");
        cz.put("role.prefix.add", "Přidat prefix role");
        cz.put("role.prefix.remove", "Odebrat");
        cz.put("color.picker.title", "Vybrat barvu");
        
        // --- Ranks placeholders ---
        cz.put("ranks.value.placeholder", "Prefix/Suffix text (např. &a[VIP])");
        cz.put("ranks.hours.placeholder", "Požadované hodiny");
        
        // --- Language and locale ---
        cz.put("language.changed", "Jazyk změněn! Obnovování...");
        cz.put("locale.reset.confirm", "Opravdu chcete resetovat všechny lokalizované zprávy?");
        cz.put("locale.reset.success", "Zprávy úspěšně resetovány!");
        cz.put("locale.reset.error", "Nepodařilo se resetovat zprávy");
        
        // --- Web config fields ---
        cz.put("config.web.language", "Jazyk rozhraní");
        cz.put("config.web.language.desc", "Vyberte jazyk rozhraní ovládacího panelu");
        
        // --- More field descriptions ---
        cz.put("config.general.enableMod.desc", "Hlavní přepínač pro zapnutí/vypnutí celého Voidium modu");
        cz.put("config.general.enableRestarts.desc", "Povolit automatické restarty serveru");
        cz.put("config.general.enableAnnouncements.desc", "Povolit automatická oznámení");
        cz.put("config.general.enableSkinRestorer.desc", "Povolit obnovení skinů pro warez hráče");
        cz.put("config.general.enableDiscord.desc", "Povolit integraci Discord bota");
        cz.put("config.general.enableWeb.desc", "Povolit tento webový ovládací panel");
        cz.put("config.general.enableStats.desc", "Povolit sledování statistik serveru");
        cz.put("config.general.enableRanks.desc", "Povolit systém ranků podle odehraného času");
        cz.put("config.general.enableVote.desc", "Povolit hlasovací systém Votifier");
        cz.put("config.general.enablePlayerList.desc", "Povolit vlastní TAB seznam");
        cz.put("config.general.skinCacheHours.desc", "Jak dlouho ukládat skiny do cache (hodiny)");
        cz.put("config.general.modPrefix.desc", "Prefix pro všechny zprávy modu v chatu");
        
        // --- Vote descriptions ---
        cz.put("config.vote.enabled.desc", "Povolit hlasovací systém Votifier");
        cz.put("config.vote.host.desc", "Host adresa pro Votifier listener (obvykle 0.0.0.0)");
        cz.put("config.vote.port.desc", "Port pro Votifier listener (výchozí: 8192)");
        cz.put("config.vote.rsaPrivateKeyPath.desc", "Cesta k souboru s RSA privátním klíčem");
        cz.put("config.vote.rsaPublicKeyPath.desc", "Cesta k souboru s RSA veřejným klíčem");
        cz.put("config.vote.sharedSecret.desc", "Sdílené tajemství pro V2 protokol");
        cz.put("config.vote.announceVotes.desc", "Oznamovat hlasy všem hráčům");
        cz.put("config.vote.announcementCooldown.desc", "Prodleva mezi oznámeními hlasů (sekundy)");
        
        // --- Ranks descriptions ---
        cz.put("config.ranks.enableAutoRanks.desc", "Automaticky přidělovat ranky podle odehraného času");
        cz.put("config.ranks.checkIntervalMinutes.desc", "Jak často kontrolovat odehraný čas (minuty)");
        cz.put("config.ranks.promotionMessage.desc", "Zpráva při povýšení. Proměnné: {player}, {rank}, {hours}");
        cz.put("config.ranks.ranks.desc", "Seznam ranků. Typ: PREFIX (před jménem) nebo SUFFIX (za jménem)");
        
        // --- PlayerList descriptions ---
        cz.put("config.playerlist.enableCustomPlayerList.desc", "Povolit vlastní TAB header a footer");
        cz.put("config.playerlist.headerLine1.desc", "Proměnné: %online%, %max%, %tps%, %playtime%, %time%, %memory%");
        cz.put("config.playerlist.headerLine2.desc", "Proměnné: %online%, %max%, %tps%, %playtime%, %time%, %memory%");
        cz.put("config.playerlist.headerLine3.desc", "Proměnné: %online%, %max%, %tps%, %playtime%, %time%, %memory%");
        cz.put("config.playerlist.footerLine1.desc", "Proměnné: %online%, %max%, %tps%, %playtime%, %time%, %memory%");
        cz.put("config.playerlist.footerLine2.desc", "Proměnné: %online%, %max%, %tps%, %playtime%, %time%, %memory%");
        cz.put("config.playerlist.footerLine3.desc", "Proměnné: %online%, %max%, %tps%, %playtime%, %time%, %memory%");
        cz.put("config.playerlist.enableCustomNames.desc", "Povolit formátování jmen hráčů v TABu");
        cz.put("config.playerlist.playerNameFormat.desc", "Proměnné: %rank_prefix%, %player_name%, %rank_suffix%");
        cz.put("config.playerlist.defaultPrefix.desc", "Výchozí prefix bez ranku. Barvy: &0-f, &#RRGGBB");
        cz.put("config.playerlist.defaultSuffix.desc", "Výchozí suffix bez ranku. Barvy: &0-f, &#RRGGBB");
        cz.put("config.playerlist.combineMultipleRanks.desc", "Kombinovat všechny ranky místo použití jen nejvyšší priority");
        cz.put("config.playerlist.updateIntervalSeconds.desc", "Jak často aktualizovat TAB seznam (sekundy)");
        
        // --- Stats descriptions ---
        cz.put("config.stats.enableStats.desc", "Povolit denní reporty statistik serveru");
        cz.put("config.stats.reportChannelId.desc", "ID Discord kanálu pro denní reporty");
        cz.put("config.stats.reportTime.desc", "Čas odeslání denního reportu (formát: HH:MM)");
        
        // --- Restart descriptions ---
        cz.put("config.restart.restartType.desc", "FIXED_TIME: V konkrétní časy | INTERVAL: Každých X hodin | DELAY: X minut od startu");
        cz.put("config.restart.fixedRestartTimes.desc", "Přesné časy restartů (formát: HH:MM)");
        cz.put("config.restart.intervalHours.desc", "Hodiny mezi automatickými restarty");
        cz.put("config.restart.delayMinutes.desc", "Minuty čekání před prvním restartem");
        
        // --- Announcement descriptions ---
        cz.put("config.announcement.prefix.desc", "Textový prefix před zprávou. Barvy: &0-f, &#RRGGBB");
        cz.put("config.announcement.announcementIntervalMinutes.desc", "Minuty mezi automatickými oznámeními");
        cz.put("config.announcement.announcements.desc", "Zprávy k vysílání. Barvy: &0-f (legacy), &#RRGGBB (hex)");
        
        // --- Tickets descriptions ---
        cz.put("config.tickets.enableTickets.desc", "Povolit Discord systém ticketů");
        cz.put("config.tickets.ticketCategoryId.desc", "ID Discord kategorie pro vytváření ticket kanálů");
        cz.put("config.tickets.supportRoleId.desc", "ID role s přístupem k ticketům");
        cz.put("config.tickets.ticketChannelTopic.desc", "Téma kanálu ticketu. Proměnné: %player%, %reason%");
        cz.put("config.tickets.maxTicketsPerUser.desc", "Maximum otevřených ticketů na hráče");
        cz.put("config.tickets.ticketCreatedMessage.desc", "Zpráva v MC při vytvoření ticketu. Proměnné: %channel%");
        cz.put("config.tickets.ticketWelcomeMessage.desc", "Uvítací zpráva v ticketu. Proměnné: %user%, %reason%");
        cz.put("config.tickets.ticketCloseMessage.desc", "Zpráva při uzavření ticketu");
        cz.put("config.tickets.noPermissionMessage.desc", "Zpráva při nedostatečných oprávněních");
        cz.put("config.tickets.ticketLimitReachedMessage.desc", "Zpráva při dosažení limitu. Proměnné: %max%");
        cz.put("config.tickets.ticketAlreadyClosedMessage.desc", "Zpráva když je ticket již zavřený");
        cz.put("config.tickets.enableTranscript.desc", "Uložit konverzaci při zavření ticketu");
        cz.put("config.tickets.transcriptFormat.desc", "TXT = čitelný text, JSON = strukturovaná data");
        cz.put("config.tickets.transcriptFilename.desc", "Šablona názvu souboru. Proměnné: %user%, %date%, %reason%");
        
        // --- Discord descriptions ---
        cz.put("config.discord.enableDiscord.desc", "Hlavní přepínač Discord integrace");
        cz.put("config.discord.botToken.desc", "Token bota z Discord Developer Portal (udržujte v tajnosti!)");
        cz.put("config.discord.guildId.desc", "ID Discord serveru (pravý klik na server → Kopírovat ID)");
        cz.put("config.discord.botActivityType.desc", "PLAYING, WATCHING, LISTENING, nebo COMPETING");
        cz.put("config.discord.botActivityText.desc", "Text ve statusu bota. Proměnné: %online%, %max%");
        cz.put("config.discord.enableWhitelist.desc", "Povolit vstup pouze propojeným Discord účtům");
        cz.put("config.discord.kickMessage.desc", "Zpráva pro nepropojené hráče. Proměnné: %code%");
        cz.put("config.discord.linkSuccessMessage.desc", "Zpráva při úspěšném propojení. Proměnné: %player%");
        cz.put("config.discord.alreadyLinkedMessage.desc", "Zpráva při dosažení limitu. Proměnné: %max%");
        cz.put("config.discord.maxAccountsPerDiscord.desc", "Maximum MC účtů na jeden Discord");
        cz.put("config.discord.chatChannelId.desc", "ID kanálu pro MC ↔ Discord chat");
        cz.put("config.discord.consoleChannelId.desc", "ID kanálu pro výstup konzole");
        cz.put("config.discord.statusChannelId.desc", "ID kanálu pro status zprávy serveru");
        cz.put("config.discord.linkChannelId.desc", "ID kanálu kde hráči zadávají kódy");
        cz.put("config.discord.linkedRoleId.desc", "ID role přidělené propojeným uživatelům");
        cz.put("config.discord.rolePrefixes.desc", "Nastavení prefixů/suffixů pro Discord role");
        cz.put("config.discord.syncBansDiscordToMc.desc", "Zabanovat MC hráče při banu z Discordu");
        cz.put("config.discord.syncBansMcToDiscord.desc", "Zabanovat Discord uživatele při banu v MC");
        cz.put("config.discord.enableChatBridge.desc", "Povolit obousměrný chat mezi MC a Discordem");
        cz.put("config.discord.minecraftToDiscordFormat.desc", "Formát MC→Discord. Proměnné: %player%, %message%");
        cz.put("config.discord.discordToMinecraftFormat.desc", "Formát Discord→MC. Proměnné: %user%, %message%. Barvy: &0-f");
        cz.put("config.discord.translateEmojis.desc", "Převádět Discord emoji na Unicode znaky");
        cz.put("config.discord.chatWebhookUrl.desc", "Webhook URL pro chat (zobrazí hlavy hráčů jako avatar)");
        cz.put("config.discord.enableConsoleLog.desc", "Odesílat výstup konzole na Discord");
        cz.put("config.discord.enableStatusMessages.desc", "Odesílat zprávy o startu/stopu serveru");
        cz.put("config.discord.statusMessageStarting.desc", "Zpráva když server startuje");
        cz.put("config.discord.statusMessageStarted.desc", "Zpráva když je server online");
        cz.put("config.discord.statusMessageStopping.desc", "Zpráva když se server vypíná");
        cz.put("config.discord.statusMessageStopped.desc", "Zpráva když je server offline");
        cz.put("config.discord.enableTopicUpdate.desc", "Aktualizovat téma kanálu s info o serveru");
        cz.put("config.discord.channelTopicFormat.desc", "Formát tématu. Proměnné: %online%, %max%, %tps%, %uptime%");
        cz.put("config.discord.uptimeFormat.desc", "Formát uptime. Proměnné: %d% (dny), %h% (hodiny), %m% (minuty)");
        cz.put("config.discord.invalidCodeMessage.desc", "Zpráva při zadání neplatného/expirovaného kódu");
        cz.put("config.discord.notLinkedMessage.desc", "Zpráva když uživatel není propojený");
        cz.put("config.discord.alreadyLinkedSingleMessage.desc", "Zpráva když už je propojený (1 účet). Proměnné: %uuid%");
        cz.put("config.discord.alreadyLinkedMultipleMessage.desc", "Zpráva při více propojených účtech. Proměnné: %count%");
        cz.put("config.discord.unlinkSuccessMessage.desc", "Zpráva při odpojení účtů");
        cz.put("config.discord.wrongGuildMessage.desc", "Zpráva při použití příkazu na špatném serveru");
        cz.put("config.discord.ticketCreatedMessage.desc", "Zpráva při vytvoření ticketu");
        cz.put("config.discord.ticketClosingMessage.desc", "Zpráva při zavírání ticketu");
        cz.put("config.discord.textChannelOnlyMessage.desc", "Zpráva když příkaz vyžaduje textový kanál");
        
        // --- Help text for color codes ---
        cz.put("help.colorcodes", "Barevné kódy: &0-9, &a-f (legacy), &#RRGGBB (hex). Formátování: &l (tučně), &o (kurzíva), &n (podtržení), &m (přeškrtnutí), &r (reset)");
        
        LANG.put("cz", cz);
    }
    
    // ========================================================================
    //                     KONSTRUKTOR A SINGLETON
    // ========================================================================
    
    /**
     * Privátní konstruktor pro singleton pattern
     */
    public WebManager() {
        instance = this;
    }
    
    /**
     * Získá singleton instanci WebManageru
     * @return Instance WebManageru
     */
    public static WebManager getInstance() {
        if (instance == null) {
            instance = new WebManager();
        }
        return instance;
    }
    
    /**
     * Nastaví referenci na Minecraft server
     * @param server Minecraft server instance
     */
    public void setServer(MinecraftServer server) {
        this.mcServer = server;
    }
    
    // ========================================================================
    //                     START / STOP SERVERU
    // ========================================================================
    
    /**
     * Spustí HTTP server pro web panel
     */
    public void start() {
        try {
            WebConfig config = WebConfig.getInstance();
            authToken = UUID.randomUUID().toString();
            
            server = HttpServer.create(new InetSocketAddress(config.getPort()), 0);
            
            // Registrace HTTP handlerů
            server.createContext("/", new DashboardHandler());
            server.createContext("/css/style.css", new StyleHandler());
            server.createContext("/api/action", new ActionHandler());
            server.createContext("/api/config", new ConfigApiHandler());
            server.createContext("/api/locale", new LocaleResetHandler());
            server.createContext("/api/stats/history", new StatsHistoryHandler());
            server.createContext("/api/discord/roles", new DiscordRolesHandler());
            
            server.setExecutor(null);
            server.start();
            
            LOGGER.info("Web Control Panel started on port {}", config.getPort());
            LOGGER.info("Access URL: {}", getWebUrl());
        } catch (IOException e) {
            LOGGER.error("Failed to start Web Control Panel", e);
        }
    }
    
    /**
     * Zastaví HTTP server
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            LOGGER.info("Web Control Panel stopped");
        }
    }
    
    /**
     * Vygeneruje URL pro přístup k web panelu
     * @return URL s autentizačním tokenem
     */
    public String getWebUrl() {
        WebConfig config = WebConfig.getInstance();
        String hostname = config.getPublicHostname();
        
        // Pokus o detekci IP adresy serveru
        if (("localhost".equals(hostname) || "127.0.0.1".equals(hostname)) && mcServer != null) {
            try {
                String serverIp = mcServer.getLocalIp();
                if (serverIp != null && !serverIp.isEmpty() && !"0.0.0.0".equals(serverIp)) {
                    hostname = serverIp;
                } else {
                    // Fallback na detekci z network interface
                    java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
                    while (interfaces.hasMoreElements()) {
                        java.net.NetworkInterface iface = interfaces.nextElement();
                        if (iface.isLoopback() || !iface.isUp()) continue;
                        
                        java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            java.net.InetAddress addr = addresses.nextElement();
                            if (addr instanceof java.net.Inet4Address) {
                                hostname = addr.getHostAddress();
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to detect server IP", e);
            }
        }
        
        return "http://" + hostname + ":" + config.getPort() + "/?token=" + authToken;
    }
    
    // ========================================================================
    //                     UTILITY METODY
    // ========================================================================
    
    /**
     * Získá překlad textu podle aktuálního jazyka
     * @param key Klíč překladu
     * @return Přeložený text nebo klíč pokud překlad neexistuje
     */
    private String t(String key) {
        String langCode = WebConfig.getInstance().getLanguage();
        return LANG.getOrDefault(langCode, LANG.get("en")).getOrDefault(key, key);
    }
    
    /**
     * Escapuje string pro bezpečné vložení do JSON
     * @param str Vstupní string
     * @return Escapovaný string
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Escapuje string pro bezpečné vložení do HTML
     * @param str Vstupní string
     * @return Escapovaný string
     */
    private String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    /**
     * Ověří, zda je request autentizován
     * @param exchange HTTP exchange
     * @return true pokud je autentizován
     */
    private boolean isAuthenticated(HttpExchange exchange) {
        // Kontrola tokenu v URL
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.contains("token=" + authToken)) {
            // Nastavit cookie pro budoucí requesty
            exchange.getResponseHeaders().set("Set-Cookie", "session=" + authToken + "; Path=/; HttpOnly");
            return true;
        }
        
        // Kontrola cookie
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        return cookie != null && cookie.contains("session=" + authToken) && authToken != null;
    }
    
    /**
     * Odešle HTTP response
     * @param exchange HTTP exchange
     * @param response Tělo odpovědi
     * @param code HTTP status kód
     */
    private void sendResponse(HttpExchange exchange, String response, int code) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    /**
     * Přesměruje na jinou URL
     * @param exchange HTTP exchange
     * @param location Cílová URL
     */
    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }
    
    /**
     * Parsuje URL-encoded parametry z těla requestu
     * @param body Tělo requestu
     * @return Mapa parametrů
     */
    private Map<String, String> parseParams(String body) {
        Map<String, String> params = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                params.put(kv[0], java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return params;
    }
    
    // ========================================================================
    //                     HTTP HANDLERY
    // ========================================================================
    // Každý handler zpracovává specifický endpoint
    // ========================================================================
    
    // TODO: Implementovat handlery v další části
    
    /**
     * Handler pro hlavní stránku (Dashboard)
     */
    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                String html = "<html><body style='background:#1a1a1a;color:#fff;font-family:sans-serif;text-align:center;padding-top:50px;'>" +
                              "<h1>" + t("unauthorized") + "</h1></body></html>";
                sendResponse(exchange, html, 401);
                return;
            }
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            String html = buildFullHtml();
            sendResponse(exchange, html, 200);
        }
    }
    
    /**
     * Handler pro CSS styly
     */
    private class StyleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/css");
            String css = buildCss();
            sendResponse(exchange, css, 200);
        }
    }
    
    /**
     * Handler pro akce (restart, kick, broadcast)
     */
    private class ActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseParams(body);
                String action = params.get("action");
                
                if ("restart".equals(action)) {
                    mcServer.execute(() -> mcServer.halt(false));
                } else if ("announce".equals(action)) {
                    String msg = params.get("message");
                    if (msg != null && !msg.isEmpty()) {
                        mcServer.execute(() -> mcServer.getPlayerList().broadcastSystemMessage(
                            Component.literal("§8[§bVoidium§8] §f" + msg.replace("&", "§")), false
                        ));
                    }
                } else if ("kick".equals(action)) {
                    String player = params.get("player");
                    ServerPlayer sp = mcServer.getPlayerList().getPlayerByName(player);
                    if (sp != null) {
                        mcServer.execute(() -> sp.connection.disconnect(Component.literal(t("kicked_by_admin"))));
                    }
                }
                
                redirect(exchange, "/");
            }
        }
    }
    
    /**
     * Handler pro ukládání konfigurace
     */
    private class ConfigApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            
            // GET - vrátí aktuální konfiguraci
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                try {
                    String configJson = buildConfigJson();
                    sendResponse(exchange, configJson, 200);
                } catch (Exception e) {
                    LOGGER.error("Failed to get config", e);
                    sendResponse(exchange, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}", 500);
                }
                return;
            }
            
            // POST - uloží konfiguraci
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                try {
                    JsonObject json = JsonParser.parseReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)
                    ).getAsJsonObject();
                    
                    List<String> changedFiles = new ArrayList<>();
                    
                    // Formát: {section: 'web', data: {...}} NEBO {web: {...}, general: {...}, ...}
                    String section = json.has("section") ? json.get("section").getAsString() : null;
                    JsonObject data = json.has("data") ? json.getAsJsonObject("data") : null;
                    
                    // Pokud je to section/data formát
                    if (section != null && data != null) {
                        switch (section) {
                            case "web":
                                if (data.has("language")) {
                                    Field f = WebConfig.class.getDeclaredField("language");
                                    f.setAccessible(true);
                                    f.set(WebConfig.getInstance(), data.get("language").getAsString());
                                    WebConfig.getInstance().save();
                                    changedFiles.add("WebConfig");
                                }
                                break;
                            case "general":
                                if (updateConfig(GeneralConfig.class, GeneralConfig.getInstance(), data)) 
                                    changedFiles.add("GeneralConfig");
                                break;
                            case "restart":
                                if (updateConfig(RestartConfig.class, RestartConfig.getInstance(), data)) 
                                    changedFiles.add("RestartConfig");
                                break;
                            case "announcement":
                                if (updateConfig(AnnouncementConfig.class, AnnouncementConfig.getInstance(), data)) 
                                    changedFiles.add("AnnouncementConfig");
                                break;
                            case "discord":
                                if (updateConfig(DiscordConfig.class, DiscordConfig.getInstance(), data)) 
                                    changedFiles.add("DiscordConfig");
                                break;
                            case "stats":
                                if (updateConfig(StatsConfig.class, StatsConfig.getInstance(), data)) 
                                    changedFiles.add("StatsConfig");
                                break;
                            case "vote":
                                if (updateConfig(VoteConfig.class, VoteConfig.getInstance(), data)) 
                                    changedFiles.add("VoteConfig");
                                break;
                            case "ranks":
                                if (updateConfig(RanksConfig.class, RanksConfig.getInstance(), data)) 
                                    changedFiles.add("RanksConfig");
                                break;
                            case "tickets":
                                if (updateConfig(TicketConfig.class, TicketConfig.getInstance(), data)) 
                                    changedFiles.add("TicketConfig");
                                break;
                            case "playerlist":
                                if (updateConfig(PlayerListConfig.class, PlayerListConfig.getInstance(), data)) 
                                    changedFiles.add("PlayerListConfig");
                                break;
                        }
                    } else {
                        // Starý formát - {web: {...}, general: {...}, ...}
                        // Web Config - jazyk
                        if (json.has("web")) {
                            JsonObject webJson = json.getAsJsonObject("web");
                            if (webJson.has("language")) {
                                Field f = WebConfig.class.getDeclaredField("language");
                                f.setAccessible(true);
                                f.set(WebConfig.getInstance(), webJson.get("language").getAsString());
                                WebConfig.getInstance().save();
                                changedFiles.add("WebConfig");
                            }
                        }
                        
                        // Ostatní configy
                        if (updateConfig(GeneralConfig.class, GeneralConfig.getInstance(), json.get("general"))) 
                            changedFiles.add("GeneralConfig");
                        if (updateConfig(RestartConfig.class, RestartConfig.getInstance(), json.get("restart"))) 
                            changedFiles.add("RestartConfig");
                        if (updateConfig(AnnouncementConfig.class, AnnouncementConfig.getInstance(), json.get("announcement"))) 
                            changedFiles.add("AnnouncementConfig");
                        if (updateConfig(DiscordConfig.class, DiscordConfig.getInstance(), json.get("discord"))) 
                            changedFiles.add("DiscordConfig");
                        if (updateConfig(StatsConfig.class, StatsConfig.getInstance(), json.get("stats"))) 
                            changedFiles.add("StatsConfig");
                        if (updateConfig(VoteConfig.class, VoteConfig.getInstance(), json.get("vote"))) 
                            changedFiles.add("VoteConfig");
                        if (updateConfig(RanksConfig.class, RanksConfig.getInstance(), json.get("ranks"))) 
                            changedFiles.add("RanksConfig");
                        if (updateConfig(TicketConfig.class, TicketConfig.getInstance(), json.get("tickets"))) 
                            changedFiles.add("TicketConfig");
                        if (updateConfig(PlayerListConfig.class, PlayerListConfig.getInstance(), json.get("playerlist"))) 
                            changedFiles.add("PlayerListConfig");
                    }
                    
                    sendResponse(exchange, "{\"status\":\"ok\"}", 200);
                } catch (Exception e) {
                    LOGGER.error("Failed to update config", e);
                    sendResponse(exchange, "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}", 500);
                }
            }
        }
        
        /**
         * Aktualizuje config pomocí reflexe
         */
        private <T> boolean updateConfig(Class<T> clazz, T instance, JsonElement json) {
            if (json == null || instance == null) return false;
            try {
                T temp = GSON.fromJson(json, clazz);
                for (Field field : clazz.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
                    field.setAccessible(true);
                    Object newValue = field.get(temp);
                    if (newValue != null) {
                        field.set(instance, newValue);
                    }
                }
                clazz.getMethod("save").invoke(instance);
                return true;
            } catch (Exception e) {
                LOGGER.error("Error updating " + clazz.getSimpleName(), e);
                return false;
            }
        }
    }
    
    /**
     * Handler pro reset locale
     */
    private class LocaleResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendResponse(exchange, "{\"status\":\"error\",\"message\":\"Unauthorized\"}", 401);
                return;
            }
            
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                try {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    
                    // Parse JSON body
                    JsonObject json = GSON.fromJson(body, JsonObject.class);
                    String locale = json.has("locale") ? json.get("locale").getAsString() : null;
                    
                    if (locale == null || (!locale.equals("en") && !locale.equals("cz"))) {
                        sendResponse(exchange, "{\"status\":\"error\",\"message\":\"Invalid locale\"}", 400);
                        return;
                    }
                    
                    // Aplikovat locale na všechny configy
                    GeneralConfig.getInstance().applyLocale(locale);
                    AnnouncementConfig.getInstance().applyLocale(locale);
                    DiscordConfig.getInstance().applyLocale(locale);
                    RanksConfig.getInstance().applyLocale(locale);
                    VoteConfig.getInstance().applyLocale(locale);
                    TicketConfig.getInstance().applyLocale(locale);
                    
                    sendResponse(exchange, "{\"status\":\"ok\"}", 200);
                } catch (Exception e) {
                    LOGGER.error("Failed to reset locale", e);
                    sendResponse(exchange, "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}", 500);
                }
            }
        }
    }
    
    /**
     * Handler pro historii statistik
     */
    private class StatsHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            List<StatsManager.DataPoint> history = StatsManager.getInstance().getHistory();
            String json = GSON.toJson(history);
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, json, 200);
        }
    }
    
    /**
     * Handler pro Discord role
     */
    private class DiscordRolesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendResponse(exchange, "Unauthorized", 401);
                return;
            }
            
            try {
                List<Role> roles = DiscordManager.getInstance().getRoles();
                List<Map<String, String>> roleList = new ArrayList<>();
                
                for (Role role : roles) {
                    Map<String, String> map = new HashMap<>();
                    map.put("id", role.getId());
                    map.put("name", role.getName());
                    String hexColor = role.getColor() != null 
                        ? String.format("#%06x", role.getColor().getRGB() & 0xFFFFFF) 
                        : "#99aab5";
                    map.put("color", hexColor);
                    map.put("colorHex", hexColor.equals("#000000") ? "" : "&#" + hexColor.substring(1).toUpperCase());
                    roleList.add(map);
                }
                
                String json = GSON.toJson(roleList);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                sendResponse(exchange, json, 200);
            } catch (Exception e) {
                LOGGER.error("Failed to fetch Discord roles", e);
                sendResponse(exchange, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}", 500);
            }
        }
    }
    
    // ========================================================================
    //                     HTML GENERÁTORY
    // ========================================================================
    // TODO: Implementovat v další části
    // ========================================================================
    
    /**
     * Sestaví kompletní HTML stránku
     * @return HTML string
     */
    private String buildFullHtml() {
        StringBuilder sb = new StringBuilder();
        
        // Získat aktuální jazyk z konfigurace
        String lang = "en";
        if (WebConfig.getInstance() != null && WebConfig.getInstance().getLanguage() != null) {
            lang = WebConfig.getInstance().getLanguage();
        }
        
        // =====================================================================
        // HTML HEAD
        // =====================================================================
        sb.append("<!DOCTYPE html><html><head>");
        sb.append("<title>").append(t("title")).append("</title>");
        sb.append("<meta charset='UTF-8'>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<link rel='stylesheet' href='/css/style.css'>");
        sb.append("<link rel='preconnect' href='https://fonts.googleapis.com'>");
        sb.append("<link rel='preconnect' href='https://fonts.gstatic.com' crossorigin>");
        sb.append("<link href='https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap' rel='stylesheet'>");
        
        // Základní JS funkce v head (aby byly dostupné hned)
        sb.append("<script>");
        sb.append("function switchTab(tabId){");
        sb.append("  document.querySelectorAll('.tab').forEach(t=>t.classList.remove('active'));");
        sb.append("  document.querySelectorAll('.tab-content').forEach(c=>c.classList.remove('active'));");
        sb.append("  event.target.classList.add('active');");
        sb.append("  document.getElementById(tabId).classList.add('active');");
        sb.append("  var bar=document.getElementById('sticky-save-bar');");
        sb.append("  if(bar)bar.style.display=tabId==='config'?'flex':'none';");
        sb.append("}");
        sb.append("function toggleCard(el){");
        sb.append("  var card=el.closest('.card');");
        sb.append("  if(card)card.classList.toggle('collapsed');");
        sb.append("}");
        sb.append("</script>");
        sb.append("</head><body>");
        
        // =====================================================================
        // HEADER
        // =====================================================================
        sb.append("<header>");
        sb.append("<h1>⚡ Voidium Control Panel</h1>");
        sb.append("<span class='status-badge'>● ").append(t("online")).append("</span>");
        sb.append("</header>");
        
        sb.append("<div class='container'>");
        
        // =====================================================================
        // TABY (DASHBOARD / CONFIG)
        // =====================================================================
        sb.append("<div class='tabs'>");
        sb.append("<div class='tab active' onclick='switchTab(\"dashboard\")'>📊 ").append(t("dashboard")).append("</div>");
        sb.append("<div class='tab' onclick='switchTab(\"config\")'>⚙️ ").append(t("config")).append("</div>");
        sb.append("</div>");
        
        // =====================================================================
        // DASHBOARD TAB
        // =====================================================================
        sb.append("<div id='dashboard' class='tab-content active'>");
        sb.append(buildDashboardContent());
        sb.append("</div>");
        
        // =====================================================================
        // CONFIG TAB
        // =====================================================================
        sb.append("<div id='config' class='tab-content'>");
        sb.append(buildConfigContent());
        sb.append("</div>");
        
        // =====================================================================
        // STICKY SAVE BAR
        // =====================================================================
        sb.append("<div class='sticky-save-bar' style='display:none;' id='sticky-save-bar'>");
        sb.append("<button id='save-btn' class='success' onclick='saveConfig()'>💾 ").append(t("save")).append("</button>");
        sb.append("<button class='danger' onclick='location.reload()'>❌ ").append(t("cancel")).append("</button>");
        sb.append("</div>");
        
        sb.append("</div>"); // container
        
        // =====================================================================
        // CONFIG DATA (JSON)
        // =====================================================================
        String configJson = buildConfigJson();
        sb.append("<script type='application/json' id='config-data'>").append(configJson.replace("</", "<\\/")).append("</script>");
        
        // =====================================================================
        // JAVASCRIPT
        // =====================================================================
        sb.append(buildJavaScript(lang));
        
        sb.append("</body></html>");
        return sb.toString();
    }
    
    /**
     * Sestaví CSS styly
     * @return CSS string
     */
    private String buildCss() {
        StringBuilder css = new StringBuilder();
        
        // =====================================================================
        // RESET A ZÁKLADNÍ STYLY
        // =====================================================================
        css.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
        css.append("body {");
        css.append("  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;");
        css.append("  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);");
        css.append("  color: #e0e0e0;");
        css.append("  min-height: 100vh;");
        css.append("  overflow-x: hidden;");
        css.append("}");
        
        // =====================================================================
        // KONTEJNER
        // =====================================================================
        css.append(".container {");
        css.append("  max-width: 1400px;");
        css.append("  margin: 0 auto;");
        css.append("  padding: 20px;");
        css.append("  padding-bottom: 100px;"); // Prostor pro sticky bar
        css.append("}");
        
        // =====================================================================
        // HEADER
        // =====================================================================
        css.append("header {");
        css.append("  background: rgba(0,0,0,0.5);");
        css.append("  backdrop-filter: blur(20px);");
        css.append("  padding: 25px 30px;");
        css.append("  border-bottom: 2px solid rgba(138, 43, 226, 0.3);");
        css.append("  display: flex;");
        css.append("  justify-content: space-between;");
        css.append("  align-items: center;");
        css.append("  margin-bottom: 30px;");
        css.append("  border-radius: 0 0 20px 20px;");
        css.append("  box-shadow: 0 10px 40px rgba(0,0,0,0.4);");
        css.append("}");
        css.append("h1 {");
        css.append("  color: #bb86fc;");
        css.append("  font-weight: 600;");
        css.append("  font-size: 2em;");
        css.append("  text-shadow: 0 0 20px rgba(187, 134, 252, 0.5);");
        css.append("  letter-spacing: -0.5px;");
        css.append("}");
        
        // =====================================================================
        // STATUS BADGE
        // =====================================================================
        css.append(".status-badge {");
        css.append("  background: linear-gradient(135deg, #00d4aa, #00e676);");
        css.append("  color: #000;");
        css.append("  padding: 8px 20px;");
        css.append("  border-radius: 30px;");
        css.append("  font-size: 0.9em;");
        css.append("  font-weight: 700;");
        css.append("  box-shadow: 0 4px 20px rgba(0, 212, 170, 0.4);");
        css.append("  animation: pulse-glow 2s ease-in-out infinite;");
        css.append("}");
        css.append("@keyframes pulse-glow {");
        css.append("  0%, 100% { box-shadow: 0 4px 20px rgba(0, 212, 170, 0.4); }");
        css.append("  50% { box-shadow: 0 4px 30px rgba(0, 212, 170, 0.7); }");
        css.append("}");
        
        // =====================================================================
        // KARTY
        // =====================================================================
        css.append(".card {");
        css.append("  background: linear-gradient(145deg, rgba(30, 30, 50, 0.8), rgba(20, 20, 35, 0.9));");
        css.append("  backdrop-filter: blur(15px);");
        css.append("  border: 1px solid rgba(187, 134, 252, 0.1);");
        css.append("  border-radius: 20px;");
        css.append("  padding: 30px;");
        css.append("  margin-bottom: 25px;");
        css.append("  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.05);");
        css.append("  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);");
        css.append("  position: relative;");
        css.append("  overflow: hidden;");
        css.append("}");
        css.append(".card::before {");
        css.append("  content: '';");
        css.append("  position: absolute;");
        css.append("  top: 0;");
        css.append("  left: -100%;");
        css.append("  width: 100%;");
        css.append("  height: 100%;");
        css.append("  background: linear-gradient(90deg, transparent, rgba(187, 134, 252, 0.1), transparent);");
        css.append("  transition: left 0.5s;");
        css.append("}");
        css.append(".card:hover::before { left: 100%; }");
        css.append(".card:hover {");
        css.append("  transform: translateY(-5px);");
        css.append("  border-color: rgba(187, 134, 252, 0.3);");
        css.append("  box-shadow: 0 15px 50px rgba(187, 134, 252, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.1);");
        css.append("}");
        css.append(".card h2 {");
        css.append("  margin: 0 0 20px 0;");
        css.append("  color: #bb86fc;");
        css.append("  border-bottom: 2px solid rgba(187, 134, 252, 0.2);");
        css.append("  padding-bottom: 15px;");
        css.append("  font-size: 1.5em;");
        css.append("  font-weight: 600;");
        css.append("  display: flex;");
        css.append("  align-items: center;");
        css.append("  gap: 10px;");
        css.append("}");
        css.append(".card h2::before {");
        css.append("  content: '●';");
        css.append("  font-size: 0.6em;");
        css.append("  color: #00d4aa;");
        css.append("  animation: blink 2s ease-in-out infinite;");
        css.append("}");
        css.append("@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }");
        
        // =====================================================================
        // COLLAPSIBLE KARTY
        // =====================================================================
        css.append(".collapsible-card { cursor: pointer; user-select: none; }");
        css.append(".collapsible-card h2::after {");
        css.append("  content: '▼';");
        css.append("  float: right;");
        css.append("  font-size: 0.7em;");
        css.append("  transition: transform 0.3s;");
        css.append("}");
        css.append(".collapsible-card.collapsed h2::after { transform: rotate(-90deg); }");
        css.append(".collapsible-card .card-body {");
        css.append("  max-height: 5000px;");
        css.append("  overflow: hidden;");
        css.append("  transition: max-height 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275), opacity 0.3s;");
        css.append("}");
        css.append(".collapsible-card.collapsed .card-body { max-height: 0; opacity: 0; }");
        
        // =====================================================================
        // STATISTIKY GRID
        // =====================================================================
        css.append(".stat-grid {");
        css.append("  display: grid;");
        css.append("  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));");
        css.append("  gap: 20px;");
        css.append("  margin-top: 20px;");
        css.append("}");
        css.append(".stat-item {");
        css.append("  background: rgba(0, 0, 0, 0.3);");
        css.append("  padding: 20px;");
        css.append("  border-radius: 15px;");
        css.append("  border: 1px solid rgba(255, 255, 255, 0.05);");
        css.append("  transition: all 0.3s;");
        css.append("}");
        css.append(".stat-item:hover {");
        css.append("  background: rgba(187, 134, 252, 0.1);");
        css.append("  border-color: rgba(187, 134, 252, 0.3);");
        css.append("  transform: scale(1.05);");
        css.append("}");
        css.append(".stat-label {");
        css.append("  font-size: 0.85em;");
        css.append("  color: #aaa;");
        css.append("  text-transform: uppercase;");
        css.append("  letter-spacing: 1px;");
        css.append("  margin-bottom: 8px;");
        css.append("}");
        css.append(".stat-value {");
        css.append("  font-size: 2em;");
        css.append("  font-weight: 700;");
        css.append("  color: #fff;");
        css.append("  text-shadow: 0 2px 10px rgba(187, 134, 252, 0.3);");
        css.append("}");
        css.append(".stat-value.highlight {");
        css.append("  color: #00d4aa;");
        css.append("  text-shadow: 0 2px 10px rgba(0, 212, 170, 0.5);");
        css.append("}");
        
        // =====================================================================
        // FORMULÁŘOVÉ PRVKY
        // =====================================================================
        css.append("input[type='text'], input[type='number'], input[type='password'], select, input[type='time'], textarea {");
        css.append("  width: 100%;");
        css.append("  padding: 14px;");
        css.append("  margin: 8px 0 20px;");
        css.append("  background: rgba(0, 0, 0, 0.4);");
        css.append("  border: 2px solid rgba(187, 134, 252, 0.2);");
        css.append("  color: #fff;");
        css.append("  border-radius: 12px;");
        css.append("  box-sizing: border-box;");
        css.append("  transition: all 0.3s;");
        css.append("  font-family: inherit;");
        css.append("  font-size: 1em;");
        css.append("}");
        css.append("input:focus, select:focus, textarea:focus {");
        css.append("  outline: none;");
        css.append("  border-color: #bb86fc;");
        css.append("  background: rgba(187, 134, 252, 0.05);");
        css.append("  box-shadow: 0 0 20px rgba(187, 134, 252, 0.2);");
        css.append("}");
        css.append("select {");
        css.append("  cursor: pointer;");
        css.append("  appearance: none;");
        css.append("  -webkit-appearance: none;");
        css.append("  -moz-appearance: none;");
        css.append("  background-image: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23bb86fc' d='M6 8L1 3h10z'/%3E%3C/svg%3E\");");
        css.append("  background-repeat: no-repeat;");
        css.append("  background-position: right 12px center;");
        css.append("  padding-right: 36px;");
        css.append("}");
        css.append("select option {");
        css.append("  background: #1e1e2e;");
        css.append("  color: #fff;");
        css.append("  padding: 10px;");
        css.append("}");
        css.append("select option:hover, select option:checked {");
        css.append("  background: #2d2d44;");
        css.append("  color: #bb86fc;");
        css.append("}");
        css.append("textarea { min-height: 100px; resize: vertical; }");
        css.append(".form-group { margin-bottom: 25px; }");
        css.append(".form-group label {");
        css.append("  display: block;");
        css.append("  margin-bottom: 10px;");
        css.append("  color: #e0e0e0;");
        css.append("  font-weight: 600;");
        css.append("  font-size: 0.95em;");
        css.append("}");
        
        // =====================================================================
        // TLAČÍTKA
        // =====================================================================
        css.append("button {");
        css.append("  background: linear-gradient(135deg, #6200ea, #b388ff);");
        css.append("  color: #fff;");
        css.append("  border: none;");
        css.append("  padding: 14px 30px;");
        css.append("  border-radius: 12px;");
        css.append("  cursor: pointer;");
        css.append("  font-weight: 700;");
        css.append("  text-transform: uppercase;");
        css.append("  letter-spacing: 1.5px;");
        css.append("  transition: all 0.3s;");
        css.append("  box-shadow: 0 5px 20px rgba(98, 0, 234, 0.4);");
        css.append("  font-size: 0.9em;");
        css.append("  font-family: inherit;");
        css.append("}");
        css.append("button:hover:not(:disabled) {");
        css.append("  transform: translateY(-3px);");
        css.append("  box-shadow: 0 8px 30px rgba(98, 0, 234, 0.6);");
        css.append("}");
        css.append("button:active:not(:disabled) { transform: translateY(-1px); }");
        css.append("button:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }");
        css.append("button.danger {");
        css.append("  background: linear-gradient(135deg, #d32f2f, #f44336);");
        css.append("  box-shadow: 0 5px 20px rgba(211, 47, 47, 0.4);");
        css.append("}");
        css.append("button.danger:hover:not(:disabled) { box-shadow: 0 8px 30px rgba(211, 47, 47, 0.6); }");
        css.append("button.success {");
        css.append("  background: linear-gradient(135deg, #00b09b, #96c93d);");
        css.append("  box-shadow: 0 5px 20px rgba(0, 176, 155, 0.4);");
        css.append("}");
        css.append("button.success:hover:not(:disabled) { box-shadow: 0 8px 30px rgba(0, 176, 155, 0.6); }");
        
        // =====================================================================
        // SWITCH (TOGGLE)
        // =====================================================================
        css.append(".switch {");
        css.append("  position: relative;");
        css.append("  display: inline-block;");
        css.append("  width: 60px;");
        css.append("  height: 30px;");
        css.append("  float: right;");
        css.append("}");
        css.append(".switch input { opacity: 0; width: 0; height: 0; }");
        css.append(".slider {");
        css.append("  position: absolute;");
        css.append("  cursor: pointer;");
        css.append("  top: 0; left: 0; right: 0; bottom: 0;");
        css.append("  background-color: rgba(255,255,255,0.1);");
        css.append("  transition: .4s;");
        css.append("  border-radius: 30px;");
        css.append("  border: 2px solid rgba(255,255,255,0.2);");
        css.append("}");
        css.append(".slider:before {");
        css.append("  position: absolute;");
        css.append("  content: '';");
        css.append("  height: 22px;");
        css.append("  width: 22px;");
        css.append("  left: 3px;");
        css.append("  bottom: 2px;");
        css.append("  background: linear-gradient(135deg, #fff, #f0f0f0);");
        css.append("  transition: .4s;");
        css.append("  border-radius: 50%;");
        css.append("  box-shadow: 0 2px 5px rgba(0,0,0,0.3);");
        css.append("}");
        css.append("input:checked + .slider {");
        css.append("  background: linear-gradient(135deg, #bb86fc, #9c6fd9);");
        css.append("  border-color: #bb86fc;");
        css.append("}");
        css.append("input:checked + .slider:before { transform: translateX(28px); }");
        
        // =====================================================================
        // TABY
        // =====================================================================
        css.append(".tabs {");
        css.append("  display: flex;");
        css.append("  gap: 10px;");
        css.append("  margin-bottom: 30px;");
        css.append("  background: rgba(0,0,0,0.3);");
        css.append("  padding: 8px;");
        css.append("  border-radius: 15px;");
        css.append("  border: 1px solid rgba(187, 134, 252, 0.1);");
        css.append("}");
        css.append(".tab {");
        css.append("  padding: 14px 30px;");
        css.append("  cursor: pointer;");
        css.append("  color: #aaa;");
        css.append("  transition: all 0.3s;");
        css.append("  border-radius: 10px;");
        css.append("  flex: 1;");
        css.append("  text-align: center;");
        css.append("  font-weight: 600;");
        css.append("  text-transform: uppercase;");
        css.append("  letter-spacing: 1px;");
        css.append("  font-size: 0.9em;");
        css.append("}");
        css.append(".tab:hover:not(.active) { color: #fff; background: rgba(255,255,255,0.05); }");
        css.append(".tab.active {");
        css.append("  color: #fff;");
        css.append("  background: linear-gradient(135deg, #6200ea, #b388ff);");
        css.append("  box-shadow: 0 5px 20px rgba(98, 0, 234, 0.4);");
        css.append("}");
        css.append(".tab-content { display: none; animation: fadeInUp 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275); }");
        css.append(".tab-content.active { display: block; }");
        css.append("@keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }");
        
        // =====================================================================
        // TABULKA
        // =====================================================================
        css.append("table { width: 100%; border-collapse: separate; border-spacing: 0 12px; }");
        css.append("thead { position: sticky; top: 0; z-index: 10; }");
        css.append("th, td { text-align: left; padding: 18px; }");
        css.append("tbody tr {");
        css.append("  background: rgba(187, 134, 252, 0.05);");
        css.append("  transition: all 0.3s;");
        css.append("  border-radius: 12px;");
        css.append("}");
        css.append("tbody tr:hover {");
        css.append("  background: rgba(187, 134, 252, 0.15);");
        css.append("  transform: scale(1.02);");
        css.append("  box-shadow: 0 5px 20px rgba(187, 134, 252, 0.2);");
        css.append("}");
        css.append("td:first-child { border-radius: 12px 0 0 12px; }");
        css.append("td:last-child { border-radius: 0 12px 12px 0; }");
        css.append("th {");
        css.append("  color: #bb86fc;");
        css.append("  font-weight: 700;");
        css.append("  text-transform: uppercase;");
        css.append("  font-size: 0.85em;");
        css.append("  letter-spacing: 1.5px;");
        css.append("  background: rgba(0,0,0,0.3);");
        css.append("  padding: 15px 18px;");
        css.append("}");
        css.append("thead tr th:first-child { border-radius: 12px 0 0 12px; }");
        css.append("thead tr th:last-child { border-radius: 0 12px 12px 0; }");
        
        // =====================================================================
        // POLE SEZNAMU (ARRAY ITEMS)
        // =====================================================================
        css.append(".array-item {");
        css.append("  display: flex;");
        css.append("  gap: 10px;");
        css.append("  margin-bottom: 10px;");
        css.append("  align-items: center;");
        css.append("}");
        css.append(".array-item input { margin: 0 !important; flex: 1; }");
        css.append(".array-item button { padding: 10px 15px; margin: 0; }");
        
        // =====================================================================
        // NO PLAYERS
        // =====================================================================
        css.append(".no-players {");
        css.append("  text-align: center;");
        css.append("  padding: 40px;");
        css.append("  color: #aaa;");
        css.append("  font-style: italic;");
        css.append("}");
        
        // =====================================================================
        // STICKY SAVE BAR
        // =====================================================================
        css.append(".sticky-save-bar {");
        css.append("  position: fixed;");
        css.append("  bottom: 0;");
        css.append("  left: 0;");
        css.append("  right: 0;");
        css.append("  background: rgba(0,0,0,0.95);");
        css.append("  backdrop-filter: blur(20px);");
        css.append("  padding: 20px;");
        css.append("  box-shadow: 0 -10px 40px rgba(0,0,0,0.8);");
        css.append("  z-index: 1000;");
        css.append("  border-top: 2px solid rgba(187,134,252,0.3);");
        css.append("  display: flex;");
        css.append("  justify-content: center;");
        css.append("  gap: 15px;");
        css.append("  animation: slideUp 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);");
        css.append("}");
        css.append("@keyframes slideUp { from { transform: translateY(100%); opacity: 0; } to { transform: translateY(0); opacity: 1; } }");
        css.append(".sticky-save-bar button { min-width: 200px; }");
        
        // =====================================================================
        // TOOLTIPS
        // =====================================================================
        css.append("[title] { position: relative; cursor: help; }");
        css.append("[title]:hover::after {");
        css.append("  content: attr(title);");
        css.append("  position: absolute;");
        css.append("  bottom: 100%;");
        css.append("  left: 50%;");
        css.append("  transform: translateX(-50%);");
        css.append("  background: rgba(0,0,0,0.95);");
        css.append("  color: #fff;");
        css.append("  padding: 8px 12px;");
        css.append("  border-radius: 8px;");
        css.append("  font-size: 0.85em;");
        css.append("  z-index: 1000;");
        css.append("  margin-bottom: 8px;");
        css.append("  box-shadow: 0 4px 20px rgba(0,0,0,0.5);");
        css.append("  border: 1px solid rgba(187,134,252,0.3);");
        css.append("  max-width: 300px;");
        css.append("  white-space: normal;");
        css.append("}");
        
        // =====================================================================
        // RESPONZIVNÍ DESIGN
        // =====================================================================
        css.append("@media (max-width: 768px) {");
        css.append("  .stat-grid { grid-template-columns: 1fr; }");
        css.append("  .tabs { flex-direction: column; }");
        css.append("  h1 { font-size: 1.5em; }");
        css.append("  .sticky-save-bar { flex-direction: column; }");
        css.append("  .sticky-save-bar button { min-width: unset; width: 100%; }");
        css.append("}");
        
        // =====================================================================
        // CONFIG SEKCE (pro JavaScript renderování)
        // =====================================================================
        css.append(".config-section {");
        css.append("  background: linear-gradient(145deg, rgba(30, 30, 50, 0.8), rgba(20, 20, 35, 0.9));");
        css.append("  backdrop-filter: blur(15px);");
        css.append("  border: 1px solid rgba(187, 134, 252, 0.1);");
        css.append("  border-radius: 20px;");
        css.append("  margin-bottom: 25px;");
        css.append("  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);");
        css.append("  overflow: hidden;");
        css.append("  transition: all 0.3s;");
        css.append("}");
        css.append(".config-section:hover {");
        css.append("  border-color: rgba(187, 134, 252, 0.3);");
        css.append("  box-shadow: 0 15px 50px rgba(187, 134, 252, 0.15);");
        css.append("}");
        css.append(".section-title {");
        css.append("  background: rgba(0, 0, 0, 0.4);");
        css.append("  color: #bb86fc;");
        css.append("  padding: 20px 25px;");
        css.append("  margin: 0;");
        css.append("  font-size: 1.2em;");
        css.append("  font-weight: 600;");
        css.append("  cursor: pointer;");
        css.append("  display: flex;");
        css.append("  justify-content: space-between;");
        css.append("  align-items: center;");
        css.append("  border-bottom: 1px solid rgba(187, 134, 252, 0.1);");
        css.append("  transition: all 0.3s;");
        css.append("}");
        css.append(".section-title:hover { background: rgba(187, 134, 252, 0.1); }");
        css.append(".section-title::after {");
        css.append("  content: '▼';");
        css.append("  font-size: 0.7em;");
        css.append("  transition: transform 0.3s;");
        css.append("  color: #aaa;");
        css.append("}");
        css.append(".config-section.collapsed .section-title::after { transform: rotate(-90deg); }");
        css.append(".section-content {");
        css.append("  padding: 25px;");
        css.append("  max-height: 5000px;");
        css.append("  overflow: hidden;");
        css.append("  transition: max-height 0.4s, padding 0.4s, opacity 0.3s;");
        css.append("}");
        css.append(".config-section.collapsed .section-content {");
        css.append("  max-height: 0;");
        css.append("  padding: 0 25px;");
        css.append("  opacity: 0;");
        css.append("}");
        
        // =====================================================================
        // FORM GROUP (lepší styling)
        // =====================================================================
        css.append(".form-group {");
        css.append("  margin-bottom: 20px;");
        css.append("  padding: 15px;");
        css.append("  background: rgba(0, 0, 0, 0.2);");
        css.append("  border-radius: 12px;");
        css.append("  border: 1px solid rgba(255, 255, 255, 0.05);");
        css.append("  transition: all 0.3s;");
        css.append("}");
        css.append(".form-group:hover {");
        css.append("  background: rgba(187, 134, 252, 0.05);");
        css.append("  border-color: rgba(187, 134, 252, 0.2);");
        css.append("}");
        css.append(".form-group label {");
        css.append("  display: block;");
        css.append("  margin-bottom: 10px;");
        css.append("  color: #e0e0e0;");
        css.append("  font-weight: 600;");
        css.append("  font-size: 0.95em;");
        css.append("}");
        css.append(".form-group input[type='text'],");
        css.append(".form-group input[type='number'],");
        css.append(".form-group input[type='password'],");
        css.append(".form-group input[type='time'],");
        css.append(".form-group select,");
        css.append(".form-group textarea {");
        css.append("  margin: 0;");
        css.append("}");
        css.append(".form-group input[type='checkbox'] {");
        css.append("  width: 20px;");
        css.append("  height: 20px;");
        css.append("  cursor: pointer;");
        css.append("  accent-color: #bb86fc;");
        css.append("}");
        css.append(".field-desc {");
        css.append("  display: block;");
        css.append("  margin-top: 8px;");
        css.append("  padding: 8px 12px;");
        css.append("  color: #a0a0a0;");
        css.append("  font-size: 0.85em;");
        css.append("  background: rgba(187, 134, 252, 0.08);");
        css.append("  border-left: 3px solid #bb86fc;");
        css.append("  border-radius: 4px;");
        css.append("}");
        
        // =====================================================================
        // TLAČÍTKO SAVE V SEKCI
        // =====================================================================
        css.append(".btn-save {");
        css.append("  background: linear-gradient(135deg, #00b09b, #96c93d);");
        css.append("  color: #fff;");
        css.append("  border: none;");
        css.append("  padding: 14px 40px;");
        css.append("  border-radius: 12px;");
        css.append("  cursor: pointer;");
        css.append("  font-weight: 700;");
        css.append("  text-transform: uppercase;");
        css.append("  letter-spacing: 1.5px;");
        css.append("  transition: all 0.3s;");
        css.append("  box-shadow: 0 5px 20px rgba(0, 176, 155, 0.4);");
        css.append("  margin-top: 20px;");
        css.append("  width: 100%;");
        css.append("}");
        css.append(".btn-save:hover {");
        css.append("  transform: translateY(-3px);");
        css.append("  box-shadow: 0 8px 30px rgba(0, 176, 155, 0.6);");
        css.append("}");
        
        // =====================================================================
        // LIST EDITOR (pro timelist, stringlist)
        // =====================================================================
        css.append(".list-editor {");
        css.append("  background: rgba(0, 0, 0, 0.2);");
        css.append("  border-radius: 12px;");
        css.append("  padding: 15px;");
        css.append("  border: 1px solid rgba(255, 255, 255, 0.05);");
        css.append("}");
        css.append(".list-item {");
        css.append("  display: flex;");
        css.append("  gap: 10px;");
        css.append("  margin-bottom: 10px;");
        css.append("  align-items: center;");
        css.append("}");
        css.append(".list-item input {");
        css.append("  flex: 1;");
        css.append("  margin: 0 !important;");
        css.append("}");
        css.append(".btn-small {");
        css.append("  padding: 8px 15px !important;");
        css.append("  font-size: 0.8em !important;");
        css.append("  min-width: auto !important;");
        css.append("}");
        css.append(".btn-danger {");
        css.append("  background: linear-gradient(135deg, #d32f2f, #f44336) !important;");
        css.append("  box-shadow: 0 3px 10px rgba(211, 47, 47, 0.3) !important;");
        css.append("}");
        css.append(".btn-add {");
        css.append("  background: linear-gradient(135deg, #6200ea, #b388ff) !important;");
        css.append("  margin-top: 10px;");
        css.append("}");
        
        // =====================================================================
        // ROLE PREFIX EDITOR - VYLEPŠENÝ
        // =====================================================================
        css.append(".role-prefix-editor {");
        css.append("  background: rgba(0, 0, 0, 0.2);");
        css.append("  border-radius: 12px;");
        css.append("  padding: 15px;");
        css.append("}");
        css.append(".role-prefix-item {");
        css.append("  display: grid;");
        css.append("  grid-template-columns: 200px 1fr 1fr auto;");
        css.append("  gap: 15px;");
        css.append("  margin-bottom: 12px;");
        css.append("  align-items: end;");
        css.append("  padding: 15px;");
        css.append("  background: rgba(0, 0, 0, 0.3);");
        css.append("  border-radius: 10px;");
        css.append("  border: 1px solid rgba(255,255,255,0.05);");
        css.append("}");
        css.append(".role-select-wrapper {");
        css.append("  display: flex;");
        css.append("  align-items: center;");
        css.append("  gap: 8px;");
        css.append("}");
        css.append(".role-select-wrapper select {");
        css.append("  flex: 1;");
        css.append("  margin: 0 !important;");
        css.append("}");
        css.append(".role-color-preview {");
        css.append("  width: 20px;");
        css.append("  height: 20px;");
        css.append("  border-radius: 50%;");
        css.append("  border: 2px solid rgba(255,255,255,0.3);");
        css.append("  flex-shrink: 0;");
        css.append("}");
        css.append(".prefix-group, .suffix-group {");
        css.append("  display: flex;");
        css.append("  flex-direction: column;");
        css.append("  gap: 5px;");
        css.append("}");
        css.append(".prefix-group label, .suffix-group label {");
        css.append("  font-size: 0.75em;");
        css.append("  color: #bb86fc;");
        css.append("  text-transform: uppercase;");
        css.append("  letter-spacing: 1px;");
        css.append("}");
        css.append(".input-with-color {");
        css.append("  display: flex;");
        css.append("  gap: 8px;");
        css.append("  align-items: center;");
        css.append("}");
        css.append(".input-with-color input[type='text'] {");
        css.append("  flex: 1;");
        css.append("  margin: 0 !important;");
        css.append("}");
        css.append(".color-picker {");
        css.append("  width: 36px;");
        css.append("  height: 36px;");
        css.append("  border: none;");
        css.append("  border-radius: 6px;");
        css.append("  cursor: pointer;");
        css.append("  padding: 0;");
        css.append("  background: transparent;");
        css.append("}");
        css.append(".color-picker::-webkit-color-swatch-wrapper { padding: 2px; }");
        css.append(".color-picker::-webkit-color-swatch { border-radius: 4px; border: 1px solid rgba(255,255,255,0.2); }");
        css.append("@media (max-width: 900px) {");
        css.append("  .role-prefix-item { grid-template-columns: 1fr; }");
        css.append("}");
        
        // =====================================================================
        // RANK LIST EDITOR
        // =====================================================================
        css.append(".rank-list-editor {");
        css.append("  background: rgba(0, 0, 0, 0.2);");
        css.append("  border-radius: 12px;");
        css.append("  padding: 15px;");
        css.append("}");
        css.append(".rank-item {");
        css.append("  display: grid;");
        css.append("  grid-template-columns: 1fr 100px 1fr 1fr auto;");
        css.append("  gap: 10px;");
        css.append("  margin-bottom: 10px;");
        css.append("  align-items: center;");
        css.append("  padding: 10px;");
        css.append("  background: rgba(0, 0, 0, 0.2);");
        css.append("  border-radius: 8px;");
        css.append("}");
        css.append(".rank-item input {");
        css.append("  margin: 0 !important;");
        css.append("}");
        css.append("@media (max-width: 768px) {");
        css.append("  .rank-item { grid-template-columns: 1fr; }");
        css.append("}");
        
        // =====================================================================
        // PLAYTIME RANK LIST EDITOR (type/value/hours)
        // =====================================================================
        css.append(".playtime-rank-list-editor {");
        css.append("  background: rgba(0, 0, 0, 0.2);");
        css.append("  border-radius: 12px;");
        css.append("  padding: 15px;");
        css.append("}");
        css.append(".playtime-rank-item {");
        css.append("  display: grid;");
        css.append("  grid-template-columns: 120px 1fr 100px auto;");
        css.append("  gap: 10px;");
        css.append("  margin-bottom: 10px;");
        css.append("  align-items: center;");
        css.append("  padding: 10px;");
        css.append("  background: rgba(0, 0, 0, 0.2);");
        css.append("  border-radius: 8px;");
        css.append("}");
        css.append(".playtime-rank-item select, .playtime-rank-item input {");
        css.append("  margin: 0 !important;");
        css.append("}");
        css.append(".playtime-rank-item select {");
        css.append("  background: rgba(0, 0, 0, 0.3);");
        css.append("  color: #bb86fc;");
        css.append("  font-weight: 600;");
        css.append("}");
        css.append("@media (max-width: 768px) {");
        css.append("  .playtime-rank-item { grid-template-columns: 1fr; }");
        css.append("}");
        
        // =====================================================================
        // LANGUAGE BUTTONS
        // =====================================================================
        css.append(".lang-buttons {");
        css.append("  display: flex;");
        css.append("  gap: 10px;");
        css.append("  margin: 5px 0;");
        css.append("}");
        css.append(".lang-btn {");
        css.append("  padding: 12px 24px;");
        css.append("  border: 2px solid rgba(187, 134, 252, 0.3);");
        css.append("  background: rgba(0, 0, 0, 0.3);");
        css.append("  color: #e0e0e0;");
        css.append("  border-radius: 8px;");
        css.append("  cursor: pointer;");
        css.append("  font-size: 1em;");
        css.append("  font-weight: 600;");
        css.append("  transition: all 0.3s;");
        css.append("}");
        css.append(".lang-btn:hover {");
        css.append("  background: rgba(187, 134, 252, 0.1);");
        css.append("  border-color: rgba(187, 134, 252, 0.5);");
        css.append("}");
        css.append(".lang-btn.active {");
        css.append("  background: linear-gradient(135deg, #6200ea, #b388ff);");
        css.append("  border-color: #bb86fc;");
        css.append("  color: #fff;");
        css.append("  box-shadow: 0 5px 20px rgba(98, 0, 234, 0.4);");
        css.append("}");
        
        // =====================================================================
        // TOAST NOTIFIKACE
        // =====================================================================
        css.append(".toast {");
        css.append("  position: fixed;");
        css.append("  bottom: 30px;");
        css.append("  right: 30px;");
        css.append("  padding: 15px 25px;");
        css.append("  border-radius: 12px;");
        css.append("  color: #fff;");
        css.append("  font-weight: 600;");
        css.append("  z-index: 10000;");
        css.append("  transform: translateY(100px);");
        css.append("  opacity: 0;");
        css.append("  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);");
        css.append("  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);");
        css.append("}");
        css.append(".toast.show {");
        css.append("  transform: translateY(0);");
        css.append("  opacity: 1;");
        css.append("}");
        css.append(".toast-success {");
        css.append("  background: linear-gradient(135deg, #00b09b, #96c93d);");
        css.append("}");
        css.append(".toast-error {");
        css.append("  background: linear-gradient(135deg, #d32f2f, #f44336);");
        css.append("}");
        css.append(".toast-info {");
        css.append("  background: linear-gradient(135deg, #6200ea, #b388ff);");
        css.append("}");
        
        // =====================================================================
        // DISCORD SECTION SUBHEADERS
        // =====================================================================
        css.append(".section-content h4 {");
        css.append("  color: #bb86fc;");
        css.append("  margin: 25px 0 15px 0;");
        css.append("  padding-bottom: 10px;");
        css.append("  border-bottom: 1px solid rgba(187, 134, 252, 0.2);");
        css.append("  font-size: 1.1em;");
        css.append("}");
        css.append(".section-content h4:first-child { margin-top: 0; }");
        
        return css.toString();
    }
    
    // ========================================================================
    //                     CONFIG JSON BUILDER
    // ========================================================================
    
    /**
     * Sestaví obsah Dashboard tabu
     * @return HTML string
     */
    private String buildDashboardContent() {
        StringBuilder sb = new StringBuilder();
        
        // Výpočet paměti
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        int memoryPercent = (int) ((usedMemory * 100) / maxMemory);
        
        // =====================================================================
        // GRAF HISTORIE
        // =====================================================================
        sb.append("<div class='card'>");
        sb.append("<h2>📈 Performance History</h2>");
        sb.append("<canvas id='statsChart' style='width:100%;height:300px;'></canvas>");
        sb.append("</div>");
        
        // =====================================================================
        // STATISTIKY SERVERU
        // =====================================================================
        sb.append("<div class='card'>");
        sb.append("<h2>").append(t("server_info")).append("</h2>");
        sb.append("<p style='color:#aaa;font-size:0.9em;margin:-10px 0 20px;'>").append(t("desc.server_info")).append("</p>");
        sb.append("<div class='stat-grid'>");
        
        // Hráči
        sb.append("<div class='stat-item'>");
        sb.append("<div class='stat-label'>").append(t("active_players")).append("</div>");
        sb.append("<div class='stat-value highlight'>").append(mcServer.getPlayerCount());
        sb.append("<span style='font-size:0.5em;color:#aaa;'>/").append(mcServer.getMaxPlayers()).append("</span></div>");
        sb.append("</div>");
        
        // Použitá paměť
        sb.append("<div class='stat-item'>");
        sb.append("<div class='stat-label'>").append(t("used_memory")).append("</div>");
        sb.append("<div class='stat-value'>").append(usedMemory).append("<span style='font-size:0.4em;color:#aaa;'> MB</span></div>");
        sb.append("<div style='background:rgba(0,0,0,0.3);height:8px;border-radius:10px;margin-top:10px;overflow:hidden;'>");
        sb.append("<div style='width:").append(memoryPercent).append("%;height:100%;background:linear-gradient(90deg,#00d4aa,#bb86fc);border-radius:10px;'></div>");
        sb.append("</div></div>");
        
        // Max paměť
        sb.append("<div class='stat-item'>");
        sb.append("<div class='stat-label'>").append(t("max_memory")).append("</div>");
        sb.append("<div class='stat-value'>").append(maxMemory).append("<span style='font-size:0.4em;color:#aaa;'> MB</span></div>");
        sb.append("</div>");
        
        // Volná paměť
        sb.append("<div class='stat-item'>");
        sb.append("<div class='stat-label'>").append(t("free_memory")).append("</div>");
        sb.append("<div class='stat-value'>").append(maxMemory - usedMemory).append("<span style='font-size:0.4em;color:#aaa;'> MB</span></div>");
        sb.append("</div>");
        
        sb.append("</div></div>");
        
        // =====================================================================
        // RYCHLÉ AKCE
        // =====================================================================
        sb.append("<div class='card'>");
        sb.append("<h2>").append(t("actions")).append("</h2>");
        sb.append("<p style='color:#aaa;font-size:0.9em;margin:-10px 0 20px;'>").append(t("desc.actions")).append("</p>");
        
        sb.append("<div style='display:flex;gap:15px;flex-wrap:wrap;margin-bottom:20px;'>");
        
        // Restart tlačítko
        sb.append("<form method='POST' action='/api/action' style='flex:1;min-width:200px;'>");
        sb.append("<input type='hidden' name='action' value='restart'>");
        sb.append("<button type='submit' class='danger' style='width:100%;' onclick='return confirm(\"");
        sb.append(t("confirm_action")).append("\")'>🔄 ").append(t("restart")).append("</button>");
        sb.append("</form>");
        
        // Refresh tlačítko
        sb.append("<button onclick='location.reload()' style='flex:1;min-width:200px;'>🔃 ").append(t("refresh")).append("</button>");
        sb.append("</div>");
        
        // Broadcast formulář
        sb.append("<form method='POST' action='/api/action' style='display:flex;gap:15px;'>");
        sb.append("<input type='hidden' name='action' value='announce'>");
        sb.append("<input type='text' name='message' placeholder='").append(t("message_placeholder")).append("' style='margin:0;flex:1;'>");
        sb.append("<button type='submit'>📢 ").append(t("send")).append("</button>");
        sb.append("</form>");
        sb.append("</div>");
        
        // =====================================================================
        // SEZNAM HRÁČŮ
        // =====================================================================
        sb.append("<div class='card'>");
        sb.append("<h2>").append(t("player_list")).append(" <span style='font-size:0.7em;color:#aaa;'>(").append(mcServer.getPlayerCount()).append(")</span></h2>");
        sb.append("<p style='color:#aaa;font-size:0.9em;margin:-10px 0 20px;'>").append(t("desc.player_list")).append("</p>");
        
        if (mcServer.getPlayerCount() == 0) {
            sb.append("<div class='no-players'>").append(t("no_players")).append("</div>");
        } else {
            sb.append("<table><thead><tr>");
            sb.append("<th>👤 Name</th>");
            sb.append("<th>🆔 UUID</th>");
            sb.append("<th>⚡ Action</th>");
            sb.append("</tr></thead><tbody>");
            
            for (ServerPlayer player : mcServer.getPlayerList().getPlayers()) {
                sb.append("<tr>");
                sb.append("<td><strong>").append(escapeHtml(player.getGameProfile().getName())).append("</strong></td>");
                sb.append("<td style='font-family:monospace;font-size:0.85em;color:#aaa;'>").append(player.getUUID()).append("</td>");
                sb.append("<td>");
                sb.append("<form method='POST' action='/api/action' style='display:inline;'>");
                sb.append("<input type='hidden' name='action' value='kick'>");
                sb.append("<input type='hidden' name='player' value='").append(escapeHtml(player.getGameProfile().getName())).append("'>");
                sb.append("<button type='submit' class='danger' style='padding:8px 16px;font-size:0.85em;' onclick='return confirm(\"");
                sb.append(t("kick")).append(" ").append(escapeHtml(player.getGameProfile().getName())).append("?\")'>❌ ").append(t("kick")).append("</button>");
                sb.append("</form>");
                sb.append("</td></tr>");
            }
            sb.append("</tbody></table>");
        }
        sb.append("</div>");
        
        return sb.toString();
    }
    
    /**
     * Sestaví obsah Config tabu
     * @return HTML string
     */
    private String buildConfigContent() {
        StringBuilder sb = new StringBuilder();
        
        // =====================================================================
        // RESET LOCALE SEKCE
        // =====================================================================
        sb.append("<div style='background:rgba(255,255,255,0.05);padding:15px;border-radius:8px;margin-bottom:20px;border:1px solid rgba(147,51,234,0.3);'>");
        sb.append("<h3 style='margin:0 0 10px 0;color:#c084fc;'>🌐 ").append(t("locale_reset_title")).append("</h3>");
        sb.append("<p style='margin:0 0 15px 0;opacity:0.8;font-size:0.9em;'>").append(t("locale_reset_description")).append("</p>");
        sb.append("<div style='display:flex;gap:10px;'>");
        sb.append("<button class='success' onclick='resetLocale(\"en\")' style='flex:1;padding:12px;'>🇬🇧 ").append(t("reset_to_english")).append("</button>");
        sb.append("<button class='success' onclick='resetLocale(\"cz\")' style='flex:1;padding:12px;'>🇨🇿 ").append(t("reset_to_czech")).append("</button>");
        sb.append("</div>");
        sb.append("</div>");
        
        // =====================================================================
        // CONFIG EDITOR (vyplní JavaScript)
        // =====================================================================
        sb.append("<div id='config-editor'>Loading configuration...</div>");
        
        return sb.toString();
    }
    
    /**
     * Sestaví JSON s konfigurací pro frontend
     * @return JSON string
     */
    private String buildConfigJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        // =====================================================================
        // WEB CONFIG
        // =====================================================================
        if (WebConfig.getInstance() != null) {
            json.append("\"web\":{");
            json.append("\"language\":\"").append(escapeJson(WebConfig.getInstance().getLanguage())).append("\",");
            json.append("\"port\":").append(WebConfig.getInstance().getPort());
            json.append("},");
        }
        
        // =====================================================================
        // GENERAL CONFIG
        // =====================================================================
        if (GeneralConfig.getInstance() != null) {
            GeneralConfig gc = GeneralConfig.getInstance();
            json.append("\"general\":{");
            json.append("\"enableMod\":").append(gc.isEnableMod()).append(",");
            json.append("\"enableRestarts\":").append(gc.isEnableRestarts()).append(",");
            json.append("\"enableAnnouncements\":").append(gc.isEnableAnnouncements()).append(",");
            json.append("\"enableSkinRestorer\":").append(gc.isEnableSkinRestorer()).append(",");
            json.append("\"enableDiscord\":").append(gc.isEnableDiscord()).append(",");
            json.append("\"enableWeb\":").append(gc.isEnableWeb()).append(",");
            json.append("\"enableStats\":").append(gc.isEnableStats()).append(",");
            json.append("\"enableRanks\":").append(gc.isEnableRanks()).append(",");
            json.append("\"enableVote\":").append(gc.isEnableVote()).append(",");
            json.append("\"enablePlayerList\":").append(gc.isEnablePlayerList()).append(",");
            json.append("\"skinCacheHours\":").append(gc.getSkinCacheHours());
            json.append("},");
        }
        
        // =====================================================================
        // RESTART CONFIG
        // =====================================================================
        if (RestartConfig.getInstance() != null) {
            RestartConfig rc = RestartConfig.getInstance();
            json.append("\"restart\":{");
            json.append("\"restartType\":\"").append(rc.getRestartType().name()).append("\",");
            json.append("\"intervalHours\":").append(rc.getIntervalHours()).append(",");
            json.append("\"delayMinutes\":").append(rc.getDelayMinutes()).append(",");
            json.append("\"fixedRestartTimes\":[");
            List<LocalTime> times = rc.getFixedRestartTimes();
            for (int i = 0; i < times.size(); i++) {
                json.append("\"").append(times.get(i).toString()).append("\"");
                if (i < times.size() - 1) json.append(",");
            }
            json.append("]},");
        }
        
        // =====================================================================
        // ANNOUNCEMENT CONFIG
        // =====================================================================
        if (AnnouncementConfig.getInstance() != null) {
            AnnouncementConfig ac = AnnouncementConfig.getInstance();
            json.append("\"announcement\":{");
            json.append("\"prefix\":\"").append(escapeJson(ac.getPrefix())).append("\",");
            json.append("\"announcementIntervalMinutes\":").append(ac.getAnnouncementIntervalMinutes()).append(",");
            json.append("\"announcements\":[");
            List<String> anns = ac.getAnnouncements();
            for (int i = 0; i < anns.size(); i++) {
                json.append("\"").append(escapeJson(anns.get(i))).append("\"");
                if (i < anns.size() - 1) json.append(",");
            }
            json.append("]},");
        }
        
        // =====================================================================
        // DISCORD CONFIG
        // =====================================================================
        if (DiscordConfig.getInstance() != null) {
            DiscordConfig dc = DiscordConfig.getInstance();
            json.append("\"discord\":{");
            json.append("\"enableDiscord\":").append(dc.isEnableDiscord()).append(",");
            json.append("\"botToken\":\"").append(escapeJson(dc.getBotToken())).append("\",");
            json.append("\"guildId\":\"").append(escapeJson(dc.getGuildId())).append("\",");
            json.append("\"botActivityType\":\"").append(escapeJson(dc.getBotActivityType())).append("\",");
            json.append("\"botActivityText\":\"").append(escapeJson(dc.getBotActivityText())).append("\",");
            json.append("\"enableWhitelist\":").append(dc.isEnableWhitelist()).append(",");
            json.append("\"kickMessage\":\"").append(escapeJson(dc.getKickMessage())).append("\",");
            json.append("\"linkSuccessMessage\":\"").append(escapeJson(dc.getLinkSuccessMessage())).append("\",");
            json.append("\"alreadyLinkedMessage\":\"").append(escapeJson(dc.getAlreadyLinkedMessage())).append("\",");
            json.append("\"maxAccountsPerDiscord\":").append(dc.getMaxAccountsPerDiscord()).append(",");
            json.append("\"chatChannelId\":\"").append(escapeJson(dc.getChatChannelId())).append("\",");
            json.append("\"consoleChannelId\":\"").append(escapeJson(dc.getConsoleChannelId())).append("\",");
            json.append("\"statusChannelId\":\"").append(escapeJson(dc.getStatusChannelId())).append("\",");
            json.append("\"linkChannelId\":\"").append(escapeJson(dc.getLinkChannelId())).append("\",");
            json.append("\"linkedRoleId\":\"").append(escapeJson(dc.getLinkedRoleId())).append("\",");
            json.append("\"syncBansDiscordToMc\":").append(dc.isSyncBansDiscordToMc()).append(",");
            json.append("\"syncBansMcToDiscord\":").append(dc.isSyncBansMcToDiscord()).append(",");
            json.append("\"enableChatBridge\":").append(dc.isEnableChatBridge()).append(",");
            json.append("\"minecraftToDiscordFormat\":\"").append(escapeJson(dc.getMinecraftToDiscordFormat())).append("\",");
            json.append("\"discordToMinecraftFormat\":\"").append(escapeJson(dc.getDiscordToMinecraftFormat())).append("\",");
            json.append("\"translateEmojis\":").append(dc.isTranslateEmojis()).append(",");
            json.append("\"chatWebhookUrl\":\"").append(escapeJson(dc.getChatWebhookUrl())).append("\",");
            json.append("\"enableConsoleLog\":").append(dc.isEnableConsoleLog()).append(",");
            json.append("\"enableStatusMessages\":").append(dc.isEnableStatusMessages()).append(",");
            json.append("\"statusMessageStarting\":\"").append(escapeJson(dc.getStatusMessageStarting())).append("\",");
            json.append("\"statusMessageStarted\":\"").append(escapeJson(dc.getStatusMessageStarted())).append("\",");
            json.append("\"statusMessageStopping\":\"").append(escapeJson(dc.getStatusMessageStopping())).append("\",");
            json.append("\"statusMessageStopped\":\"").append(escapeJson(dc.getStatusMessageStopped())).append("\",");
            json.append("\"enableTopicUpdate\":").append(dc.isEnableTopicUpdate()).append(",");
            json.append("\"channelTopicFormat\":\"").append(escapeJson(dc.getChannelTopicFormat())).append("\",");
            json.append("\"uptimeFormat\":\"").append(escapeJson(dc.getUptimeFormat())).append("\",");
            json.append("\"rolePrefixes\":").append(GSON.toJson(dc.getRolePrefixes()));
            json.append("},");
        }
        
        // =====================================================================
        // STATS CONFIG
        // =====================================================================
        if (StatsConfig.getInstance() != null) {
            StatsConfig sc = StatsConfig.getInstance();
            json.append("\"stats\":{");
            json.append("\"enableStats\":").append(sc.isEnableStats()).append(",");
            json.append("\"reportChannelId\":\"").append(escapeJson(sc.getReportChannelId())).append("\",");
            json.append("\"reportTime\":\"").append(sc.getReportTime().toString()).append("\"");
            json.append("},");
        }
        
        // =====================================================================
        // VOTE CONFIG
        // =====================================================================
        if (VoteConfig.getInstance() != null) {
            VoteConfig vc = VoteConfig.getInstance();
            json.append("\"vote\":{");
            json.append("\"enabled\":").append(vc.isEnabled()).append(",");
            json.append("\"host\":\"").append(escapeJson(vc.getHost())).append("\",");
            json.append("\"port\":").append(vc.getPort()).append(",");
            json.append("\"rsaPrivateKeyPath\":\"").append(escapeJson(vc.getRsaPrivateKeyPath())).append("\",");
            json.append("\"rsaPublicKeyPath\":\"").append(escapeJson(vc.getRsaPublicKeyPath())).append("\",");
            json.append("\"sharedSecret\":\"").append(escapeJson(vc.getSharedSecret())).append("\",");
            json.append("\"announceVotes\":").append(vc.isAnnounceVotes()).append(",");
            json.append("\"announcementMessage\":\"").append(escapeJson(vc.getAnnouncementMessage())).append("\",");
            json.append("\"announcementCooldown\":").append(vc.getAnnouncementCooldown()).append(",");
            json.append("\"commands\":[");
            List<String> cmds = vc.getCommands();
            for (int i = 0; i < cmds.size(); i++) {
                json.append("\"").append(escapeJson(cmds.get(i))).append("\"");
                if (i < cmds.size() - 1) json.append(",");
            }
            json.append("]},");
        }
        
        // =====================================================================
        // RANKS CONFIG
        // =====================================================================
        if (RanksConfig.getInstance() != null) {
            RanksConfig rk = RanksConfig.getInstance();
            json.append("\"ranks\":{");
            json.append("\"enableAutoRanks\":").append(rk.isEnableAutoRanks()).append(",");
            json.append("\"checkIntervalMinutes\":").append(rk.getCheckIntervalMinutes()).append(",");
            json.append("\"promotionMessage\":\"").append(escapeJson(rk.getPromotionMessage())).append("\",");
            json.append("\"ranks\":").append(GSON.toJson(rk.getRanks()));
            json.append("},");
        }
        
        // =====================================================================
        // TICKETS CONFIG
        // =====================================================================
        if (TicketConfig.getInstance() != null) {
            TicketConfig tc = TicketConfig.getInstance();
            json.append("\"tickets\":{");
            json.append("\"enableTickets\":").append(tc.isEnableTickets()).append(",");
            json.append("\"ticketCategoryId\":\"").append(escapeJson(tc.getTicketCategoryId())).append("\",");
            json.append("\"supportRoleId\":\"").append(escapeJson(tc.getSupportRoleId())).append("\",");
            json.append("\"ticketChannelTopic\":\"").append(escapeJson(tc.getTicketChannelTopic())).append("\",");
            json.append("\"maxTicketsPerUser\":").append(tc.getMaxTicketsPerUser()).append(",");
            json.append("\"ticketCreatedMessage\":\"").append(escapeJson(tc.getTicketCreatedMessage())).append("\",");
            json.append("\"ticketWelcomeMessage\":\"").append(escapeJson(tc.getTicketWelcomeMessage())).append("\",");
            json.append("\"ticketCloseMessage\":\"").append(escapeJson(tc.getTicketCloseMessage())).append("\",");
            json.append("\"noPermissionMessage\":\"").append(escapeJson(tc.getNoPermissionMessage())).append("\",");
            json.append("\"ticketLimitReachedMessage\":\"").append(escapeJson(tc.getTicketLimitReachedMessage())).append("\",");
            json.append("\"ticketAlreadyClosedMessage\":\"").append(escapeJson(tc.getTicketAlreadyClosedMessage())).append("\",");
            json.append("\"enableTranscript\":").append(tc.isEnableTranscript()).append(",");
            json.append("\"transcriptFormat\":\"").append(escapeJson(tc.getTranscriptFormat())).append("\",");
            json.append("\"transcriptFilename\":\"").append(escapeJson(tc.getTranscriptFilename())).append("\"");
            json.append("},");
        }
        
        // =====================================================================
        // PLAYERLIST CONFIG
        // =====================================================================
        if (PlayerListConfig.getInstance() != null) {
            PlayerListConfig pl = PlayerListConfig.getInstance();
            json.append("\"playerlist\":{");
            json.append("\"enableCustomPlayerList\":").append(pl.isEnableCustomPlayerList()).append(",");
            json.append("\"headerLine1\":\"").append(escapeJson(pl.getHeaderLine1())).append("\",");
            json.append("\"headerLine2\":\"").append(escapeJson(pl.getHeaderLine2())).append("\",");
            json.append("\"headerLine3\":\"").append(escapeJson(pl.getHeaderLine3())).append("\",");
            json.append("\"footerLine1\":\"").append(escapeJson(pl.getFooterLine1())).append("\",");
            json.append("\"footerLine2\":\"").append(escapeJson(pl.getFooterLine2())).append("\",");
            json.append("\"footerLine3\":\"").append(escapeJson(pl.getFooterLine3())).append("\",");
            json.append("\"enableCustomNames\":").append(pl.isEnableCustomNames()).append(",");
            json.append("\"playerNameFormat\":\"").append(escapeJson(pl.getPlayerNameFormat())).append("\",");
            json.append("\"defaultPrefix\":\"").append(escapeJson(pl.getDefaultPrefix())).append("\",");
            json.append("\"defaultSuffix\":\"").append(escapeJson(pl.getDefaultSuffix())).append("\",");
            json.append("\"combineMultipleRanks\":").append(pl.isCombineMultipleRanks()).append(",");
            json.append("\"updateIntervalSeconds\":").append(pl.getUpdateIntervalSeconds());
            json.append("}");
        }
        
        // Odstranit trailing čárku pokud je
        String result = json.toString();
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1);
        }
        result += "}";
        
        return result;
    }
    
    // =========================================================================
    // JAVASCRIPT - HLAVNÍ SKRIPT PRO FRONTEND
    // =========================================================================
    
    /**
     * Sestaví JavaScript pro konfigurační panel
     * Obsahuje: načítání konfigurace, renderování, ukládání, role editor, toast
     * @param lang Jazyk (en/cz)
     * @return JavaScript kód
     */
    private String buildJavaScript(String lang) {
        StringBuilder js = new StringBuilder();
        
        // Začátek script tagu
        js.append("<script>\n");
        
        // =====================================================================
        // GLOBÁLNÍ PROMĚNNÉ A INICIALIZACE
        // =====================================================================
        js.append("// ============================================================\n");
        js.append("// VOIDIUM WEB PANEL - JAVASCRIPT\n");
        js.append("// Generated by WebManager\n");
        js.append("// ============================================================\n\n");
        
        js.append("let configData = null;\n");
        js.append("let currentLang = '").append(lang).append("';\n");
        js.append("let discordRoles = [];\n\n");
        
        // =====================================================================
        // TRANSLATIONS OBJEKT
        // =====================================================================
        js.append("// Překlady pro JavaScript\n");
        js.append("const TRANSLATIONS = {\n");
        
        // Export ALL translations to JavaScript (not just a subset)
        Map<String, String> langMap = LANG.getOrDefault(lang, LANG.get("en"));
        Map<String, String> enMap = LANG.get("en");
        
        // Merge all keys from both current language and English fallback
        java.util.Set<String> allKeys = new java.util.HashSet<>();
        allKeys.addAll(langMap.keySet());
        allKeys.addAll(enMap.keySet());
        
        int keyIndex = 0;
        int totalKeys = allKeys.size();
        for (String key : allKeys) {
            String value = langMap.getOrDefault(key, enMap.get(key));
            if (value == null) value = key;
            js.append("    '").append(key).append("': '").append(escapeJs(value)).append("'");
            if (keyIndex < totalKeys - 1) js.append(",");
            js.append("\n");
            keyIndex++;
        }
        js.append("};\n\n");
        
        // =====================================================================
        // UTILITY FUNKCE
        // =====================================================================
        js.append("// ============================================================\n");
        js.append("// UTILITY FUNKCE\n");
        js.append("// ============================================================\n\n");
        
        js.append("function t(key) {\n");
        js.append("    return TRANSLATIONS[key] || key;\n");
        js.append("}\n\n");
        
        // Toast notifikace
        js.append("function showToast(message, type = 'info') {\n");
        js.append("    const toast = document.createElement('div');\n");
        js.append("    toast.className = 'toast toast-' + type;\n");
        js.append("    toast.textContent = message;\n");
        js.append("    document.body.appendChild(toast);\n");
        js.append("    setTimeout(() => toast.classList.add('show'), 10);\n");
        js.append("    setTimeout(() => {\n");
        js.append("        toast.classList.remove('show');\n");
        js.append("        setTimeout(() => toast.remove(), 300);\n");
        js.append("    }, 3000);\n");
        js.append("}\n\n");
        
        // =====================================================================
        // CONFIG LOADING
        // =====================================================================
        js.append("// ============================================================\n");
        js.append("// NAČÍTÁNÍ KONFIGURACE\n");
        js.append("// ============================================================\n\n");
        
        js.append("async function loadConfig() {\n");
        js.append("    try {\n");
        js.append("        const response = await fetch('/api/config');\n");
        js.append("        if (!response.ok) throw new Error('HTTP ' + response.status);\n");
        js.append("        configData = await response.json();\n");
        js.append("        renderConfig();\n");
        js.append("    } catch (e) {\n");
        js.append("        console.error('Config load error:', e);\n");
        js.append("        showToast(t('config.load.error'), 'error');\n");
        js.append("    }\n");
        js.append("}\n\n");
        
        // =====================================================================
        // DISCORD ROLES LOADING
        // =====================================================================
        js.append("async function loadDiscordRoles() {\n");
        js.append("    try {\n");
        js.append("        const response = await fetch('/api/discord/roles');\n");
        js.append("        if (response.ok) {\n");
        js.append("            discordRoles = await response.json();\n");
        js.append("        }\n");
        js.append("    } catch (e) {\n");
        js.append("        console.warn('Could not load Discord roles:', e);\n");
        js.append("    }\n");
        js.append("}\n\n");
        
        // =====================================================================
        // CONFIG RENDERING
        // =====================================================================
        js.append("// ============================================================\n");
        js.append("// RENDEROVÁNÍ KONFIGURACE\n");
        js.append("// ============================================================\n\n");
        
        js.append("function renderConfig() {\n");
        js.append("    const editor = document.getElementById('config-editor');\n");
        js.append("    if (!editor || !configData) return;\n");
        js.append("    \n");
        js.append("    let html = '';\n");
        js.append("    \n");
        
        // Render každé sekce
        js.append("    // Sekce: Web\n");
        js.append("    if (configData.web) {\n");
        js.append("        html += renderSection('web', t('config.section.web'), [\n");
        js.append("            {key: 'language', type: 'langbuttons', value: configData.web.language},\n");
        js.append("            {key: 'port', type: 'number', min: 1, max: 65535, value: configData.web.port}\n");
        js.append("        ]);\n");
        js.append("    }\n");
        js.append("    \n");
        
        js.append("    // Sekce: General\n");
        js.append("    if (configData.general) {\n");
        js.append("        html += renderSection('general', t('config.section.general'), [\n");
        js.append("            {key: 'enableMod', type: 'checkbox', value: configData.general.enableMod},\n");
        js.append("            {key: 'enableRestarts', type: 'checkbox', value: configData.general.enableRestarts},\n");
        js.append("            {key: 'enableAnnouncements', type: 'checkbox', value: configData.general.enableAnnouncements},\n");
        js.append("            {key: 'enableSkinRestorer', type: 'checkbox', value: configData.general.enableSkinRestorer},\n");
        js.append("            {key: 'enableDiscord', type: 'checkbox', value: configData.general.enableDiscord},\n");
        js.append("            {key: 'enableWeb', type: 'checkbox', value: configData.general.enableWeb},\n");
        js.append("            {key: 'enableStats', type: 'checkbox', value: configData.general.enableStats},\n");
        js.append("            {key: 'enableRanks', type: 'checkbox', value: configData.general.enableRanks},\n");
        js.append("            {key: 'enableVote', type: 'checkbox', value: configData.general.enableVote},\n");
        js.append("            {key: 'enablePlayerList', type: 'checkbox', value: configData.general.enablePlayerList},\n");
        js.append("            {key: 'skinCacheHours', type: 'number', min: 1, max: 720, value: configData.general.skinCacheHours}\n");
        js.append("        ]);\n");
        js.append("    }\n");
        js.append("    \n");
        
        js.append("    // Sekce: Restart\n");
        js.append("    if (configData.restart) {\n");
        js.append("        html += renderSection('restart', t('config.section.restart'), [\n");
        js.append("            {key: 'restartType', type: 'select', options: ['FIXED_TIMES', 'INTERVAL'], value: configData.restart.restartType},\n");
        js.append("            {key: 'intervalHours', type: 'number', min: 1, max: 168, value: configData.restart.intervalHours},\n");
        js.append("            {key: 'delayMinutes', type: 'number', min: 1, max: 60, value: configData.restart.delayMinutes},\n");
        js.append("            {key: 'fixedRestartTimes', type: 'timelist', value: configData.restart.fixedRestartTimes}\n");
        js.append("        ]);\n");
        js.append("    }\n");
        js.append("    \n");
        
        js.append("    // Sekce: Announcement\n");
        js.append("    if (configData.announcement) {\n");
        js.append("        html += renderSection('announcement', t('config.section.announcement'), [\n");
        js.append("            {key: 'prefix', type: 'text', value: configData.announcement.prefix},\n");
        js.append("            {key: 'announcementIntervalMinutes', type: 'number', min: 1, max: 1440, value: configData.announcement.announcementIntervalMinutes},\n");
        js.append("            {key: 'announcements', type: 'stringlist', value: configData.announcement.announcements}\n");
        js.append("        ]);\n");
        js.append("    }\n");
        js.append("    \n");
        
        js.append("    // Sekce: Discord (zkráceno pro přehlednost, obsahuje všechna pole)\n");
        js.append("    if (configData.discord) {\n");
        js.append("        html += renderDiscordSection();\n");
        js.append("    }\n");
        js.append("    \n");
        
        js.append("    // Sekce: Stats\n");
        js.append("    if (configData.stats) {\n");
        js.append("        html += renderSection('stats', t('config.section.stats'), [\n");
        js.append("            {key: 'enableStats', type: 'checkbox', value: configData.stats.enableStats},\n");
        js.append("            {key: 'reportChannelId', type: 'text', value: configData.stats.reportChannelId},\n");
        js.append("            {key: 'reportTime', type: 'time', value: configData.stats.reportTime}\n");
        js.append("        ]);\n");
        js.append("    }\n");
        js.append("    \n");
        
        js.append("    // Sekce: Vote\n");
        js.append("    if (configData.vote) {\n");
        js.append("        html += renderSection('vote', t('config.section.vote'), [\n");
        js.append("            {key: 'enabled', type: 'checkbox', value: configData.vote.enabled},\n");
        js.append("            {key: 'host', type: 'text', value: configData.vote.host},\n");
        js.append("            {key: 'port', type: 'number', min: 1, max: 65535, value: configData.vote.port},\n");
        js.append("            {key: 'rsaPrivateKeyPath', type: 'text', value: configData.vote.rsaPrivateKeyPath},\n");
        js.append("            {key: 'rsaPublicKeyPath', type: 'text', value: configData.vote.rsaPublicKeyPath},\n");
        js.append("            {key: 'sharedSecret', type: 'text', value: configData.vote.sharedSecret},\n");
        js.append("            {key: 'announceVotes', type: 'checkbox', value: configData.vote.announceVotes},\n");
        js.append("            {key: 'announcementMessage', type: 'text', value: configData.vote.announcementMessage},\n");
        js.append("            {key: 'announcementCooldown', type: 'number', min: 0, max: 3600, value: configData.vote.announcementCooldown},\n");
        js.append("            {key: 'commands', type: 'stringlist', value: configData.vote.commands}\n");
        js.append("        ]);\n");
        js.append("    }\n");
        js.append("    \n");
        
        js.append("    // Sekce: Ranks\n");
        js.append("    if (configData.ranks) {\n");
        js.append("        html += renderSection('ranks', t('config.section.ranks'), [\n");
        js.append("            {key: 'enableAutoRanks', type: 'checkbox', value: configData.ranks.enableAutoRanks},\n");
        js.append("            {key: 'checkIntervalMinutes', type: 'number', min: 1, max: 1440, value: configData.ranks.checkIntervalMinutes},\n");
        js.append("            {key: 'promotionMessage', type: 'text', value: configData.ranks.promotionMessage},\n");
        js.append("            {key: 'ranks', type: 'playtimeranklist', value: configData.ranks.ranks}\n");
        js.append("        ]);\n");
        js.append("    }\n");
        js.append("    \n");
        
        js.append("    // Sekce: Tickets\n");
        js.append("    if (configData.tickets) {\n");
        js.append("        html += renderSection('tickets', t('config.section.tickets'), [\n");
        js.append("            {key: 'enableTickets', type: 'checkbox', value: configData.tickets.enableTickets},\n");
        js.append("            {key: 'ticketCategoryId', type: 'text', value: configData.tickets.ticketCategoryId},\n");
        js.append("            {key: 'supportRoleId', type: 'text', value: configData.tickets.supportRoleId},\n");
        js.append("            {key: 'ticketChannelTopic', type: 'text', value: configData.tickets.ticketChannelTopic},\n");
        js.append("            {key: 'maxTicketsPerUser', type: 'number', min: 1, max: 10, value: configData.tickets.maxTicketsPerUser},\n");
        js.append("            {key: 'ticketCreatedMessage', type: 'textarea', value: configData.tickets.ticketCreatedMessage},\n");
        js.append("            {key: 'ticketWelcomeMessage', type: 'textarea', value: configData.tickets.ticketWelcomeMessage},\n");
        js.append("            {key: 'ticketCloseMessage', type: 'textarea', value: configData.tickets.ticketCloseMessage},\n");
        js.append("            {key: 'noPermissionMessage', type: 'text', value: configData.tickets.noPermissionMessage},\n");
        js.append("            {key: 'ticketLimitReachedMessage', type: 'text', value: configData.tickets.ticketLimitReachedMessage},\n");
        js.append("            {key: 'ticketAlreadyClosedMessage', type: 'text', value: configData.tickets.ticketAlreadyClosedMessage},\n");
        js.append("            {key: 'enableTranscript', type: 'checkbox', value: configData.tickets.enableTranscript},\n");
        js.append("            {key: 'transcriptFormat', type: 'select', options: ['TXT', 'HTML', 'JSON'], value: configData.tickets.transcriptFormat},\n");
        js.append("            {key: 'transcriptFilename', type: 'text', value: configData.tickets.transcriptFilename}\n");
        js.append("        ]);\n");
        js.append("    }\n");
        js.append("    \n");
        
        js.append("    // Sekce: PlayerList\n");
        js.append("    if (configData.playerlist) {\n");
        js.append("        html += renderSection('playerlist', t('config.section.playerlist'), [\n");
        js.append("            {key: 'enableCustomPlayerList', type: 'checkbox', value: configData.playerlist.enableCustomPlayerList},\n");
        js.append("            {key: 'headerLine1', type: 'text', value: configData.playerlist.headerLine1},\n");
        js.append("            {key: 'headerLine2', type: 'text', value: configData.playerlist.headerLine2},\n");
        js.append("            {key: 'headerLine3', type: 'text', value: configData.playerlist.headerLine3},\n");
        js.append("            {key: 'footerLine1', type: 'text', value: configData.playerlist.footerLine1},\n");
        js.append("            {key: 'footerLine2', type: 'text', value: configData.playerlist.footerLine2},\n");
        js.append("            {key: 'footerLine3', type: 'text', value: configData.playerlist.footerLine3},\n");
        js.append("            {key: 'enableCustomNames', type: 'checkbox', value: configData.playerlist.enableCustomNames},\n");
        js.append("            {key: 'playerNameFormat', type: 'text', value: configData.playerlist.playerNameFormat},\n");
        js.append("            {key: 'defaultPrefix', type: 'text', value: configData.playerlist.defaultPrefix},\n");
        js.append("            {key: 'defaultSuffix', type: 'text', value: configData.playerlist.defaultSuffix},\n");
        js.append("            {key: 'combineMultipleRanks', type: 'checkbox', value: configData.playerlist.combineMultipleRanks},\n");
        js.append("            {key: 'updateIntervalSeconds', type: 'number', min: 1, max: 300, value: configData.playerlist.updateIntervalSeconds}\n");
        js.append("        ]);\n");
        js.append("    }\n");
        js.append("    \n");
        
        js.append("    editor.innerHTML = html;\n");
        js.append("    attachEventListeners();\n");
        js.append("}\n\n");
        
        // =====================================================================
        // RENDER SECTION HELPER
        // =====================================================================
        js.append("// ============================================================\n");
        js.append("// RENDER SECTION HELPER\n");
        js.append("// ============================================================\n\n");
        
        js.append("function renderSection(sectionId, title, fields) {\n");
        js.append("    let html = '<div class=\"config-section\" id=\"section-' + sectionId + '\">';\n");
        js.append("    html += '<h3 class=\"section-title\">' + title + '</h3>';\n");
        js.append("    html += '<div class=\"section-content\">';\n");
        js.append("    \n");
        js.append("    fields.forEach(field => {\n");
        js.append("        html += renderField(sectionId, field);\n");
        js.append("    });\n");
        js.append("    \n");
        js.append("    html += '<button class=\"btn btn-save\" onclick=\"saveSection(\\'' + sectionId + '\\')\">' + t('btn.save') + '</button>';\n");
        js.append("    html += '</div></div>';\n");
        js.append("    return html;\n");
        js.append("}\n\n");
        
        // =====================================================================
        // RENDER FIELD HELPER
        // =====================================================================
        js.append("function renderField(section, field) {\n");
        js.append("    const id = section + '_' + field.key;\n");
        js.append("    const labelKey = 'config.' + section + '.' + field.key;\n");
        js.append("    const descKey = labelKey + '.desc';\n");
        js.append("    let html = '<div class=\"form-group\">';\n");
        js.append("    html += '<label for=\"' + id + '\">' + (TRANSLATIONS[labelKey] || field.key) + '</label>';\n");
        js.append("    \n");
        js.append("    switch (field.type) {\n");
        js.append("        case 'checkbox':\n");
        js.append("            html += '<input type=\"checkbox\" id=\"' + id + '\" ' + (field.value ? 'checked' : '') + '>';\n");
        js.append("            break;\n");
        js.append("        case 'number':\n");
        js.append("            html += '<input type=\"number\" id=\"' + id + '\" value=\"' + (field.value || 0) + '\"';\n");
        js.append("            if (field.min !== undefined) html += ' min=\"' + field.min + '\"';\n");
        js.append("            if (field.max !== undefined) html += ' max=\"' + field.max + '\"';\n");
        js.append("            html += '>';\n");
        js.append("            break;\n");
        js.append("        case 'select':\n");
        js.append("            html += '<select id=\"' + id + '\">';\n");
        js.append("            field.options.forEach(opt => {\n");
        js.append("                html += '<option value=\"' + opt + '\"' + (field.value === opt ? ' selected' : '') + '>' + opt + '</option>';\n");
        js.append("            });\n");
        js.append("            html += '</select>';\n");
        js.append("            break;\n");
        js.append("        case 'textarea':\n");
        js.append("            html += '<textarea id=\"' + id + '\" rows=\"3\">' + (field.value || '') + '</textarea>';\n");
        js.append("            break;\n");
        js.append("        case 'password':\n");
        js.append("            html += '<input type=\"password\" id=\"' + id + '\" value=\"' + (field.value || '') + '\">';\n");
        js.append("            break;\n");
        js.append("        case 'time':\n");
        js.append("            html += '<input type=\"time\" id=\"' + id + '\" value=\"' + (field.value || '00:00') + '\">';\n");
        js.append("            break;\n");
        js.append("        case 'timelist':\n");
        js.append("            html += renderTimeList(id, field.value || []);\n");
        js.append("            break;\n");
        js.append("        case 'stringlist':\n");
        js.append("            html += renderStringList(id, field.value || []);\n");
        js.append("            break;\n");
        js.append("        case 'ranklist':\n");
        js.append("            html += renderRankList(id, field.value || []);\n");
        js.append("            break;\n");
        js.append("        case 'playtimeranklist':\n");
        js.append("            html += renderPlaytimeRankList(id, field.value || []);\n");
        js.append("            break;\n");
        js.append("        case 'langbuttons':\n");
        js.append("            html += '<div class=\"lang-buttons\" id=\"' + id + '\">';\n");
        js.append("            html += '<button type=\"button\" class=\"lang-btn' + (field.value === 'en' ? ' active' : '') + '\" data-lang=\"en\" onclick=\"setLanguage(\\'en\\')\">';\n");
        js.append("            html += '🇬🇧 EN</button>';\n");
        js.append("            html += '<button type=\"button\" class=\"lang-btn' + (field.value === 'cz' ? ' active' : '') + '\" data-lang=\"cz\" onclick=\"setLanguage(\\'cz\\')\">';\n");
        js.append("            html += '🇨🇿 CZ</button>';\n");
        js.append("            html += '</div>';\n");
        js.append("            break;\n");
        js.append("        default: // text\n");
        js.append("            html += '<input type=\"text\" id=\"' + id + '\" value=\"' + (field.value || '') + '\">';\n");
        js.append("    }\n");
        js.append("    \n");
        js.append("    if (TRANSLATIONS[descKey]) {\n");
        js.append("        html += '<small class=\"field-desc\">' + TRANSLATIONS[descKey] + '</small>';\n");
        js.append("    }\n");
        js.append("    html += '</div>';\n");
        js.append("    return html;\n");
        js.append("}\n\n");
        
        // =====================================================================
        // SPECIALIZED RENDER FUNCTIONS
        // =====================================================================
        js.append("// ============================================================\n");
        js.append("// SPECIÁLNÍ RENDER FUNKCE\n");
        js.append("// ============================================================\n\n");
        
        // Time list
        js.append("function renderTimeList(id, times) {\n");
        js.append("    let html = '<div class=\"list-editor\" id=\"' + id + '-container\">';\n");
        js.append("    times.forEach((time, idx) => {\n");
        js.append("        html += '<div class=\"list-item\">';\n");
        js.append("        html += '<input type=\"time\" data-list=\"' + id + '\" data-idx=\"' + idx + '\" value=\"' + time + '\">';\n");
        js.append("        html += '<button class=\"btn btn-small btn-danger\" onclick=\"removeListItem(\\'' + id + '\\', ' + idx + ')\">×</button>';\n");
        js.append("        html += '</div>';\n");
        js.append("    });\n");
        js.append("    html += '<button class=\"btn btn-small btn-add\" onclick=\"addTimeItem(\\'' + id + '\\')\">' + t('btn.add') + '</button>';\n");
        js.append("    html += '</div>';\n");
        js.append("    return html;\n");
        js.append("}\n\n");
        
        // String list
        js.append("function renderStringList(id, strings) {\n");
        js.append("    let html = '<div class=\"list-editor\" id=\"' + id + '-container\">';\n");
        js.append("    strings.forEach((str, idx) => {\n");
        js.append("        html += '<div class=\"list-item\">';\n");
        js.append("        html += '<input type=\"text\" data-list=\"' + id + '\" data-idx=\"' + idx + '\" value=\"' + escapeHtml(str) + '\">';\n");
        js.append("        html += '<button class=\"btn btn-small btn-danger\" onclick=\"removeListItem(\\'' + id + '\\', ' + idx + ')\">×</button>';\n");
        js.append("        html += '</div>';\n");
        js.append("    });\n");
        js.append("    html += '<button class=\"btn btn-small btn-add\" onclick=\"addStringItem(\\'' + id + '\\')\">' + t('btn.add') + '</button>';\n");
        js.append("    html += '</div>';\n");
        js.append("    return html;\n");
        js.append("}\n\n");
        
        // Rank list (complex)
        js.append("function renderRankList(id, ranks) {\n");
        js.append("    let html = '<div class=\"rank-list-editor\" id=\"' + id + '-container\">';\n");
        js.append("    ranks.forEach((rank, idx) => {\n");
        js.append("        html += '<div class=\"rank-item\" data-idx=\"' + idx + '\">';\n");
        js.append("        html += '<input type=\"text\" placeholder=\"Name\" value=\"' + (rank.name || '') + '\" data-field=\"name\">';\n");
        js.append("        html += '<input type=\"number\" placeholder=\"Hours\" value=\"' + (rank.requiredPlaytimeHours || 0) + '\" data-field=\"requiredPlaytimeHours\">';\n");
        js.append("        html += '<input type=\"text\" placeholder=\"Permission\" value=\"' + (rank.permission || '') + '\" data-field=\"permission\">';\n");
        js.append("        html += '<input type=\"text\" placeholder=\"Command\" value=\"' + (rank.command || '') + '\" data-field=\"command\">';\n");
        js.append("        html += '<button class=\"btn btn-small btn-danger\" onclick=\"removeRank(' + idx + ')\">×</button>';\n");
        js.append("        html += '</div>';\n");
        js.append("    });\n");
        js.append("    html += '<button class=\"btn btn-small btn-add\" onclick=\"addRank()\">' + t('btn.add') + '</button>';\n");
        js.append("    html += '</div>';\n");
        js.append("    return html;\n");
        js.append("}\n\n");
        
        // Playtime Rank list - type (PREFIX/SUFFIX), value, hours
        js.append("function renderPlaytimeRankList(id, ranks) {\n");
        js.append("    let html = '<div class=\"playtime-rank-list-editor\" id=\"' + id + '-container\">';\n");
        js.append("    ranks.forEach((rank, idx) => {\n");
        js.append("        html += '<div class=\"playtime-rank-item\" data-idx=\"' + idx + '\">';\n");
        js.append("        html += '<select data-field=\"type\">';\n");
        js.append("        html += '<option value=\"PREFIX\"' + (rank.type === 'PREFIX' ? ' selected' : '') + '>PREFIX</option>';\n");
        js.append("        html += '<option value=\"SUFFIX\"' + (rank.type === 'SUFFIX' ? ' selected' : '') + '>SUFFIX</option>';\n");
        js.append("        html += '</select>';\n");
        js.append("        html += '<input type=\"text\" placeholder=\"' + t('ranks.value.placeholder') + '\" value=\"' + escapeHtml(rank.value || '') + '\" data-field=\"value\">';\n");
        js.append("        html += '<input type=\"number\" placeholder=\"' + t('ranks.hours.placeholder') + '\" value=\"' + (rank.hours || 0) + '\" min=\"0\" data-field=\"hours\">';\n");
        js.append("        html += '<button class=\"btn btn-small btn-danger\" onclick=\"removePlaytimeRank(' + idx + ')\">×</button>';\n");
        js.append("        html += '</div>';\n");
        js.append("    });\n");
        js.append("    html += '<button class=\"btn btn-small btn-add\" onclick=\"addPlaytimeRank()\">' + t('btn.add') + '</button>';\n");
        js.append("    html += '</div>';\n");
        js.append("    return html;\n");
        js.append("}\n\n");
        
        // Discord section (special)
        js.append("function renderDiscordSection() {\n");
        js.append("    const dc = configData.discord;\n");
        js.append("    let html = '<div class=\"config-section\" id=\"section-discord\">';\n");
        js.append("    html += '<h3 class=\"section-title\">' + t('config.section.discord') + '</h3>';\n");
        js.append("    html += '<div class=\"section-content\">';\n");
        js.append("    \n");
        js.append("    // Základní nastavení\n");
        js.append("    html += renderField('discord', {key: 'enableDiscord', type: 'checkbox', value: dc.enableDiscord});\n");
        js.append("    html += renderField('discord', {key: 'botToken', type: 'text', value: dc.botToken});\n");
        js.append("    html += renderField('discord', {key: 'guildId', type: 'text', value: dc.guildId});\n");
        js.append("    html += renderField('discord', {key: 'botActivityType', type: 'select', options: ['PLAYING', 'WATCHING', 'LISTENING', 'COMPETING'], value: dc.botActivityType});\n");
        js.append("    html += renderField('discord', {key: 'botActivityText', type: 'text', value: dc.botActivityText});\n");
        js.append("    \n");
        js.append("    // Whitelist\n");
        js.append("    html += '<h4>Whitelist</h4>';\n");
        js.append("    html += renderField('discord', {key: 'enableWhitelist', type: 'checkbox', value: dc.enableWhitelist});\n");
        js.append("    html += renderField('discord', {key: 'kickMessage', type: 'text', value: dc.kickMessage});\n");
        js.append("    html += renderField('discord', {key: 'maxAccountsPerDiscord', type: 'number', min: 1, max: 10, value: dc.maxAccountsPerDiscord});\n");
        js.append("    \n");
        js.append("    // Channel IDs\n");
        js.append("    html += '<h4>Channels</h4>';\n");
        js.append("    html += renderField('discord', {key: 'chatChannelId', type: 'text', value: dc.chatChannelId});\n");
        js.append("    html += renderField('discord', {key: 'consoleChannelId', type: 'text', value: dc.consoleChannelId});\n");
        js.append("    html += renderField('discord', {key: 'statusChannelId', type: 'text', value: dc.statusChannelId});\n");
        js.append("    html += renderField('discord', {key: 'linkChannelId', type: 'text', value: dc.linkChannelId});\n");
        js.append("    html += renderField('discord', {key: 'linkedRoleId', type: 'text', value: dc.linkedRoleId});\n");
        js.append("    \n");
        js.append("    // Chat bridge\n");
        js.append("    html += '<h4>Chat Bridge</h4>';\n");
        js.append("    html += renderField('discord', {key: 'enableChatBridge', type: 'checkbox', value: dc.enableChatBridge});\n");
        js.append("    html += renderField('discord', {key: 'minecraftToDiscordFormat', type: 'text', value: dc.minecraftToDiscordFormat});\n");
        js.append("    html += renderField('discord', {key: 'discordToMinecraftFormat', type: 'text', value: dc.discordToMinecraftFormat});\n");
        js.append("    html += renderField('discord', {key: 'translateEmojis', type: 'checkbox', value: dc.translateEmojis});\n");
        js.append("    html += renderField('discord', {key: 'chatWebhookUrl', type: 'text', value: dc.chatWebhookUrl});\n");
        js.append("    \n");
        js.append("    // Status messages\n");
        js.append("    html += '<h4>Status Messages</h4>';\n");
        js.append("    html += renderField('discord', {key: 'enableStatusMessages', type: 'checkbox', value: dc.enableStatusMessages});\n");
        js.append("    html += renderField('discord', {key: 'statusMessageStarting', type: 'text', value: dc.statusMessageStarting});\n");
        js.append("    html += renderField('discord', {key: 'statusMessageStarted', type: 'text', value: dc.statusMessageStarted});\n");
        js.append("    html += renderField('discord', {key: 'statusMessageStopping', type: 'text', value: dc.statusMessageStopping});\n");
        js.append("    html += renderField('discord', {key: 'statusMessageStopped', type: 'text', value: dc.statusMessageStopped});\n");
        js.append("    \n");
        js.append("    // Role prefixes (special editor)\n");
        js.append("    html += '<h4>' + t('role.prefix.editor') + '</h4>';\n");
        js.append("    html += renderRolePrefixEditor(dc.rolePrefixes || {});\n");
        js.append("    \n");
        js.append("    html += '<button class=\"btn btn-save\" onclick=\"saveSection(\\'discord\\')\">' + t('btn.save') + '</button>';\n");
        js.append("    html += '</div></div>';\n");
        js.append("    return html;\n");
        js.append("}\n\n");
        
        // Role prefix editor - VYLEPŠENÝ s color pickerem a auto-generací
        js.append("function renderRolePrefixEditor(prefixes) {\n");
        js.append("    let html = '<div class=\"role-prefix-editor\" id=\"role-prefix-container\">';\n");
        js.append("    \n");
        js.append("    // Zobrazit existující prefixy\n");
        js.append("    Object.entries(prefixes).forEach(([roleId, prefixData]) => {\n");
        js.append("        const role = discordRoles.find(r => r.id === roleId);\n");
        js.append("        const roleName = role?.name || roleId;\n");
        js.append("        // Použij color z konfigurace, pak z Discord role, pak default\n");
        js.append("        const savedColor = typeof prefixData === 'object' ? (prefixData.color || '') : '';\n");
        js.append("        const roleColor = savedColor || role?.color || '#99aab5';\n");
        js.append("        // Podpora starého formátu (string) i nového (objekt s prefix/suffix)\n");
        js.append("        const prefix = typeof prefixData === 'string' ? prefixData : (prefixData.prefix || '');\n");
        js.append("        const suffix = typeof prefixData === 'string' ? '' : (prefixData.suffix || '');\n");
        js.append("        const priority = typeof prefixData === 'object' ? (prefixData.priority || 0) : 0;\n");
        js.append("        html += renderRolePrefixItem(roleId, roleName, roleColor, prefix, suffix, priority);\n");
        js.append("    });\n");
        js.append("    \n");
        js.append("    // Tlačítko přidat\n");
        js.append("    html += '<button class=\"btn btn-small btn-add\" onclick=\"addRolePrefix()\">' + t('role.prefix.add') + '</button>';\n");
        js.append("    html += '</div>';\n");
        js.append("    return html;\n");
        js.append("}\n\n");
        
        js.append("function renderRolePrefixItem(roleId, roleName, roleColor, prefix, suffix, priority) {\n");
        js.append("    let html = '<div class=\"role-prefix-item\" data-role=\"' + roleId + '\" data-priority=\"' + (priority || 0) + '\">';\n");
        js.append("    \n");
        js.append("    // Role select\n");
        js.append("    html += '<div class=\"role-select-wrapper\">';\n");
        js.append("    html += '<select class=\"role-select\" onchange=\"onRoleSelect(this)\">';\n");
        js.append("    html += '<option value=\"' + roleId + '\" data-color=\"' + roleColor + '\">' + roleName + '</option>';\n");
        js.append("    discordRoles.forEach(role => {\n");
        js.append("        if (role.id !== roleId) {\n");
        js.append("            html += '<option value=\"' + role.id + '\" data-color=\"' + role.color + '\">' + role.name + '</option>';\n");
        js.append("        }\n");
        js.append("    });\n");
        js.append("    html += '</select>';\n");
        js.append("    html += '<span class=\"role-color-preview\" style=\"background:' + roleColor + '\"></span>';\n");
        js.append("    html += '</div>';\n");
        js.append("    \n");
        js.append("    // Prefix input s color picker\n");
        js.append("    html += '<div class=\"prefix-group\">';\n");
        js.append("    html += '<label>Prefix</label>';\n");
        js.append("    html += '<div class=\"input-with-color\">';\n");
        js.append("    html += '<input type=\"color\" class=\"color-picker\" value=\"' + roleColor + '\" onchange=\"insertColorCode(this, \\'prefix\\')\" title=\"' + t('color.picker.title') + '\">';\n");
        js.append("    html += '<input type=\"text\" class=\"prefix-input\" value=\"' + escapeHtml(prefix) + '\" placeholder=\"&c[Admin] \">';\n");
        js.append("    html += '</div>';\n");
        js.append("    html += '</div>';\n");
        js.append("    \n");
        js.append("    // Suffix input s color picker\n");
        js.append("    html += '<div class=\"suffix-group\">';\n");
        js.append("    html += '<label>Suffix</label>';\n");
        js.append("    html += '<div class=\"input-with-color\">';\n");
        js.append("    html += '<input type=\"color\" class=\"color-picker\" value=\"' + roleColor + '\" onchange=\"insertColorCode(this, \\'suffix\\')\" title=\"' + t('color.picker.title') + '\">';\n");
        js.append("    html += '<input type=\"text\" class=\"suffix-input\" value=\"' + escapeHtml(suffix) + '\" placeholder=\" &c★\">';\n");
        js.append("    html += '</div>';\n");
        js.append("    html += '</div>';\n");
        js.append("    \n");
        js.append("    // Remove button\n");
        js.append("    html += '<button class=\"btn btn-small btn-danger\" onclick=\"this.closest(\\'.role-prefix-item\\').remove()\">×</button>';\n");
        js.append("    html += '</div>';\n");
        js.append("    return html;\n");
        js.append("}\n\n");
        
        // Funkce pro výběr role - auto-generuje prefix
        js.append("function onRoleSelect(selectEl) {\n");
        js.append("    const item = selectEl.closest('.role-prefix-item');\n");
        js.append("    const roleId = selectEl.value;\n");
        js.append("    const role = discordRoles.find(r => r.id === roleId);\n");
        js.append("    if (!role) return;\n");
        js.append("    \n");
        js.append("    // Update data-role\n");
        js.append("    item.setAttribute('data-role', roleId);\n");
        js.append("    \n");
        js.append("    // Update color preview\n");
        js.append("    const colorPreview = item.querySelector('.role-color-preview');\n");
        js.append("    if (colorPreview) colorPreview.style.background = role.color;\n");
        js.append("    \n");
        js.append("    // Update color pickers\n");
        js.append("    item.querySelectorAll('.color-picker').forEach(cp => cp.value = role.color);\n");
        js.append("    \n");
        js.append("    // Auto-generate default prefix based on new role\n");
        js.append("    const prefixInput = item.querySelector('.prefix-input');\n");
        js.append("    if (prefixInput) {\n");
        js.append("        // Použij hex barvu pro moderní klienty nebo MC barvu jako fallback\n");
        js.append("        let colorCode = hexToMcColor(role.color);\n");
        js.append("        // Pokud role má vlastní barvu (ne default), použij hex formát\n");
        js.append("        if (role.color && role.color !== '#000000' && role.color !== '#99aab5') {\n");
        js.append("            colorCode = '&' + role.color; // Formát &#RRGGBB\n");
        js.append("        }\n");
        js.append("        prefixInput.value = colorCode + '[' + role.name + '] ';\n");
        js.append("    }\n");
        js.append("    \n");
        js.append("    // Clear suffix when role changes\n");
        js.append("    const suffixInput = item.querySelector('.suffix-input');\n");
        js.append("    if (suffixInput) {\n");
        js.append("        suffixInput.value = '';\n");
        js.append("    }\n");
        js.append("}\n\n");
        
        // Převod hex na MC color code
        js.append("function hexToMcColor(hex) {\n");
        js.append("    if (!hex) return '&f';\n");
        js.append("    hex = hex.replace('#', '').toLowerCase();\n");
        js.append("    // Pokud je barva černá nebo velmi tmavá (Discord default), použij bílou\n");
        js.append("    if (hex === '000000' || hex === '99aab5') return '&f';\n");
        js.append("    \n");
        js.append("    // Mapování na nejbližší MC barvu\n");
        js.append("    const mcColors = {\n");
        js.append("        '000000': '&0', '0000aa': '&1', '00aa00': '&2', '00aaaa': '&3',\n");
        js.append("        'aa0000': '&4', 'aa00aa': '&5', 'ffaa00': '&6', 'aaaaaa': '&7',\n");
        js.append("        '555555': '&8', '5555ff': '&9', '55ff55': '&a', '55ffff': '&b',\n");
        js.append("        'ff5555': '&c', 'ff55ff': '&d', 'ffff55': '&e', 'ffffff': '&f'\n");
        js.append("    };\n");
        js.append("    \n");
        js.append("    // Najdi nejbližší barvu\n");
        js.append("    let closest = '&f';\n");
        js.append("    let minDist = Infinity;\n");
        js.append("    const r = parseInt(hex.substr(0,2), 16);\n");
        js.append("    const g = parseInt(hex.substr(2,2), 16);\n");
        js.append("    const b = parseInt(hex.substr(4,2), 16);\n");
        js.append("    \n");
        js.append("    for (const [mcHex, code] of Object.entries(mcColors)) {\n");
        js.append("        const mr = parseInt(mcHex.substr(0,2), 16);\n");
        js.append("        const mg = parseInt(mcHex.substr(2,2), 16);\n");
        js.append("        const mb = parseInt(mcHex.substr(4,2), 16);\n");
        js.append("        const dist = Math.sqrt(Math.pow(r-mr,2) + Math.pow(g-mg,2) + Math.pow(b-mb,2));\n");
        js.append("        if (dist < minDist) { minDist = dist; closest = code; }\n");
        js.append("    }\n");
        js.append("    return closest;\n");
        js.append("}\n\n");
        
        // Vložení color kódu do inputu
        js.append("function insertColorCode(colorPicker, type) {\n");
        js.append("    const item = colorPicker.closest('.role-prefix-item');\n");
        js.append("    const input = item.querySelector('.' + type + '-input');\n");
        js.append("    if (!input) return;\n");
        js.append("    \n");
        js.append("    const mcColor = hexToMcColor(colorPicker.value);\n");
        js.append("    const pos = input.selectionStart || input.value.length;\n");
        js.append("    input.value = input.value.slice(0, pos) + mcColor + input.value.slice(pos);\n");
        js.append("    input.focus();\n");
        js.append("    input.setSelectionRange(pos + mcColor.length, pos + mcColor.length);\n");
        js.append("}\n\n");
        
        // =====================================================================
        // SAVE FUNCTIONS
        // =====================================================================
        js.append("// ============================================================\n");
        js.append("// UKLÁDÁNÍ KONFIGURACE\n");
        js.append("// ============================================================\n\n");
        
        // Funkce pro uložení všech sekcí najednou (pro sticky save bar)
        js.append("async function saveConfig() {\n");
        js.append("    const sections = ['web', 'general', 'restart', 'announcement', 'discord', 'stats', 'vote', 'ranks', 'tickets', 'playerlist'];\n");
        js.append("    let success = true;\n");
        js.append("    \n");
        js.append("    for (const sectionId of sections) {\n");
        js.append("        const section = document.getElementById('section-' + sectionId);\n");
        js.append("        if (!section) continue;\n");
        js.append("        \n");
        js.append("        const data = collectSectionData(sectionId);\n");
        js.append("        if (Object.keys(data).length === 0) continue;\n");
        js.append("        \n");
        js.append("        try {\n");
        js.append("            const response = await fetch('/api/config', {\n");
        js.append("                method: 'POST',\n");
        js.append("                headers: {'Content-Type': 'application/json'},\n");
        js.append("                body: JSON.stringify({section: sectionId, data: data})\n");
        js.append("            });\n");
        js.append("            if (!response.ok) success = false;\n");
        js.append("        } catch (e) {\n");
        js.append("            console.error('Save error for ' + sectionId + ':', e);\n");
        js.append("            success = false;\n");
        js.append("        }\n");
        js.append("    }\n");
        js.append("    \n");
        js.append("    if (success) {\n");
        js.append("        showToast(t('config.save.success'), 'success');\n");
        js.append("        loadConfig();\n");
        js.append("    } else {\n");
        js.append("        showToast(t('config.save.error'), 'error');\n");
        js.append("    }\n");
        js.append("}\n\n");
        
        js.append("async function saveSection(sectionId) {\n");
        js.append("    const data = collectSectionData(sectionId);\n");
        js.append("    \n");
        js.append("    try {\n");
        js.append("        const response = await fetch('/api/config', {\n");
        js.append("            method: 'POST',\n");
        js.append("            headers: {'Content-Type': 'application/json'},\n");
        js.append("            body: JSON.stringify({section: sectionId, data: data})\n");
        js.append("        });\n");
        js.append("        \n");
        js.append("        if (response.ok) {\n");
        js.append("            showToast(t('config.save.success'), 'success');\n");
        js.append("            loadConfig(); // Reload\n");
        js.append("        } else {\n");
        js.append("            throw new Error('Save failed');\n");
        js.append("        }\n");
        js.append("    } catch (e) {\n");
        js.append("        console.error('Save error:', e);\n");
        js.append("        showToast(t('config.save.error'), 'error');\n");
        js.append("    }\n");
        js.append("}\n\n");
        
        js.append("function collectSectionData(sectionId) {\n");
        js.append("    const section = document.getElementById('section-' + sectionId);\n");
        js.append("    if (!section) return {};\n");
        js.append("    \n");
        js.append("    const data = {};\n");
        js.append("    \n");
        js.append("    // Collect all inputs\n");
        js.append("    section.querySelectorAll('input, select, textarea').forEach(el => {\n");
        js.append("        const id = el.id;\n");
        js.append("        if (!id || !id.startsWith(sectionId + '_')) return;\n");
        js.append("        \n");
        js.append("        const key = id.replace(sectionId + '_', '');\n");
        js.append("        \n");
        js.append("        if (el.type === 'checkbox') {\n");
        js.append("            data[key] = el.checked;\n");
        js.append("        } else if (el.type === 'number') {\n");
        js.append("            data[key] = parseInt(el.value) || 0;\n");
        js.append("        } else {\n");
        js.append("            data[key] = el.value;\n");
        js.append("        }\n");
        js.append("    });\n");
        js.append("    \n");
        js.append("    // Collect lists\n");
        js.append("    section.querySelectorAll('.list-editor').forEach(container => {\n");
        js.append("        const listId = container.id.replace('-container', '');\n");
        js.append("        const key = listId.replace(sectionId + '_', '');\n");
        js.append("        const items = [];\n");
        js.append("        container.querySelectorAll('input[data-list]').forEach(input => {\n");
        js.append("            if (input.value) items.push(input.value);\n");
        js.append("        });\n");
        js.append("        data[key] = items;\n");
        js.append("    });\n");
        js.append("    \n");
        js.append("    // Collect role prefixes for discord (s prefix a suffix)\n");
        js.append("    if (sectionId === 'discord') {\n");
        js.append("        const prefixes = {};\n");
        js.append("        document.querySelectorAll('.role-prefix-item').forEach((item, index) => {\n");
        js.append("            const roleId = item.dataset.role || item.querySelector('.role-select')?.value;\n");
        js.append("            const prefix = item.querySelector('.prefix-input')?.value || '';\n");
        js.append("            const suffix = item.querySelector('.suffix-input')?.value || '';\n");
        js.append("            const colorPicker = item.querySelector('.color-picker');\n");
        js.append("            const color = colorPicker ? colorPicker.value : '';\n");
        js.append("            if (roleId) {\n");
        js.append("                prefixes[roleId] = {prefix: prefix, suffix: suffix, color: color, priority: index};\n");
        js.append("            }\n");
        js.append("        });\n");
        js.append("        data.rolePrefixes = prefixes;\n");
        js.append("    }\n");
        js.append("    \n");
        js.append("    // Collect playtime ranks\n");
        js.append("    if (sectionId === 'ranks') {\n");
        js.append("        const ranks = [];\n");
        js.append("        document.querySelectorAll('.playtime-rank-item').forEach(item => {\n");
        js.append("            const type = item.querySelector('select[data-field=\"type\"]').value;\n");
        js.append("            const value = item.querySelector('input[data-field=\"value\"]').value;\n");
        js.append("            const hours = parseInt(item.querySelector('input[data-field=\"hours\"]').value) || 0;\n");
        js.append("            ranks.push({type, value, hours});\n");
        js.append("        });\n");
        js.append("        data.ranks = ranks;\n");
        js.append("    }\n");
        js.append("    \n");
        js.append("    return data;\n");
        js.append("}\n\n");
        
        // =====================================================================
        // LIST MANIPULATION FUNCTIONS
        // =====================================================================
        js.append("// ============================================================\n");
        js.append("// MANIPULACE SE SEZNAMY\n");
        js.append("// ============================================================\n\n");
        
        js.append("function addTimeItem(listId) {\n");
        js.append("    const container = document.getElementById(listId + '-container');\n");
        js.append("    const items = container.querySelectorAll('.list-item');\n");
        js.append("    const newIdx = items.length;\n");
        js.append("    const div = document.createElement('div');\n");
        js.append("    div.className = 'list-item';\n");
        js.append("    div.innerHTML = '<input type=\"time\" data-list=\"' + listId + '\" data-idx=\"' + newIdx + '\" value=\"12:00\">' +\n");
        js.append("                    '<button class=\"btn btn-small btn-danger\" onclick=\"removeListItem(\\'' + listId + '\\', ' + newIdx + ')\">×</button>';\n");
        js.append("    container.insertBefore(div, container.lastElementChild);\n");
        js.append("}\n\n");
        
        js.append("function addStringItem(listId) {\n");
        js.append("    const container = document.getElementById(listId + '-container');\n");
        js.append("    const items = container.querySelectorAll('.list-item');\n");
        js.append("    const newIdx = items.length;\n");
        js.append("    const div = document.createElement('div');\n");
        js.append("    div.className = 'list-item';\n");
        js.append("    div.innerHTML = '<input type=\"text\" data-list=\"' + listId + '\" data-idx=\"' + newIdx + '\" value=\"\">' +\n");
        js.append("                    '<button class=\"btn btn-small btn-danger\" onclick=\"removeListItem(\\'' + listId + '\\', ' + newIdx + ')\">×</button>';\n");
        js.append("    container.insertBefore(div, container.lastElementChild);\n");
        js.append("}\n\n");
        
        js.append("function removeListItem(listId, idx) {\n");
        js.append("    const container = document.getElementById(listId + '-container');\n");
        js.append("    const items = container.querySelectorAll('.list-item');\n");
        js.append("    if (items[idx]) items[idx].remove();\n");
        js.append("}\n\n");
        
        js.append("function addRolePrefix() {\n");
        js.append("    const container = document.getElementById('role-prefix-container');\n");
        js.append("    if (discordRoles.length === 0) {\n");
        js.append("        showToast('No Discord roles available', 'error');\n");
        js.append("        return;\n");
        js.append("    }\n");
        js.append("    \n");
        js.append("    // Najdi první roli která ještě není přidaná\n");
        js.append("    const usedRoleIds = Array.from(container.querySelectorAll('.role-prefix-item')).map(el => el.dataset.role);\n");
        js.append("    const availableRole = discordRoles.find(r => !usedRoleIds.includes(r.id)) || discordRoles[0];\n");
        js.append("    const newPriority = container.querySelectorAll('.role-prefix-item').length;\n");
        js.append("    \n");
        js.append("    const div = document.createElement('div');\n");
        js.append("    div.innerHTML = renderRolePrefixItem(availableRole.id, availableRole.name, availableRole.color, '', '', newPriority);\n");
        js.append("    const newItem = div.firstElementChild;\n");
        js.append("    \n");
        js.append("    // Auto-generuj default prefix s hex barvou\n");
        js.append("    const prefixInput = newItem.querySelector('.prefix-input');\n");
        js.append("    if (prefixInput) {\n");
        js.append("        let colorCode = hexToMcColor(availableRole.color);\n");
        js.append("        if (availableRole.color && availableRole.color !== '#000000' && availableRole.color !== '#99aab5') {\n");
        js.append("            colorCode = '&' + availableRole.color;\n");
        js.append("        }\n");
        js.append("        prefixInput.value = colorCode + '[' + availableRole.name + '] ';\n");
        js.append("    }\n");
        js.append("    \n");
        js.append("    container.insertBefore(newItem, container.lastElementChild);\n");
        js.append("}\n\n");
        
        // Playtime rank functions
        js.append("function addPlaytimeRank() {\n");
        js.append("    const container = document.getElementById('ranks_ranks-container');\n");
        js.append("    const items = container.querySelectorAll('.playtime-rank-item');\n");
        js.append("    const newIdx = items.length;\n");
        js.append("    const div = document.createElement('div');\n");
        js.append("    div.className = 'playtime-rank-item';\n");
        js.append("    div.setAttribute('data-idx', newIdx);\n");
        js.append("    div.innerHTML = '<select data-field=\"type\"><option value=\"PREFIX\" selected>PREFIX</option><option value=\"SUFFIX\">SUFFIX</option></select>' +\n");
        js.append("                    '<input type=\"text\" placeholder=\"' + t('ranks.value.placeholder') + '\" value=\"\" data-field=\"value\">' +\n");
        js.append("                    '<input type=\"number\" placeholder=\"' + t('ranks.hours.placeholder') + '\" value=\"0\" min=\"0\" data-field=\"hours\">' +\n");
        js.append("                    '<button class=\"btn btn-small btn-danger\" onclick=\"removePlaytimeRank(' + newIdx + ')\">×</button>';\n");
        js.append("    container.insertBefore(div, container.lastElementChild);\n");
        js.append("}\n\n");
        
        js.append("function removePlaytimeRank(idx) {\n");
        js.append("    const container = document.getElementById('ranks_ranks-container');\n");
        js.append("    const item = container.querySelector('.playtime-rank-item[data-idx=\"' + idx + '\"]');\n");
        js.append("    if (item) item.remove();\n");
        js.append("}\n\n");
        
        // Language and locale functions
        js.append("async function setLanguage(lang) {\n");
        js.append("    // Update UI buttons\n");
        js.append("    document.querySelectorAll('.lang-btn').forEach(btn => {\n");
        js.append("        btn.classList.toggle('active', btn.dataset.lang === lang);\n");
        js.append("    });\n");
        js.append("    \n");
        js.append("    // Save to config\n");
        js.append("    try {\n");
        js.append("        const response = await fetch('/api/config', {\n");
        js.append("            method: 'POST',\n");
        js.append("            headers: {'Content-Type': 'application/json'},\n");
        js.append("            body: JSON.stringify({section: 'web', data: {language: lang}})\n");
        js.append("        });\n");
        js.append("        if (response.ok) {\n");
        js.append("            showToast(t('language.changed'), 'success');\n");
        js.append("            // Reload page to apply new language\n");
        js.append("            setTimeout(() => window.location.reload(), 500);\n");
        js.append("        } else {\n");
        js.append("            throw new Error('Failed to save language');\n");
        js.append("        }\n");
        js.append("    } catch (e) {\n");
        js.append("        console.error('Language save error:', e);\n");
        js.append("        showToast(t('config.save.error'), 'error');\n");
        js.append("    }\n");
        js.append("}\n\n");
        
        js.append("async function resetLocale(localeType) {\n");
        js.append("    if (!confirm(t('locale.reset.confirm'))) return;\n");
        js.append("    \n");
        js.append("    try {\n");
        js.append("        const response = await fetch('/api/locale', {\n");
        js.append("            method: 'POST',\n");
        js.append("            headers: {'Content-Type': 'application/json'},\n");
        js.append("            body: JSON.stringify({locale: localeType})\n");
        js.append("        });\n");
        js.append("        if (response.ok) {\n");
        js.append("            showToast(t('locale.reset.success'), 'success');\n");
        js.append("            // Reload config to show new values\n");
        js.append("            setTimeout(() => loadConfig(), 500);\n");
        js.append("        } else {\n");
        js.append("            throw new Error('Reset failed');\n");
        js.append("        }\n");
        js.append("    } catch (e) {\n");
        js.append("        console.error('Locale reset error:', e);\n");
        js.append("        showToast(t('locale.reset.error'), 'error');\n");
        js.append("    }\n");
        js.append("}\n\n");
        
        // =====================================================================
        // UTILITY FUNCTIONS
        // =====================================================================
        js.append("// ============================================================\n");
        js.append("// UTILITY\n");
        js.append("// ============================================================\n\n");
        
        js.append("function escapeHtml(str) {\n");
        js.append("    if (!str) return '';\n");
        js.append("    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\"/g, '&quot;');\n");
        js.append("}\n\n");
        
        js.append("function attachEventListeners() {\n");
        js.append("    // Collapsible sections\n");
        js.append("    document.querySelectorAll('.section-title').forEach(title => {\n");
        js.append("        title.addEventListener('click', () => {\n");
        js.append("            title.parentElement.classList.toggle('collapsed');\n");
        js.append("        });\n");
        js.append("    });\n");
        js.append("}\n\n");
        
        // =====================================================================
        // INITIALIZATION
        // =====================================================================
        js.append("// ============================================================\n");
        js.append("// INICIALIZACE\n");
        js.append("// ============================================================\n\n");
        
        js.append("document.addEventListener('DOMContentLoaded', async () => {\n");
        js.append("    await loadDiscordRoles();\n");
        js.append("    await loadConfig();\n");
        js.append("});\n");
        
        // Konec script tagu
        js.append("</script>\n");
        
        return js.toString();
    }
    
    /**
     * Escape JavaScript string
     * @param s Input string
     * @return Escaped string
     */
    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}