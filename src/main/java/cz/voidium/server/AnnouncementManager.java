package cz.voidium.server;

import cz.voidium.config.VoidiumConfig;
import cz.voidium.config.AnnouncementConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AnnouncementManager {
    private final MinecraftServer server;
    private final ScheduledExecutorService scheduler;
    private final AtomicInteger currentAnnouncementIndex;

    public AnnouncementManager(MinecraftServer server) {
        this.server = server;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.currentAnnouncementIndex = new AtomicInteger(0);
        scheduleAnnouncements();
    }

    private void scheduleAnnouncements() {
        AnnouncementConfig config = AnnouncementConfig.getInstance();
        int intervalMinutes = config.getAnnouncementIntervalMinutes();

        scheduler.scheduleAtFixedRate(
            this::broadcastNextAnnouncement,
            intervalMinutes,
            intervalMinutes,
            TimeUnit.MINUTES
        );
    }

    private void broadcastNextAnnouncement() {
        AnnouncementConfig config = AnnouncementConfig.getInstance();
        if (config.getAnnouncements().isEmpty()) return;

        int index = currentAnnouncementIndex.getAndUpdate(i -> 
            (i + 1) % config.getAnnouncements().size()
        );

        String announcement = config.getAnnouncements().get(index);
        String fullMessage = config.getPrefix() + announcement;

        server.getPlayerList().getPlayers().forEach(player ->
            player.sendSystemMessage(VoidiumConfig.formatMessage(fullMessage))
        );
    }

    public void broadcastToOps(String message) {
        AnnouncementConfig config = AnnouncementConfig.getInstance();
        String fullMessage = config.getPrefix() + message;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                player.sendSystemMessage(VoidiumConfig.formatMessage(fullMessage));
            }
        }
    }

    public void broadcastMessage(String message) {
        AnnouncementConfig config = AnnouncementConfig.getInstance();
        String fullMessage = config.getPrefix() + message;
        
        server.getPlayerList().getPlayers().forEach(player ->
            player.sendSystemMessage(VoidiumConfig.formatMessage(fullMessage))
        );
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
