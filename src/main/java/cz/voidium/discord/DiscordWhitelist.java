package cz.voidium.discord;

import cz.voidium.config.DiscordConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class DiscordWhitelist {

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        DiscordConfig config = DiscordConfig.getInstance();
        if (!config.isEnableDiscord() || !config.isEnableWhitelist()) {
            return;
        }

        LinkManager linkManager = LinkManager.getInstance();
        if (!linkManager.isLinked(player.getUUID())) {
            // Not linked -> Generate code and kick
            String code = linkManager.generateCode(player.getUUID());
            
            // Log code to console as backup
            System.out.println("Player " + player.getName().getString() + " (" + player.getUUID() + ") is not linked. Generated code: " + code);
            
            String message = config.getKickMessage()
                    .replace("%code%", code)
                    .replace("&", "§"); // Simple color code replacement
            
            player.connection.disconnect(Component.literal(message));
        } else {
            // Linked -> Check if still in guild (Optional, maybe later)
            // For now, just let them in.
        }
    }
}
