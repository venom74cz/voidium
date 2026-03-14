package cz.voidium.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import cz.voidium.config.AIConfig;
import cz.voidium.config.AnnouncementConfig;
import cz.voidium.config.DiscordConfig;
import cz.voidium.config.GeneralConfig;
import cz.voidium.config.LocalePresets;
import cz.voidium.config.PlayerListConfig;
import cz.voidium.config.RanksConfig;
import cz.voidium.config.RestartConfig;
import cz.voidium.config.StatsConfig;
import cz.voidium.config.TicketConfig;
import cz.voidium.config.EntityCleanerConfig;
import cz.voidium.config.StorageHelper;
import cz.voidium.config.VoteConfig;
import cz.voidium.config.WebConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.FMLPaths;

public class ConfigStudioService {
    private static final ConfigStudioService INSTANCE = new ConfigStudioService();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);
    private static final Set<String> WEB_RESTART_FIELDS = Set.of("port", "bindAddress");
    private static final List<String> CONFIG_FILES = List.of(
            "general.json", "web.json", "announcements.json", "restart.json", "ranks.json", "playerlist.json", "tickets.json", "discord.json", "stats.json", "votes.json", "entitycleaner.json", "ai.json");

    private ConfigStudioService() {
    }

    public static ConfigStudioService getInstance() {
        return INSTANCE;
    }

    public Map<String, Object> getSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("general", section("General", false, List.of(
                field("enableMod", "boolean", "Enable mod", "Master power switch for the whole mod."),
                field("enableRestarts", "boolean", "Automatic restarts", "Controls scheduled restart automation."),
                field("enableAnnouncements", "boolean", "Announcements", "Enables automatic broadcast messages."),
                field("enableSkinRestorer", "boolean", "Skin Restorer", "Fetches real skins in offline mode."),
                field("enableDiscord", "boolean", "Discord integration", "Controls Discord bot and bridge features."),
                field("enableWeb", "boolean", "Web UI", "Controls the web control panel availability."),
                field("enableStats", "boolean", "Statistics", "Enables tracking and report generation."),
                field("enableRanks", "boolean", "Ranks", "Enables the automatic playtime rank system."),
                field("enableVote", "boolean", "Vote manager", "Enables the vote listener and reward processing."),
                field("enablePlayerList", "boolean", "Custom player list", "Enables TAB list enhancements."),
                field("maintenanceMode", "boolean", "Maintenance mode", "Blocks player login and shows dashboard banner."),
                field("skinCacheHours", "number", "Skin cache hours", "Lower values refresh skins more often."),
                field("modPrefix", "text", "Mod prefix", "Shown in Voidium messages. Supports & color codes."))));
        schema.put("web", section("Web", true, List.of(
                field("port", "number", "Port", "HTTP port used by the Voidium panel."),
                field("language", "select", "Language", "Current WebUI locale.", List.of("en", "cz")),
                field("publicHostname", "text", "Public hostname", "Address shown in generated access links."),
                field("bindAddress", "text", "Bind address", "Network interface used by the embedded web server."),
                secretField("adminToken", "Persistent admin token", "Static token for repeated admin access."),
                field("sessionTtlMinutes", "number", "Session TTL", "How long the session cookie remains valid."))));
        schema.put("announcements", section("Announcements", false, List.of(
                field("prefix", "text", "Prefix", "Added before each announcement line."),
                field("announcementIntervalMinutes", "number", "Interval minutes", "Set 0 to disable automatic broadcasts."),
                field("announcements", "multiline-list", "Announcement lines", "One line per announcement."))));
        schema.put("restart", section("Restart", false, List.of(
                field("restartType", "select", "Restart type", "Controls how restarts are scheduled.", List.of("FIXED_TIME", "INTERVAL", "DELAY")),
                field("fixedRestartTimes", "multiline-list", "Fixed restart times", "One HH:MM time per line."),
                field("intervalHours", "number", "Interval hours", "Used when restart type is INTERVAL."),
                field("delayMinutes", "number", "Delay minutes", "Used when restart type is DELAY."),
                field("warningMessage", "text", "Warning message", "Supports %minutes% placeholder."),
                field("restartingNowMessage", "text", "Restarting now", "Shown when restart begins."),
                field("kickMessage", "text", "Kick message", "Shown when players are disconnected."))));
        schema.put("ranks", section("Ranks", false, List.of(
                field("enableAutoRanks", "boolean", "Enable auto ranks", "Turns on automatic rank checks."),
                field("checkIntervalMinutes", "number", "Check interval", "How often rank promotion checks run."),
                field("promotionMessage", "text", "Promotion message", "Supports %rank% placeholder."),
                field("tooltipPlayed", "text", "Tooltip played", "Hover text for current playtime."),
                field("tooltipRequired", "text", "Tooltip required", "Hover text for required playtime."),
                field("ranks", "rank-list", "Rank definitions", "Structured rank editor with type, value, hours, and optional custom conditions JSON."))));
        schema.put("playerlist", section("Player List", false, List.of(
                field("enableCustomPlayerList", "boolean", "Enable custom list", "Turns on header/footer rendering."),
                field("headerLine1", "text", "Header line 1", "Supports placeholders like %online% and %max%."),
                field("headerLine2", "text", "Header line 2", "Supports placeholders like %tps%."),
                field("headerLine3", "text", "Header line 3", "Optional third header line."),
                field("footerLine1", "text", "Footer line 1", "Optional first footer line."),
                field("footerLine2", "text", "Footer line 2", "Optional second footer line."),
                field("footerLine3", "text", "Footer line 3", "Optional third footer line."),
                field("enableCustomNames", "boolean", "Custom player names", "Turns on formatted TAB player names."),
                field("playerNameFormat", "text", "Player name format", "Supports %rank_prefix%, %player_name%, %rank_suffix%."),
                field("defaultPrefix", "text", "Default prefix", "Used when no prefix is available."),
                field("defaultSuffix", "text", "Default suffix", "Used when no suffix is available."),
                field("combineMultipleRanks", "boolean", "Combine multiple ranks", "When enabled, multiple rank parts are merged."),
                field("updateIntervalSeconds", "number", "Update interval", "Minimum safe interval is 3 seconds."))));
            schema.put("tickets", section("Tickets", false, List.of(
                field("enableTickets", "boolean", "Enable tickets", "Turns the Discord ticket workflow on or off."),
                field("ticketCategoryId", "text", "Ticket category ID", "Discord category where ticket channels are created."),
                field("supportRoleId", "text", "Support role ID", "Discord role granted visibility to ticket channels."),
                field("ticketChannelTopic", "text", "Ticket channel topic", "Supports %user% and %reason% placeholders."),
                field("maxTicketsPerUser", "number", "Max tickets per user", "Maximum number of open tickets allowed per user."),
                field("ticketCreatedMessage", "text", "Ticket created message", "Discord message shown after a ticket is created."),
                field("ticketWelcomeMessage", "text", "Ticket welcome message", "Welcome text sent to a newly created ticket channel."),
                field("ticketCloseMessage", "text", "Ticket close message", "Message sent when the ticket is being closed."),
                field("noPermissionMessage", "text", "No permission message", "Shown when a user tries a ticket action without permission."),
                field("ticketLimitReachedMessage", "text", "Ticket limit reached", "Shown when the user already has too many open tickets."),
                field("ticketAlreadyClosedMessage", "text", "Ticket already closed", "Shown when a close action targets an already closed ticket."),
                field("enableTranscript", "boolean", "Enable transcript", "Upload a transcript file when the ticket closes."),
                field("transcriptFormat", "select", "Transcript format", "Select TXT or JSON transcript output.", List.of("TXT", "JSON")),
                field("transcriptFilename", "text", "Transcript filename", "Supports %user%, %date%, and %reason% placeholders."),
                field("mcBotNotConnectedMessage", "text", "MC bot not connected", "In-game message shown when the Discord bot is offline."),
                field("mcGuildNotFoundMessage", "text", "MC guild not found", "In-game message shown when the configured guild cannot be found."),
                field("mcCategoryNotFoundMessage", "text", "MC category not found", "In-game message shown when the ticket category is missing."),
                field("mcTicketCreatedMessage", "text", "MC ticket created", "In-game confirmation shown after ticket creation succeeds."),
                field("mcDiscordNotFoundMessage", "text", "MC Discord not found", "In-game message shown when the linked Discord user is missing."))));
            schema.put("discord", section("Discord", false, List.of(
                field("enableDiscord", "boolean", "Enable Discord", "Turns on Discord integration for the server."),
                secretField("botToken", "Bot token", "Discord bot token used to log in the bot account."),
                field("guildId", "text", "Guild ID", "Discord server ID used by the bot and whitelist flow."),
                field("botActivityType", "select", "Bot activity type", "Discord activity type shown on the bot profile.", List.of("PLAYING", "WATCHING", "LISTENING", "COMPETING")),
                field("botActivityText", "text", "Bot activity text", "Discord activity message shown under the bot name."),
                field("enableWhitelist", "boolean", "Enable whitelist", "Requires players to verify through Discord before joining."),
                field("kickMessage", "text", "Whitelist kick message", "Shown in Minecraft when an unverified player is rejected."),
                field("verificationHintMessage", "text", "Verification hint", "Hint shown in Minecraft with the verification code."),
                field("linkSuccessMessage", "text", "Link success message", "Discord response shown when linking succeeds."),
                field("alreadyLinkedMessage", "text", "Already linked message", "Discord response when the account limit is reached."),
                field("maxAccountsPerDiscord", "number", "Max accounts per Discord", "Maximum number of Minecraft accounts linked to one Discord account."),
                field("chatChannelId", "text", "Chat channel ID", "Discord channel ID for chat bridge output."),
                field("consoleChannelId", "text", "Console channel ID", "Discord channel ID for console output."),
                field("linkChannelId", "text", "Link channel ID", "Discord channel ID where linking commands are allowed."),
                field("statusChannelId", "text", "Status channel ID", "Discord channel ID for status updates. Falls back when blank."),
                field("enableStatusMessages", "boolean", "Enable status messages", "Send start, stop, and restart lifecycle messages to Discord."),
                field("statusMessageStarting", "text", "Status starting", "Message sent when the server starts booting."),
                field("statusMessageStarted", "text", "Status started", "Message sent when the server is fully online."),
                field("statusMessageStopping", "text", "Status stopping", "Message sent when the server begins stopping."),
                field("statusMessageStopped", "text", "Status stopped", "Message sent when the server is offline."),
                field("linkedRoleId", "text", "Linked role ID", "Role granted after a successful Minecraft to Discord link."),
                field("renameOnLink", "boolean", "Rename on link", "Rename Discord members after they link their Minecraft account."),
                field("rolePrefixes", "discord-role-style-list", "Role style map", "Structured Discord role style editor for prefix, suffix, color, and priority by role ID."),
                field("useHexColors", "boolean", "Use hex colors", "Use RGB hex colors for Discord role styles when supported."),
                field("syncBansDiscordToMc", "boolean", "Sync bans Discord to MC", "Mirror Discord moderation bans into Minecraft."),
                field("syncBansMcToDiscord", "boolean", "Sync bans MC to Discord", "Mirror Minecraft bans into Discord moderation."),
                field("enableChatBridge", "boolean", "Enable chat bridge", "Forward chat messages between Minecraft and Discord."),
                field("minecraftToDiscordFormat", "text", "Minecraft to Discord format", "Format used when Minecraft chat is relayed to Discord."),
                field("discordToMinecraftFormat", "text", "Discord to Minecraft format", "Format used when Discord chat is relayed to Minecraft."),
                field("translateEmojis", "boolean", "Translate emojis", "Translate Discord emojis into Minecraft-friendly text."),
                field("chatWebhookUrl", "text", "Chat webhook URL", "Optional Discord webhook URL for rich chat delivery."),
                field("enableTopicUpdate", "boolean", "Enable topic update", "Update channel topic with online count and uptime."),
                field("channelTopicFormat", "text", "Channel topic format", "Supports %online%, %max%, and %uptime% placeholders."),
                field("uptimeFormat", "text", "Uptime format", "Formatting used inside the Discord topic uptime value."),
                field("invalidCodeMessage", "text", "Invalid code message", "Discord response for invalid or expired verification codes."),
                field("notLinkedMessage", "text", "Not linked message", "Discord response when no linked account exists."),
                field("alreadyLinkedSingleMessage", "text", "Already linked single", "Discord response when one account is already linked."),
                field("alreadyLinkedMultipleMessage", "text", "Already linked multiple", "Discord response when multiple accounts are linked."),
                field("unlinkSuccessMessage", "text", "Unlink success message", "Discord response after unlinking completes."),
                field("wrongGuildMessage", "text", "Wrong guild message", "Discord response for commands executed in the wrong server."),
                field("ticketCreatedMessage", "text", "Ticket created message", "Discord response after a ticket channel is created."),
                field("ticketClosingMessage", "text", "Ticket closing message", "Discord response shown while a ticket is closing."),
                field("textChannelOnlyMessage", "text", "Text channel only message", "Discord response when a command is used outside a text channel."))));
            schema.put("stats", section("Stats", false, List.of(
                field("enableStats", "boolean", "Enable stats", "Turns scheduled statistics reports on or off."),
                field("reportChannelId", "text", "Report channel ID", "Discord channel ID used for daily stats reports."),
                field("reportTime", "text", "Report time", "Daily report time in HH:MM format."),
                field("reportTitle", "text", "Report title", "Supports %date% placeholder."),
                field("reportPeakLabel", "text", "Peak label", "Label for peak players in the report."),
                field("reportAverageLabel", "text", "Average label", "Label for average players in the report."),
                field("reportFooter", "text", "Report footer", "Footer text shown in the stats report."))));
            schema.put("vote", section("Vote", false, List.of(
                field("enabled", "boolean", "Enable vote listener", "Master switch for the Votifier listener."),
                field("host", "text", "Host", "Interface address used by the vote listener."),
                field("port", "number", "Port", "Listener port for vote events."),
                field("rsaPrivateKeyPath", "text", "RSA private key path", "Path to the private key relative to config/voidium."),
                field("rsaPublicKeyPath", "text", "RSA public key path", "Path for the public key file relative to config/voidium."),
                secretField("sharedSecret", "Shared secret", "NuVotifier shared secret used for token validation."),
                field("announceVotes", "boolean", "Announce votes", "Broadcast a message when a vote is paid out."),
                field("announcementMessage", "text", "Announcement message", "Supports %PLAYER% placeholder."),
                field("announcementCooldown", "number", "Announcement cooldown", "Cooldown in seconds between vote announcements."),
                field("maxVoteAgeHours", "number", "Max vote age hours", "Votes older than this are ignored."),
                field("commands", "multiline-list", "Reward commands", "One server command per line. Supports %PLAYER%."),
                field("logging.voteLog", "boolean", "Vote log file", "Write plain vote records to a log file."),
                field("logging.voteLogFile", "text", "Vote log path", "Path to the vote log file in storage."),
                field("logging.archiveJson", "boolean", "Archive JSON", "Append vote history as NDJSON."),
                field("logging.archivePath", "text", "Archive path", "Path to the vote history archive."),
                field("logging.notifyOpsOnError", "boolean", "Notify ops on error", "Warn operators when vote processing fails."),
                field("logging.pendingQueueFile", "text", "Pending queue path", "Storage file for pending offline votes."),
                field("logging.pendingVoteMessage", "text", "Pending vote message", "Shown when pending votes are paid out."))));
            schema.put("entitycleaner", section("EntityCleaner", false, List.of(
                field("enabled", "boolean", "Enable cleaner", "Turns automatic entity cleanup on or off."),
                field("cleanupIntervalSeconds", "number", "Cleanup interval", "Time between cleanups in seconds."),
                field("warningTimes", "multiline-list", "Warning times", "One number of seconds per line before cleanup."),
                field("removeDroppedItems", "boolean", "Remove dropped items", "Remove dropped item entities."),
                field("removePassiveMobs", "boolean", "Remove passive mobs", "Remove animals during cleanup."),
                field("removeHostileMobs", "boolean", "Remove hostile mobs", "Remove hostile mobs during cleanup."),
                field("removeXpOrbs", "boolean", "Remove XP orbs", "Remove XP orbs during cleanup."),
                field("removeArrows", "boolean", "Remove arrows", "Remove arrows stuck in blocks."),
                field("removeNamedEntities", "boolean", "Remove named entities", "When false, named entities are protected."),
                field("removeTamedAnimals", "boolean", "Remove tamed animals", "When false, tamed animals are protected."),
                field("protectBosses", "boolean", "Protect bosses", "When enabled, vanilla and detectable modded bosses are never removed."),
                field("entityWhitelist", "multiline-list", "Entity whitelist", "One entity ID per line that is never removed."),
                field("itemWhitelist", "multiline-list", "Item whitelist", "One item ID per line that is never removed."),
                field("warningMessage", "text", "Warning message", "Supports %seconds% placeholder."),
                field("cleanupMessage", "text", "Cleanup message", "Supports %items%, %mobs%, %xp%, %arrows%."))));
        schema.put("ai", section("AI", false, List.of(
                field("enablePlayerChat", "boolean", "Enable player AI", "Turns on /ai for players."),
            field("playerAccessMode", "select", "Player access mode", "Controls whether player AI is open, time-gated, Discord-role gated, or both.", List.of("ALL", "PLAYTIME", "DISCORD_ROLE", "PLAYTIME_OR_DISCORD_ROLE", "PLAYTIME_AND_DISCORD_ROLE")),
            field("playerAccessMinHours", "number", "Minimum played hours", "Used for playtime-based AI access. Set 0 to disable hour gating."),
            field("playerAccessDiscordRoleIds", "multiline-list", "Allowed Discord role IDs", "One Discord role ID per line for AI access gating."),
            field("disabledWorlds", "multiline-list", "Disabled worlds", "Dimension IDs where /ai is blocked (e.g. minecraft:the_nether). One per line."),
            field("disabledGameModes", "multiline-list", "Disabled game modes", "Game modes where /ai is blocked (e.g. spectator, adventure). One per line."),
                field("enableAdminAssistant", "boolean", "Enable admin assistant", "Turns on WebUI admin AI."),
                field("redactSensitiveValues", "boolean", "Redact secrets", "Masks sensitive values before admin AI sees config context."),
                field("playerPromptMaxLength", "number", "Player prompt max", "Maximum length of a player /ai prompt."),
                field("playerCooldownSeconds", "number", "Player cooldown", "Cooldown between player AI requests."),
                field("adminPromptMaxLength", "number", "Admin prompt max", "Maximum length of an admin AI prompt."),
                field("adminContextMaxChars", "number", "Admin context max", "Maximum size of context passed to admin AI."),
                field("playerApi.endpointUrl", "text", "Player endpoint URL", "OpenAI-compatible endpoint for player chat."),
                secretField("playerApi.apiKey", "Player API key", "Stored server-side for player AI."),
                field("playerApi.model", "text", "Player model", "Model name used for player chat."),
                field("playerApi.systemPrompt", "text", "Player system prompt", "Instructions for player AI replies."),
                field("playerApi.temperature", "number", "Player temperature", "Sampling temperature for player chat."),
                field("playerApi.maxTokens", "number", "Player max tokens", "Max generated tokens for player AI."),
                field("playerApi.timeoutSeconds", "number", "Player timeout", "Request timeout in seconds for player AI."),
                field("adminApi.endpointUrl", "text", "Admin endpoint URL", "OpenAI-compatible endpoint for admin AI."),
                secretField("adminApi.apiKey", "Admin API key", "Stored server-side for admin AI."),
                field("adminApi.model", "text", "Admin model", "Model name used for admin AI."),
                field("adminApi.systemPrompt", "text", "Admin system prompt", "Instructions for admin AI replies."),
                field("adminApi.temperature", "number", "Admin temperature", "Sampling temperature for admin AI."),
                field("adminApi.maxTokens", "number", "Admin max tokens", "Max generated tokens for admin AI."),
                field("adminApi.timeoutSeconds", "number", "Admin timeout", "Request timeout in seconds for admin AI."))));
        return localizeSchema(schema);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> exportJsonSchema() {
        Map<String, Object> internalSchema = getSchema();
        Map<String, Object> jsonSchema = new LinkedHashMap<>();
        jsonSchema.put("$schema", "http://json-schema.org/draft-07/schema#");
        jsonSchema.put("title", "Voidium Configuration");
        jsonSchema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> sectionEntry : internalSchema.entrySet()) {
            Map<String, Object> section = castMap(sectionEntry.getValue());
            Map<String, Object> sectionSchema = new LinkedHashMap<>();
            sectionSchema.put("type", "object");
            sectionSchema.put("title", stringValue(section.get("label")));

            Map<String, Object> fieldProps = new LinkedHashMap<>();
            List<Map<String, Object>> fields = (List<Map<String, Object>>) section.get("fields");
            for (Map<String, Object> field : fields) {
                Map<String, Object> fieldSchema = new LinkedHashMap<>();
                String type = stringValue(field.get("type"));
                switch (type) {
                    case "boolean" -> fieldSchema.put("type", "boolean");
                    case "number" -> fieldSchema.put("type", "number");
                    case "text", "secret" -> fieldSchema.put("type", "string");
                    case "select" -> {
                        fieldSchema.put("type", "string");
                        if (field.containsKey("options")) {
                            fieldSchema.put("enum", field.get("options"));
                        }
                    }
                    case "multiline-list" -> {
                        fieldSchema.put("type", "array");
                        fieldSchema.put("items", Map.of("type", "string"));
                    }
                    case "json", "rank-list", "discord-role-style-list" -> {
                        fieldSchema.put("type", "array");
                        fieldSchema.put("items", Map.of("type", "object"));
                    }
                    default -> fieldSchema.put("type", "string");
                }
                fieldSchema.put("title", stringValue(field.get("label")));
                fieldSchema.put("description", stringValue(field.get("description")));
                fieldProps.put(stringValue(field.get("key")), fieldSchema);
            }
            sectionSchema.put("properties", fieldProps);
            properties.put(sectionEntry.getKey(), sectionSchema);
        }
        jsonSchema.put("properties", properties);
        return jsonSchema;
    }

    public Map<String, Object> getValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        GeneralConfig general = GeneralConfig.getInstance();
        WebConfig web = WebConfig.getInstance();
        AnnouncementConfig announcements = AnnouncementConfig.getInstance();
        RestartConfig restart = RestartConfig.getInstance();
        RanksConfig ranks = RanksConfig.getInstance();
        PlayerListConfig playerList = PlayerListConfig.getInstance();
        TicketConfig tickets = TicketConfig.getInstance();
        DiscordConfig discord = DiscordConfig.getInstance();
        StatsConfig stats = StatsConfig.getInstance();
        VoteConfig vote = VoteConfig.getInstance();
        EntityCleanerConfig entityCleaner = EntityCleanerConfig.getInstance();
        AIConfig ai = AIConfig.getInstance();

        values.put("general", mapOf(
                entry("enableMod", general.isEnableMod()),
                entry("enableRestarts", general.isEnableRestarts()),
                entry("enableAnnouncements", general.isEnableAnnouncements()),
                entry("enableSkinRestorer", general.isEnableSkinRestorer()),
                entry("enableDiscord", general.isEnableDiscord()),
                entry("enableWeb", general.isEnableWeb()),
                entry("enableStats", general.isEnableStats()),
                entry("enableRanks", general.isEnableRanks()),
                entry("enableVote", general.isEnableVote()),
                entry("enablePlayerList", general.isEnablePlayerList()),
                entry("maintenanceMode", general.isMaintenanceMode()),
                entry("skinCacheHours", general.getSkinCacheHours()),
                entry("modPrefix", general.getModPrefix())));

        values.put("web", mapOf(
                entry("port", web.getPort()),
                entry("language", web.getLanguage()),
                entry("publicHostname", web.getPublicHostname()),
                entry("bindAddress", web.getBindAddress()),
                entry("adminToken", web.getAdminToken()),
                entry("sessionTtlMinutes", web.getSessionTtlMinutes())));

        values.put("announcements", mapOf(
                entry("prefix", announcements.getPrefix()),
                entry("announcementIntervalMinutes", announcements.getAnnouncementIntervalMinutes()),
                entry("announcements", new ArrayList<>(announcements.getAnnouncements()))));

        values.put("restart", mapOf(
                entry("restartType", restart.getRestartType().name()),
                entry("fixedRestartTimes", restart.getFixedRestartTimes().stream().map(LocalTime::toString).toList()),
                entry("intervalHours", restart.getIntervalHours()),
                entry("delayMinutes", restart.getDelayMinutes()),
                entry("warningMessage", restart.getWarningMessage()),
                entry("restartingNowMessage", restart.getRestartingNowMessage()),
                entry("kickMessage", restart.getKickMessage())));

        values.put("ranks", mapOf(
                entry("enableAutoRanks", ranks.isEnableAutoRanks()),
                entry("checkIntervalMinutes", ranks.getCheckIntervalMinutes()),
                entry("promotionMessage", ranks.getPromotionMessage()),
                entry("tooltipPlayed", ranks.getTooltipPlayed()),
                entry("tooltipRequired", ranks.getTooltipRequired()),
                entry("ranks", GSON.toJson(ranks.getRanks()))));

        values.put("playerlist", mapOf(
                entry("enableCustomPlayerList", playerList.isEnableCustomPlayerList()),
                entry("headerLine1", playerList.getHeaderLine1()),
                entry("headerLine2", playerList.getHeaderLine2()),
                entry("headerLine3", playerList.getHeaderLine3()),
                entry("footerLine1", playerList.getFooterLine1()),
                entry("footerLine2", playerList.getFooterLine2()),
                entry("footerLine3", playerList.getFooterLine3()),
                entry("enableCustomNames", playerList.isEnableCustomNames()),
                entry("playerNameFormat", playerList.getPlayerNameFormat()),
                entry("defaultPrefix", playerList.getDefaultPrefix()),
                entry("defaultSuffix", playerList.getDefaultSuffix()),
                entry("combineMultipleRanks", playerList.isCombineMultipleRanks()),
                entry("updateIntervalSeconds", playerList.getUpdateIntervalSeconds())));

            values.put("tickets", mapOf(
                entry("enableTickets", tickets.isEnableTickets()),
                entry("ticketCategoryId", tickets.getTicketCategoryId()),
                entry("supportRoleId", tickets.getSupportRoleId()),
                entry("ticketChannelTopic", tickets.getTicketChannelTopic()),
                entry("maxTicketsPerUser", tickets.getMaxTicketsPerUser()),
                entry("ticketCreatedMessage", tickets.getTicketCreatedMessage()),
                entry("ticketWelcomeMessage", tickets.getTicketWelcomeMessage()),
                entry("ticketCloseMessage", tickets.getTicketCloseMessage()),
                entry("noPermissionMessage", tickets.getNoPermissionMessage()),
                entry("ticketLimitReachedMessage", tickets.getTicketLimitReachedMessage()),
                entry("ticketAlreadyClosedMessage", tickets.getTicketAlreadyClosedMessage()),
                entry("enableTranscript", tickets.isEnableTranscript()),
                entry("transcriptFormat", tickets.getTranscriptFormat()),
                entry("transcriptFilename", tickets.getTranscriptFilename()),
                entry("mcBotNotConnectedMessage", tickets.getMcBotNotConnectedMessage()),
                entry("mcGuildNotFoundMessage", tickets.getMcGuildNotFoundMessage()),
                entry("mcCategoryNotFoundMessage", tickets.getMcCategoryNotFoundMessage()),
                entry("mcTicketCreatedMessage", tickets.getMcTicketCreatedMessage()),
                entry("mcDiscordNotFoundMessage", tickets.getMcDiscordNotFoundMessage())));

            values.put("discord", mapOf(
                entry("enableDiscord", discord.isEnableDiscord()),
                entry("botToken", maskSecret(discord.getBotToken())),
                entry("guildId", discord.getGuildId()),
                entry("botActivityType", discord.getBotActivityType()),
                entry("botActivityText", discord.getBotActivityText()),
                entry("enableWhitelist", discord.isEnableWhitelist()),
                entry("kickMessage", discord.getKickMessage()),
                entry("verificationHintMessage", discord.getVerificationHintMessage()),
                entry("linkSuccessMessage", discord.getLinkSuccessMessage()),
                entry("alreadyLinkedMessage", discord.getAlreadyLinkedMessage()),
                entry("maxAccountsPerDiscord", discord.getMaxAccountsPerDiscord()),
                entry("chatChannelId", discord.getChatChannelId()),
                entry("consoleChannelId", discord.getConsoleChannelId()),
                entry("linkChannelId", discord.getLinkChannelId()),
                entry("statusChannelId", discord.getStatusChannelId()),
                entry("enableStatusMessages", discord.isEnableStatusMessages()),
                entry("statusMessageStarting", discord.getStatusMessageStarting()),
                entry("statusMessageStarted", discord.getStatusMessageStarted()),
                entry("statusMessageStopping", discord.getStatusMessageStopping()),
                entry("statusMessageStopped", discord.getStatusMessageStopped()),
                entry("linkedRoleId", discord.getLinkedRoleId()),
                entry("renameOnLink", discord.isRenameOnLink()),
                entry("rolePrefixes", GSON.toJson(discord.getRolePrefixes())),
                entry("useHexColors", discord.isUseHexColors()),
                entry("syncBansDiscordToMc", discord.isSyncBansDiscordToMc()),
                entry("syncBansMcToDiscord", discord.isSyncBansMcToDiscord()),
                entry("enableChatBridge", discord.isEnableChatBridge()),
                entry("minecraftToDiscordFormat", discord.getMinecraftToDiscordFormat()),
                entry("discordToMinecraftFormat", discord.getDiscordToMinecraftFormat()),
                entry("translateEmojis", discord.isTranslateEmojis()),
                entry("chatWebhookUrl", discord.getChatWebhookUrl()),
                entry("enableTopicUpdate", discord.isEnableTopicUpdate()),
                entry("channelTopicFormat", discord.getChannelTopicFormat()),
                entry("uptimeFormat", discord.getUptimeFormat()),
                entry("invalidCodeMessage", discord.getInvalidCodeMessage()),
                entry("notLinkedMessage", discord.getNotLinkedMessage()),
                entry("alreadyLinkedSingleMessage", discord.getAlreadyLinkedSingleMessage()),
                entry("alreadyLinkedMultipleMessage", discord.getAlreadyLinkedMultipleMessage()),
                entry("unlinkSuccessMessage", discord.getUnlinkSuccessMessage()),
                entry("wrongGuildMessage", discord.getWrongGuildMessage()),
                entry("ticketCreatedMessage", discord.getTicketCreatedMessage()),
                entry("ticketClosingMessage", discord.getTicketClosingMessage()),
                entry("textChannelOnlyMessage", discord.getTextChannelOnlyMessage())));

            values.put("stats", mapOf(
                entry("enableStats", stats.isEnableStats()),
                entry("reportChannelId", stats.getReportChannelId()),
                entry("reportTime", stats.getReportTime().toString()),
                entry("reportTitle", stats.getReportTitle()),
                entry("reportPeakLabel", stats.getReportPeakLabel()),
                entry("reportAverageLabel", stats.getReportAverageLabel()),
                entry("reportFooter", stats.getReportFooter())));

            values.put("vote", mapOf(
                entry("enabled", vote.isEnabled()),
                entry("host", vote.getHost()),
                entry("port", vote.getPort()),
                entry("rsaPrivateKeyPath", vote.getRsaPrivateKeyPath()),
                entry("rsaPublicKeyPath", vote.getRsaPublicKeyPath()),
                entry("sharedSecret", maskSecret(vote.getSharedSecret())),
                entry("announceVotes", vote.isAnnounceVotes()),
                entry("announcementMessage", vote.getAnnouncementMessage()),
                entry("announcementCooldown", vote.getAnnouncementCooldown()),
                entry("maxVoteAgeHours", vote.getMaxVoteAgeHours()),
                entry("commands", new ArrayList<>(vote.getCommands())),
                entry("logging.voteLog", vote.getLogging().isVoteLog()),
                entry("logging.voteLogFile", vote.getLogging().getVoteLogFile()),
                entry("logging.archiveJson", vote.getLogging().isArchiveJson()),
                entry("logging.archivePath", vote.getLogging().getArchivePath()),
                entry("logging.notifyOpsOnError", vote.getLogging().isNotifyOpsOnError()),
                entry("logging.pendingQueueFile", vote.getLogging().getPendingQueueFile()),
                entry("logging.pendingVoteMessage", vote.getLogging().getPendingVoteMessage())));

            values.put("entitycleaner", mapOf(
                entry("enabled", entityCleaner.isEnabled()),
                entry("cleanupIntervalSeconds", entityCleaner.getCleanupIntervalSeconds()),
                entry("warningTimes", entityCleaner.getWarningTimes().stream().map(String::valueOf).toList()),
                entry("removeDroppedItems", entityCleaner.isRemoveDroppedItems()),
                entry("removePassiveMobs", entityCleaner.isRemovePassiveMobs()),
                entry("removeHostileMobs", entityCleaner.isRemoveHostileMobs()),
                entry("removeXpOrbs", entityCleaner.isRemoveXpOrbs()),
                entry("removeArrows", entityCleaner.isRemoveArrows()),
                entry("removeNamedEntities", entityCleaner.isRemoveNamedEntities()),
                entry("removeTamedAnimals", entityCleaner.isRemoveTamedAnimals()),
                entry("protectBosses", entityCleaner.isProtectBosses()),
                entry("entityWhitelist", new ArrayList<>(entityCleaner.getEntityWhitelist())),
                entry("itemWhitelist", new ArrayList<>(entityCleaner.getItemWhitelist())),
                entry("warningMessage", entityCleaner.getWarningMessage()),
                entry("cleanupMessage", entityCleaner.getCleanupMessage())));

        values.put("ai", mapOf(
                entry("enablePlayerChat", ai.isEnablePlayerChat()),
            entry("playerAccessMode", ai.getPlayerAccessMode()),
            entry("playerAccessMinHours", ai.getPlayerAccessMinHours()),
            entry("playerAccessDiscordRoleIds", new ArrayList<>(ai.getPlayerAccessDiscordRoleIds())),
            entry("disabledWorlds", new ArrayList<>(ai.getDisabledWorlds())),
            entry("disabledGameModes", new ArrayList<>(ai.getDisabledGameModes())),
                entry("enableAdminAssistant", ai.isEnableAdminAssistant()),
                entry("redactSensitiveValues", ai.isRedactSensitiveValues()),
                entry("playerPromptMaxLength", ai.getPlayerPromptMaxLength()),
                entry("playerCooldownSeconds", ai.getPlayerCooldownSeconds()),
                entry("adminPromptMaxLength", ai.getAdminPromptMaxLength()),
                entry("adminContextMaxChars", ai.getAdminContextMaxChars()),
                entry("playerApi.endpointUrl", ai.getPlayerApi().getEndpointUrl()),
                entry("playerApi.apiKey", maskSecret(ai.getPlayerApi().getApiKey())),
                entry("playerApi.model", ai.getPlayerApi().getModel()),
                entry("playerApi.systemPrompt", ai.getPlayerApi().getSystemPrompt()),
                entry("playerApi.temperature", ai.getPlayerApi().getTemperature()),
                entry("playerApi.maxTokens", ai.getPlayerApi().getMaxTokens()),
                entry("playerApi.timeoutSeconds", ai.getPlayerApi().getTimeoutSeconds()),
                entry("adminApi.endpointUrl", ai.getAdminApi().getEndpointUrl()),
                entry("adminApi.apiKey", maskSecret(ai.getAdminApi().getApiKey())),
                entry("adminApi.model", ai.getAdminApi().getModel()),
                entry("adminApi.systemPrompt", ai.getAdminApi().getSystemPrompt()),
                entry("adminApi.temperature", ai.getAdminApi().getTemperature()),
                entry("adminApi.maxTokens", ai.getAdminApi().getMaxTokens()),
                entry("adminApi.timeoutSeconds", ai.getAdminApi().getTimeoutSeconds())));
        return values;
    }

    public Map<String, Object> preview(JsonObject payload) {
        Map<String, Object> merged = mergeValues(payload);
        List<String> errors = validateMerged(merged);
        Map<String, Object> previews = new LinkedHashMap<>();
        previews.put("general", generalPreview(castMap(merged.get("general"))));
        previews.put("web", webPreview(castMap(merged.get("web"))));
        previews.put("announcements", announcementsPreview(castMap(merged.get("announcements"))));
        previews.put("restart", restartPreview(castMap(merged.get("restart"))));
        previews.put("ranks", ranksPreview(castMap(merged.get("ranks"))));
        previews.put("playerlist", playerListPreview(castMap(merged.get("playerlist"))));
        previews.put("tickets", ticketsPreview(castMap(merged.get("tickets"))));
        previews.put("discord", discordPreview(castMap(merged.get("discord"))));
        previews.put("stats", statsPreview(castMap(merged.get("stats"))));
        previews.put("vote", votePreview(castMap(merged.get("vote"))));
        previews.put("entitycleaner", entityCleanerPreview(castMap(merged.get("entitycleaner"))));
        previews.put("ai", aiPreview(castMap(merged.get("ai"))));
        return mapOf(entry("previews", previews), entry("validationErrors", errors), entry("valid", errors.isEmpty()));
    }

    public Map<String, Object> restoreDefaults(JsonObject payload) {
        String section = payload != null && payload.has("section") ? payload.get("section").getAsString() : "";
        Map<String, Object> values = mergeValues(payload);
        Map<String, Object> defaults = buildDefaultValues();
        if (!defaults.containsKey(section)) {
            return mapOf(entry("values", values), entry("message", msg("Defaults not available for section: ", "Výchozí hodnoty nejsou dostupné pro sekci: ") + section));
        }
        values.put(section, defaults.get(section));
        return mapOf(entry("values", values), entry("message", msg("Defaults restored for ", "Výchozí hodnoty obnoveny pro ") + section + "."));
    }

    public Map<String, Object> applyLocalePreset(JsonObject payload) {
        String section = payload != null && payload.has("section") ? payload.get("section").getAsString() : "";
        String locale = payload != null && payload.has("locale") ? payload.get("locale").getAsString() : "en";
        Map<String, Object> values = mergeValues(payload);
        if (!Set.of("en", "cz").contains(locale)) {
            locale = "en";
        }
        switch (section) {
            case "general" -> values.put(section, generalLocaleValues(locale));
            case "announcements" -> values.put(section, announcementLocaleValues(locale));
            case "restart" -> values.put(section, restartLocaleValues(locale));
            case "ranks" -> values.put(section, ranksLocaleValues(locale));
            case "playerlist" -> values.put(section, playerListLocaleValues(locale));
            case "tickets" -> values.put(section, ticketLocaleValues(locale));
            case "discord" -> values.put(section, discordLocaleValues(locale));
            case "stats" -> values.put(section, statsLocaleValues(locale));
            case "vote" -> values.put(section, voteLocaleValues(locale));
            case "entitycleaner" -> values.put(section, entityCleanerLocaleValues(locale));
            case "web" -> castMap(values.get(section)).put("language", locale);
            default -> {
                return mapOf(entry("values", values), entry("message", msg("Locale preset not supported for section: ", "Jazykový preset není podporován pro sekci: ") + section));
            }
        }
        return mapOf(entry("values", values), entry("message", msg("Applied ", "Použit preset ") + locale.toUpperCase() + msg(" for ", " pro ") + section + "."));
    }

    public Map<String, Object> stageAiSuggestion(JsonObject payload) {
        Map<String, Object> merged = deepCopy(getValues());
        List<String> warnings = new ArrayList<>();
        int appliedChanges = 0;
        JsonArray changes = payload != null && payload.has("changes") && payload.get("changes").isJsonArray()
                ? payload.getAsJsonArray("changes")
                : new JsonArray();

        for (JsonElement element : changes) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject change = element.getAsJsonObject();
            String file = change.has("file") ? change.get("file").getAsString() : "";
            String path = change.has("path") ? change.get("path").getAsString() : "";
            String proposed = change.has("proposed") ? change.get("proposed").getAsString() : "";
            String section = sectionForSuggestedFile(file);
            if (section.isBlank()) {
                warnings.add("Unsupported AI suggestion file: " + file);
                continue;
            }
            Map<String, Object> sectionValues = castMap(merged.get(section));
            String field = normalizeSuggestedPath(path, section);
            if (!sectionValues.containsKey(field)) {
                warnings.add("Unsupported AI suggestion path: " + file + " -> " + path);
                continue;
            }
            Object currentValue = sectionValues.get(field);
            try {
                sectionValues.put(field, coerceSuggestedValue(currentValue, proposed));
                appliedChanges++;
            } catch (Exception e) {
                warnings.add("Failed to stage AI suggestion for " + file + " -> " + path + ": " + e.getMessage());
            }
        }

        JsonObject stagedPayload = payloadForValues(merged);
        Map<String, Object> diff = previewDiff(stagedPayload);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> diffChanges = (List<Map<String, Object>>) diff.get("changes");
        warnings.addAll(stringList(payload == null ? null : fromJson(payload.get("warnings"), List.of())));
        return mapOf(
                entry("staged", appliedChanges > 0 && !diffChanges.isEmpty()),
                entry("values", merged),
                entry("appliedChangeCount", appliedChanges),
                entry("warnings", warnings),
                entry("stagedSummary", diff.get("summary")),
                entry("stagedDiffPreview", formatDiffPreview(diffChanges)),
                entry("stagedPreview", diff));
    }

    public Map<String, Object> previewDiff(JsonObject payload) {
        Map<String, Object> merged = mergeValues(payload);
        List<Map<String, Object>> changes = new ArrayList<>();
        boolean requiresWebRestart = false;
        Map<String, Object> current = getValues();

        for (Map.Entry<String, Object> sectionEntry : current.entrySet()) {
            String section = sectionEntry.getKey();
            Map<String, Object> currentSection = castMap(sectionEntry.getValue());
            Map<String, Object> proposedSection = castMap(merged.get(section));
            for (Map.Entry<String, Object> fieldEntry : currentSection.entrySet()) {
                String field = fieldEntry.getKey();
                Object currentValue = fieldEntry.getValue();
                Object proposedValue = proposedSection.getOrDefault(field, currentValue);
                if (!equalsValue(currentValue, proposedValue)) {
                    changes.add(change(section, field, currentValue, proposedValue));
                    if ("web".equals(section) && WEB_RESTART_FIELDS.contains(field)) {
                        requiresWebRestart = true;
                    }
                }
            }
        }

        Map<String, Object> preview = preview(payload);
        return mapOf(
                entry("changes", changes),
                entry("changeCount", changes.size()),
                entry("requiresWebRestart", requiresWebRestart),
            entry("recommendedAction", summarizeImpact(changes)),
                entry("validationErrors", preview.get("validationErrors")),
                entry("previews", preview.get("previews")),
                entry("summary", changes.isEmpty() ? msg("No changes detected.", "Nebyly zjištěny žádné změny.") : changes.size() + msg(" change(s) ready.", " změn připraveno.")));
    }

    private JsonObject payloadForValues(Map<String, Object> values) {
        JsonObject payload = new JsonObject();
        payload.add("values", GSON.toJsonTree(values).getAsJsonObject());
        return payload;
    }

    private String sectionForSuggestedFile(String file) {
        return switch (stringValue(file).toLowerCase()) {
            case "general.json" -> "general";
            case "web.json" -> "web";
            case "announcements.json" -> "announcements";
            case "restart.json" -> "restart";
            case "ranks.json" -> "ranks";
            case "playerlist.json" -> "playerlist";
            case "tickets.json" -> "tickets";
            case "discord.json" -> "discord";
            case "stats.json" -> "stats";
            case "votes.json" -> "vote";
            case "entitycleaner.json" -> "entitycleaner";
            case "ai.json" -> "ai";
            default -> "";
        };
    }

    private String normalizeSuggestedPath(String path, String section) {
        String normalized = stringValue(path).trim()
                .replace("\\", ".")
                .replace("/", ".")
                .replace("[", ".")
                .replace("]", "")
                .replace("\"", "")
                .replace("'", "");
        while (normalized.startsWith("$.")) {
            normalized = normalized.substring(2);
        }
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("values." + section + ".")) {
            normalized = normalized.substring(("values." + section + ".").length());
        } else if (normalized.startsWith(section + ".")) {
            normalized = normalized.substring((section + ".").length());
        }
        return normalized;
    }

    private Object coerceSuggestedValue(Object currentValue, String proposed) {
        if (currentValue instanceof Boolean) {
            String normalized = stringValue(proposed).trim().toLowerCase();
            return Set.of("true", "1", "yes", "on", "enabled").contains(normalized);
        }
        if (currentValue instanceof Integer) {
            return Integer.parseInt(stringValue(proposed).trim().replaceAll("[^0-9-]", ""));
        }
        if (currentValue instanceof Double || currentValue instanceof Float) {
            return Double.parseDouble(stringValue(proposed).trim().replace(',', '.').replaceAll("[^0-9.-]", ""));
        }
        if (currentValue instanceof List<?>) {
            String trimmed = stringValue(proposed).trim();
            if (trimmed.startsWith("[")) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Object> parsed = GSON.fromJson(trimmed, List.class);
                    return parsed == null ? List.of() : parsed.stream().map(this::stringValue).toList();
                } catch (Exception ignored) {
                }
            }
            return trimmed.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
        }
        return proposed;
    }

    private String formatDiffPreview(List<Map<String, Object>> changes) {
        if (changes == null || changes.isEmpty()) {
            return msg("No staged config changes.", "Žádné staged config změny.");
        }
        return changes.stream()
                .map(change -> stringValue(change.get("section")) + "." + stringValue(change.get("field"))
                        + "\n- " + formatPreviewValue(change.get("current"))
                        + "\n+ " + formatPreviewValue(change.get("proposed")))
                .collect(java.util.stream.Collectors.joining("\n\n"));
    }

    private String formatPreviewValue(Object value) {
        return value instanceof List<?> list ? list.stream().map(this::stringValue).collect(java.util.stream.Collectors.joining(" | ")) : stringValue(value);
    }

    public Map<String, Object> apply(JsonObject payload) throws IOException {
        Map<String, Object> merged = mergeValues(payload);
        List<String> errors = validateMerged(merged);
        if (!errors.isEmpty()) {
            return mapOf(entry("applied", false), entry("message", msg("Validation failed.", "Validace selhala.")), entry("validationErrors", errors));
        }

        Map<String, Object> diff = previewDiff(payload);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> changes = (List<Map<String, Object>>) diff.get("changes");
        if (changes.isEmpty()) {
            return mapOf(entry("applied", false), entry("message", msg("No changes to apply.", "Nejsou žádné změny k použití.")), entry("validationErrors", List.of()));
        }

        Path backupDir = createBackup(CONFIG_FILES);
        applyGeneral(castMap(merged.get("general")));
        applyWeb(castMap(merged.get("web")));
        applyAnnouncements(castMap(merged.get("announcements")));
        applyRestart(castMap(merged.get("restart")));
        applyRanks(castMap(merged.get("ranks")));
        applyPlayerList(castMap(merged.get("playerlist")));
        applyTickets(castMap(merged.get("tickets")));
        applyDiscord(castMap(merged.get("discord")));
        applyStats(castMap(merged.get("stats")));
        applyVote(castMap(merged.get("vote")));
        applyEntityCleaner(castMap(merged.get("entitycleaner")));
        applyAI(castMap(merged.get("ai")));

        return mapOf(
                entry("applied", true),
                entry("message", msg("Configuration saved.", "Konfigurace uložena.")),
                entry("backupDir", backupDir.toString()),
                entry("requiresWebRestart", diff.get("requiresWebRestart")),
            entry("recommendedAction", diff.get("recommendedAction")),
                entry("changes", changes),
                entry("validationErrors", List.of()));
    }

    public Map<String, Object> rollbackLatest() throws IOException {
        Path backupRoot = getBackupRoot();
        if (!Files.exists(backupRoot)) {
            return mapOf(entry("rolledBack", false), entry("message", msg("No backups found.", "Nebyly nalezeny žádné zálohy.")));
        }

        Path latest = Files.list(backupRoot)
                .filter(Files::isDirectory)
                .max(Comparator.comparing(Path::getFileName))
                .orElse(null);
        if (latest == null) {
            return mapOf(entry("rolledBack", false), entry("message", msg("No backups found.", "Nebyly nalezeny žádné zálohy.")));
        }

        Path configDir = getConfigDir();
        List<String> restoredFiles = new ArrayList<>();
        Files.list(latest).filter(Files::isRegularFile).forEach(path -> {
            try {
                Path target = configDir.resolve(path.getFileName().toString());
                Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                restoredFiles.add(path.getFileName().toString());
            } catch (IOException ignored) {
            }
        });

        return mapOf(
                entry("rolledBack", !restoredFiles.isEmpty()),
                entry("message", restoredFiles.isEmpty() ? msg("Rollback failed.", "Rollback selhal.") : msg("Restored latest backup.", "Obnovena poslední záloha.")),
                entry("restoredFiles", restoredFiles),
                entry("backupDir", latest.toString()));
    }

    private Map<String, Object> mergeValues(JsonObject payload) {
        Map<String, Object> merged = deepCopy(getValues());
        JsonObject values = payload != null && payload.has("values") ? payload.getAsJsonObject("values") : new JsonObject();
        for (Map.Entry<String, Object> sectionEntry : merged.entrySet()) {
            String section = sectionEntry.getKey();
            if (!values.has(section) || !values.get(section).isJsonObject()) {
                continue;
            }
            Map<String, Object> currentSection = castMap(sectionEntry.getValue());
            JsonObject proposedSection = values.getAsJsonObject(section);
            for (Map.Entry<String, Object> fieldEntry : currentSection.entrySet()) {
                String field = fieldEntry.getKey();
                Object currentValue = fieldEntry.getValue();
                if (proposedSection.has(field)) {
                    currentSection.put(field, fromJson(proposedSection.get(field), currentValue));
                }
            }
        }
        return merged;
    }

    private List<String> validateMerged(Map<String, Object> merged) {
        List<String> errors = new ArrayList<>();
        Map<String, Object> web = castMap(merged.get("web"));
        int port = intValue(web.get("port"));
        int sessionTtl = intValue(web.get("sessionTtlMinutes"));
        if (port < 1 || port > 65535) {
            errors.add("web.port must be between 1 and 65535.");
        }
        if (sessionTtl < 5) {
            errors.add("web.sessionTtlMinutes must be at least 5.");
        }
        if (stringValue(web.get("publicHostname")).isBlank()) {
            errors.add("web.publicHostname cannot be blank.");
        }
        if (stringValue(web.get("bindAddress")).isBlank()) {
            errors.add("web.bindAddress cannot be blank.");
        }

        Map<String, Object> general = castMap(merged.get("general"));
        if (intValue(general.get("skinCacheHours")) < 1) {
            errors.add("general.skinCacheHours must be at least 1.");
        }

        Map<String, Object> restart = castMap(merged.get("restart"));
        String restartType = stringValue(restart.get("restartType"));
        if (!List.of("FIXED_TIME", "INTERVAL", "DELAY").contains(restartType)) {
            errors.add("restart.restartType must be FIXED_TIME, INTERVAL, or DELAY.");
        }
        @SuppressWarnings("unchecked")
        List<String> fixedTimes = (List<String>) restart.get("fixedRestartTimes");
        if ("FIXED_TIME".equals(restartType)) {
            if (fixedTimes.isEmpty()) {
                errors.add("restart.fixedRestartTimes must contain at least one HH:MM time for FIXED_TIME mode.");
            }
            for (String time : fixedTimes) {
                try {
                    LocalTime.parse(time.trim());
                } catch (DateTimeParseException e) {
                    errors.add("restart.fixedRestartTimes contains invalid time: " + time);
                }
            }
        }
        if ("INTERVAL".equals(restartType) && intValue(restart.get("intervalHours")) < 1) {
            errors.add("restart.intervalHours must be at least 1 for INTERVAL mode.");
        }
        if ("DELAY".equals(restartType) && intValue(restart.get("delayMinutes")) < 1) {
            errors.add("restart.delayMinutes must be at least 1 for DELAY mode.");
        }

        Map<String, Object> ranks = castMap(merged.get("ranks"));
        if (intValue(ranks.get("checkIntervalMinutes")) < 1) {
            errors.add("ranks.checkIntervalMinutes must be at least 1.");
        }
        errors.addAll(validateRanksJson(stringValue(ranks.get("ranks"))));

        Map<String, Object> playerList = castMap(merged.get("playerlist"));
        if (intValue(playerList.get("updateIntervalSeconds")) < 3) {
            errors.add("playerlist.updateIntervalSeconds must be at least 3.");
        }

        Map<String, Object> tickets = castMap(merged.get("tickets"));
        if (intValue(tickets.get("maxTicketsPerUser")) < 1) {
            errors.add("tickets.maxTicketsPerUser must be at least 1.");
        }
        if (!List.of("TXT", "JSON").contains(stringValue(tickets.get("transcriptFormat")))) {
            errors.add("tickets.transcriptFormat must be TXT or JSON.");
        }

        Map<String, Object> discord = castMap(merged.get("discord"));
        if (!List.of("PLAYING", "WATCHING", "LISTENING", "COMPETING").contains(stringValue(discord.get("botActivityType")))) {
            errors.add("discord.botActivityType is invalid.");
        }
        if (intValue(discord.get("maxAccountsPerDiscord")) < 1) {
            errors.add("discord.maxAccountsPerDiscord must be at least 1.");
        }
        try {
            GSON.fromJson(stringValue(discord.get("rolePrefixes")), new TypeToken<Map<String, DiscordConfig.RoleStyle>>() {}.getType());
        } catch (Exception e) {
            errors.add("discord.rolePrefixes must be valid role style JSON.");
        }

        Map<String, Object> ai = castMap(merged.get("ai"));
        if (intValue(ai.get("playerPromptMaxLength")) < 32) {
            errors.add("ai.playerPromptMaxLength must be at least 32.");
        }
        if (!List.of("ALL", "PLAYTIME", "DISCORD_ROLE", "PLAYTIME_OR_DISCORD_ROLE", "PLAYTIME_AND_DISCORD_ROLE").contains(stringValue(ai.get("playerAccessMode")))) {
            errors.add("ai.playerAccessMode is invalid.");
        }
        if (intValue(ai.get("playerAccessMinHours")) < 0) {
            errors.add("ai.playerAccessMinHours cannot be negative.");
        }
        if (intValue(ai.get("adminPromptMaxLength")) < 200) {
            errors.add("ai.adminPromptMaxLength must be at least 200.");
        }
        if (intValue(ai.get("adminContextMaxChars")) < 1000) {
            errors.add("ai.adminContextMaxChars must be at least 1000.");
        }
        if (intValue(ai.get("playerCooldownSeconds")) < 0) {
            errors.add("ai.playerCooldownSeconds cannot be negative.");
        }
        if (stringValue(ai.get("playerApi.endpointUrl")).isBlank()) {
            errors.add("ai.playerApi.endpointUrl cannot be blank.");
        }
        if (stringValue(ai.get("adminApi.endpointUrl")).isBlank()) {
            errors.add("ai.adminApi.endpointUrl cannot be blank.");
        }
        if (stringValue(ai.get("playerApi.model")).isBlank()) {
            errors.add("ai.playerApi.model cannot be blank.");
        }
        if (stringValue(ai.get("adminApi.model")).isBlank()) {
            errors.add("ai.adminApi.model cannot be blank.");
        }

        Map<String, Object> stats = castMap(merged.get("stats"));
        try {
            LocalTime.parse(stringValue(stats.get("reportTime")));
        } catch (Exception e) {
            errors.add("stats.reportTime must be a valid HH:MM value.");
        }

        Map<String, Object> vote = castMap(merged.get("vote"));
        if (stringValue(vote.get("host")).isBlank()) {
            errors.add("vote.host cannot be blank.");
        }
        if (intValue(vote.get("port")) < 1 || intValue(vote.get("port")) > 65535) {
            errors.add("vote.port must be between 1 and 65535.");
        }
        if (intValue(vote.get("announcementCooldown")) < 0) {
            errors.add("vote.announcementCooldown cannot be negative.");
        }
        if (intValue(vote.get("maxVoteAgeHours")) < 1) {
            errors.add("vote.maxVoteAgeHours must be at least 1.");
        }
        if (stringList(vote.get("commands")).isEmpty()) {
            errors.add("vote.commands must contain at least one command.");
        }

        Map<String, Object> entityCleaner = castMap(merged.get("entitycleaner"));
        if (intValue(entityCleaner.get("cleanupIntervalSeconds")) < 10) {
            errors.add("entitycleaner.cleanupIntervalSeconds must be at least 10.");
        }
        errors.addAll(validateIntegerList("entitycleaner.warningTimes", stringList(entityCleaner.get("warningTimes")), 0));
        return errors;
    }

    private List<String> validateRanksJson(String value) {
        List<String> errors = new ArrayList<>();
        try {
            RanksConfig.RankDefinition[] parsed = GSON.fromJson(value, RanksConfig.RankDefinition[].class);
            if (parsed == null) {
                errors.add("ranks.ranks must be a JSON array.");
                return errors;
            }
            for (RanksConfig.RankDefinition rank : parsed) {
                if (rank.type == null || rank.type.isBlank()) {
                    errors.add("ranks.ranks contains an entry without type.");
                }
                if (rank.value == null) {
                    errors.add("ranks.ranks contains an entry without value.");
                }
                if (rank.hours < 0) {
                    errors.add("ranks.ranks contains an entry with negative hours.");
                }
            }
        } catch (Exception e) {
            errors.add("ranks.ranks must be valid JSON array: " + e.getMessage());
        }
        return errors;
    }

    private String generalPreview(Map<String, Object> section) {
        String prefix = stringValue(section.get("modPrefix"));
        boolean maint = boolValue(section.get("maintenanceMode"));
        String base = msg("Prefix preview: ", "Náhled prefixu: ") + prefix + msg("Example message", "Ukázková zpráva");
        if (maint) {
            base += "\n⚠ " + msg("MAINTENANCE MODE IS ON – players cannot join.", "REŽIM ÚDRŽBY JE ZAPNUTÝ – hráči se nemohou připojit.");
        }
        return base;
    }

    private String webPreview(Map<String, Object> section) {
        return msg("Base URL: http://", "Základní URL: http://") + stringValue(section.get("publicHostname")) + ":" + intValue(section.get("port"))
            + "\n" + msg("Session TTL: ", "TTL sezení: ") + intValue(section.get("sessionTtlMinutes")) + " min";
    }

    private String announcementsPreview(Map<String, Object> section) {
        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>) section.get("announcements");
        String sample = lines.isEmpty() ? msg("No announcements configured.", "Nejsou nastavená žádná oznámení.") : lines.get(0);
        return stringValue(section.get("prefix")) + sample;
    }

    private String restartPreview(Map<String, Object> section) {
        String type = stringValue(section.get("restartType"));
        String schedule;
        if ("FIXED_TIME".equals(type)) {
            @SuppressWarnings("unchecked")
            List<String> times = (List<String>) section.get("fixedRestartTimes");
            schedule = msg("Fixed: ", "Pevné časy: ") + String.join(", ", times);
        } else if ("INTERVAL".equals(type)) {
            schedule = msg("Every ", "Každých ") + intValue(section.get("intervalHours")) + "h";
        } else {
            schedule = msg("After ", "Po ") + intValue(section.get("delayMinutes")) + msg(" min from startup", " min od startu");
        }
        return schedule + "\n" + msg("Warn: ", "Varování: ") + stringValue(section.get("warningMessage"));
    }

    private String ranksPreview(Map<String, Object> section) {
        String rankSummary;
        try {
            RanksConfig.RankDefinition[] parsed = GSON.fromJson(stringValue(section.get("ranks")), RanksConfig.RankDefinition[].class);
            rankSummary = parsed == null ? msg("0 rank entries", "0 rank položek") : parsed.length + msg(" rank entries", " rank položek");
        } catch (Exception ignored) {
            rankSummary = msg("rank JSON invalid", "JSON ranků je neplatný");
        }
        return msg("Promotion: ", "Povýšení: ") + stringValue(section.get("promotionMessage")) + "\n" + rankSummary;
    }

    private String playerListPreview(Map<String, Object> section) {
        return stringValue(section.get("headerLine1")) + "\n" + stringValue(section.get("headerLine2"))
            + "\n" + msg("Name sample: ", "Ukázka jména: ") + stringValue(section.get("playerNameFormat")).replace("%rank_prefix%", stringValue(section.get("defaultPrefix")))
                .replace("%player_name%", "Steve").replace("%rank_suffix%", stringValue(section.get("defaultSuffix")));
    }

    private String ticketsPreview(Map<String, Object> section) {
        return msg("Category: ", "Kategorie: ") + stringValue(section.get("ticketCategoryId"))
                + "\n" + msg("Topic preview: ", "Náhled topicu: ") + stringValue(section.get("ticketChannelTopic")).replace("%user%", "User#1234").replace("%reason%", "Support")
                + "\n" + msg("Transcript: ", "Transcript: ") + boolValue(section.get("enableTranscript")) + " / " + stringValue(section.get("transcriptFormat"));
    }

    private String discordPreview(Map<String, Object> section) {
        return msg("Guild: ", "Guild: ") + stringValue(section.get("guildId"))
                + "\n" + msg("Activity: ", "Aktivita: ") + stringValue(section.get("botActivityType")) + " " + stringValue(section.get("botActivityText"))
                + "\n" + msg("Bridge: ", "Bridge: ") + boolValue(section.get("enableChatBridge"))
            + "\n" + msg("Topic: ", "Topic: ") + stringValue(section.get("channelTopicFormat"))
            + "\n" + msg("Role styles: ", "Role styly: ") + countRoleStyles(stringValue(section.get("rolePrefixes")));
    }

    private String countRoleStyles(String raw) {
        try {
            Map<String, DiscordConfig.RoleStyle> styles = GSON.fromJson(raw, new TypeToken<Map<String, DiscordConfig.RoleStyle>>() {}.getType());
            return String.valueOf(styles == null ? 0 : styles.size());
        } catch (Exception ignored) {
            return msg("invalid JSON", "neplatný JSON");
        }
    }

    private String aiPreview(Map<String, Object> section) {
        return msg("Player AI: ", "Hráčské AI: ") + boolValue(section.get("enablePlayerChat")) + msg(" using ", " používá ") + stringValue(section.get("playerApi.model"))
            + "\n" + msg("Player access: ", "Přístup hráčů: ") + stringValue(section.get("playerAccessMode"))
            + "\n" + msg("Admin AI: ", "Admin AI: ") + boolValue(section.get("enableAdminAssistant")) + msg(" using ", " používá ") + stringValue(section.get("adminApi.model"));
    }

    private String statsPreview(Map<String, Object> section) {
        return msg("Daily report at ", "Denní report v ") + stringValue(section.get("reportTime"))
                + "\n" + stringValue(section.get("reportTitle")).replace("%date%", "2026-03-14");
    }

    private String votePreview(Map<String, Object> section) {
        List<String> commands = stringList(section.get("commands"));
        String sampleCommand = commands.isEmpty() ? msg("No commands configured.", "Nejsou nastavené žádné příkazy.") : commands.get(0).replace("%PLAYER%", "Steve");
        return msg("Announcement: ", "Oznámení: ") + stringValue(section.get("announcementMessage")).replace("%PLAYER%", "Steve")
                + "\n" + msg("First command: ", "První příkaz: ") + sampleCommand;
    }

    private String entityCleanerPreview(Map<String, Object> section) {
        return msg("Every ", "Každých ") + intValue(section.get("cleanupIntervalSeconds")) + msg(" seconds", " sekund")
                + "\n" + msg("Boss protection: ", "Ochrana bossů: ") + boolValue(section.get("protectBosses"))
                + "\n" + stringValue(section.get("warningMessage")).replace("%seconds%", "30")
                + "\n" + stringValue(section.get("cleanupMessage")).replace("%items%", "12").replace("%mobs%", "4").replace("%xp%", "7").replace("%arrows%", "2");
    }

    private void applyGeneral(Map<String, Object> values) {
        GeneralConfig config = GeneralConfig.getInstance();
        config.setEnableMod(boolValue(values.get("enableMod")));
        config.setEnableRestarts(boolValue(values.get("enableRestarts")));
        config.setEnableAnnouncements(boolValue(values.get("enableAnnouncements")));
        config.setEnableSkinRestorer(boolValue(values.get("enableSkinRestorer")));
        config.setEnableDiscord(boolValue(values.get("enableDiscord")));
        config.setEnableWeb(boolValue(values.get("enableWeb")));
        config.setEnableStats(boolValue(values.get("enableStats")));
        config.setEnableRanks(boolValue(values.get("enableRanks")));
        config.setEnableVote(boolValue(values.get("enableVote")));
        config.setEnablePlayerList(boolValue(values.get("enablePlayerList")));
        config.setMaintenanceMode(boolValue(values.get("maintenanceMode")));
        config.setSkinCacheHours(intValue(values.get("skinCacheHours")));
        config.setModPrefix(stringValue(values.get("modPrefix")));
        config.save();
    }

    private void applyWeb(Map<String, Object> values) {
        WebConfig config = WebConfig.getInstance();
        config.setPort(intValue(values.get("port")));
        config.setLanguage(stringValue(values.get("language")));
        config.setPublicHostname(stringValue(values.get("publicHostname")));
        config.setBindAddress(stringValue(values.get("bindAddress")));
        if (!isMasked(values.get("adminToken"))) {
            config.setAdminToken(stringValue(values.get("adminToken")));
        }
        config.setSessionTtlMinutes(intValue(values.get("sessionTtlMinutes")));
        config.save();
    }

    private void applyAnnouncements(Map<String, Object> values) {
        AnnouncementConfig config = AnnouncementConfig.getInstance();
        config.setPrefix(stringValue(values.get("prefix")));
        config.setAnnouncementIntervalMinutes(intValue(values.get("announcementIntervalMinutes")));
        config.setAnnouncements(stringList(values.get("announcements")));
        config.save();
    }

    private void applyRestart(Map<String, Object> values) {
        RestartConfig config = RestartConfig.getInstance();
        config.setRestartType(RestartConfig.RestartType.valueOf(stringValue(values.get("restartType"))));
        config.setFixedRestartTimes(stringList(values.get("fixedRestartTimes")).stream().map(LocalTime::parse).toList());
        config.setIntervalHours(intValue(values.get("intervalHours")));
        config.setDelayMinutes(intValue(values.get("delayMinutes")));
        config.setWarningMessage(stringValue(values.get("warningMessage")));
        config.setRestartingNowMessage(stringValue(values.get("restartingNowMessage")));
        config.setKickMessage(stringValue(values.get("kickMessage")));
        config.save();
    }

    private void applyRanks(Map<String, Object> values) {
        RanksConfig config = RanksConfig.getInstance();
        config.setEnableAutoRanks(boolValue(values.get("enableAutoRanks")));
        config.setCheckIntervalMinutes(intValue(values.get("checkIntervalMinutes")));
        config.setPromotionMessage(stringValue(values.get("promotionMessage")));
        config.setTooltipPlayed(stringValue(values.get("tooltipPlayed")));
        config.setTooltipRequired(stringValue(values.get("tooltipRequired")));
        RanksConfig.RankDefinition[] rankDefinitions = GSON.fromJson(stringValue(values.get("ranks")), RanksConfig.RankDefinition[].class);
        config.setRanks(rankDefinitions == null ? List.of() : List.of(rankDefinitions));
        config.save();
    }

    private void applyPlayerList(Map<String, Object> values) {
        PlayerListConfig config = PlayerListConfig.getInstance();
        config.setEnableCustomPlayerList(boolValue(values.get("enableCustomPlayerList")));
        config.setHeaderLine1(stringValue(values.get("headerLine1")));
        config.setHeaderLine2(stringValue(values.get("headerLine2")));
        config.setHeaderLine3(stringValue(values.get("headerLine3")));
        config.setFooterLine1(stringValue(values.get("footerLine1")));
        config.setFooterLine2(stringValue(values.get("footerLine2")));
        config.setFooterLine3(stringValue(values.get("footerLine3")));
        config.setEnableCustomNames(boolValue(values.get("enableCustomNames")));
        config.setPlayerNameFormat(stringValue(values.get("playerNameFormat")));
        config.setDefaultPrefix(stringValue(values.get("defaultPrefix")));
        config.setDefaultSuffix(stringValue(values.get("defaultSuffix")));
        config.setCombineMultipleRanks(boolValue(values.get("combineMultipleRanks")));
        config.setUpdateIntervalSeconds(intValue(values.get("updateIntervalSeconds")));
        config.save();
    }

    private void applyTickets(Map<String, Object> values) {
        TicketConfig config = TicketConfig.getInstance();
        config.setEnableTickets(boolValue(values.get("enableTickets")));
        config.setTicketCategoryId(stringValue(values.get("ticketCategoryId")));
        config.setSupportRoleId(stringValue(values.get("supportRoleId")));
        config.setTicketChannelTopic(stringValue(values.get("ticketChannelTopic")));
        config.setMaxTicketsPerUser(intValue(values.get("maxTicketsPerUser")));
        config.setTicketCreatedMessage(stringValue(values.get("ticketCreatedMessage")));
        config.setTicketWelcomeMessage(stringValue(values.get("ticketWelcomeMessage")));
        config.setTicketCloseMessage(stringValue(values.get("ticketCloseMessage")));
        config.setNoPermissionMessage(stringValue(values.get("noPermissionMessage")));
        config.setTicketLimitReachedMessage(stringValue(values.get("ticketLimitReachedMessage")));
        config.setTicketAlreadyClosedMessage(stringValue(values.get("ticketAlreadyClosedMessage")));
        config.setEnableTranscript(boolValue(values.get("enableTranscript")));
        config.setTranscriptFormat(stringValue(values.get("transcriptFormat")));
        config.setTranscriptFilename(stringValue(values.get("transcriptFilename")));
        config.setMcBotNotConnectedMessage(stringValue(values.get("mcBotNotConnectedMessage")));
        config.setMcGuildNotFoundMessage(stringValue(values.get("mcGuildNotFoundMessage")));
        config.setMcCategoryNotFoundMessage(stringValue(values.get("mcCategoryNotFoundMessage")));
        config.setMcTicketCreatedMessage(stringValue(values.get("mcTicketCreatedMessage")));
        config.setMcDiscordNotFoundMessage(stringValue(values.get("mcDiscordNotFoundMessage")));
        config.save();
    }

    private void applyDiscord(Map<String, Object> values) {
        DiscordConfig config = DiscordConfig.getInstance();
        config.setEnableDiscord(boolValue(values.get("enableDiscord")));
        if (!isMasked(values.get("botToken"))) {
            config.setBotToken(stringValue(values.get("botToken")));
        }
        config.setGuildId(stringValue(values.get("guildId")));
        config.setBotActivityType(stringValue(values.get("botActivityType")));
        config.setBotActivityText(stringValue(values.get("botActivityText")));
        config.setEnableWhitelist(boolValue(values.get("enableWhitelist")));
        config.setKickMessage(stringValue(values.get("kickMessage")));
        config.setVerificationHintMessage(stringValue(values.get("verificationHintMessage")));
        config.setLinkSuccessMessage(stringValue(values.get("linkSuccessMessage")));
        config.setAlreadyLinkedMessage(stringValue(values.get("alreadyLinkedMessage")));
        config.setMaxAccountsPerDiscord(intValue(values.get("maxAccountsPerDiscord")));
        config.setChatChannelId(stringValue(values.get("chatChannelId")));
        config.setConsoleChannelId(stringValue(values.get("consoleChannelId")));
        config.setLinkChannelId(stringValue(values.get("linkChannelId")));
        config.setStatusChannelId(stringValue(values.get("statusChannelId")));
        config.setEnableStatusMessages(boolValue(values.get("enableStatusMessages")));
        config.setStatusMessageStarting(stringValue(values.get("statusMessageStarting")));
        config.setStatusMessageStarted(stringValue(values.get("statusMessageStarted")));
        config.setStatusMessageStopping(stringValue(values.get("statusMessageStopping")));
        config.setStatusMessageStopped(stringValue(values.get("statusMessageStopped")));
        config.setLinkedRoleId(stringValue(values.get("linkedRoleId")));
        config.setRenameOnLink(boolValue(values.get("renameOnLink")));
        java.lang.reflect.Type roleStyleType = new TypeToken<Map<String, DiscordConfig.RoleStyle>>() {}.getType();
        Map<String, DiscordConfig.RoleStyle> rolePrefixes = GSON.fromJson(stringValue(values.get("rolePrefixes")), roleStyleType);
        config.setRolePrefixes(rolePrefixes);
        config.setUseHexColors(boolValue(values.get("useHexColors")));
        config.setSyncBansDiscordToMc(boolValue(values.get("syncBansDiscordToMc")));
        config.setSyncBansMcToDiscord(boolValue(values.get("syncBansMcToDiscord")));
        config.setEnableChatBridge(boolValue(values.get("enableChatBridge")));
        config.setMinecraftToDiscordFormat(stringValue(values.get("minecraftToDiscordFormat")));
        config.setDiscordToMinecraftFormat(stringValue(values.get("discordToMinecraftFormat")));
        config.setTranslateEmojis(boolValue(values.get("translateEmojis")));
        config.setChatWebhookUrl(stringValue(values.get("chatWebhookUrl")));
        config.setEnableTopicUpdate(boolValue(values.get("enableTopicUpdate")));
        config.setChannelTopicFormat(stringValue(values.get("channelTopicFormat")));
        config.setUptimeFormat(stringValue(values.get("uptimeFormat")));
        config.setInvalidCodeMessage(stringValue(values.get("invalidCodeMessage")));
        config.setNotLinkedMessage(stringValue(values.get("notLinkedMessage")));
        config.setAlreadyLinkedSingleMessage(stringValue(values.get("alreadyLinkedSingleMessage")));
        config.setAlreadyLinkedMultipleMessage(stringValue(values.get("alreadyLinkedMultipleMessage")));
        config.setUnlinkSuccessMessage(stringValue(values.get("unlinkSuccessMessage")));
        config.setWrongGuildMessage(stringValue(values.get("wrongGuildMessage")));
        config.setTicketCreatedMessage(stringValue(values.get("ticketCreatedMessage")));
        config.setTicketClosingMessage(stringValue(values.get("ticketClosingMessage")));
        config.setTextChannelOnlyMessage(stringValue(values.get("textChannelOnlyMessage")));
        config.save();
    }

    private void applyAI(Map<String, Object> values) {
        AIConfig config = AIConfig.getInstance();
        config.setEnablePlayerChat(boolValue(values.get("enablePlayerChat")));
        config.setPlayerAccessMode(stringValue(values.get("playerAccessMode")));
        config.setPlayerAccessMinHours(intValue(values.get("playerAccessMinHours")));
        config.setPlayerAccessDiscordRoleIds(stringList(values.get("playerAccessDiscordRoleIds")));
        config.setDisabledWorlds(stringList(values.get("disabledWorlds")));
        config.setDisabledGameModes(stringList(values.get("disabledGameModes")));
        config.setEnableAdminAssistant(boolValue(values.get("enableAdminAssistant")));
        config.setRedactSensitiveValues(boolValue(values.get("redactSensitiveValues")));
        config.setPlayerPromptMaxLength(intValue(values.get("playerPromptMaxLength")));
        config.setPlayerCooldownSeconds(intValue(values.get("playerCooldownSeconds")));
        config.setAdminPromptMaxLength(intValue(values.get("adminPromptMaxLength")));
        config.setAdminContextMaxChars(intValue(values.get("adminContextMaxChars")));
        config.getPlayerApi().setEndpointUrl(stringValue(values.get("playerApi.endpointUrl")));
        if (!isMasked(values.get("playerApi.apiKey"))) {
            config.getPlayerApi().setApiKey(stringValue(values.get("playerApi.apiKey")));
        }
        config.getPlayerApi().setModel(stringValue(values.get("playerApi.model")));
        config.getPlayerApi().setSystemPrompt(stringValue(values.get("playerApi.systemPrompt")));
        config.getPlayerApi().setTemperature(doubleValue(values.get("playerApi.temperature")));
        config.getPlayerApi().setMaxTokens(intValue(values.get("playerApi.maxTokens")));
        config.getPlayerApi().setTimeoutSeconds(intValue(values.get("playerApi.timeoutSeconds")));
        config.getAdminApi().setEndpointUrl(stringValue(values.get("adminApi.endpointUrl")));
        if (!isMasked(values.get("adminApi.apiKey"))) {
            config.getAdminApi().setApiKey(stringValue(values.get("adminApi.apiKey")));
        }
        config.getAdminApi().setModel(stringValue(values.get("adminApi.model")));
        config.getAdminApi().setSystemPrompt(stringValue(values.get("adminApi.systemPrompt")));
        config.getAdminApi().setTemperature(doubleValue(values.get("adminApi.temperature")));
        config.getAdminApi().setMaxTokens(intValue(values.get("adminApi.maxTokens")));
        config.getAdminApi().setTimeoutSeconds(intValue(values.get("adminApi.timeoutSeconds")));
        config.save();
    }

    private void applyStats(Map<String, Object> values) {
        StatsConfig config = StatsConfig.getInstance();
        config.setEnableStats(boolValue(values.get("enableStats")));
        config.setReportChannelId(stringValue(values.get("reportChannelId")));
        config.setReportTime(stringValue(values.get("reportTime")));
        config.setReportTitle(stringValue(values.get("reportTitle")));
        config.setReportPeakLabel(stringValue(values.get("reportPeakLabel")));
        config.setReportAverageLabel(stringValue(values.get("reportAverageLabel")));
        config.setReportFooter(stringValue(values.get("reportFooter")));
        config.save();
    }

    private void applyVote(Map<String, Object> values) {
        VoteConfig config = VoteConfig.getInstance();
        config.setEnabled(boolValue(values.get("enabled")));
        config.setHost(stringValue(values.get("host")));
        config.setPort(intValue(values.get("port")));
        config.setRsaPrivateKeyPath(stringValue(values.get("rsaPrivateKeyPath")));
        config.setRsaPublicKeyPath(stringValue(values.get("rsaPublicKeyPath")));
        if (!isMasked(values.get("sharedSecret"))) {
            config.setSharedSecret(stringValue(values.get("sharedSecret")));
        }
        config.setAnnounceVotes(boolValue(values.get("announceVotes")));
        config.setAnnouncementMessage(stringValue(values.get("announcementMessage")));
        config.setAnnouncementCooldown(intValue(values.get("announcementCooldown")));
        config.setMaxVoteAgeHours(intValue(values.get("maxVoteAgeHours")));
        config.setCommands(stringList(values.get("commands")));
        config.getLogging().setVoteLog(boolValue(values.get("logging.voteLog")));
        config.getLogging().setVoteLogFile(stringValue(values.get("logging.voteLogFile")));
        config.getLogging().setArchiveJson(boolValue(values.get("logging.archiveJson")));
        config.getLogging().setArchivePath(stringValue(values.get("logging.archivePath")));
        config.getLogging().setNotifyOpsOnError(boolValue(values.get("logging.notifyOpsOnError")));
        config.getLogging().setPendingQueueFile(stringValue(values.get("logging.pendingQueueFile")));
        config.getLogging().setPendingVoteMessage(stringValue(values.get("logging.pendingVoteMessage")));
        config.save();
    }

    private void applyEntityCleaner(Map<String, Object> values) {
        EntityCleanerConfig config = EntityCleanerConfig.getInstance();
        config.setEnabled(boolValue(values.get("enabled")));
        config.setCleanupIntervalSeconds(intValue(values.get("cleanupIntervalSeconds")));
        config.setWarningTimes(parseIntegerList(stringList(values.get("warningTimes"))));
        config.setRemoveDroppedItems(boolValue(values.get("removeDroppedItems")));
        config.setRemovePassiveMobs(boolValue(values.get("removePassiveMobs")));
        config.setRemoveHostileMobs(boolValue(values.get("removeHostileMobs")));
        config.setRemoveXpOrbs(boolValue(values.get("removeXpOrbs")));
        config.setRemoveArrows(boolValue(values.get("removeArrows")));
        config.setRemoveNamedEntities(boolValue(values.get("removeNamedEntities")));
        config.setRemoveTamedAnimals(boolValue(values.get("removeTamedAnimals")));
        config.setProtectBosses(boolValue(values.get("protectBosses")));
        config.setEntityWhitelist(stringList(values.get("entityWhitelist")));
        config.setItemWhitelist(stringList(values.get("itemWhitelist")));
        config.setWarningMessage(stringValue(values.get("warningMessage")));
        config.setCleanupMessage(stringValue(values.get("cleanupMessage")));
        config.save();
    }

    private Map<String, Object> buildDefaultValues() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        GeneralConfig general = new GeneralConfig(getConfigDir().resolve("general.json"));
        WebConfig web = new WebConfig(getConfigDir().resolve("web.json"));
        AnnouncementConfig announcements = new AnnouncementConfig(getConfigDir().resolve("announcements.json"));
        RestartConfig restart = new RestartConfig(getConfigDir().resolve("restart.json"));
        RanksConfig ranks = new RanksConfig(getConfigDir().resolve("ranks.json"));
        TicketConfig tickets = new TicketConfig(getConfigDir().resolve("tickets.json"));
        DiscordConfig discord = new DiscordConfig(getConfigDir().resolve("discord.json"));
        StatsConfig stats = new StatsConfig(getConfigDir().resolve("stats.json"));
        VoteConfig vote = new VoteConfig(getConfigDir().resolve("votes.json"));
        EntityCleanerConfig entityCleaner = new EntityCleanerConfig(getConfigDir().resolve("entitycleaner.json"));
        AIConfig ai = new AIConfig(getConfigDir().resolve("ai.json"));
        defaults.put("general", mapOf(
                entry("enableMod", general.isEnableMod()),
                entry("enableRestarts", general.isEnableRestarts()),
                entry("enableAnnouncements", general.isEnableAnnouncements()),
                entry("enableSkinRestorer", general.isEnableSkinRestorer()),
                entry("enableDiscord", general.isEnableDiscord()),
                entry("enableWeb", general.isEnableWeb()),
                entry("enableStats", general.isEnableStats()),
                entry("enableRanks", general.isEnableRanks()),
                entry("enableVote", general.isEnableVote()),
                entry("enablePlayerList", general.isEnablePlayerList()),
                entry("maintenanceMode", general.isMaintenanceMode()),
                entry("skinCacheHours", general.getSkinCacheHours()),
                entry("modPrefix", general.getModPrefix())));
        defaults.put("web", mapOf(
                entry("port", web.getPort()),
                entry("language", web.getLanguage()),
                entry("publicHostname", web.getPublicHostname()),
                entry("bindAddress", web.getBindAddress()),
                entry("adminToken", maskSecret(web.getAdminToken())),
                entry("sessionTtlMinutes", web.getSessionTtlMinutes())));
        defaults.put("announcements", mapOf(
                entry("prefix", announcements.getPrefix()),
                entry("announcementIntervalMinutes", announcements.getAnnouncementIntervalMinutes()),
                entry("announcements", new ArrayList<>(announcements.getAnnouncements()))));
        defaults.put("restart", mapOf(
                entry("restartType", restart.getRestartType().name()),
                entry("fixedRestartTimes", restart.getFixedRestartTimes().stream().map(LocalTime::toString).toList()),
                entry("intervalHours", restart.getIntervalHours()),
                entry("delayMinutes", restart.getDelayMinutes()),
                entry("warningMessage", restart.getWarningMessage()),
                entry("restartingNowMessage", restart.getRestartingNowMessage()),
                entry("kickMessage", restart.getKickMessage())));
        defaults.put("ranks", mapOf(
                entry("enableAutoRanks", ranks.isEnableAutoRanks()),
                entry("checkIntervalMinutes", ranks.getCheckIntervalMinutes()),
                entry("promotionMessage", ranks.getPromotionMessage()),
                entry("tooltipPlayed", ranks.getTooltipPlayed()),
                entry("tooltipRequired", ranks.getTooltipRequired()),
                entry("ranks", GSON.toJson(ranks.getRanks()))));
        defaults.put("playerlist", mapOf(
                entry("enableCustomPlayerList", true),
                entry("headerLine1", "§b§l✦ VOIDIUM SERVER ✦"),
                entry("headerLine2", "§7Online: §a%online%§7/§a%max%"),
                entry("headerLine3", ""),
                entry("footerLine1", "§7TPS: §a%tps%"),
                entry("footerLine2", "§7Ping: §e%ping%ms"),
                entry("footerLine3", ""),
                entry("enableCustomNames", true),
                entry("playerNameFormat", "%rank_prefix%%player_name%%rank_suffix%"),
                entry("defaultPrefix", "§7"),
                entry("defaultSuffix", ""),
                entry("combineMultipleRanks", true),
                entry("updateIntervalSeconds", 5)));
            defaults.put("tickets", mapOf(
                entry("enableTickets", tickets.isEnableTickets()),
                entry("ticketCategoryId", tickets.getTicketCategoryId()),
                entry("supportRoleId", tickets.getSupportRoleId()),
                entry("ticketChannelTopic", tickets.getTicketChannelTopic()),
                entry("maxTicketsPerUser", tickets.getMaxTicketsPerUser()),
                entry("ticketCreatedMessage", tickets.getTicketCreatedMessage()),
                entry("ticketWelcomeMessage", tickets.getTicketWelcomeMessage()),
                entry("ticketCloseMessage", tickets.getTicketCloseMessage()),
                entry("noPermissionMessage", tickets.getNoPermissionMessage()),
                entry("ticketLimitReachedMessage", tickets.getTicketLimitReachedMessage()),
                entry("ticketAlreadyClosedMessage", tickets.getTicketAlreadyClosedMessage()),
                entry("enableTranscript", tickets.isEnableTranscript()),
                entry("transcriptFormat", tickets.getTranscriptFormat()),
                entry("transcriptFilename", tickets.getTranscriptFilename()),
                entry("mcBotNotConnectedMessage", tickets.getMcBotNotConnectedMessage()),
                entry("mcGuildNotFoundMessage", tickets.getMcGuildNotFoundMessage()),
                entry("mcCategoryNotFoundMessage", tickets.getMcCategoryNotFoundMessage()),
                entry("mcTicketCreatedMessage", tickets.getMcTicketCreatedMessage()),
                entry("mcDiscordNotFoundMessage", tickets.getMcDiscordNotFoundMessage())));
            defaults.put("discord", mapOf(
                entry("enableDiscord", discord.isEnableDiscord()),
                entry("botToken", maskSecret(discord.getBotToken())),
                entry("guildId", discord.getGuildId()),
                entry("botActivityType", discord.getBotActivityType()),
                entry("botActivityText", discord.getBotActivityText()),
                entry("enableWhitelist", discord.isEnableWhitelist()),
                entry("kickMessage", discord.getKickMessage()),
                entry("verificationHintMessage", discord.getVerificationHintMessage()),
                entry("linkSuccessMessage", discord.getLinkSuccessMessage()),
                entry("alreadyLinkedMessage", discord.getAlreadyLinkedMessage()),
                entry("maxAccountsPerDiscord", discord.getMaxAccountsPerDiscord()),
                entry("chatChannelId", discord.getChatChannelId()),
                entry("consoleChannelId", discord.getConsoleChannelId()),
                entry("linkChannelId", discord.getLinkChannelId()),
                entry("statusChannelId", ""),
                entry("enableStatusMessages", discord.isEnableStatusMessages()),
                entry("statusMessageStarting", discord.getStatusMessageStarting()),
                entry("statusMessageStarted", discord.getStatusMessageStarted()),
                entry("statusMessageStopping", discord.getStatusMessageStopping()),
                entry("statusMessageStopped", discord.getStatusMessageStopped()),
                entry("linkedRoleId", discord.getLinkedRoleId()),
                entry("renameOnLink", discord.isRenameOnLink()),
                entry("rolePrefixes", GSON.toJson(discord.getRolePrefixes())),
                entry("useHexColors", discord.isUseHexColors()),
                entry("syncBansDiscordToMc", discord.isSyncBansDiscordToMc()),
                entry("syncBansMcToDiscord", discord.isSyncBansMcToDiscord()),
                entry("enableChatBridge", discord.isEnableChatBridge()),
                entry("minecraftToDiscordFormat", discord.getMinecraftToDiscordFormat()),
                entry("discordToMinecraftFormat", discord.getDiscordToMinecraftFormat()),
                entry("translateEmojis", discord.isTranslateEmojis()),
                entry("chatWebhookUrl", discord.getChatWebhookUrl()),
                entry("enableTopicUpdate", discord.isEnableTopicUpdate()),
                entry("channelTopicFormat", discord.getChannelTopicFormat()),
                entry("uptimeFormat", discord.getUptimeFormat()),
                entry("invalidCodeMessage", discord.getInvalidCodeMessage()),
                entry("notLinkedMessage", discord.getNotLinkedMessage()),
                entry("alreadyLinkedSingleMessage", discord.getAlreadyLinkedSingleMessage()),
                entry("alreadyLinkedMultipleMessage", discord.getAlreadyLinkedMultipleMessage()),
                entry("unlinkSuccessMessage", discord.getUnlinkSuccessMessage()),
                entry("wrongGuildMessage", discord.getWrongGuildMessage()),
                entry("ticketCreatedMessage", discord.getTicketCreatedMessage()),
                entry("ticketClosingMessage", discord.getTicketClosingMessage()),
                entry("textChannelOnlyMessage", discord.getTextChannelOnlyMessage())));
            defaults.put("stats", mapOf(
                entry("enableStats", stats.isEnableStats()),
                entry("reportChannelId", stats.getReportChannelId()),
                entry("reportTime", stats.getReportTime().toString()),
                entry("reportTitle", stats.getReportTitle()),
                entry("reportPeakLabel", stats.getReportPeakLabel()),
                entry("reportAverageLabel", stats.getReportAverageLabel()),
                entry("reportFooter", stats.getReportFooter())));
            defaults.put("vote", mapOf(
                entry("enabled", vote.isEnabled()),
                entry("host", vote.getHost()),
                entry("port", vote.getPort()),
                entry("rsaPrivateKeyPath", vote.getRsaPrivateKeyPath()),
                entry("rsaPublicKeyPath", vote.getRsaPublicKeyPath()),
                entry("sharedSecret", maskSecret(vote.getSharedSecret())),
                entry("announceVotes", vote.isAnnounceVotes()),
                entry("announcementMessage", vote.getAnnouncementMessage()),
                entry("announcementCooldown", vote.getAnnouncementCooldown()),
                entry("maxVoteAgeHours", vote.getMaxVoteAgeHours()),
                entry("commands", new ArrayList<>(vote.getCommands())),
                entry("logging.voteLog", vote.getLogging().isVoteLog()),
                entry("logging.voteLogFile", vote.getLogging().getVoteLogFile()),
                entry("logging.archiveJson", vote.getLogging().isArchiveJson()),
                entry("logging.archivePath", vote.getLogging().getArchivePath()),
                entry("logging.notifyOpsOnError", vote.getLogging().isNotifyOpsOnError()),
                entry("logging.pendingQueueFile", vote.getLogging().getPendingQueueFile()),
                entry("logging.pendingVoteMessage", vote.getLogging().getPendingVoteMessage())));
            defaults.put("entitycleaner", mapOf(
                entry("enabled", entityCleaner.isEnabled()),
                entry("cleanupIntervalSeconds", entityCleaner.getCleanupIntervalSeconds()),
                entry("warningTimes", entityCleaner.getWarningTimes().stream().map(String::valueOf).toList()),
                entry("removeDroppedItems", entityCleaner.isRemoveDroppedItems()),
                entry("removePassiveMobs", entityCleaner.isRemovePassiveMobs()),
                entry("removeHostileMobs", entityCleaner.isRemoveHostileMobs()),
                entry("removeXpOrbs", entityCleaner.isRemoveXpOrbs()),
                entry("removeArrows", entityCleaner.isRemoveArrows()),
                entry("removeNamedEntities", entityCleaner.isRemoveNamedEntities()),
                entry("removeTamedAnimals", entityCleaner.isRemoveTamedAnimals()),
                entry("protectBosses", entityCleaner.isProtectBosses()),
                entry("entityWhitelist", new ArrayList<>(entityCleaner.getEntityWhitelist())),
                entry("itemWhitelist", new ArrayList<>(entityCleaner.getItemWhitelist())),
                entry("warningMessage", entityCleaner.getWarningMessage()),
                entry("cleanupMessage", entityCleaner.getCleanupMessage())));
        defaults.put("ai", mapOf(
                entry("enablePlayerChat", ai.isEnablePlayerChat()),
                entry("playerAccessMode", ai.getPlayerAccessMode()),
                entry("playerAccessMinHours", ai.getPlayerAccessMinHours()),
                entry("playerAccessDiscordRoleIds", new ArrayList<>(ai.getPlayerAccessDiscordRoleIds())),
                entry("disabledWorlds", new ArrayList<>(ai.getDisabledWorlds())),
                entry("disabledGameModes", new ArrayList<>(ai.getDisabledGameModes())),
                entry("enableAdminAssistant", ai.isEnableAdminAssistant()),
                entry("redactSensitiveValues", ai.isRedactSensitiveValues()),
                entry("playerPromptMaxLength", ai.getPlayerPromptMaxLength()),
                entry("playerCooldownSeconds", ai.getPlayerCooldownSeconds()),
                entry("adminPromptMaxLength", ai.getAdminPromptMaxLength()),
                entry("adminContextMaxChars", ai.getAdminContextMaxChars()),
                entry("playerApi.endpointUrl", ai.getPlayerApi().getEndpointUrl()),
                entry("playerApi.apiKey", maskSecret(ai.getPlayerApi().getApiKey())),
                entry("playerApi.model", ai.getPlayerApi().getModel()),
                entry("playerApi.systemPrompt", ai.getPlayerApi().getSystemPrompt()),
                entry("playerApi.temperature", ai.getPlayerApi().getTemperature()),
                entry("playerApi.maxTokens", ai.getPlayerApi().getMaxTokens()),
                entry("playerApi.timeoutSeconds", ai.getPlayerApi().getTimeoutSeconds()),
                entry("adminApi.endpointUrl", ai.getAdminApi().getEndpointUrl()),
                entry("adminApi.apiKey", maskSecret(ai.getAdminApi().getApiKey())),
                entry("adminApi.model", ai.getAdminApi().getModel()),
                entry("adminApi.systemPrompt", ai.getAdminApi().getSystemPrompt()),
                entry("adminApi.temperature", ai.getAdminApi().getTemperature()),
                entry("adminApi.maxTokens", ai.getAdminApi().getMaxTokens()),
                entry("adminApi.timeoutSeconds", ai.getAdminApi().getTimeoutSeconds())));
        return defaults;
    }

    private Map<String, Object> generalLocaleValues(String locale) {
        Map<String, Object> values = castMap(buildDefaultValues().get("general"));
        values.put("modPrefix", LocalePresets.getGeneralMessages(locale).get("modPrefix"));
        return values;
    }

    private Map<String, Object> announcementLocaleValues(String locale) {
        Map<String, Object> values = castMap(buildDefaultValues().get("announcements"));
        Map<String, Object> preset = LocalePresets.getAnnouncementMessages(locale);
        values.put("prefix", preset.get("prefix"));
        Object announcements = preset.get("announcements");
        if (announcements instanceof String[] lines) {
            values.put("announcements", new ArrayList<>(List.of(lines)));
        }
        return values;
    }

    private Map<String, Object> restartLocaleValues(String locale) {
        Map<String, Object> values = castMap(buildDefaultValues().get("restart"));
        Map<String, String> preset = LocalePresets.getRestartMessages(locale);
        values.put("warningMessage", preset.get("warningMessage"));
        values.put("restartingNowMessage", preset.get("restartingNowMessage"));
        values.put("kickMessage", preset.get("kickMessage"));
        return values;
    }

    private Map<String, Object> ranksLocaleValues(String locale) {
        Map<String, Object> values = castMap(buildDefaultValues().get("ranks"));
        values.put("promotionMessage", LocalePresets.getRankMessages(locale).get("promotionMessage"));
        return values;
    }

    private Map<String, Object> playerListLocaleValues(String locale) {
        Map<String, Object> values = castMap(buildDefaultValues().get("playerlist"));
        Map<String, String> preset = LocalePresets.getPlayerListMessages(locale);
        values.put("headerLine1", preset.get("headerLine1"));
        values.put("headerLine2", preset.get("headerLine2"));
        values.put("headerLine3", preset.get("headerLine3"));
        values.put("footerLine1", preset.get("footerLine1"));
        values.put("footerLine2", preset.get("footerLine2"));
        values.put("footerLine3", preset.get("footerLine3"));
        return values;
    }

    private Map<String, Object> ticketLocaleValues(String locale) {
        Map<String, Object> values = castMap(buildDefaultValues().get("tickets"));
        Map<String, String> preset = LocalePresets.getTicketMessages(locale);
        values.put("ticketCreatedMessage", preset.get("ticketCreatedMessage"));
        values.put("ticketWelcomeMessage", preset.get("ticketWelcomeMessage"));
        values.put("ticketCloseMessage", preset.get("ticketCloseMessage"));
        values.put("noPermissionMessage", preset.get("noPermissionMessage"));
        values.put("ticketLimitReachedMessage", preset.get("ticketLimitReachedMessage"));
        values.put("ticketAlreadyClosedMessage", preset.get("ticketAlreadyClosedMessage"));
        values.put("mcBotNotConnectedMessage", preset.get("mcBotNotConnectedMessage"));
        values.put("mcGuildNotFoundMessage", preset.get("mcGuildNotFoundMessage"));
        values.put("mcCategoryNotFoundMessage", preset.get("mcCategoryNotFoundMessage"));
        values.put("mcTicketCreatedMessage", preset.get("mcTicketCreatedMessage"));
        values.put("mcDiscordNotFoundMessage", preset.get("mcDiscordNotFoundMessage"));
        return values;
    }

    private Map<String, Object> discordLocaleValues(String locale) {
        Map<String, Object> values = castMap(buildDefaultValues().get("discord"));
        Map<String, String> preset = LocalePresets.getDiscordMessages(locale);
        values.put("kickMessage", preset.get("kickMessage"));
        values.put("linkSuccessMessage", preset.get("linkSuccessMessage"));
        values.put("alreadyLinkedMessage", preset.get("alreadyLinkedMessage"));
        values.put("minecraftToDiscordFormat", preset.get("minecraftToDiscordFormat"));
        values.put("discordToMinecraftFormat", preset.get("discordToMinecraftFormat"));
        values.put("statusMessageStarting", preset.get("statusMessageStarting"));
        values.put("statusMessageStarted", preset.get("statusMessageStarted"));
        values.put("statusMessageStopping", preset.get("statusMessageStopping"));
        values.put("statusMessageStopped", preset.get("statusMessageStopped"));
        values.put("channelTopicFormat", preset.get("channelTopicFormat"));
        values.put("uptimeFormat", preset.get("uptimeFormat"));
        values.put("invalidCodeMessage", preset.get("invalidCodeMessage"));
        values.put("notLinkedMessage", preset.get("notLinkedMessage"));
        values.put("alreadyLinkedSingleMessage", preset.get("alreadyLinkedSingleMessage"));
        values.put("alreadyLinkedMultipleMessage", preset.get("alreadyLinkedMultipleMessage"));
        values.put("unlinkSuccessMessage", preset.get("unlinkSuccessMessage"));
        values.put("wrongGuildMessage", preset.get("wrongGuildMessage"));
        values.put("ticketCreatedMessage", preset.get("ticketCreatedMessage"));
        values.put("ticketClosingMessage", preset.get("ticketClosingMessage"));
        values.put("textChannelOnlyMessage", preset.get("textChannelOnlyMessage"));
        return values;
    }

    private Map<String, Object> statsLocaleValues(String locale) {
        Map<String, Object> values = castMap(buildDefaultValues().get("stats"));
        Map<String, String> preset = LocalePresets.getStatsMessages(locale);
        values.put("reportTitle", preset.get("reportTitle"));
        values.put("reportPeakLabel", preset.get("reportPeakLabel"));
        values.put("reportAverageLabel", preset.get("reportAverageLabel"));
        values.put("reportFooter", preset.get("reportFooter"));
        return values;
    }

    private Map<String, Object> voteLocaleValues(String locale) {
        Map<String, Object> values = castMap(buildDefaultValues().get("vote"));
        values.put("announcementMessage", LocalePresets.getVoteMessages(locale).get("announcementMessage"));
        return values;
    }

    private Map<String, Object> entityCleanerLocaleValues(String locale) {
        Map<String, Object> values = castMap(buildDefaultValues().get("entitycleaner"));
        Map<String, String> preset = LocalePresets.getEntityCleanerMessages(locale);
        values.put("warningMessage", preset.get("warningMessage"));
        values.put("cleanupMessage", preset.get("cleanupMessage"));
        return values;
    }

    private List<String> validateIntegerList(String path, List<String> values, int minValue) {
        List<String> errors = new ArrayList<>();
        for (String value : values) {
            try {
                if (Integer.parseInt(value.trim()) < minValue) {
                    errors.add(path + " must not contain values below " + minValue + ".");
                }
            } catch (NumberFormatException e) {
                errors.add(path + " contains invalid integer: " + value);
            }
        }
        return errors;
    }

    private List<Integer> parseIntegerList(List<String> values) {
        List<Integer> parsed = new ArrayList<>();
        for (String value : values) {
            parsed.add(Integer.parseInt(value.trim()));
        }
        return parsed;
    }

    private Path createBackup(List<String> fileNames) throws IOException {
        Path backupDir = getBackupRoot().resolve(BACKUP_FORMAT.format(Instant.now()));
        Files.createDirectories(backupDir);
        Path configDir = getConfigDir();
        for (String fileName : fileNames) {
            Path source = configDir.resolve(fileName);
            if (Files.exists(source)) {
                Files.copy(source, backupDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return backupDir;
    }

    private Path getBackupRoot() throws IOException {
        Path root = StorageHelper.getStorageDir() != null
                ? StorageHelper.getStorageDir().resolve("web-backups")
                : getConfigDir().resolve("storage").resolve("web-backups");
        Files.createDirectories(root);
        return root;
    }

    private Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get().resolve("voidium");
    }

    private Map<String, Object> section(String label, boolean restartRequired, List<Map<String, Object>> fields) {
        return mapOf(entry("label", label), entry("restartRequired", restartRequired), entry("fields", fields));
    }

    private Map<String, Object> field(String key, String type, String label, String description) {
        return mapOf(entry("key", key), entry("type", type), entry("label", label), entry("description", description));
    }

    private Map<String, Object> field(String key, String type, String label, String description, List<String> options) {
        return mapOf(entry("key", key), entry("type", type), entry("label", label), entry("description", description), entry("options", options));
    }

    private Map<String, Object> secretField(String key, String label, String description) {
        return mapOf(entry("key", key), entry("type", "secret"), entry("label", label), entry("description", description));
    }

    private static final String MASKED_PLACEHOLDER = "••••••";

    private String maskSecret(String value) {
        if (value == null || value.isBlank() || value.length() <= 4) {
            return MASKED_PLACEHOLDER;
        }
        return value.substring(0, 2) + MASKED_PLACEHOLDER + value.substring(value.length() - 2);
    }

    private boolean isMasked(Object value) {
        return value instanceof String s && s.contains(MASKED_PLACEHOLDER);
    }

    private Map<String, Object> change(String section, String field, Object currentValue, Object proposedValue) {
        Map<String, Object> impact = classifyImpact(section, field);
        return mapOf(
                entry("section", section),
                entry("field", field),
                entry("current", currentValue),
                entry("proposed", proposedValue),
                entry("impact", impact.get("type")),
                entry("impactLabel", impact.get("label")));
    }

    private Map<String, Object> classifyImpact(String section, String field) {
        if ("web".equals(section) && WEB_RESTART_FIELDS.contains(field)) {
            return mapOf(entry("type", "web_restart"), entry("label", "Embedded web restart recommended"));
        }
        if ("announcements".equals(section) || "restart".equals(section) || "ranks".equals(section) || "playerlist".equals(section)) {
            return mapOf(entry("type", "manager_restart"), entry("label", "Manager reload required"));
        }
        if ("general".equals(section)) {
            return mapOf(entry("type", "voidium_reload"), entry("label", "Voidium reload recommended"));
        }
        return mapOf(entry("type", "live"), entry("label", "Live-safe after save"));
    }

    private Map<String, Object> summarizeImpact(List<Map<String, Object>> changes) {
        String strongest = "live";
        for (Map<String, Object> change : changes) {
            String impact = stringValue(change.get("impact"));
            if (impactPriority(impact) > impactPriority(strongest)) {
                strongest = impact;
            }
        }
        String label = switch (strongest) {
            case "web_restart" -> "Some changes require restarting the embedded web server.";
            case "voidium_reload" -> "Run /voidium reload to fully apply these changes.";
            case "manager_restart" -> "Run /voidium reload; affected managers will be restarted.";
            default -> "Changes are live-safe after save.";
        };
        return mapOf(entry("type", strongest), entry("label", label));
    }

    private int impactPriority(String impact) {
        return switch (impact) {
            case "web_restart" -> 3;
            case "voidium_reload" -> 2;
            case "manager_restart" -> 1;
            default -> 0;
        };
    }

    private Map.Entry<String, Object> entry(String key, Object value) {
        return Map.entry(key, value);
    }

    @SafeVarargs
    private final Map<String, Object> mapOf(Map.Entry<String, Object>... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private Map<String, Object> deepCopy(Map<String, Object> value) {
        return GSON.fromJson(GSON.toJson(value), Map.class);
    }

    private Map<String, Object> localizeSchema(Map<String, Object> schema) {
        if (!isCzech()) {
            return schema;
        }
        Map<String, Object> localized = deepCopy(schema);
        for (Map.Entry<String, Object> entry : localized.entrySet()) {
            Map<String, Object> section = castMap(entry.getValue());
            section.put("label", localizeSectionLabel(stringValue(section.get("label"))));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fields = (List<Map<String, Object>>) section.get("fields");
            for (Map<String, Object> field : fields) {
                field.put("label", localizeFieldLabel(stringValue(field.get("label"))));
                field.put("description", localizeFieldDescription(stringValue(field.get("description"))));
            }
        }
        return localized;
    }

    private String localizeSectionLabel(String label) {
        return switch (label) {
            case "General" -> "Obecné";
            case "Web" -> "Web";
            case "Announcements" -> "Oznámení";
            case "Restart" -> "Restart";
            case "Ranks" -> "Ranky";
            case "Player List" -> "Seznam hráčů";
            case "AI" -> "AI";
            default -> label;
        };
    }

    private String localizeFieldLabel(String label) {
        return switch (label) {
            case "Enable mod" -> "Povolit mod";
            case "Automatic restarts" -> "Automatické restarty";
            case "Announcements" -> "Oznámení";
            case "Skin Restorer" -> "Skin Restorer";
            case "Discord integration" -> "Discord integrace";
            case "Web UI" -> "Web UI";
            case "Statistics" -> "Statistiky";
            case "Ranks" -> "Ranky";
            case "Vote manager" -> "Vote manager";
            case "Custom player list" -> "Vlastní seznam hráčů";
            case "Skin cache hours" -> "Hodiny cache skinů";
            case "Mod prefix" -> "Prefix modu";
            case "Port" -> "Port";
            case "Language" -> "Jazyk";
            case "Public hostname" -> "Veřejný hostname";
            case "Bind address" -> "Bind adresa";
            case "Persistent admin token" -> "Trvalý admin token";
            case "Session TTL" -> "TTL sezení";
            case "Prefix" -> "Prefix";
            case "Interval minutes" -> "Interval minut";
            case "Announcement lines" -> "Řádky oznámení";
            case "Restart type" -> "Typ restartu";
            case "Fixed restart times" -> "Pevné časy restartu";
            case "Interval hours" -> "Interval hodin";
            case "Delay minutes" -> "Zpoždění v minutách";
            case "Warning message" -> "Varovná zpráva";
            case "Restarting now" -> "Restart právě probíhá";
            case "Kick message" -> "Kick zpráva";
            case "Enable auto ranks" -> "Povolit auto ranky";
            case "Check interval" -> "Interval kontroly";
            case "Promotion message" -> "Zpráva o povýšení";
            case "Tooltip played" -> "Tooltip odehráno";
            case "Tooltip required" -> "Tooltip požadováno";
            case "Rank definitions" -> "Definice ranků";
            case "Enable custom list" -> "Povolit vlastní list";
            case "Header line 1" -> "Header řádek 1";
            case "Header line 2" -> "Header řádek 2";
            case "Header line 3" -> "Header řádek 3";
            case "Footer line 1" -> "Footer řádek 1";
            case "Footer line 2" -> "Footer řádek 2";
            case "Footer line 3" -> "Footer řádek 3";
            case "Custom player names" -> "Vlastní jména hráčů";
            case "Player name format" -> "Formát jména hráče";
            case "Default prefix" -> "Výchozí prefix";
            case "Default suffix" -> "Výchozí suffix";
            case "Combine multiple ranks" -> "Kombinovat více ranků";
            case "Update interval" -> "Interval aktualizace";
            case "Enable stats" -> "Povolit statistiky";
            case "Report channel ID" -> "ID kanálu reportu";
            case "Report time" -> "Čas reportu";
            case "Report title" -> "Název reportu";
            case "Peak label" -> "Label maxima";
            case "Average label" -> "Label průměru";
            case "Report footer" -> "Patička reportu";
            case "Enable vote listener" -> "Povolit vote listener";
            case "Host" -> "Host";
            case "RSA private key path" -> "Cesta k RSA private key";
            case "RSA public key path" -> "Cesta k RSA public key";
            case "Shared secret" -> "Sdílený secret";
            case "Announce votes" -> "Oznamovat hlasy";
            case "Announcement message" -> "Zpráva oznámení";
            case "Announcement cooldown" -> "Cooldown oznámení";
            case "Max vote age hours" -> "Max stáří hlasu v hodinách";
            case "Reward commands" -> "Reward příkazy";
            case "Vote log file" -> "Vote log soubor";
            case "Vote log path" -> "Cesta vote logu";
            case "Archive JSON" -> "Archivovat JSON";
            case "Archive path" -> "Cesta archivu";
            case "Notify ops on error" -> "Upozornit opy při chybě";
            case "Pending queue path" -> "Cesta pending queue";
            case "Pending vote message" -> "Zpráva pending hlasů";
            case "Enable cleaner" -> "Povolit cleaner";
            case "Cleanup interval" -> "Interval čištění";
            case "Warning times" -> "Časy varování";
            case "Remove dropped items" -> "Mazat dropped itemy";
            case "Remove passive mobs" -> "Mazat pasivní moby";
            case "Remove hostile mobs" -> "Mazat hostile moby";
            case "Remove XP orbs" -> "Mazat XP orbíky";
            case "Remove arrows" -> "Mazat šípy";
            case "Remove named entities" -> "Mazat pojmenované entity";
            case "Remove tamed animals" -> "Mazat ochočená zvířata";
            case "Protect bosses" -> "Chránit bossy";
            case "Entity whitelist" -> "Entity whitelist";
            case "Item whitelist" -> "Item whitelist";
            case "Enable player AI" -> "Povolit hráčské AI";
            case "Player access mode" -> "Režim přístupu hráčů";
            case "Minimum played hours" -> "Minimální odehrané hodiny";
            case "Allowed Discord role IDs" -> "Povolená Discord role ID";
            case "Enable admin assistant" -> "Povolit admin asistenta";
            case "Redact secrets" -> "Redigovat tajné údaje";
            case "Player prompt max" -> "Max délka promptu hráče";
            case "Player cooldown" -> "Cooldown hráče";
            case "Admin prompt max" -> "Max délka admin promptu";
            case "Admin context max" -> "Max délka admin kontextu";
            case "Player endpoint URL" -> "URL hráč endpointu";
            case "Player API key" -> "API klíč hráče";
            case "Player model" -> "Model hráče";
            case "Player system prompt" -> "System prompt hráče";
            case "Player temperature" -> "Teplota hráče";
            case "Player max tokens" -> "Max tokeny hráče";
            case "Player timeout" -> "Timeout hráče";
            case "Admin endpoint URL" -> "URL admin endpointu";
            case "Admin API key" -> "API klíč admina";
            case "Admin model" -> "Admin model";
            case "Admin system prompt" -> "Admin system prompt";
            case "Admin temperature" -> "Teplota admina";
            case "Admin max tokens" -> "Max tokeny admina";
            case "Admin timeout" -> "Admin timeout";
            default -> label;
        };
    }

    private String localizeFieldDescription(String description) {
        return switch (description) {
            case "Master power switch for the whole mod." -> "Hlavní přepínač pro celý mod.";
            case "Controls scheduled restart automation." -> "Řídí automatizaci plánovaných restartů.";
            case "Enables automatic broadcast messages." -> "Povoluje automatické broadcast zprávy.";
            case "Fetches real skins in offline mode." -> "Načítá reálné skiny v offline režimu.";
            case "Controls Discord bot and bridge features." -> "Řídí Discord bota a bridge funkce.";
            case "Controls the web control panel availability." -> "Řídí dostupnost webového ovládacího panelu.";
            case "Enables tracking and report generation." -> "Povoluje sběr statistik a generování reportů.";
            case "Enables the automatic playtime rank system." -> "Povoluje automatický rank systém podle playtime.";
            case "Enables the vote listener and reward processing." -> "Povoluje vote listener a zpracování odměn.";
            case "Enables TAB list enhancements." -> "Povoluje rozšíření TAB listu.";
            case "Lower values refresh skins more often." -> "Nižší hodnoty obnovují skiny častěji.";
            case "Shown in Voidium messages. Supports & color codes." -> "Zobrazuje se ve Voidium zprávách. Podporuje & color kódy.";
            case "HTTP port used by the Voidium panel." -> "HTTP port používaný Voidium panelem.";
            case "Current WebUI locale." -> "Aktuální locale WebUI.";
            case "Address shown in generated access links." -> "Adresa zobrazovaná v generovaných přístupových odkazech.";
            case "Network interface used by the embedded web server." -> "Síťové rozhraní použité embedded web serverem.";
            case "Static token for repeated admin access." -> "Statický token pro opakovaný admin přístup.";
            case "How long the session cookie remains valid." -> "Jak dlouho zůstává session cookie platná.";
            case "Added before each announcement line." -> "Přidá se před každý řádek oznámení.";
            case "Set 0 to disable automatic broadcasts." -> "Nastav 0 pro vypnutí automatických broadcastů.";
            case "One line per announcement." -> "Jeden řádek na jedno oznámení.";
            case "Controls how restarts are scheduled." -> "Řídí, jak jsou restarty plánované.";
            case "One HH:MM time per line." -> "Jeden čas HH:MM na řádek.";
            case "Used when restart type is INTERVAL." -> "Používá se, když je typ restartu INTERVAL.";
            case "Used when restart type is DELAY." -> "Používá se, když je typ restartu DELAY.";
            case "Supports %minutes% placeholder." -> "Podporuje placeholder %minutes%.";
            case "Shown when restart begins." -> "Zobrazí se při zahájení restartu.";
            case "Shown when players are disconnected." -> "Zobrazí se při odpojení hráčů.";
            case "Turns on automatic rank checks." -> "Zapne automatické kontroly ranků.";
            case "How often rank promotion checks run." -> "Jak často běží kontroly povýšení ranku.";
            case "Supports %rank% placeholder." -> "Podporuje placeholder %rank%.";
            case "Hover text for current playtime." -> "Hover text pro aktuální playtime.";
            case "Hover text for required playtime." -> "Hover text pro požadovanou playtime.";
            case "Structured rank editor with type, value, hours, and optional custom conditions JSON." -> "Strukturovaný editor ranků s typem, hodnotou, hodinami a volitelným JSON custom conditions.";
            case "Turns on header/footer rendering." -> "Zapne vykreslování headeru/footeru.";
            case "Supports placeholders like %online% and %max%." -> "Podporuje placeholdery jako %online% a %max%.";
            case "Supports placeholders like %tps%." -> "Podporuje placeholdery jako %tps%.";
            case "Optional third header line." -> "Volitelný třetí řádek headeru.";
            case "Optional first footer line." -> "Volitelný první řádek footeru.";
            case "Optional second footer line." -> "Volitelný druhý řádek footeru.";
            case "Optional third footer line." -> "Volitelný třetí řádek footeru.";
            case "Turns on formatted TAB player names." -> "Zapne formátovaná jména hráčů v TAB.";
            case "Supports %rank_prefix%, %player_name%, %rank_suffix%." -> "Podporuje %rank_prefix%, %player_name%, %rank_suffix%.";
            case "Used when no prefix is available." -> "Použije se, když není dostupný prefix.";
            case "Used when no suffix is available." -> "Použije se, když není dostupný suffix.";
            case "When enabled, multiple rank parts are merged." -> "Když je zapnuto, spojí se více částí ranků.";
            case "Minimum safe interval is 3 seconds." -> "Minimální bezpečný interval jsou 3 sekundy.";
            case "Turns scheduled statistics reports on or off." -> "Zapíná nebo vypíná plánované statistické reporty.";
            case "Discord channel ID used for daily stats reports." -> "ID Discord kanálu používaného pro denní stats reporty.";
            case "Daily report time in HH:MM format." -> "Denní čas reportu ve formátu HH:MM.";
            case "Supports %date% placeholder." -> "Podporuje placeholder %date%.";
            case "Label for peak players in the report." -> "Popisek maxima hráčů v reportu.";
            case "Label for average players in the report." -> "Popisek průměru hráčů v reportu.";
            case "Footer text shown in the stats report." -> "Text patičky zobrazený ve stats reportu.";
            case "Master switch for the Votifier listener." -> "Hlavní přepínač pro Votifier listener.";
            case "Interface address used by the vote listener." -> "Adresa rozhraní používaná vote listenerem.";
            case "Listener port for vote events." -> "Port listeneru pro vote eventy.";
            case "Path to the private key relative to config/voidium." -> "Cesta k private key relativně vůči config/voidium.";
            case "Path for the public key file relative to config/voidium." -> "Cesta k public key souboru relativně vůči config/voidium.";
            case "NuVotifier shared secret used for token validation." -> "NuVotifier shared secret používaný pro validaci tokenu.";
            case "Broadcast a message when a vote is paid out." -> "Pošle zprávu, když je hlas vyplacen.";
            case "Supports %PLAYER% placeholder." -> "Podporuje placeholder %PLAYER%.";
            case "Cooldown in seconds between vote announcements." -> "Cooldown v sekundách mezi vote oznámeními.";
            case "Votes older than this are ignored." -> "Hlasy starší než tato hodnota se ignorují.";
            case "One server command per line. Supports %PLAYER%." -> "Jeden server command na řádek. Podporuje %PLAYER%.";
            case "Write plain vote records to a log file." -> "Zapisuje plain vote záznamy do log souboru.";
            case "Path to the vote log file in storage." -> "Cesta k vote log souboru ve storage.";
            case "Append vote history as NDJSON." -> "Připojuje vote historii jako NDJSON.";
            case "Path to the vote history archive." -> "Cesta k archivu vote historie.";
            case "Warn operators when vote processing fails." -> "Upozorní operátory při selhání zpracování hlasu.";
            case "Storage file for pending offline votes." -> "Storage soubor pro pending offline hlasy.";
            case "Shown when pending votes are paid out." -> "Zobrazí se při vyplacení pending hlasů.";
            case "Turns automatic entity cleanup on or off." -> "Zapíná nebo vypíná automatické čištění entit.";
            case "Time between cleanups in seconds." -> "Čas mezi čištěními v sekundách.";
            case "One number of seconds per line before cleanup." -> "Jedno číslo sekund na řádek před čištěním.";
            case "Remove dropped item entities." -> "Maže dropped item entity.";
            case "Remove animals during cleanup." -> "Maže zvířata během čištění.";
            case "Remove hostile mobs during cleanup." -> "Maže hostile moby během čištění.";
            case "Remove XP orbs during cleanup." -> "Maže XP orbíky během čištění.";
            case "Remove arrows stuck in blocks." -> "Maže šípy zapíchnuté v blocích.";
            case "When false, named entities are protected." -> "Když je vypnuto, pojmenované entity jsou chráněné.";
            case "When false, tamed animals are protected." -> "Když je vypnuto, ochočená zvířata jsou chráněná.";
            case "When enabled, vanilla and detectable modded bosses are never removed." -> "Když je zapnuto, vanilla i detekovatelní modovaní bossové se nikdy nemažou.";
            case "One entity ID per line that is never removed." -> "Jedno entity ID na řádek, které se nikdy nemaže.";
            case "One item ID per line that is never removed." -> "Jedno item ID na řádek, které se nikdy nemaže.";
            case "Supports %seconds% placeholder." -> "Podporuje placeholder %seconds%.";
            case "Supports %items%, %mobs%, %xp%, %arrows%." -> "Podporuje %items%, %mobs%, %xp%, %arrows%.";
            case "Turns on /ai for players." -> "Zapne /ai pro hráče.";
            case "Controls whether player AI is open, time-gated, Discord-role gated, or both." -> "Řídí, zda je hráčské AI otevřené, omezené časem, Discord rolí, nebo obojím.";
            case "Used for playtime-based AI access. Set 0 to disable hour gating." -> "Používá se pro přístup k AI podle playtime. Nastav 0 pro vypnutí omezení hodinami.";
            case "One Discord role ID per line for AI access gating." -> "Jedno Discord role ID na řádek pro omezení přístupu k AI.";
            case "Turns on WebUI admin AI." -> "Zapne admin AI ve WebUI.";
            case "Masks sensitive values before admin AI sees config context." -> "Maskuje citlivé hodnoty dřív, než admin AI uvidí config context.";
            case "Maximum length of a player /ai prompt." -> "Maximální délka promptu hráče pro /ai.";
            case "Cooldown between player AI requests." -> "Cooldown mezi AI dotazy hráče.";
            case "Maximum length of an admin AI prompt." -> "Maximální délka admin AI promptu.";
            case "Maximum size of context passed to admin AI." -> "Maximální velikost kontextu posílaného admin AI.";
            case "OpenAI-compatible endpoint for player chat." -> "OpenAI-kompatibilní endpoint pro hráčský chat.";
            case "Stored server-side for player AI." -> "Uložené server-side pro hráčské AI.";
            case "Model name used for player chat." -> "Název modelu používaný pro hráčský chat.";
            case "Instructions for player AI replies." -> "Instrukce pro odpovědi hráčského AI.";
            case "Sampling temperature for player chat." -> "Sampling temperature pro hráčský chat.";
            case "Max generated tokens for player AI." -> "Max generované tokeny pro hráčské AI.";
            case "Request timeout in seconds for player AI." -> "Timeout requestu v sekundách pro hráčské AI.";
            case "OpenAI-compatible endpoint for admin AI." -> "OpenAI-kompatibilní endpoint pro admin AI.";
            case "Stored server-side for admin AI." -> "Uložené server-side pro admin AI.";
            case "Model name used for admin AI." -> "Název modelu používaný pro admin AI.";
            case "Instructions for admin AI replies." -> "Instrukce pro odpovědi admin AI.";
            case "Sampling temperature for admin AI." -> "Sampling temperature pro admin AI.";
            case "Max generated tokens for admin AI." -> "Max generované tokeny pro admin AI.";
            case "Request timeout in seconds for admin AI." -> "Timeout requestu v sekundách pro admin AI.";
            default -> description;
        };
    }

    private boolean isCzech() {
        return "cz".equalsIgnoreCase(currentLocale());
    }

    private String currentLocale() {
        return WebConfig.getInstance() != null ? WebConfig.getInstance().getLanguage() : "en";
    }

    private String msg(String english, String czech) {
        return isCzech() ? czech : english;
    }

    private Object fromJson(JsonElement value, Object currentValue) {
        if (currentValue instanceof Boolean) {
            return value.getAsBoolean();
        }
        if (currentValue instanceof Integer || currentValue instanceof Long) {
            return value.getAsInt();
        }
        if (currentValue instanceof Double || currentValue instanceof Float) {
            return value.getAsDouble();
        }
        if (currentValue instanceof List<?>) {
            List<String> list = new ArrayList<>();
            value.getAsJsonArray().forEach(item -> list.add(item.getAsString()));
            return list;
        }
        return value.getAsString();
    }

    private boolean equalsValue(Object currentValue, Object proposedValue) {
        if (currentValue instanceof List<?> currentList && proposedValue instanceof List<?> proposedList) {
            return currentList.equals(proposedList);
        }
        if (currentValue instanceof Number && proposedValue instanceof Number) {
            return ((Number) currentValue).doubleValue() == ((Number) proposedValue).doubleValue();
        }
        return String.valueOf(currentValue).equals(String.valueOf(proposedValue));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private boolean boolValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        return value == null ? List.of() : new ArrayList<>((List<String>) value);
    }
}
