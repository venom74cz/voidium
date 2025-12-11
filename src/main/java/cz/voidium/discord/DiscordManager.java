package cz.voidium.discord;

import cz.voidium.config.DiscordConfig;
import cz.voidium.config.StatsConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.EmbedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.awt.Color;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;

import cz.voidium.config.RanksConfig;
import net.minecraft.stats.Stats;

public class DiscordManager extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Discord");
    private static DiscordManager instance;
    private JDA jda;
    private MinecraftServer server;

    private record LogQueueItem(String message, org.apache.logging.log4j.Level level) {
    }

    private ScheduledExecutorService consoleExecutor;
    private final ConcurrentLinkedQueue<LogQueueItem> consoleQueue = new ConcurrentLinkedQueue<>();
    private DiscordConsoleAppender consoleAppender;

    private ScheduledExecutorService topicExecutor;
    private long serverStartTime;

    private DiscordManager() {
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public static synchronized DiscordManager getInstance() {
        if (instance == null) {
            instance = new DiscordManager();
        }
        return instance;
    }

    public void start() {
        DiscordConfig config = DiscordConfig.getInstance();
        if (!config.isEnableDiscord()) {
            LOGGER.info("Discord integration is disabled in config.");
            return;
        }

        String token = config.getBotToken();
        if (token == null || token.equals("YOUR_BOT_TOKEN_HERE") || token.isEmpty()) {
            LOGGER.warn("Discord bot token is invalid. Discord integration will not start.");
            return;
        }

        serverStartTime = System.currentTimeMillis();

        try {
            Activity activity = Activity.playing("Minecraft");
            String type = config.getBotActivityType();
            String text = config.getBotActivityText();
            if (type != null && text != null && !text.isEmpty()) {
                switch (type.toUpperCase()) {
                    case "WATCHING":
                        activity = Activity.watching(text);
                        break;
                    case "LISTENING":
                        activity = Activity.listening(text);
                        break;
                    case "COMPETING":
                        activity = Activity.competing(text);
                        break;
                    default:
                        activity = Activity.playing(text);
                        break;
                }
            }

            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MEMBERS)
                    .setActivity(activity)
                    .addEventListeners(this)
                    .build();

            jda.awaitReady();
            LOGGER.info("Discord bot started as {}", jda.getSelfUser().getAsTag());

            // Register commands
            jda.updateCommands().addCommands(
                    Commands.slash("link", "Propoji tvuj Discord ucet s Minecraft uctem")
                            .addOption(OptionType.STRING, "code", "Overovaci kod ze hry", true),
                    Commands.slash("unlink", "Odpoji tvuj Discord ucet od Minecraft uctu"),
                    Commands.slash("ticket", "Sprava ticketu")
                            .addSubcommands(
                                    new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("create",
                                            "Vytvori novy ticket")
                                            .addOption(OptionType.STRING, "reason", "Duvod ticketu", true),
                                    new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("close",
                                            "Uzavre aktualni ticket")))
                    .queue();

            startConsoleLogger();
            startTopicUpdater();

        } catch (InvalidTokenException e) {
            LOGGER.error("Invalid Discord Bot Token! Please check your configuration in config/voidium/discord.json.");
            LOGGER.error("Discord integration will be disabled until a valid token is provided.");
        } catch (Exception e) {
            LOGGER.error("Failed to start Discord bot", e);
        }
    }

    public void stop() {
        stopConsoleLogger();
        stopTopicUpdater();
        if (jda != null) {
            // Use shutdownNow for immediate shutdown without blocking
            jda.shutdownNow();
            jda = null;
            LOGGER.info("Discord bot stopped.");
        }
    }

    public void reload() {
        stop();
        start();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        DiscordConfig config = DiscordConfig.getInstance();
        if (event.getName().equals("link")) {
            // Check if command is from the correct guild
            String configuredGuildId = config.getGuildId();
            if (event.getGuild() == null || !event.getGuild().getId().equals(configuredGuildId)) {
                event.reply(config.getWrongGuildMessage()).setEphemeral(true).queue();
                return;
            }

            String code = event.getOption("code").getAsString();
            long discordId = event.getUser().getIdLong();

            LinkManager linkManager = LinkManager.getInstance();
            UUID playerUuid = linkManager.getPlayerFromCode(code);

            if (playerUuid == null) {
                event.reply(config.getInvalidCodeMessage()).setEphemeral(true).queue();
                return;
            }

            boolean success = linkManager.verifyCode(code, discordId);
            if (success) {
                // Try to get player name from server if online, otherwise use UUID
                String playerName = playerUuid.toString();
                if (server != null) {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
                    if (player != null) {
                        playerName = player.getName().getString();
                    }
                }

                String message = DiscordConfig.getInstance().getLinkSuccessMessage()
                        .replace("%player%", playerName);

                event.reply(message).setEphemeral(true).queue();

            } else {
                event.reply(DiscordConfig.getInstance().getAlreadyLinkedMessage()
                        .replace("%max%", String.valueOf(DiscordConfig.getInstance().getMaxAccountsPerDiscord())))
                        .setEphemeral(true).queue();
            }
        } else if (event.getName().equals("unlink")) {
            long discordId = event.getUser().getIdLong();
            LinkManager linkManager = LinkManager.getInstance();

            // Check if user is linked
            // Since we don't have a direct method to check by Discord ID in LinkManager
            // (only by UUID),
            // we can use unlinkDiscordId which removes all entries for that Discord ID.
            // But first we might want to check if they are linked at all to give a proper
            // message.
            // For now, let's just try to unlink.

            linkManager.unlinkDiscordId(discordId);
            event.reply(config.getUnlinkSuccessMessage()).setEphemeral(true).queue();
        } else if (event.getName().equals("ticket")) {
            String subcommand = event.getSubcommandName();
            if (subcommand == null)
                return;

            if (subcommand.equals("create")) {
                String reason = event.getOption("reason").getAsString();
                TicketManager.getInstance().createTicket(event.getMember(), reason);
                event.reply(config.getTicketCreatedMessage()).setEphemeral(true).queue();
            } else if (subcommand.equals("close")) {
                if (event.getChannel() instanceof net.dv8tion.jda.api.entities.channel.concrete.TextChannel) {
                    TicketManager.getInstance().closeTicket(
                            (net.dv8tion.jda.api.entities.channel.concrete.TextChannel) event.getChannel(),
                            event.getMember());
                    event.reply(config.getTicketClosingMessage()).setEphemeral(true).queue();
                } else {
                    event.reply(config.getTextChannelOnlyMessage()).setEphemeral(true).queue();
                }
            }
        }
    }

    @Override
    public void onButtonInteraction(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
        if (event.getComponentId().equals("close_ticket")) {
            if (event.getChannel() instanceof net.dv8tion.jda.api.entities.channel.concrete.TextChannel) {
                TicketManager.getInstance().closeTicket(
                        (net.dv8tion.jda.api.entities.channel.concrete.TextChannel) event.getChannel(),
                        event.getMember());
                event.reply(DiscordConfig.getInstance().getTicketClosingMessage()).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onGuildMemberRemove(net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent event) {
        String configuredGuildId = DiscordConfig.getInstance().getGuildId();
        if (event.getGuild().getId().equals(configuredGuildId)) {
            long discordId = event.getUser().getIdLong();
            LinkManager linkManager = LinkManager.getInstance();

            // Check if user is actually linked before logging
            if (!linkManager.getUuids(discordId).isEmpty()) {
                linkManager.unlinkDiscordId(discordId);
                LOGGER.info("Unlinked accounts for user {} because they left the guild.", event.getUser().getAsTag());
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot())
            return;

        String linkChannelId = DiscordConfig.getInstance().getLinkChannelId();
        // LOGGER.debug("Message received in channel {}, linkChannelId={}",
        // event.getChannel().getId(), linkChannelId);

        if (linkChannelId == null || linkChannelId.isEmpty()) {
            LOGGER.debug("Link channel ID is not configured, skipping message processing");
            return;
        }

        if (event.getChannel().getId().equals(linkChannelId)) {
            String message = event.getMessage().getContentRaw().trim();
            long discordId = event.getAuthor().getIdLong();

            LOGGER.info("Processing message '{}' from user {} (ID: {}) in link channel", message,
                    event.getAuthor().getAsTag(), discordId);

            // Delete the message to keep channel clean (security/privacy)
            try {
                event.getMessage().delete().queue();
            } catch (Exception ignored) {
            }

            LinkManager linkManager = LinkManager.getInstance();
            DiscordConfig config = DiscordConfig.getInstance();

            // First check if user is already linked
            List<UUID> linkedUuids = linkManager.getUuids(discordId);
            if (!linkedUuids.isEmpty()) {
                // User is already linked
                LOGGER.info("User {} (ID: {}) is already linked to {} account(s)", event.getAuthor().getAsTag(),
                        discordId, linkedUuids.size());
                StringBuilder sb = new StringBuilder();
                sb.append(event.getAuthor().getAsMention());
                if (linkedUuids.size() == 1) {
                    sb.append(config.getAlreadyLinkedSingleMessage().replace("%uuid%", linkedUuids.get(0).toString()));
                } else {
                    sb.append(config.getAlreadyLinkedMultipleMessage().replace("%count%",
                            String.valueOf(linkedUuids.size())));
                }
                event.getChannel().sendMessage(sb.toString())
                        .queue(msg -> msg.delete().queueAfter(8, java.util.concurrent.TimeUnit.SECONDS));
                return;
            }

            // Try to verify the code
            UUID playerUuid = linkManager.getPlayerFromCode(message);

            if (playerUuid == null) {
                // Invalid code or not a code - check if it's just a status check
                LOGGER.info("Invalid or expired code '{}' from user {} (ID: {}) - user not linked", message,
                        event.getAuthor().getAsTag(), discordId);
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + " " + config.getNotLinkedMessage())
                        .queue(msg -> msg.delete().queueAfter(8, java.util.concurrent.TimeUnit.SECONDS));
                return;
            }

            LOGGER.info("Valid code found for player UUID: {}", playerUuid);
            boolean success = linkManager.verifyCode(message, discordId);
            if (success) {
                LOGGER.info("Successfully linked Discord ID {} to player UUID {}", discordId, playerUuid);
                String successMsg = DiscordConfig.getInstance().getLinkSuccessMessage()
                        .replace("%player%", playerUuid.toString());

                event.getChannel().sendMessage(event.getAuthor().getAsMention() + " " + successMsg)
                        .queue(msg -> msg.delete().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS));

                // Assign Role
                String roleId = DiscordConfig.getInstance().getLinkedRoleId();
                if (roleId != null && !roleId.isEmpty()) {
                    try {
                        Role role = event.getGuild().getRoleById(roleId);
                        if (role != null) {
                            event.getGuild().addRoleToMember(event.getMember(), role).queue();
                            LOGGER.info("Assigned role {} to user {}", roleId, event.getAuthor().getAsTag());
                        } else {
                            LOGGER.warn("Configured linkedRoleId {} not found in guild.", roleId);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to assign role: {}", e.getMessage());
                    }
                }
            } else {
                LOGGER.warn("Link verification failed for Discord ID {} - max accounts limit reached", discordId);
                String msg = DiscordConfig.getInstance().getAlreadyLinkedMessage()
                        .replace("%max%", String.valueOf(DiscordConfig.getInstance().getMaxAccountsPerDiscord()));
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + " " + msg)
                        .queue(m -> m.delete().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS));
            }
        }

        // Check if message is in chat channel
        if (event.getChannel().getId().equals(DiscordConfig.getInstance().getChatChannelId())) {
            String message = event.getMessage().getContentDisplay(); // Gets readable content (resolves mentions)
            String user = event.getMember() != null ? event.getMember().getEffectiveName()
                    : event.getAuthor().getName();

            ChatBridge.getInstance().onDiscordMessage(user, message);
            return;
        }

        // Check if message is in a ticket channel
        if (event.getChannel() instanceof net.dv8tion.jda.api.entities.channel.concrete.TextChannel) {
            net.dv8tion.jda.api.entities.channel.concrete.TextChannel textChannel = (net.dv8tion.jda.api.entities.channel.concrete.TextChannel) event
                    .getChannel();
            if (textChannel.getName().startsWith("ticket-")) {
                // This is a ticket channel, look up the player name from the channel ID
                String playerName = TicketManager.getInstance().getPlayerNameForTicket(textChannel.getId());

                if (playerName == null) {
                    LOGGER.warn("No player mapping found for ticket channel {} ({})", textChannel.getName(),
                            textChannel.getId());
                    return;
                }

                String message = event.getMessage().getContentDisplay();
                String user = event.getMember() != null ? event.getMember().getEffectiveName()
                        : event.getAuthor().getName();

                LOGGER.info("Ticket message in channel '{}' (ID: {}) from '{}': found player '{}'",
                        textChannel.getName(), textChannel.getId(), user, playerName);

                // Send to player in Minecraft
                ChatBridge.getInstance().sendTicketMessageToPlayer(playerName, user, message);
                return;
            }
        }

        // LOGGER.debug("Message in channel {} ignored (not link, chat, or ticket
        // channel)", event.getChannel().getId());
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        if (!DiscordConfig.getInstance().isSyncBansDiscordToMc())
            return;
        if (server == null)
            return;

        String configuredGuildId = DiscordConfig.getInstance().getGuildId();
        if (!event.getGuild().getId().equals(configuredGuildId))
            return;

        long discordId = event.getUser().getIdLong();
        List<UUID> uuids = LinkManager.getInstance().getUuids(discordId);

        for (UUID uuid : uuids) {
            GameProfile profile = new GameProfile(uuid, null); // Name not needed for ban
            UserBanListEntry entry = new UserBanListEntry(profile, null, "Discord Ban Sync", null, "Banned on Discord");
            server.getPlayerList().getBans().add(entry);

            // Kick if online
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.connection.disconnect(Component.literal("You have been banned on Discord."));
            }
            LOGGER.info("Banned MC player {} because they were banned on Discord.", uuid);
        }
    }

    public void banDiscordUser(long discordId, String reason) {
        if (jda == null)
            return;
        String guildId = DiscordConfig.getInstance().getGuildId();
        net.dv8tion.jda.api.entities.Guild guild = jda.getGuildById(guildId);
        if (guild != null) {
            guild.ban(net.dv8tion.jda.api.entities.User.fromId(discordId), 0, java.util.concurrent.TimeUnit.DAYS)
                    .reason(reason)
                    .queue(
                            success -> LOGGER.info("Banned Discord user {} due to MC ban.", discordId),
                            error -> LOGGER.error("Failed to ban Discord user {}: {}", discordId, error.getMessage()));
        }
    }

    public void sendStatsReport(String date, int peak, double average) {
        LOGGER.info("sendStatsReport called: date={}, peak={}, average={}", date, peak, average);

        if (jda == null) {
            LOGGER.warn("Cannot send stats report - JDA is null");
            return;
        }

        String channelId = StatsConfig.getInstance().getReportChannelId();
        LOGGER.info("Stats report channelId from config: '{}'", channelId);

        if (channelId == null || channelId.isEmpty()) {
            LOGGER.warn("Cannot send stats report - reportChannelId is not configured");
            return;
        }

        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            LOGGER.info("Sending stats report to channel: {}", channel.getName());

            StatsConfig sc = StatsConfig.getInstance();
            String title = sc.getReportTitle().replace("%date%", date);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(title);
            eb.setColor(Color.CYAN);
            eb.addField(sc.getReportPeakLabel(), String.valueOf(peak), true);
            eb.addField(sc.getReportAverageLabel(), String.format("%.2f", average), true);
            eb.setFooter(sc.getReportFooter());

            channel.sendMessageEmbeds(eb.build()).queue(
                    success -> LOGGER.info("Stats report sent successfully"),
                    error -> LOGGER.error("Failed to send stats report: {}", error.getMessage()));
        } else {
            LOGGER.warn("Stats report channel {} not found. Check if channel ID is correct and bot has access.",
                    channelId);
        }
    }

    public void sendChatMessage(String player, UUID uuid, String message) {
        if (jda == null)
            return;

        String displayPlayer = player != null ? stripMinecraftColors(player) : "Server";
        String cleanMessage = stripMinecraftColors(message);

        String webhookUrl = DiscordConfig.getInstance().getChatWebhookUrl();
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            sendWebhookMessage(webhookUrl, displayPlayer, uuid, cleanMessage);
            return;
        }

        String channelId = DiscordConfig.getInstance().getChatChannelId();
        if (channelId == null || channelId.isEmpty())
            return;

        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            String format = DiscordConfig.getInstance().getMinecraftToDiscordFormat();
            String safeMessage = cleanMessage.replace("*", "\\*").replace("_", "\\_").replace("~", "\\~").replace("`",
                    "\\`");

            String finalMessage = format
                    .replace("%player%", displayPlayer)
                    .replace("%message%", safeMessage);

            channel.sendMessage(finalMessage).queue();
        }
    }

    private String stripMinecraftColors(String text) {
        if (text == null)
            return "";
        return text.replaceAll("§x§[0-9a-fA-F]§[0-9a-fA-F]§[0-9a-fA-F]§[0-9a-fA-F]§[0-9a-fA-F]§[0-9a-fA-F]", "")
                .replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    private void sendWebhookMessage(String url, String username, UUID uuid, String message) {
        try {
            // Simple JSON construction
            String avatarUrl;
            if (uuid != null) {
                avatarUrl = "https://minotar.net/helm/" + uuid.toString() + "/128.png";
            } else {
                // Use a default avatar for server messages (e.g. console or system)
                avatarUrl = "https://cdn.discordapp.com/embed/avatars/0.png";
            }

            // Escape JSON strings
            String safeUsername = username.replace("\"", "\\\"");
            String safeMessage = message.replace("\"", "\\\"").replace("\n", "\\n");

            String json = String.format("{\"username\": \"%s\", \"avatar_url\": \"%s\", \"content\": \"%s\"}",
                    safeUsername, avatarUrl, safeMessage);

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();

            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            LOGGER.error("Failed to send webhook message", e);
        }
    }

    private void startTopicUpdater() {
        if (topicExecutor != null)
            return;
        if (!DiscordConfig.getInstance().isEnableTopicUpdate())
            return;

        topicExecutor = Executors.newSingleThreadScheduledExecutor();
        // Discord rate limit: ~2 updates per 10 minutes, so 10 min interval is safe
        topicExecutor.scheduleAtFixedRate(this::updateChannelTopic, 2, 10, TimeUnit.MINUTES);
    }

    private void stopTopicUpdater() {
        if (topicExecutor != null) {
            topicExecutor.shutdownNow();
            topicExecutor = null;
        }
    }

    private void updateChannelTopic() {
        if (jda == null || server == null)
            return;
        if (!DiscordConfig.getInstance().isEnableTopicUpdate())
            return;

        String channelId = DiscordConfig.getInstance().getChatChannelId();
        if (channelId == null || channelId.isEmpty())
            return;

        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null)
            return;

        int online = server.getPlayerCount();
        int max = server.getMaxPlayers();
        long uptimeMillis = System.currentTimeMillis() - serverStartTime;
        String uptime = formatDuration(uptimeMillis);
        double tps = 20.0;

        String format = DiscordConfig.getInstance().getChannelTopicFormat();
        String topic = format
                .replace("%online%", String.valueOf(online))
                .replace("%max%", String.valueOf(max))
                .replace("%tps%", String.format("%.1f", tps))
                .replace("%uptime%", uptime);

        // Check if topic is different to avoid API calls
        if (topic.equals(channel.getTopic()))
            return;

        channel.getManager().setTopic(topic).queue(
                success -> {
                }, // LOGGER.debug("Channel topic updated: {}", topic),
                error -> LOGGER.error("Failed to update channel topic: {}", error.getMessage()));
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        String format = DiscordConfig.getInstance().getUptimeFormat();
        if (format == null || format.isEmpty()) {
            format = "%days%d %hours%h %minutes%m";
        }

        return format
                .replace("%days%", String.valueOf(days))
                .replace("%hours%", String.valueOf(hours % 24))
                .replace("%minutes%", String.valueOf(minutes % 60))
                .replace("%seconds%", String.valueOf(seconds % 60));
    }

    public JDA getJda() {
        return jda;
    }

    // New overload for specific level
    public void queueConsoleMessage(String message, org.apache.logging.log4j.Level level) {
        if (message.contains("Gathered mod list to write to world save")) {
            return;
        }
        consoleQueue.offer(new LogQueueItem(message, level));
    }

    // Kept for compatibility if called from elsewhere, defaults to INFO
    public void queueConsoleMessage(String message) {
        queueConsoleMessage(message, org.apache.logging.log4j.Level.INFO);
    }

    private void startConsoleLogger() {
        DiscordConfig config = DiscordConfig.getInstance();

        // Debug/Warning checks
        // Auto-enable if channel ID is present
        String channelId = config.getConsoleChannelId();

        if (channelId == null || channelId.isEmpty()) {
            return;
        }

        LOGGER.info("Discord Console Logging enabled for channel {}", channelId);

        try {
            // Attach Appender
            // Use specific context selector if needed, but usually false (current context)
            // is correct for runtime attachment
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration logConfig = ctx.getConfiguration();

            // Remove existing if present to avoid duplicates during reload
            if (consoleAppender != null) {
                stopConsoleLogger();
            }

            consoleAppender = DiscordConsoleAppender.createAppender("DiscordConsole", null,
                    PatternLayout.createDefaultLayout());
            consoleAppender.start();

            logConfig.addAppender(consoleAppender);

            // Add to Root Logger to capture everything
            org.apache.logging.log4j.core.config.LoggerConfig rootLoggerConfig = logConfig.getRootLogger();
            rootLoggerConfig.addAppender(consoleAppender, null, null);

            ctx.updateLoggers();
            LOGGER.info("Discord Console Appender attached successfully.");

            // Start Consumer Task
            consoleExecutor = Executors.newSingleThreadScheduledExecutor();
            consoleExecutor.scheduleAtFixedRate(this::processConsoleQueue, 1, 3, TimeUnit.SECONDS);

        } catch (Exception e) {
            LOGGER.error("Failed to start Discord Console Logger", e);
        }
    }

    private void stopConsoleLogger() {
        try {
            if (consoleExecutor != null) {
                consoleExecutor.shutdownNow();
                consoleExecutor = null;
            }

            if (consoleAppender != null) {
                LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
                Configuration logConfig = ctx.getConfiguration();

                consoleAppender.stop();
                logConfig.getRootLogger().removeAppender("DiscordConsole");
                ctx.updateLoggers();
                consoleAppender = null;
            }
            consoleQueue.clear();
        } catch (Exception e) {
            LOGGER.error("Error stopping Discord Console Logger", e);
        }
    }

    private void processConsoleQueue() {
        if (jda == null || consoleQueue.isEmpty())
            return;

        String channelId = DiscordConfig.getInstance().getConsoleChannelId();
        if (channelId == null || channelId.isEmpty())
            return;

        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null)
            return;

        java.util.function.Consumer<Throwable> errorHandler = error -> {
            if (error instanceof java.util.concurrent.CancellationException
                    || error instanceof java.io.InterruptedIOException) {
                return;
            }
            if (error instanceof net.dv8tion.jda.api.exceptions.ErrorResponseException) {
                if (error.getCause() instanceof java.io.InterruptedIOException) {
                    return;
                }
            }
            // Only log real errors
            // LOGGER.warn("Failed to send console log: {}", error.getMessage());
        };

        StringBuilder batch = new StringBuilder();
        while (!consoleQueue.isEmpty()) {
            LogQueueItem item = consoleQueue.poll();
            String line = item.message();

            // Format line with ANSI colors based on level
            String ansiColor = "\u001b[0m"; // Default
            int levelInt = item.level().intLevel();

            if (levelInt <= org.apache.logging.log4j.Level.ERROR.intLevel()) { // Error or Fatal
                ansiColor = "\u001b[0;31m"; // Red
            } else if (levelInt <= org.apache.logging.log4j.Level.WARN.intLevel()) {
                ansiColor = "\u001b[0;33m"; // Yellow
            } else if (levelInt <= org.apache.logging.log4j.Level.INFO.intLevel()) {
                ansiColor = "\u001b[0;36m"; // Cyan (or Green 32)
            }

            // Minimal cleanup of Minecraft codes if present (optional)
            line = stripMinecraftColors(line);

            String formattedLine = ansiColor + line + "\u001b[0m";

            if (batch.length() + formattedLine.length() + 1 > 1900) {
                // Send current batch
                channel.sendMessage("```ansi\n" + batch.toString() + "```").queue(null, errorHandler);
                batch.setLength(0);
            }
            batch.append(formattedLine).append("\n");
        }

        if (batch.length() > 0) {
            channel.sendMessage("```ansi\n" + batch.toString() + "```").queue(null, errorHandler);
        }
    }

    public void sendStatusMessage(String message) {
        if (jda == null || !DiscordConfig.getInstance().isEnableStatusMessages())
            return;

        String channelId = DiscordConfig.getInstance().getStatusChannelId();
        if (channelId == null || channelId.isEmpty()) {
            LOGGER.warn(
                    "Status messages are enabled but no channel ID is configured (statusChannelId or chatChannelId)");
            return;
        }

        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(message).queue(
                    success -> LOGGER.debug("Status message sent: {}", message),
                    error -> LOGGER.error("Failed to send status message: {}", error.getMessage()));
        } else {
            LOGGER.error("Status channel not found with ID: {}", channelId);
        }
    }

    public List<Role> getRoles() {
        if (jda == null)
            return java.util.Collections.emptyList();
        String guildId = DiscordConfig.getInstance().getGuildId();
        if (guildId == null || guildId.isEmpty())
            return java.util.Collections.emptyList();
        net.dv8tion.jda.api.entities.Guild guild = jda.getGuildById(guildId);
        if (guild == null)
            return java.util.Collections.emptyList();
        return guild.getRoles();
    }

    /**
     * Get Discord role IDs for a player by their UUID
     * 
     * @param playerUuid Player's UUID
     * @return List of Discord role IDs (as strings), or empty list if not linked or
     *         unavailable
     */
    public List<String> getPlayerDiscordRoles(java.util.UUID playerUuid) {
        if (jda == null)
            return java.util.Collections.emptyList();

        // Get Discord ID from linked accounts
        Long discordId = LinkManager.getInstance().getDiscordId(playerUuid);
        if (discordId == null)
            return java.util.Collections.emptyList();

        // Get guild
        String guildId = DiscordConfig.getInstance().getGuildId();
        if (guildId == null || guildId.isEmpty())
            return java.util.Collections.emptyList();
        net.dv8tion.jda.api.entities.Guild guild = jda.getGuildById(guildId);
        if (guild == null)
            return java.util.Collections.emptyList();

        try {
            // Retrieve member (synchronously for simplicity)
            net.dv8tion.jda.api.entities.Member member = guild.retrieveMemberById(discordId).complete();
            if (member == null)
                return java.util.Collections.emptyList();

            // Extract role IDs
            List<String> roleIds = new java.util.ArrayList<>();
            for (Role role : member.getRoles()) {
                roleIds.add(role.getId());
            }
            return roleIds;
        } catch (Exception e) {
            LOGGER.error("Error fetching Discord roles for UUID {}: {}", playerUuid, e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public Role getRole(String roleId) {
        if (jda == null)
            return null;
        DiscordConfig config = DiscordConfig.getInstance();
        String guildId = config.getGuildId();
        if (guildId == null || guildId.isEmpty())
            return null;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null)
            return null;

        return guild.getRoleById(roleId);
    }
}
