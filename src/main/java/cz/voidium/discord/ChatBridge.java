package cz.voidium.discord;

import cz.voidium.config.DiscordConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.CommandEvent;

import java.util.HashMap;
import java.util.Map;

public class ChatBridge {
    private static ChatBridge instance;
    private MinecraftServer server;

    // Simple Emoji Map (Discord -> MC)
    private static final Map<String, String> EMOJI_MAP = new HashMap<>();
    static {
        // Prevent legacy replacement, but we might want to map Unicode -> Shortcode for
        // client rendering
        // E.g. 😄 -> :smile:
    }

    // Map Unicode to Shortcode so client can render them
    private static final Map<String, String> UNICODE_TO_ALIAS = new HashMap<>();
    static {
        UNICODE_TO_ALIAS.put("😄", ":smile:");
        UNICODE_TO_ALIAS.put("😃", ":smiley:");
        UNICODE_TO_ALIAS.put("♥", ":heart:");
        UNICODE_TO_ALIAS.put("💀", ":skull:");
        UNICODE_TO_ALIAS.put("⭐", ":star:");
        UNICODE_TO_ALIAS.put("👀", ":eyes:");
        // Add more common ones as needed
    }

    private ChatBridge() {
    }

    public static synchronized ChatBridge getInstance() {
        if (instance == null) {
            instance = new ChatBridge();
        }
        return instance;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        DiscordConfig config = DiscordConfig.getInstance();
        if (!config.isEnableDiscord() || !config.isEnableChatBridge())
            return;

        String message = event.getMessage().getString();
        String player = event.getPlayer().getName().getString();
        java.util.UUID uuid = event.getPlayer().getUUID();

        // Send to Discord
        DiscordManager.getInstance().sendChatMessage(player, uuid, message);
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        DiscordConfig config = DiscordConfig.getInstance();
        if (!config.isEnableDiscord() || !config.isEnableChatBridge())
            return;

        String command = event.getParseResults().getReader().getString();
        if (command.startsWith("say ")) {
            String msg = command.substring(4).trim();
            if (msg.isEmpty())
                return;

            String sourceName;
            try {
                sourceName = event.getParseResults().getContext().getSource().getTextName();
            } catch (Exception e) {
                sourceName = "Server";
            }

            java.util.UUID uuid = null;
            try {
                if (event.getParseResults().getContext().getSource()
                        .getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                    uuid = player.getUUID();
                }
            } catch (Exception ignored) {
            }

            DiscordManager.getInstance().sendChatMessage(sourceName, uuid, msg);
        }
    }

    public void onDiscordMessage(String user, String message) {
        if (server == null)
            return;

        DiscordConfig config = DiscordConfig.getInstance();
        String format = config.getDiscordToMinecraftFormat();

        // Translate Emojis
        /*
         * // Disabled legacy emoji translation - we want to keep :codes: for the client
         * to render
         * if (config.isTranslateEmojis()) {
         * for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
         * message = message.replace(entry.getKey(), entry.getValue());
         * }
         * }
         */

        // Map Unicode emojis to shortcodes (so client can render them)
        for (Map.Entry<String, String> entry : UNICODE_TO_ALIAS.entrySet()) {
            if (message.contains(entry.getKey())) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }

        // Translate formatting (Basic Markdown to Legacy Colors)
        // **bold** -> &l
        // *italic* -> &o
        // __underline__ -> &n
        // ~~strike~~ -> &m
        message = message.replaceAll("\\*\\*(.+?)\\*\\*", "§l$1§r");
        message = message.replaceAll("\\*(.+?)\\*", "§o$1§r");
        message = message.replaceAll("__(.+?)__", "§n$1§r");
        message = message.replaceAll("~~(.+?)~~", "§m$1§r");

        // Clickable Links (Simple detection)
        // We wrap links in a ClickEvent component if possible, but since we are
        // constructing a string format,
        // we rely on the client's auto-link detection or we need to construct a complex
        // Component.
        // For simplicity in this string-based format, we just color them to make them
        // stand out.
        message = message.replaceAll("(https?://\\S+)", "§9$1§r");

        String finalMessage = format
                .replace("%user%", user)
                .replace("%message%", message)
                .replace("&", "§");

        server.getPlayerList().broadcastSystemMessage(Component.literal(finalMessage), false);
    }

    public void sendTicketMessageToPlayer(String playerName, String discordUser, String message, String channelId) {
        if (server == null) {
            System.out.println("[Voidium-Ticket] Cannot send message - server is null");
            return;
        }

        System.out.println(
                "[Voidium-Ticket] Looking for player '" + playerName + "' to send message from '" + discordUser + "'");

        // Find player by name (case-insensitive)
        net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().stream()
                .filter(p -> p.getName().getString().equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);

        if (player == null) {
            System.out.println("[Voidium-Ticket] Player '" + playerName + "' not found online. Online players: " +
                    server.getPlayerList().getPlayers().stream()
                            .map(p -> p.getName().getString())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("none"));
            return;
        }

        System.out.println("[Voidium-Ticket] Found player '" + player.getName().getString() + "', sending message");

        // Translate Emojis
        if (DiscordConfig.getInstance().isTranslateEmojis()) {
            for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }

        // Translate markdown formatting
        message = message.replaceAll("\\*\\*(.+?)\\*\\*", "§l$1§r");
        message = message.replaceAll("\\*(.+?)\\*", "§o$1§r");
        message = message.replaceAll("__(.+?)__", "§n$1§r");
        message = message.replaceAll("~~(.+?)~~", "§m$1§r");
        message = message.replaceAll("(https?://\\S+)", "§9$1§r");

        // Send packet instead of system message
        try {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new cz.voidium.network.PacketTicketMessage("ticket-" + channelId, discordUser, message));
        } catch (UnsupportedOperationException e) {
            // Fallback for vanilla chat clients
            String fallbackMessage = "§8[§6Ticket§8] §e" + discordUser + "§7: §f" + message;
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(fallbackMessage));
        }
        // Fallback for vanilla chat? Maybe not needed if using modern chat overlay
        // exclusively.
        // If needed, we can keep the system message but client will duplicate it unless
        // filtered.
        // For now, let's assume Modern Chat users want it in the tab only.
        // But to be safe for users without the client mod (if any), we might still want
        // a chat msg?
        // Wait, this is a hybrid mod. If client has mod, it handles packet.
        // Let's send system message ONLY if packet fails? hard to know.
        // Actually, the user asked for it to NOT appear in global.
        // So we remove the system message.
    }

    // Helper to find the channel ID. Wait, sendTicketMessageToPlayer doesn't have
    // the channel ID passed to it.
    // I need to update the method signature of sendTicketMessageToPlayer to accept
    // channelId.
}
