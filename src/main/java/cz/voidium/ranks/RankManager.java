package cz.voidium.ranks;

import cz.voidium.config.RanksConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class RankManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Ranks");
    private static RankManager instance;

    private MinecraftServer server;
    private ScheduledExecutorService scheduler;

    private RankManager() {
    }

    public static synchronized RankManager getInstance() {
        if (instance == null) {
            instance = new RankManager();
        }
        return instance;
    }

    public void start(MinecraftServer server) {
        this.server = server;
        RanksConfig config = RanksConfig.getInstance();
        if (!config.isEnableAutoRanks())
            return;

        NeoForge.EVENT_BUS.register(this);

        scheduler = Executors.newScheduledThreadPool(1);
        int interval = Math.max(1, config.getCheckIntervalMinutes());

        scheduler.scheduleAtFixedRate(this::checkRanks, interval, interval, TimeUnit.MINUTES);
        LOGGER.info("Rank Manager started. Checking every {} minutes.", interval);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        NeoForge.EVENT_BUS.unregister(this);
    }

    public void reload() {
        stop();
        start(server);
    }

    @SubscribeEvent
    public void onNameFormat(PlayerEvent.NameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        RanksConfig config = RanksConfig.getInstance();
        if (!config.isEnableAutoRanks())
            return;

        int ticksPlayed = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        double hoursPlayed = ticksPlayed / 20.0 / 3600.0;
        String uuid = player.getUUID().toString();

        List<RanksConfig.RankDefinition> ranks = config.getRanks();

        // Find highest priority prefix and suffix
        RanksConfig.RankDefinition bestPrefix = null;
        RanksConfig.RankDefinition bestSuffix = null;

        for (RanksConfig.RankDefinition rank : ranks) {
            if (hoursPlayed < rank.hours)
                continue;

            // Check custom conditions
            boolean meetsCustomConditions = true;
            if (rank.customConditions != null && !rank.customConditions.isEmpty()) {
                for (RanksConfig.CustomCondition condition : rank.customConditions) {
                    if (!ProgressTracker.getInstance().meetsCondition(uuid, condition.type, condition.count)) {
                        meetsCustomConditions = false;
                        break;
                    }
                }
            }

            if (!meetsCustomConditions)
                continue;

            // Player meets all requirements
            if ("PREFIX".equalsIgnoreCase(rank.type)) {
                if (bestPrefix == null || rank.hours > bestPrefix.hours) {
                    bestPrefix = rank;
                }
            } else if ("SUFFIX".equalsIgnoreCase(rank.type)) {
                if (bestSuffix == null || rank.hours > bestSuffix.hours) {
                    bestSuffix = rank;
                }
            }
        }

        Component currentName = event.getDisplayname();
        Component finalName = currentName;

        if (bestPrefix != null) {
            finalName = Component.literal(formatColors(bestPrefix.value)).append(finalName);
        }
        if (bestSuffix != null) {
            finalName = finalName.copy().append(Component.literal(formatColors(bestSuffix.value)));
        }

        event.setDisplayname(finalName);
    }

    private void checkRanks() {
        if (server == null)
            return;
        RanksConfig config = RanksConfig.getInstance();
        List<RanksConfig.RankDefinition> ranks = config.getRanks();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int ticksPlayed = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            double hoursPlayed = ticksPlayed / 20.0 / 3600.0;
            String uuid = player.getUUID().toString();

            RanksConfig.RankDefinition highestRank = null;

            for (RanksConfig.RankDefinition rank : ranks) {
                if (hoursPlayed < rank.hours)
                    continue;

                boolean meetsCustomConditions = true;
                if (rank.customConditions != null && !rank.customConditions.isEmpty()) {
                    for (RanksConfig.CustomCondition condition : rank.customConditions) {
                        if (!ProgressTracker.getInstance().meetsCondition(uuid, condition.type, condition.count)) {
                            meetsCustomConditions = false;
                            break;
                        }
                    }
                }

                if (!meetsCustomConditions)
                    continue;

                if (highestRank == null || rank.hours > highestRank.hours) {
                    highestRank = rank;
                }
            }

            if (highestRank != null) {
                String rankId = highestRank.type + ":" + highestRank.hours;

                if (!RankStorage.getInstance().hasRank(player.getUUID(), rankId)) {
                    LOGGER.info("Player {} reached {} hours and met all custom conditions, awarding {} '{}'",
                            player.getName().getString(), highestRank.hours, highestRank.type, highestRank.value);

                    RankStorage.getInstance().setHighestRank(player.getUUID(), rankId);

                    String formattedRank = formatColors(highestRank.value);
                    String msg = config.getPromotionMessage()
                            .replace("%rank%", formattedRank)
                            .replace("{rank}", formattedRank)
                            .replace("{player}", player.getName().getString())
                            .replace("{hours}", String.valueOf(highestRank.hours));
                    player.sendSystemMessage(Component.literal(formatColors(msg)));
                }
            }
        }
    }

    private String formatColors(String text) {
        if (text == null)
            return "";
        // Replace & + [0-9a-fk-or] with § + code, ignoring case
        // This preserves &#RRGGBB and <#RRGGBB> formats
        return text.replaceAll("(?i)&([0-9a-fk-or])", "§$1");
    }
}
