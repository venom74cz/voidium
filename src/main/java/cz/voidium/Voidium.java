package cz.voidium;

import cz.voidium.config.VoidiumConfig;
import cz.voidium.server.RestartManager;
import cz.voidium.server.AnnouncementManager;
import cz.voidium.server.SkinRestorer;
import cz.voidium.skin.SkinCache;
import cz.voidium.commands.VoidiumCommand;
import cz.voidium.vote.VoteManager;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Voidium.MOD_ID)
public class Voidium {
    public static final String MOD_ID = "voidium";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private RestartManager restartManager;
    private AnnouncementManager announcementManager;
    private SkinRestorer skinRestorer;
    private VoteManager voteManager;

    public Voidium() {
        if (FMLEnvironment.dist.isDedicatedServer()) {
            LOGGER.info("VOIDIUM - SERVER MANAGER is loading...");
            
            // Inicializace konfigurace
            VoidiumConfig.init(FMLPaths.CONFIGDIR.get());
            
            // Registrace event listenerů
            NeoForge.EVENT_BUS.addListener(this::onServerStarted);
            NeoForge.EVENT_BUS.addListener(this::onServerStopping);
            NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
            
            LOGGER.info("Voidium configuration loaded successfully!");
            // Init persistent skin cache directory + apply TTL from general config once it exists
            try {
                SkinCache.init(FMLPaths.CONFIGDIR.get());
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

    private void onServerStarted(ServerStartedEvent event) {
        try {
            LOGGER.info("Starting Voidium managers...");
            
            restartManager = new RestartManager(event.getServer());
            announcementManager = new AnnouncementManager(event.getServer());
            voteManager = new VoteManager(event.getServer());
            VoidiumCommand.setVoteManager(voteManager);
            voteManager.start();
            // Start SkinRestorer only if server offline mode and enabled in config
            try {
                LOGGER.info("Offline mode? {}", !event.getServer().usesAuthentication());
                LOGGER.info("SkinRestorer enabled in config? {}", cz.voidium.config.GeneralConfig.getInstance().isEnableSkinRestorer());
                if (!event.getServer().usesAuthentication()) {
                    if (cz.voidium.config.GeneralConfig.getInstance().isEnableSkinRestorer()) {
                        skinRestorer = new SkinRestorer(event.getServer());
                        cz.voidium.commands.VoidiumCommand.setSkinRestorer(skinRestorer);
                        LOGGER.info("SkinRestorer instance created.");
                    } else {
                        LOGGER.info("SkinRestorer disabled by config.");
                    }
                } else {
                    LOGGER.info("Server in online mode -> SkinRestorer skipped.");
                }
            } catch (Exception ignored) {}
            
            // Nastavení managerů pro příkazy
            VoidiumCommand.setManagers(restartManager, announcementManager);
            
            LOGGER.info("Voidium managers started successfully!");
            
            // Oznámení pro OPs
            announcementManager.broadcastToOps("&aVOIDIUM - SERVER MANAGER loaded and running!");
            announcementManager.broadcastToOps("&eVersion: 1.3.1");
            announcementManager.broadcastToOps("&bConfiguration loaded successfully!");
        } catch (Exception e) {
            LOGGER.error("Failed to start Voidium managers: {}", e.getMessage(), e);
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        VoidiumCommand.register(event.getDispatcher());
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
    }
}
