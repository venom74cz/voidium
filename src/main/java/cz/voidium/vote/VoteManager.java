package cz.voidium.vote;

import cz.voidium.config.VoteConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.List;

public class VoteManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("voidium-votes");

    private final MinecraftServer server;
    private VoteListener listener;
    private PendingVoteQueue pendingQueue;

    public VoteManager(MinecraftServer server) {
        this.server = server;
        // Register player login listener for pending vote delivery
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
    }

    public void start() {
        shutdown();
        VoteConfig config = VoteConfig.getInstance();
        if (config == null || !config.isEnabled()) {
            LOGGER.info("Vote system disabled in configuration");
            return;
        }
        try {
            // Initialize pending vote queue
            Path queueFile = config.getResolvedPendingQueueFile();
            pendingQueue = new PendingVoteQueue(queueFile);
            LOGGER.info("Pending vote queue initialized: {} vote(s) waiting", pendingQueue.getTotalPending());

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

            listener = new VoteListener(server, config, privateKey, trimmedSecret, pendingQueue, LOGGER);
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
            } catch (Exception e) {
                LOGGER.error("Failed to close vote listener", e);
            }
            listener = null;
        }
    }

    public void reload() {
        shutdown();
        start();
    }

    /**
     * Handle player login - deliver pending votes
     */
    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        deliverPendingVotesToPlayer(player);
    }

    /**
     * Get pending queue for admin commands
     */
    public PendingVoteQueue getPendingQueue() {
        return pendingQueue;
    }

    /**
     * Deliver pending votes for ALL currently online players.
     * Returns total number of votes delivered.
     */
    public int deliverAllOnlinePending() {
        if (pendingQueue == null) return 0;
        int total = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int delivered = deliverPendingVotesToPlayer(player);
            if (delivered > 0) total += delivered;
        }
        return total;
    }

    public int deliverPendingVotesForPlayer(String username) {
        if (pendingQueue == null) {
            return 0;
        }
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        if (player == null) {
            return -1;
        }
        return deliverPendingVotesToPlayer(player);
    }

    public int clearPendingVotesForPlayer(String username) {
        return pendingQueue == null ? 0 : pendingQueue.clearForPlayer(username);
    }

    private int deliverPendingVotesToPlayer(ServerPlayer player) {
        if (pendingQueue == null) {
            return 0;
        }
        String username = player.getGameProfile().getName();
        if (!pendingQueue.hasPendingVotes(username)) {
            return 0;
        }

        int count = pendingQueue.getPendingCount(username);
        LOGGER.info("Player {} has {} pending vote(s) ready for delivery", username, count);
        server.execute(() -> processPendingVotesForPlayer(player, username));
        return count;
    }

    private void processPendingVotesForPlayer(ServerPlayer player, String username) {
        List<PendingVoteQueue.PendingVote> pendingVotes = pendingQueue.dequeueForPlayer(username);
        if (pendingVotes.isEmpty()) {
            return;
        }

        VoteConfig config = VoteConfig.getInstance();
        if (config == null) {
            return;
        }

        List<String> commands = config.getCommands();
        if (commands == null || commands.isEmpty()) {
            return;
        }

        LOGGER.info("Delivering {} pending vote reward(s) to {}", pendingVotes.size(), username);
        int rewardCount = 0;
        for (PendingVoteQueue.PendingVote vote : pendingVotes) {
            rewardCount++;
            LOGGER.debug("Processing pending vote #{} from {} at {}", rewardCount, vote.getUsername(), vote.getTimestamp());
            for (String commandTemplate : commands) {
                String command = commandTemplate.replace("%PLAYER%", username);
                String cmdLower = command.toLowerCase().trim();
                if (cmdLower.startsWith("broadcast ") || cmdLower.startsWith("say ")) {
                    continue;
                }
                try {
                    var source = server.createCommandSourceStack();
                    server.getCommands().performPrefixedCommand(source, command);
                } catch (Exception e) {
                    LOGGER.error("Failed to execute pending vote reward command", e);
                }
            }
        }

        String message = config.getLogging().getPendingVoteMessage()
                .replace("%COUNT%", String.valueOf(pendingVotes.size()))
                .replace("&", "§");
        player.sendSystemMessage(Component.literal(message));
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
