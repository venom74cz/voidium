package cz.voidium.discord;

import cz.voidium.config.DiscordConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class DiscordWhitelist {

    private final Set<UUID> pendingKick = ConcurrentHashMap.newKeySet();

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
            pendingKick.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        if (pendingKick.contains(player.getUUID())) {
            // Check for movement (more robust than just login time)
            boolean moved = player.xo != player.getX() || player.zo != player.getZ();

            // Kick if moved or after 5 seconds roughly (100 ticks) as backup
            if (moved || player.tickCount > 100) {
                pendingKick.remove(player.getUUID());
                kickUnverified(player);
            }
        }
    }

    private void kickUnverified(ServerPlayer player) {
        DiscordConfig config = DiscordConfig.getInstance();
        LinkManager linkManager = LinkManager.getInstance();

        // Regenerate code just in case
        String code = linkManager.generateCode(player.getUUID());

        System.out.println("Kick unverified player: " + player.getName().getString() + " Code: " + code);

        String message = config.getKickMessage()
                .replace("%code%", code)
                .replace("&", "§")
                .replace("\\n", "\n");

        System.out.println("Kick message: " + message);

        player.connection.disconnect(Component.literal(message));
    }
}
