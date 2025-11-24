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
        EMOJI_MAP.put(":smile:", "☺");
        EMOJI_MAP.put(":heart:", "❤");
        EMOJI_MAP.put(":skull:", "☠");
        EMOJI_MAP.put(":star:", "★");
        EMOJI_MAP.put(":check:", "✔");
        EMOJI_MAP.put(":cross:", "✖");
        // Add more as needed
    }

    private ChatBridge() {}

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
        if (!config.isEnableDiscord() || !config.isEnableChatBridge()) return;
        
        String message = event.getMessage().getString();
        String player = event.getPlayer().getName().getString();
        java.util.UUID uuid = event.getPlayer().getUUID();
        
        // Send to Discord
        DiscordManager.getInstance().sendChatMessage(player, uuid, message);
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        DiscordConfig config = DiscordConfig.getInstance();
        if (!config.isEnableDiscord() || !config.isEnableChatBridge()) return;

        String command = event.getParseResults().getReader().getString();
        if (command.startsWith("say ")) {
            String msg = command.substring(4).trim();
            if (msg.isEmpty()) return;

            String sourceName;
            try {
                sourceName = event.getParseResults().getContext().getSource().getTextName();
            } catch (Exception e) {
                sourceName = "Server";
            }
            
            java.util.UUID uuid = null;
            try {
                 if (event.getParseResults().getContext().getSource().getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                     uuid = player.getUUID();
                 }
            } catch (Exception ignored) {}

            DiscordManager.getInstance().sendChatMessage(sourceName, uuid, msg);
        }
    }
    
    public void onDiscordMessage(String user, String message) {
        if (server == null) return;
        
        DiscordConfig config = DiscordConfig.getInstance();
        String format = config.getDiscordToMinecraftFormat();
        
        // Translate Emojis
        if (config.isTranslateEmojis()) {
            for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
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
        // We wrap links in a ClickEvent component if possible, but since we are constructing a string format,
        // we rely on the client's auto-link detection or we need to construct a complex Component.
        // For simplicity in this string-based format, we just color them to make them stand out.
        message = message.replaceAll("(https?://\\S+)", "§9$1§r");
        
        String finalMessage = format
                .replace("%user%", user)
                .replace("%message%", message)
                .replace("&", "§");
        
        server.getPlayerList().broadcastSystemMessage(Component.literal(finalMessage), false);
    }
    
    public void sendTicketMessageToPlayer(String playerName, String discordUser, String message) {
        if (server == null) {
            System.out.println("[Voidium-Ticket] Cannot send message - server is null");
            return;
        }
        
        System.out.println("[Voidium-Ticket] Looking for player '" + playerName + "' to send message from '" + discordUser + "'");
        
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
        
        String finalMessage = "§d[Ticket] §b" + discordUser + "§f: " + message;
        player.sendSystemMessage(Component.literal(finalMessage));
    }
}
