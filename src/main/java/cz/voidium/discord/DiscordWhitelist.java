package cz.voidium.discord;

import cz.voidium.config.DiscordConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discord Whitelist - Freeze-based verification system.
 * 
 * Instead of kicking unlinked players, this system:
 * 1. Allows players to connect
 * 2. Freezes them in place (can't move)
 * 3. Shows verification code in chat
 * 4. Automatically unfreezes when account is linked
 */
public class DiscordWhitelist {

    private static final Logger LOGGER = LoggerFactory.getLogger("voidium-whitelist");

    // Players waiting for verification (UUID -> their spawn position & code)
    private final Map<UUID, FrozenPlayer> frozenPlayers = new ConcurrentHashMap<>();

    // Track last message time to avoid spam
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();

    // Message cooldown in milliseconds
    private static final long MESSAGE_COOLDOWN_MS = 10000; // 10 seconds

    private record FrozenPlayer(double x, double y, double z, float yRot, float xRot, String code) {
    }

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
            // Generate verification code
            String code = linkManager.generateCode(player.getUUID());

            // Store player's position for freezing
            frozenPlayers.put(player.getUUID(), new FrozenPlayer(
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), code));

            LOGGER.info("Player {} needs verification. Code: {}", player.getName().getString(), code);

            // Send verification message in chat (delayed slightly to ensure player is fully
            // connected)
            player.server.execute(() -> {
                sendVerificationMessage(player, code);
            });
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();
        FrozenPlayer frozen = frozenPlayers.get(playerId);

        if (frozen == null) {
            return; // Not frozen
        }

        // Check if player has been linked
        LinkManager linkManager = LinkManager.getInstance();
        if (linkManager.isLinked(playerId)) {
            // Player is now linked! Unfreeze them
            frozenPlayers.remove(playerId);
            lastMessageTime.remove(playerId);

            LOGGER.info("Player {} successfully linked, unfreezing", player.getName().getString());

            DiscordConfig config = DiscordConfig.getInstance();
            String successMessage = config.getLinkSuccessMessage()
                    .replace("%player%", player.getName().getString())
                    .replace("&", "§");

            player.sendSystemMessage(Component.literal("§a✓ " + successMessage));
            return;
        }

        // Still frozen - teleport back to frozen position if moved
        double dx = player.getX() - frozen.x;
        double dy = player.getY() - frozen.y;
        double dz = player.getZ() - frozen.z;

        if (dx * dx + dy * dy + dz * dz > 0.01) { // Moved more than 0.1 blocks
            player.teleportTo(frozen.x, frozen.y, frozen.z);
            player.setYRot(frozen.yRot);
            player.setXRot(frozen.xRot);
        }

        // Periodically remind the player (every MESSAGE_COOLDOWN_MS)
        long now = System.currentTimeMillis();
        long lastTime = lastMessageTime.getOrDefault(playerId, 0L);

        if (now - lastTime >= MESSAGE_COOLDOWN_MS) {
            lastMessageTime.put(playerId, now);
            sendVerificationMessage(player, frozen.code);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Clean up when player leaves
        frozenPlayers.remove(player.getUUID());
        lastMessageTime.remove(player.getUUID());
    }

    private void sendVerificationMessage(ServerPlayer player, String code) {
        DiscordConfig config = DiscordConfig.getInstance();

        // Use kickMessage as verification message (same format, just shown in chat)
        String message = config.getKickMessage()
                .replace("%code%", code)
                .replace("&", "§")
                .replace("\\n", "\n");

        // Split message by newlines and send each line
        String[] lines = message.split("\n");
        for (String line : lines) {
            player.sendSystemMessage(Component.literal(line));
        }

        // Additional instruction from config
        String hint = config.getVerificationHintMessage()
                .replace("%code%", code)
                .replace("&", "§");
        player.sendSystemMessage(Component.literal(hint));
    }

    /**
     * Check if a player is currently frozen (waiting for verification)
     */
    public boolean isFrozen(UUID playerId) {
        return frozenPlayers.containsKey(playerId);
    }

    /**
     * Manually unfreeze a player (e.g., admin command)
     */
    public void unfreeze(UUID playerId) {
        frozenPlayers.remove(playerId);
        lastMessageTime.remove(playerId);
    }

    /**
     * Get all frozen players
     */
    public Map<UUID, String> getFrozenPlayersWithCodes() {
        Map<UUID, String> result = new ConcurrentHashMap<>();
        for (Map.Entry<UUID, FrozenPlayer> entry : frozenPlayers.entrySet()) {
            result.put(entry.getKey(), entry.getValue().code);
        }
        return result;
    }
}
