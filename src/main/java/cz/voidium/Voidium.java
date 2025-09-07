package cz.voidium;

import cz.voidium.config.VoidiumConfig;
import cz.voidium.server.RestartManager;
import cz.voidium.server.AnnouncementManager;
import cz.voidium.commands.VoidiumCommand;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModLoadingContext;
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

    public Voidium() {
        if (FMLEnvironment.dist.isDedicatedServer()) {
            LOGGER.info("Voidium Server-Side Mod is loading...");
            
            // Inicializace konfigurace
            VoidiumConfig.init(FMLPaths.CONFIGDIR.get());
            
            // Registrace event listenerů
            NeoForge.EVENT_BUS.addListener(this::onServerStarted);
            NeoForge.EVENT_BUS.addListener(this::onServerStopping);
            NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
            
            LOGGER.info("Voidium configuration loaded successfully!");
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        try {
            System.out.println("Starting Voidium managers...");
            
            restartManager = new RestartManager(event.getServer());
            announcementManager = new AnnouncementManager(event.getServer());
            
            // Nastavení managerů pro příkazy
            VoidiumCommand.setManagers(restartManager, announcementManager);
            
            System.out.println("Voidium managers started successfully!");
            
            // Oznámení pro OPs
            announcementManager.broadcastToOps("&aVoidium mod loaded and running!");
            announcementManager.broadcastToOps("&eVersion: 1.2.5");
            announcementManager.broadcastToOps("&bConfiguration loaded successfully!");
        } catch (Exception e) {
            System.err.println("Failed to start Voidium managers: " + e.getMessage());
            e.printStackTrace();
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
    }
}
