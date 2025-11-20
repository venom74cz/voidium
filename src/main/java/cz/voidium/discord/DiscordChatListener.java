package cz.voidium.discord;

import cz.voidium.config.DiscordConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class DiscordChatListener {

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!DiscordConfig.getInstance().isEnableDiscord() || !DiscordConfig.getInstance().isEnableChatBridge()) return;
        
        String playerName = event.getEntity().getName().getString();
        String message = "**" + playerName + "** joined the game";
        DiscordManager.getInstance().sendChatMessage(null, null, message);
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!DiscordConfig.getInstance().isEnableDiscord() || !DiscordConfig.getInstance().isEnableChatBridge()) return;
        
        String playerName = event.getEntity().getName().getString();
        String message = "**" + playerName + "** left the game";
        DiscordManager.getInstance().sendChatMessage(null, null, message);
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!DiscordConfig.getInstance().isEnableDiscord() || !DiscordConfig.getInstance().isEnableChatBridge()) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            String deathMessage = event.getSource().getLocalizedDeathMessage(player).getString();
            String message = "**" + deathMessage + "**";
            DiscordManager.getInstance().sendChatMessage(null, null, message);
        }
    }
}
