package cz.voidium.discord;

import cz.voidium.config.DiscordConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.CommandEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        
        String finalMessage = format
                .replace("%user%", user)
                .replace("%message%", message)
                .replace("&", "§");
        
        server.getPlayerList().broadcastSystemMessage(Component.literal(finalMessage), false);
    }
}
