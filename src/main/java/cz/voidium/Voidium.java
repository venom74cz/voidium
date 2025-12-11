package cz.voidium;

import java.nio.file.Path;
import java.nio.file.Files;
import cz.voidium.config.VoidiumConfig;
import cz.voidium.server.RestartManager;
import cz.voidium.server.AnnouncementManager;
import cz.voidium.server.SkinRestorer;
import cz.voidium.skin.SkinCache;
import cz.voidium.commands.VoidiumCommand;
import cz.voidium.vote.VoteManager;
import cz.voidium.entitycleaner.EntityCleaner;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Voidium.MOD_ID)
public class Voidium {
    public static final String MOD_ID = "voidium";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static Voidium instance;

    private RestartManager restartManager;
    private AnnouncementManager announcementManager;
    private SkinRestorer skinRestorer;
    private VoteManager voteManager;
    private EntityCleaner entityCleaner;

    public Voidium() {
        instance = this;
        if (FMLEnvironment.dist.isDedicatedServer()) {
            LOGGER.info("VOIDIUM - INTELLIGENT SERVER CONTROL is loading...");

            Path configDir = FMLPaths.CONFIGDIR.get();
            Path voidiumDir = configDir.resolve("voidium");
            try {
                Files.createDirectories(voidiumDir);
            } catch (Exception e) {
                LOGGER.error("Failed to create voidium config directory", e);
            }

            // Initialize storage directory and migrate old files
            cz.voidium.config.StorageHelper.init(voidiumDir);
            cz.voidium.config.StorageHelper.migrateAll(voidiumDir,
                    "links.json",
                    "pending-votes.json",
                    "votes.log",
                    "votes-history.ndjson",
                    "voidium_stats_data.json",
                    "voidium_ranks_data.json",
                    "player_progress.json",
                    "skin-cache.json",
                    "last_restart.txt");

            // Inicializace konfigurace
            VoidiumConfig.init(voidiumDir);
            cz.voidium.config.DiscordConfig.init(voidiumDir);
            cz.voidium.config.WebConfig.init(voidiumDir);
            cz.voidium.config.StatsConfig.init(voidiumDir);
            cz.voidium.config.RanksConfig.init(voidiumDir);
            cz.voidium.config.TicketConfig.init(voidiumDir);
            cz.voidium.config.PlayerListConfig.init(voidiumDir);
            cz.voidium.config.EntityCleanerConfig.init(voidiumDir);

            // Registrace event listenerů
            NeoForge.EVENT_BUS.addListener(this::onServerStarting);
            NeoForge.EVENT_BUS.addListener(this::onServerStarted);
            NeoForge.EVENT_BUS.addListener(this::onServerStopping);
            NeoForge.EVENT_BUS.addListener(this::onServerStopped);
            NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
            NeoForge.EVENT_BUS.register(new cz.voidium.discord.DiscordWhitelist());
            NeoForge.EVENT_BUS.register(new cz.voidium.ranks.ProgressEventListener());

            LOGGER.info("Voidium configuration loaded successfully!");
            // Init persistent skin cache directory + apply TTL from general config once it
            // exists
            try {
                SkinCache.init(voidiumDir);
                var gc = cz.voidium.config.GeneralConfig.getInstance();
                if (gc != null) {
                    int hours = Math.max(1, gc.getSkinCacheHours());
                    SkinCache.setMaxAgeSeconds(hours * 3600L);
                    LOGGER.info("SkinCache TTL set to {} hours", hours);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to init SkinCache", e);
            }
        }
    }

    public static Voidium getInstance() {
        return instance;
    }

    public RestartManager getRestartManager() {
        return restartManager;
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    private void onServerStarting(ServerStartingEvent event) {
        cz.voidium.config.GeneralConfig gc = cz.voidium.config.GeneralConfig.getInstance();
        // Start Discord Manager early to send "Starting" message
        if (gc.isEnableDiscord()) {
            cz.voidium.discord.DiscordManager.getInstance().setServer(event.getServer());
            cz.voidium.discord.DiscordManager.getInstance().start();
            cz.voidium.discord.DiscordManager.getInstance()
                    .sendStatusMessage(cz.voidium.config.DiscordConfig.getInstance().getStatusMessageStarting());
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        try {
            LOGGER.info("Starting Voidium managers...");
            cz.voidium.config.GeneralConfig gc = cz.voidium.config.GeneralConfig.getInstance();

            if (gc.isEnableRestarts()) {
                restartManager = new RestartManager(event.getServer());
            }

            if (gc.isEnableAnnouncements()) {
                announcementManager = new AnnouncementManager(event.getServer());
            }

            if (gc.isEnableVote()) {
                voteManager = new VoteManager(event.getServer());
                VoidiumCommand.setVoteManager(voteManager);
                voteManager.start();
            }

            // Discord Manager is already started in onServerStarting
            if (gc.isEnableDiscord()) {
                cz.voidium.discord.DiscordManager.getInstance()
                        .sendStatusMessage(cz.voidium.config.DiscordConfig.getInstance().getStatusMessageStarted());
            }

            // Start Web Manager
            if (gc.isEnableWeb()) {
                cz.voidium.web.WebManager.getInstance().setServer(event.getServer());
                cz.voidium.web.WebManager.getInstance().start();
            }

            // Start Stats Manager
            if (gc.isEnableStats()) {
                cz.voidium.stats.StatsManager.getInstance().start(event.getServer());
            }

            // Start Rank Manager
            if (gc.isEnableRanks()) {
                cz.voidium.ranks.RankManager.getInstance().start(event.getServer());
            }

            // Start Player List Manager
            if (gc.isEnablePlayerList()) {
                cz.voidium.playerlist.PlayerListManager.getInstance().start(event.getServer());
                NeoForge.EVENT_BUS
                        .addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
                            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                                cz.voidium.playerlist.PlayerListManager.getInstance().onPlayerJoin(player);
                            }
                        });
                NeoForge.EVENT_BUS.addListener(
                        (net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> {
                            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                                cz.voidium.playerlist.PlayerListManager.getInstance().onPlayerLeave(player);
                            }
                        });
            }

            // Start SkinRestorer only if server offline mode and enabled in config
            try {
                LOGGER.info("Offline mode? {}", !event.getServer().usesAuthentication());
                LOGGER.info("SkinRestorer enabled in config? {}", gc.isEnableSkinRestorer());
                if (!event.getServer().usesAuthentication()) {
                    if (gc.isEnableSkinRestorer()) {
                        skinRestorer = new SkinRestorer(event.getServer());
                        cz.voidium.commands.VoidiumCommand.setSkinRestorer(skinRestorer);
                        LOGGER.info("SkinRestorer instance created.");
                    } else {
                        LOGGER.info("SkinRestorer disabled by config.");
                    }
                } else {
                    LOGGER.info("Server in online mode -> SkinRestorer skipped.");
                }
            } catch (Exception ignored) {
            }

            // Start Chat Bridge
            if (gc.isEnableDiscord()) {
                cz.voidium.discord.ChatBridge.getInstance().setServer(event.getServer());
                NeoForge.EVENT_BUS.register(cz.voidium.discord.ChatBridge.getInstance());
                NeoForge.EVENT_BUS.register(new cz.voidium.discord.DiscordChatListener());
            }

            // Start Entity Cleaner
            if (cz.voidium.config.EntityCleanerConfig.getInstance().isEnabled()) {
                entityCleaner = new EntityCleaner(event.getServer());
                entityCleaner.start();
                VoidiumCommand.setEntityCleaner(entityCleaner);
            }

            // Nastavení managerů pro příkazy
            VoidiumCommand.setManagers(restartManager, announcementManager);

            LOGGER.info("Voidium managers started successfully!");

            // Oznámení pro OPs
            if (announcementManager != null) {
                announcementManager.broadcastToOps("&aVOIDIUM - INTELLIGENT SERVER CONTROL loaded and running!");
                announcementManager.broadcastToOps("&eVersion: 2.1.6");
                announcementManager.broadcastToOps("&bConfiguration loaded successfully!");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to start Voidium managers: {}", e.getMessage(), e);
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        VoidiumCommand.register(event.getDispatcher());
        cz.voidium.commands.TicketCommand.register(event.getDispatcher());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        if (restartManager != null) {
            restartManager.shutdown();
        }
        if (announcementManager != null) {
            announcementManager.shutdown();
        }
        if (skinRestorer != null) {
            skinRestorer.shutdown();
        }
        if (voteManager != null) {
            voteManager.shutdown();
        }
        if (entityCleaner != null) {
            entityCleaner.stop();
        }

        // Stop PlayerListManager
        cz.voidium.playerlist.PlayerListManager.getInstance().stop();

        // Stop Stats and Ranks Managers
        cz.voidium.stats.StatsManager.getInstance().stop();
        cz.voidium.ranks.RankManager.getInstance().stop();

        // Save progress tracker
        cz.voidium.ranks.ProgressTracker.getInstance().save();

        cz.voidium.discord.DiscordManager.getInstance()
                .sendStatusMessage(cz.voidium.config.DiscordConfig.getInstance().getStatusMessageStopping());

        cz.voidium.web.WebManager.getInstance().stop();
        cz.voidium.stats.StatsManager.getInstance().stop();
        cz.voidium.ranks.RankManager.getInstance().stop();
    }

    private void onServerStopped(ServerStoppedEvent event) {
        cz.voidium.discord.DiscordManager.getInstance()
                .sendStatusMessage(cz.voidium.config.DiscordConfig.getInstance().getStatusMessageStopped());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
        cz.voidium.discord.DiscordManager.getInstance().stop();
    }
}
