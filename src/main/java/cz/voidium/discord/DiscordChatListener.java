package cz.voidium.discord;

import cz.voidium.config.DiscordConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class DiscordChatListener {

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!DiscordConfig.getInstance().isEnableDiscord() || !DiscordConfig.getInstance().isEnableChatBridge()) return;
        
        String playerName = event.getEntity().getName().getString();
        java.util.UUID uuid = event.getEntity().getUUID();
        DiscordManager.getInstance().sendEventEmbed(playerName, uuid, playerName + " joined the game", 0x55FF55); // green
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!DiscordConfig.getInstance().isEnableDiscord() || !DiscordConfig.getInstance().isEnableChatBridge()) return;
        
        String playerName = event.getEntity().getName().getString();
        java.util.UUID uuid = event.getEntity().getUUID();
        DiscordManager.getInstance().sendEventEmbed(playerName, uuid, playerName + " left the game", 0xFF5555); // red
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!DiscordConfig.getInstance().isEnableDiscord() || !DiscordConfig.getInstance().isEnableChatBridge()) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            String deathMessage = event.getSource().getLocalizedDeathMessage(player).getString();
            java.util.UUID uuid = player.getUUID();
            DiscordManager.getInstance().sendEventEmbed(player.getName().getString(), uuid, deathMessage, 0x555555); // gray
        }
    }
}
