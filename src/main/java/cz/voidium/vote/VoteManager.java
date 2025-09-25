package cz.voidium.vote;

import cz.voidium.config.VoteConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.security.PrivateKey;

public class VoteManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("voidium-votes");

    private final MinecraftServer server;
    private VoteListener listener;

    public VoteManager(MinecraftServer server) {
        this.server = server;
    }

    public void start() {
        shutdown();
        VoteConfig config = VoteConfig.getInstance();
        if (config == null || !config.isEnabled()) {
            LOGGER.info("Vote system disabled in configuration");
            return;
        }
        try {
            Path privateKeyPath = config.getResolvedPrivateKeyPath();
            Path publicKeyPath = config.getResolvedPublicKeyPath();
            PrivateKey privateKey = VoteKeyUtil.loadOrCreatePrivateKey(privateKeyPath, publicKeyPath);
            LOGGER.info("Vote RSA private key loaded from {}", privateKeyPath.toAbsolutePath());
            LOGGER.info("Public key ready at {}", publicKeyPath.toAbsolutePath());

            String rawSecret = config.getSharedSecret();
            String trimmedSecret = rawSecret == null ? "" : rawSecret.trim();
            if (!trimmedSecret.isEmpty()) {
                LOGGER.info("Vote listener will validate NuVotifier v2 tokens alongside legacy RSA payloads");
            } else {
                LOGGER.info("Vote listener running in legacy RSA-only mode (shared secret empty)");
            }

            listener = new VoteListener(server, config, privateKey, trimmedSecret, LOGGER);
            listener.start();
            LOGGER.info("Vote listener started");
        } catch (Exception e) {
            LOGGER.error("Failed to start vote listener", e);
            notifyOps("§cVote listener failed to start: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (listener != null) {
            try {
                listener.close();
                LOGGER.info("Vote listener stopped");
            } catch (Exception e) {
                LOGGER.error("Error while stopping vote listener", e);
            }
            listener = null;
        }
    }

    public void reload() {
        start();
    }

    private void notifyOps(String message) {
        VoteConfig config = VoteConfig.getInstance();
        if (config == null || !config.getLogging().isNotifyOpsOnError()) {
            return;
        }
        server.execute(() -> server.getPlayerList().getPlayers().forEach(player -> {
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                player.sendSystemMessage(Component.literal(message));
            }
        }));
    }
}
