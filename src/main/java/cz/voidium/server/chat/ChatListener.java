package cz.voidium.server.chat;

import cz.voidium.network.PacketSyncChatHistory;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class ChatListener {

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        String content = event.getMessage().getString();
        String sender = event.getPlayer().getName().getString();
        // Simple format for history: "Sender: Message"
        // In a real implementation, we might want to store more metadata or the full
        // Component
        ChatHistoryManager.getInstance().addGlobalMessage(sender + ": " + content);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Sync chat history
            List<String> history = ChatHistoryManager.getInstance().getGlobalHistory();
            if (!history.isEmpty()) {
                try {
                    PacketDistributor.sendToPlayer(player, new PacketSyncChatHistory(history));
                } catch (UnsupportedOperationException e) {
                    // Client doesn't have Voidium mod installed, ignore
                }
            }

            // Sync Discord emojis
            EmojiSyncService.getInstance().syncToPlayer(player);
        }
    }
}
