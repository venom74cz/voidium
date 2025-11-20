package cz.voidium.ranks;

import cz.voidium.config.RanksConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RankManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Ranks");
    private static RankManager instance;
    
    private MinecraftServer server;
    private ScheduledExecutorService scheduler;

    private RankManager() {}

    public static synchronized RankManager getInstance() {
        if (instance == null) {
            instance = new RankManager();
        }
        return instance;
    }

    public void start(MinecraftServer server) {
        this.server = server;
        RanksConfig config = RanksConfig.getInstance();
        if (!config.isEnableAutoRanks()) return;

        scheduler = Executors.newScheduledThreadPool(1);
        int interval = Math.max(1, config.getCheckIntervalMinutes());
        
        scheduler.scheduleAtFixedRate(this::checkRanks, interval, interval, TimeUnit.MINUTES);
        LOGGER.info("Rank Manager started. Checking every {} minutes.", interval);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    public void reload() {
        stop();
        start(server);
    }

    private void checkRanks() {
        if (server == null) return;
        RanksConfig config = RanksConfig.getInstance();
        Map<String, Integer> ranks = config.getPlaytimeRanks();
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Get playtime in hours
            // STAT_PLAY_ONE_MINUTE is actually in ticks (1/20 sec)
            int ticksPlayed = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            double hoursPlayed = ticksPlayed / 20.0 / 3600.0;
            
            for (Map.Entry<String, Integer> entry : ranks.entrySet()) {
                String rankName = entry.getKey();
                int requiredHours = entry.getValue();
                
                if (hoursPlayed >= requiredHours) {
                    // Check if player already has rank? 
                    // FTB Ranks doesn't have easy API check without dependency.
                    // We can just try to add it. FTB Ranks usually handles duplicates gracefully.
                    // Or we can track given ranks in a file to avoid spamming commands.
                    // For now, let's assume we run the command. 
                    // To avoid spam, we should probably store "highest rank given" in player data or local file.
                    
                    // TODO: Implement a check to see if we already gave this rank.
                    // For simplicity in this version, we will execute command.
                    // BUT executing it every 5 mins is bad.
                    // We need to store state.
                    
                    promoteIfNew(player, rankName);
                }
            }
        }
    }
    
    private void promoteIfNew(ServerPlayer player, String rankName) {
        // We need a way to know if player already has this rank from US.
        // We can use PersistentDataContainer or a simple JSON file.
        // Let's use a simple in-memory cache backed by JSON for now, similar to LinkManager.
        
        if (!RankStorage.getInstance().hasRank(player.getUUID(), rankName)) {
            LOGGER.info("Promoting {} to rank {} (Playtime reached)", player.getName().getString(), rankName);
            
            // Execute command
            String cmd = RanksConfig.getInstance().getPromotionCommand()
                    .replace("%player%", player.getName().getString())
                    .replace("%rank%", rankName);
            
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
            
            // Send message
            String msg = RanksConfig.getInstance().getPromotionMessage()
                    .replace("%player%", player.getName().getString())
                    .replace("%rank%", rankName)
                    .replace("&", "§");
            
            player.sendSystemMessage(Component.literal(msg));
            
            // Mark as given
            RankStorage.getInstance().addRank(player.getUUID(), rankName);
        }
    }
}
