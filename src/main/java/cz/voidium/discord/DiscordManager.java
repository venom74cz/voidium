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

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;

public class DiscordManager extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Discord");
    private static DiscordManager instance;
    private JDA jda;
    private MinecraftServer server;
    
    private ScheduledExecutorService consoleExecutor;
    private final ConcurrentLinkedQueue<String> consoleQueue = new ConcurrentLinkedQueue<>();
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
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                    .setActivity(Activity.playing("Minecraft"))
                    .addEventListeners(this)
                    .build();
            
            jda.awaitReady();
            LOGGER.info("Discord bot started as {}", jda.getSelfUser().getAsTag());
            
            // Register commands
            jda.updateCommands().addCommands(
                Commands.slash("link", "Propoji tvuj Discord ucet s Minecraft uctem")
                        .addOption(OptionType.STRING, "code", "Overovaci kod ze hry", true),
                Commands.slash("unlink", "Odpoji tvuj Discord ucet od Minecraft uctu")
            ).queue();
            
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
            jda.shutdown();
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
        if (event.getName().equals("link")) {
            // Check if command is from the correct guild
            String configuredGuildId = DiscordConfig.getInstance().getGuildId();
            if (event.getGuild() == null || !event.getGuild().getId().equals(configuredGuildId)) {
                event.reply("Tento prikaz lze pouzit pouze na oficialnim Discord serveru.").setEphemeral(true).queue();
                return;
            }

            String code = event.getOption("code").getAsString();
            long discordId = event.getUser().getIdLong();
            
            LinkManager linkManager = LinkManager.getInstance();
            UUID playerUuid = linkManager.getPlayerFromCode(code);
            
            if (playerUuid == null) {
                event.reply("Neplatny nebo expirovany kod.").setEphemeral(true).queue();
                return;
            }
            
            boolean success = linkManager.verifyCode(code, discordId);
            if (success) {
                String playerName = "Hrac"; // TODO: Get player name if possible
                
                String message = DiscordConfig.getInstance().getLinkSuccessMessage()
                        .replace("%player%", playerUuid.toString());
                
                event.reply(message).setEphemeral(true).queue();
                
                // Rename user if configured (TODO)
                // event.getGuild().modifyNickname(event.getMember(), playerName).queue();
                
            } else {
                event.reply(DiscordConfig.getInstance().getAlreadyLinkedMessage()
                        .replace("%max%", String.valueOf(DiscordConfig.getInstance().getMaxAccountsPerDiscord())))
                        .setEphemeral(true).queue();
            }
        } else if (event.getName().equals("unlink")) {
            long discordId = event.getUser().getIdLong();
            LinkManager linkManager = LinkManager.getInstance();
            
            // Check if user is linked
            // Since we don't have a direct method to check by Discord ID in LinkManager (only by UUID),
            // we can use unlinkDiscordId which removes all entries for that Discord ID.
            // But first we might want to check if they are linked at all to give a proper message.
            // For now, let's just try to unlink.
            
            linkManager.unlinkDiscordId(discordId);
            event.reply("Vsechny propojene ucty byly uspesne odpojeny.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onGuildMemberRemove(net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent event) {
        String configuredGuildId = DiscordConfig.getInstance().getGuildId();
        if (event.getGuild().getId().equals(configuredGuildId)) {
            long discordId = event.getUser().getIdLong();
            LinkManager linkManager = LinkManager.getInstance();
            linkManager.unlinkDiscordId(discordId);
            LOGGER.info("Unlinked accounts for user {} because they left the guild.", event.getUser().getAsTag());
        }
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        
        String linkChannelId = DiscordConfig.getInstance().getLinkChannelId();
        if (linkChannelId == null || linkChannelId.isEmpty()) return;
        
        if (event.getChannel().getId().equals(linkChannelId)) {
            String code = event.getMessage().getContentRaw().trim();
            long discordId = event.getAuthor().getIdLong();
            
            // Delete the message to keep channel clean (security/privacy)
            try {
                event.getMessage().delete().queue();
            } catch (Exception ignored) {}

            LinkManager linkManager = LinkManager.getInstance();
            UUID playerUuid = linkManager.getPlayerFromCode(code);
            
            if (playerUuid == null) {
                // Invalid code
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", tento kod je neplatny nebo expirovany.")
                        .queue(msg -> msg.delete().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS));
                return;
            }
            
            boolean success = linkManager.verifyCode(code, discordId);
            if (success) {
                String message = DiscordConfig.getInstance().getLinkSuccessMessage()
                        .replace("%player%", playerUuid.toString());
                
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + " " + message)
                        .queue(msg -> msg.delete().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS));
                
                // Assign Role
                String roleId = DiscordConfig.getInstance().getLinkedRoleId();
                if (roleId != null && !roleId.isEmpty()) {
                    try {
                        Role role = event.getGuild().getRoleById(roleId);
                        if (role != null) {
                            event.getGuild().addRoleToMember(event.getMember(), role).queue();
                        } else {
                            LOGGER.warn("Configured linkedRoleId {} not found in guild.", roleId);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to assign role: {}", e.getMessage());
                    }
                }
                
                // Rename user if configured (TODO)
                // event.getGuild().modifyNickname(event.getMember(), "PlayerName").queue();
            } else {
                String msg = DiscordConfig.getInstance().getAlreadyLinkedMessage()
                        .replace("%max%", String.valueOf(DiscordConfig.getInstance().getMaxAccountsPerDiscord()));
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + " " + msg)
                        .queue(m -> m.delete().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS));
            }
        } else if (event.getChannel().getId().equals(DiscordConfig.getInstance().getChatChannelId())) {
            String message = event.getMessage().getContentDisplay(); // Gets readable content (resolves mentions)
            String user = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
            
            ChatBridge.getInstance().onDiscordMessage(user, message);
            return;
        }
    }
    
    @Override
    public void onGuildBan(GuildBanEvent event) {
        if (!DiscordConfig.getInstance().isSyncBansDiscordToMc()) return;
        if (server == null) return;
        
        String configuredGuildId = DiscordConfig.getInstance().getGuildId();
        if (!event.getGuild().getId().equals(configuredGuildId)) return;
        
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
        if (jda == null) return;
        String guildId = DiscordConfig.getInstance().getGuildId();
        net.dv8tion.jda.api.entities.Guild guild = jda.getGuildById(guildId);
        if (guild != null) {
            guild.ban(net.dv8tion.jda.api.entities.User.fromId(discordId), 0, java.util.concurrent.TimeUnit.DAYS)
                 .reason(reason)
                 .queue(
                     success -> LOGGER.info("Banned Discord user {} due to MC ban.", discordId),
                     error -> LOGGER.error("Failed to ban Discord user {}: {}", discordId, error.getMessage())
                 );
        }
    }
    
    public void sendStatsReport(String date, int peak, double average) {
        if (jda == null) return;
        
        String channelId = StatsConfig.getInstance().getReportChannelId();
        if (channelId == null || channelId.isEmpty()) return;
        
        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("📊 Denní Statistiky - " + date);
            eb.setColor(Color.CYAN);
            eb.addField("Peak Hráčů", String.valueOf(peak), true);
            eb.addField("Průměr Hráčů", String.format("%.2f", average), true);
            eb.setFooter("Voidium Stats");
            
            channel.sendMessageEmbeds(eb.build()).queue();
        } else {
            LOGGER.warn("Stats report channel {} not found.", channelId);
        }
    }
    
    public void sendChatMessage(String player, UUID uuid, String message) {
        if (jda == null) return;
        
        String displayPlayer = player != null ? player : "Server";
        
        String webhookUrl = DiscordConfig.getInstance().getChatWebhookUrl();
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            sendWebhookMessage(webhookUrl, displayPlayer, uuid, message);
            return;
        }
        
        String channelId = DiscordConfig.getInstance().getChatChannelId();
        if (channelId == null || channelId.isEmpty()) return;
        
        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            String format = DiscordConfig.getInstance().getMinecraftToDiscordFormat();
            // Escape markdown in message to prevent injection
            String safeMessage = message.replace("*", "\\*").replace("_", "\\_").replace("~", "\\~").replace("`", "\\`");
            
            String finalMessage = format
                    .replace("%player%", displayPlayer)
                    .replace("%message%", safeMessage);
            
            channel.sendMessage(finalMessage).queue();
        }
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
        if (topicExecutor != null) return;
        if (!DiscordConfig.getInstance().isEnableTopicUpdate()) return;
        
        topicExecutor = Executors.newSingleThreadScheduledExecutor();
        topicExecutor.scheduleAtFixedRate(this::updateChannelTopic, 1, 6, TimeUnit.MINUTES);
    }

    private void stopTopicUpdater() {
        if (topicExecutor != null) {
            topicExecutor.shutdownNow();
            topicExecutor = null;
        }
    }

    private void updateChannelTopic() {
        if (jda == null || server == null) return;
        String channelId = DiscordConfig.getInstance().getChatChannelId();
        if (channelId == null || channelId.isEmpty()) return;

        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        int online = server.getPlayerCount();
        int max = server.getMaxPlayers();
        long uptimeMillis = System.currentTimeMillis() - serverStartTime;
        String uptime = formatDuration(uptimeMillis);

        String format = DiscordConfig.getInstance().getChannelTopicFormat();
        String topic = format
                .replace("%online%", String.valueOf(online))
                .replace("%max%", String.valueOf(max))
                .replace("%uptime%", uptime);
        
        // Check if topic is different to avoid API calls
        if (topic.equals(channel.getTopic())) return;

        channel.getManager().setTopic(topic).queue();
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

    public void queueConsoleMessage(String message) {
        consoleQueue.offer(message);
    }

    private void startConsoleLogger() {
        if (!DiscordConfig.getInstance().isEnableConsoleLog()) return;
        
        // Attach Appender
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        consoleAppender = DiscordConsoleAppender.createAppender("DiscordConsole", null, PatternLayout.createDefaultLayout());
        consoleAppender.start();
        config.addAppender(consoleAppender);
        config.getRootLogger().addAppender(consoleAppender, null, null);
        ctx.updateLoggers();

        // Start Consumer Task
        consoleExecutor = Executors.newSingleThreadScheduledExecutor();
        consoleExecutor.scheduleAtFixedRate(this::processConsoleQueue, 1, 2, TimeUnit.SECONDS);
    }

    private void stopConsoleLogger() {
        if (consoleExecutor != null) {
            consoleExecutor.shutdownNow();
            consoleExecutor = null;
        }
        
        if (consoleAppender != null) {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            consoleAppender.stop();
            config.getRootLogger().removeAppender("DiscordConsole");
            ctx.updateLoggers();
            consoleAppender = null;
        }
        consoleQueue.clear();
    }

    private void processConsoleQueue() {
        if (jda == null || consoleQueue.isEmpty()) return;
        
        String channelId = DiscordConfig.getInstance().getConsoleChannelId();
        if (channelId == null || channelId.isEmpty()) return;
        
        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        StringBuilder batch = new StringBuilder();
        while (!consoleQueue.isEmpty()) {
            String line = consoleQueue.poll();
            if (batch.length() + line.length() + 1 > 1900) {
                // Send current batch
                channel.sendMessage("```" + batch.toString() + "```").queue();
                batch.setLength(0);
            }
            batch.append(line).append("\n");
        }
        
        if (batch.length() > 0) {
            channel.sendMessage("```" + batch.toString() + "```").queue();
        }
    }
    
    public void sendStatusMessage(String message) {
        if (jda == null || !DiscordConfig.getInstance().isEnableStatusMessages()) return;
        
        String channelId = DiscordConfig.getInstance().getStatusChannelId();
        if (channelId == null || channelId.isEmpty()) return;
        
        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        }
    }
}
