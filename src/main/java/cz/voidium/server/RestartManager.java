package cz.voidium.server;

import cz.voidium.config.VoidiumConfig;
import cz.voidium.config.RestartConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RestartManager {
    private final MinecraftServer server;
    private final ScheduledExecutorService scheduler;
    private long lastRestartTime;
    private ScheduledFuture<?> manualRestartTask;
    private ServerBossEvent restartBossBar;

    public RestartManager(MinecraftServer server) {
        this.server = server;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.lastRestartTime = System.currentTimeMillis();
        scheduleNextRestart();
    }

    private void scheduleNextRestart() {
        RestartConfig config = RestartConfig.getInstance();
        
        if (config.getRestartType() == RestartConfig.RestartType.FIXED_TIME) {
            scheduleFixedTimeRestart();
        } else {
            scheduleIntervalRestart();
        }
    }

    private void scheduleFixedTimeRestart() {
        LocalTime now = LocalTime.now();
        LocalTime nextRestart = RestartConfig.getInstance().getFixedRestartTimes().stream()
                .filter(time -> time.isAfter(now))
                .findFirst()
                .orElse(RestartConfig.getInstance().getFixedRestartTimes().get(0));

        long delayMinutes = now.until(nextRestart, ChronoUnit.MINUTES);
        if (delayMinutes <= 0) {
            delayMinutes += 24 * 60; // Přidej 24 hodin
        }

        scheduleRestart(delayMinutes);
    }

    private void scheduleIntervalRestart() {
        int intervalHours = RestartConfig.getInstance().getIntervalHours();
        long nextRestartIn = (lastRestartTime + (intervalHours * 60 * 60 * 1000) - System.currentTimeMillis()) / (60 * 1000);
        scheduleRestart(nextRestartIn);
    }

    private void scheduleRestart(long delayMinutes) {
        // Plánování oznámení před restartem
        long[] warningTimes = {60, 30, 15, 10, 5, 3, 2, 1}; // minuty
        
        for (long warningTime : warningTimes) {
            if (delayMinutes > warningTime) {
                scheduler.schedule(
                    () -> broadcastRestartWarning(warningTime),
                    delayMinutes - warningTime,
                    TimeUnit.MINUTES
                );
            }
        }

        // Plánování samotného restartu
        scheduler.schedule(this::performRestart, delayMinutes, TimeUnit.MINUTES);
    }

    private void broadcastRestartWarning(long minutesLeft) {
        String message = String.format("&cServer restart in %d minutes!", minutesLeft);
        server.getPlayerList().getPlayers().forEach(player -> 
            player.sendSystemMessage(VoidiumConfig.getInstance().formatMessage(message))
        );
    }

    private void performRestart() {
        // Odstranění boss baru
        if (restartBossBar != null) {
            restartBossBar.removeAllPlayers();
            restartBossBar = null;
        }
        
        // Kick hráčů s důvodem RESTART
        server.getPlayerList().getPlayers().forEach(player -> 
            player.connection.disconnect(Component.literal("§cRESTART"))
        );
        
        // Naplánování skutečného restartu
        scheduler.schedule(() -> {
            server.halt(false);
            System.exit(0);
        }, 5, TimeUnit.SECONDS);
        
        lastRestartTime = System.currentTimeMillis();
    }

    public void scheduleManualRestart(int minutes) {
        manualRestartTask = scheduler.schedule(() -> {
            if (minutes >= 10) {
                startRestartBossBar(minutes);
            }
            scheduleRestart(minutes);
        }, 0, TimeUnit.SECONDS);
    }
    
    public void cancelManualRestart() {
        if (manualRestartTask != null && !manualRestartTask.isDone()) {
            manualRestartTask.cancel(false);
        }
        if (restartBossBar != null) {
            restartBossBar.removeAllPlayers();
            restartBossBar = null;
        }
    }
    
    private void startRestartBossBar(int totalMinutes) {
        if (totalMinutes < 10) return;
        
        restartBossBar = new ServerBossEvent(
            Component.literal("§cServer restart in " + totalMinutes + " minutes"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS
        );
        
        server.getPlayerList().getPlayers().forEach(restartBossBar::addPlayer);
        
        // Aktualizace každých 30 sekund
        scheduler.scheduleAtFixedRate(() -> {
            if (restartBossBar == null) return;
            
            long timeLeft = (totalMinutes * 60 * 1000) - (System.currentTimeMillis() - (System.currentTimeMillis() - totalMinutes * 60 * 1000));
            int minutesLeft = (int) (timeLeft / 60000);
            
            if (minutesLeft <= 0) {
                restartBossBar.removeAllPlayers();
                restartBossBar = null;
                return;
            }
            
            restartBossBar.setName(Component.literal("§cServer restart in " + minutesLeft + " minutes"));
            restartBossBar.setProgress((float) minutesLeft / totalMinutes);
            
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    public String getNextRestartInfo() {
        RestartConfig config = RestartConfig.getInstance();
        if (config.getRestartType() == RestartConfig.RestartType.FIXED_TIME) {
            java.time.LocalTime now = java.time.LocalTime.now();
            java.time.LocalTime next = config.getFixedRestartTimes().stream()
                .filter(time -> time.isAfter(now))
                .findFirst()
                .orElse(config.getFixedRestartTimes().get(0));
            
            long minutesUntil = now.until(next, java.time.temporal.ChronoUnit.MINUTES);
            if (minutesUntil <= 0) minutesUntil += 24 * 60;
            
            long hours = minutesUntil / 60;
            long minutes = minutesUntil % 60;
            return next.toString() + " (in " + hours + "h " + minutes + "m)";
        } else {
            long minutesLeft = (lastRestartTime + (config.getIntervalHours() * 60 * 60 * 1000) - System.currentTimeMillis()) / (60 * 1000);
            minutesLeft = Math.max(0, minutesLeft);
            long hours = minutesLeft / 60;
            long minutes = minutesLeft % 60;
            return "in " + hours + "h " + minutes + "m";
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
