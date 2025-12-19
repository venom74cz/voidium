package cz.voidium.discord;

import cz.voidium.config.TicketConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

public class TicketManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Tickets");
    private static TicketManager instance;
    
    // Map: ticket channel ID -> Minecraft player name
    private final java.util.Map<String, String> ticketToPlayer = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Map: ticket channel ID -> list of messages (for transcript)
    private final java.util.Map<String, java.util.List<String>> ticketMessages = new java.util.concurrent.ConcurrentHashMap<>();

    private TicketManager() {}

    public static synchronized TicketManager getInstance() {
        if (instance == null) {
            instance = new TicketManager();
        }
        return instance;
    }

    public void createTicket(Member member, String reason) {
        TicketConfig config = TicketConfig.getInstance();
        if (!config.isEnableTickets()) return;

        Guild guild = member.getGuild();
        String categoryId = config.getTicketCategoryId();
        Category category = guild.getCategoryById(categoryId);

        if (category == null) {
            LOGGER.warn("Ticket category not found! Check configuration.");
            return;
        }

        // Check limit
        long userTicketCount = category.getTextChannels().stream()
                .filter(c -> c.getName().startsWith("ticket-" + member.getUser().getName().toLowerCase()))
                .count();

        if (userTicketCount >= config.getMaxTicketsPerUser()) {
            // Ideally send a private message or ephemeral reply, but here we just log/return
            // The slash command handler should handle the reply based on return value or callback
            return;
        }

        String channelName = "ticket-" + member.getUser().getName();
        
        category.createTextChannel(channelName)
                .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(channel -> {
                    // Add support role permission
                    String supportRoleId = config.getSupportRoleId();
                    if (supportRoleId != null && !supportRoleId.isEmpty()) {
                        Role supportRole = guild.getRoleById(supportRoleId);
                        if (supportRole != null) {
                            channel.upsertPermissionOverride(supportRole)
                                    .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                                    .queue();
                        }
                    }

                    // Send welcome message with ping
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle("Ticket Created");
                    embed.setDescription(config.getTicketWelcomeMessage()
                            .replace("%user%", member.getAsMention())
                            .replace("%reason%", reason));
                    embed.setColor(Color.GREEN);
                    
                    channel.sendMessage(member.getAsMention())
                            .addEmbeds(embed.build())
                            .setActionRow(Button.danger("close_ticket", "Close Ticket"))
                            .queue();
                    
                    // Set topic
                    channel.getManager().setTopic(config.getTicketChannelTopic()
                            .replace("%user%", member.getUser().getAsTag())
                            .replace("%reason%", reason)).queue();

                });
    }

    public void closeTicket(TextChannel channel, Member closer) {
        TicketConfig config = TicketConfig.getInstance();
        
        String playerName = ticketToPlayer.remove(channel.getId());
        LOGGER.info("Closing ticket channel {}", channel.getName());
        
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Ticket Closed");
        embed.setDescription(config.getTicketCloseMessage()
                .replace("%user%", closer.getAsMention()));
        embed.setColor(Color.RED);
        
        channel.sendMessageEmbeds(embed.build()).queue(msg -> {
            // Generate transcript if enabled
            if (config.isEnableTranscript()) {
                generateTranscript(channel, playerName, config);
            }
            
            // Delete after 5 seconds
            channel.delete().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS);
        });
    }
    
    private void generateTranscript(TextChannel channel, String playerName, TicketConfig config) {
        // Fetch message history
        channel.getHistory().retrievePast(100).queue(messages -> {
            if (messages.isEmpty()) return;
            
            String format = config.getTranscriptFormat().toUpperCase();
            java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
            String date = java.time.LocalDateTime.now().format(dateFormatter);
            String user = playerName != null ? playerName : "unknown";
            
            String filename = config.getTranscriptFilename()
                    .replace("%user%", user)
                    .replace("%date%", date)
                    .replace("%reason%", "support");
            
            if (!filename.endsWith("." + format.toLowerCase())) {
                filename += "." + format.toLowerCase();
            }
            
            StringBuilder transcript = new StringBuilder();
            
            if ("TXT".equals(format)) {
                transcript.append("=".repeat(60)).append("\n");
                transcript.append("TICKET TRANSCRIPT\n");
                transcript.append("Channel: ").append(channel.getName()).append("\n");
                transcript.append("Player: ").append(user).append("\n");
                transcript.append("Closed: ").append(java.time.LocalDateTime.now()).append("\n");
                transcript.append("=".repeat(60)).append("\n\n");
                
                // Reverse to show oldest first
                java.util.Collections.reverse(messages);
                for (net.dv8tion.jda.api.entities.Message message : messages) {
                    String timestamp = message.getTimeCreated().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    String author = message.getAuthor().getAsTag();
                    String content = message.getContentDisplay();
                    
                    transcript.append("[").append(timestamp).append("] ");
                    transcript.append(author).append(": ");
                    transcript.append(content).append("\n");
                    
                    // Include embeds
                    if (!message.getEmbeds().isEmpty()) {
                        for (net.dv8tion.jda.api.entities.MessageEmbed embed : message.getEmbeds()) {
                            if (embed.getTitle() != null) {
                                transcript.append("  [EMBED] ").append(embed.getTitle()).append("\n");
                            }
                            if (embed.getDescription() != null) {
                                transcript.append("  ").append(embed.getDescription()).append("\n");
                            }
                        }
                    }
                }
            } else {
                // JSON format
                transcript.append("{\n");
                transcript.append("  \"channel\": \"").append(channel.getName()).append("\",\n");
                transcript.append("  \"player\": \"").append(user).append("\",\n");
                transcript.append("  \"closed\": \"").append(java.time.LocalDateTime.now()).append("\",\n");
                transcript.append("  \"messages\": [\n");
                
                java.util.Collections.reverse(messages);
                for (int i = 0; i < messages.size(); i++) {
                    net.dv8tion.jda.api.entities.Message message = messages.get(i);
                    transcript.append("    {\n");
                    transcript.append("      \"timestamp\": \"").append(message.getTimeCreated()).append("\",\n");
                    transcript.append("      \"author\": \"").append(message.getAuthor().getAsTag().replace("\"", "\\\"")).append("\",\n");
                    transcript.append("      \"content\": \"").append(message.getContentDisplay().replace("\"", "\\\"").replace("\n", "\\n")).append("\"\n");
                    transcript.append("    }").append(i < messages.size() - 1 ? "," : "").append("\n");
                }
                
                transcript.append("  ]\n");
                transcript.append("}\n");
            }
            
            // Upload transcript
            try {
                byte[] data = transcript.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                channel.sendMessage("📄 Ticket transcript:")
                        .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(data, filename))
                        .queue(
                            success -> LOGGER.info("Transcript saved for ticket {}", channel.getName()),
                            error -> LOGGER.error("Failed to upload transcript: {}", error.getMessage())
                        );
            } catch (Exception e) {
                LOGGER.error("Error generating transcript: {}", e.getMessage());
            }
        }, error -> {
            LOGGER.error("Failed to retrieve message history: {}", error.getMessage());
        });
    }
    
    public String getPlayerNameForTicket(String channelId) {
        return ticketToPlayer.get(channelId);
    }

    public void createTicket(long discordId, String reason, String initialMessage, net.minecraft.server.level.ServerPlayer player) {
        JDA jda = DiscordManager.getInstance().getJda();
        if (jda == null) {
            player.sendSystemMessage(cz.voidium.config.VoidiumConfig.formatMessage(TicketConfig.getInstance().getMcBotNotConnectedMessage()));
            return;
        }
        
        String guildId = cz.voidium.config.DiscordConfig.getInstance().getGuildId();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
             player.sendSystemMessage(cz.voidium.config.VoidiumConfig.formatMessage(TicketConfig.getInstance().getMcGuildNotFoundMessage()));
             return;
        }
        
        String playerName = player.getName().getString();
        TicketConfig config = TicketConfig.getInstance();
        
        guild.retrieveMemberById(discordId).queue(member -> {
            // Check limit BEFORE creating ticket
            String categoryId = config.getTicketCategoryId();
            Category category = guild.getCategoryById(categoryId);
            
            if (category == null) {
                player.sendSystemMessage(cz.voidium.config.VoidiumConfig.formatMessage(config.getMcCategoryNotFoundMessage()));
                LOGGER.warn("Ticket category not found! Check configuration.");
                return;
            }
            
            long userTicketCount = category.getTextChannels().stream()
                    .filter(c -> c.getName().startsWith("ticket-" + member.getUser().getName().toLowerCase()))
                    .count();
            
            if (userTicketCount >= config.getMaxTicketsPerUser()) {
                String message = config.getTicketLimitReachedMessage()
                    .replace("&", "§")
                    .replace("%max%", String.valueOf(config.getMaxTicketsPerUser()));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c" + message));
                return;
            }
            
            createTicketWithMessage(member, reason, initialMessage, player);
            player.sendSystemMessage(cz.voidium.config.VoidiumConfig.formatMessage(config.getMcTicketCreatedMessage()));
        }, error -> {
            player.sendSystemMessage(cz.voidium.config.VoidiumConfig.formatMessage(TicketConfig.getInstance().getMcDiscordNotFoundMessage()));
        });
    }
    
    private void createTicketWithMessage(Member member, String reason, String initialMessage, net.minecraft.server.level.ServerPlayer player) {
        TicketConfig config = TicketConfig.getInstance();
        if (!config.isEnableTickets()) return;

        Guild guild = member.getGuild();
        String categoryId = config.getTicketCategoryId();
        Category category = guild.getCategoryById(categoryId);

        if (category == null) {
            LOGGER.warn("Ticket category not found! Check configuration.");
            return;
        }

        String channelName = "ticket-" + member.getUser().getName();
        
        category.createTextChannel(channelName)
                .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(channel -> {
                    // Add support role permission
                    String supportRoleId = config.getSupportRoleId();
                    if (supportRoleId != null && !supportRoleId.isEmpty()) {
                        Role supportRole = guild.getRoleById(supportRoleId);
                        if (supportRole != null) {
                            channel.upsertPermissionOverride(supportRole)
                                    .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                                    .queue();
                        }
                    }

                    // Store mapping: channel ID -> player name
                    String playerName = player.getName().getString();
                    ticketToPlayer.put(channel.getId(), playerName);
                    LOGGER.info("Created ticket channel {} for player {}", channel.getName(), playerName);
                    
                    // Send Packet to Client
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, 
                        new cz.voidium.network.PacketTicketCreated("ticket-" + channel.getId(), "Ticket #" + channel.getName().replace("ticket-", ""))
                    );
                    
                    // Send welcome message with ping
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle("Ticket Created");
                    embed.setDescription(config.getTicketWelcomeMessage()
                            .replace("%user%", member.getAsMention())
                            .replace("%reason%", reason));
                    embed.setColor(Color.GREEN);
                    
                    channel.sendMessage(member.getAsMention())
                            .addEmbeds(embed.build())
                            .setActionRow(Button.danger("close_ticket", "Close Ticket"))
                            .queue();
                    
                    // Send initial message from player
                    channel.sendMessage("**" + playerName + "**: " + initialMessage).queue();
                    
                    // Set topic
                    channel.getManager().setTopic(config.getTicketChannelTopic()
                            .replace("%user%", member.getUser().getAsTag())
                            .replace("%reason%", reason)).queue();
                });
    }

    public void replyToTicket(net.minecraft.server.level.ServerPlayer player, String message) {
        String channelId = getOpenTicketChannel(player);
        if (channelId == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cYou do not have an open ticket to reply to."));
            return;
        }

        JDA jda = DiscordManager.getInstance().getJda();
        if (jda == null) return;
        
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            // Cleanup invalid entry
            ticketToPlayer.remove(channelId);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cTicket channel not found."));
            return;
        }

        String playerName = player.getName().getString();
        channel.sendMessage("**" + playerName + "**: " + message).queue(
            success -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aReply sent!")),
            error -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cFailed to send reply: " + error.getMessage()))
        );
    }
    
    public String getOpenTicketChannel(net.minecraft.server.level.ServerPlayer player) {
        String playerName = player.getName().getString();
        return ticketToPlayer.entrySet().stream()
                .filter(entry -> entry.getValue().equals(playerName))
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
