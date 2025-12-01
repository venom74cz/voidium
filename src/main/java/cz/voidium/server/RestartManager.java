package cz.voidium.server;

import cz.voidium.config.VoidiumConfig;
import cz.voidium.config.RestartConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RestartManager {
    private final MinecraftServer server;
    private ScheduledExecutorService scheduler;
    private long lastRestartTime;
    private long serverStartTime;
    @SuppressWarnings("unused")
    private ScheduledFuture<?> manualRestartTask;

    public RestartManager(MinecraftServer server) {
        this.server = server;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.lastRestartTime = System.currentTimeMillis();
        this.serverStartTime = System.currentTimeMillis();
        scheduleNextRestart();
    }

    public void reload() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduleNextRestart();
    }

    private void scheduleNextRestart() {
        RestartConfig config = RestartConfig.getInstance();
        if (config == null) {
            System.err.println("RestartConfig is null! Cannot schedule restart.");
            return;
        }
        
        if (config.getRestartType() == RestartConfig.RestartType.FIXED_TIME) {
            scheduleFixedTimeRestart();
        } else if (config.getRestartType() == RestartConfig.RestartType.INTERVAL) {
            scheduleIntervalRestart();
        } else if (config.getRestartType() == RestartConfig.RestartType.DELAY) {
            scheduleDelayRestart();
        }
    }

    private void scheduleFixedTimeRestart() {
        RestartConfig config = RestartConfig.getInstance();
        if (config.getFixedRestartTimes().isEmpty()) {
            System.err.println("No fixed restart times configured!");
            return;
        }
        
        LocalTime now = LocalTime.now();
        
        // Najdi nejbližší čas restartu - seřazený podle vzdálenosti od aktuálního času
        LocalTime nextRestart = config.getFixedRestartTimes().stream()
                .filter(time -> time.isAfter(now))
                .min((t1, t2) -> Long.compare(now.until(t1, ChronoUnit.MINUTES), now.until(t2, ChronoUnit.MINUTES)))
                .orElse(
                    // Pokud žádný čas není po aktuálním čase, vezmi nejbližší čas zítra
                    config.getFixedRestartTimes().stream()
                        .min((t1, t2) -> t1.compareTo(t2))
                        .orElse(config.getFixedRestartTimes().get(0))
                );

        long delayMinutes = now.until(nextRestart, ChronoUnit.MINUTES);
        if (delayMinutes <= 0) {
            delayMinutes += 24 * 60; // Přidej 24 hodin
        }
        
        System.out.println("Scheduling fixed time restart in " + delayMinutes + " minutes at " + nextRestart);
        scheduleRestart(delayMinutes);
    }

    private void scheduleIntervalRestart() {
        int intervalHours = RestartConfig.getInstance().getIntervalHours();
        long nextRestartIn = (lastRestartTime + (intervalHours * 60L * 60L * 1000L) - System.currentTimeMillis()) / (60L * 1000L);
        if (nextRestartIn <= 0) {
            nextRestartIn = intervalHours * 60; // Pokud je čas už prošlý, nastav na celý interval
        }
        System.out.println("Scheduling interval restart in " + nextRestartIn + " minutes (every " + intervalHours + " hours)");
        scheduleRestart(nextRestartIn);
    }
    
    private void scheduleDelayRestart() {
        int delayMinutes = RestartConfig.getInstance().getDelayMinutes();
        System.out.println("Scheduling delay restart in " + delayMinutes + " minutes from server start");
        scheduleRestart(delayMinutes);
    }

    private void scheduleRestart(long delayMinutes) {
        if (delayMinutes <= 0) {
            System.err.println("Invalid delay for restart: " + delayMinutes + " minutes");
            return;
        }
        
        System.out.println("Scheduling restart in " + delayMinutes + " minutes");
        
        // Plánování oznámení před restartem
        long[] warningTimes = {60, 30, 15, 10, 5, 3, 2, 1}; // minuty
        
        for (long warningTime : warningTimes) {
            if (delayMinutes > warningTime) {
                scheduler.schedule(
                    () -> broadcastRestartWarning(warningTime),
                    delayMinutes - warningTime,
                    TimeUnit.MINUTES
                );
                System.out.println("Scheduled warning for " + warningTime + " minutes before restart");
            }
        }

        // Plánování samotného restartu
        scheduler.schedule(this::performRestart, delayMinutes, TimeUnit.MINUTES);
        System.out.println("Restart scheduled successfully for " + delayMinutes + " minutes");
    }

    private void broadcastRestartWarning(long minutesLeft) {
        String message = String.format("&cServer restart in %d minutes!", minutesLeft);
        server.getPlayerList().getPlayers().forEach(player -> 
            player.sendSystemMessage(VoidiumConfig.formatMessage(message))
        );
    }

    private void performRestart() {
        System.out.println("Performing server restart...");
        
        // Finální zpráva hráčům
        server.getPlayerList().getPlayers().forEach(player -> 
            player.sendSystemMessage(Component.literal("§cServer is restarting now!"))
        );
        
        // Kick hráčů s důvodem RESTART
        scheduler.schedule(() -> {
            server.getPlayerList().getPlayers().forEach(player -> 
                player.connection.disconnect(Component.literal("§cRESTART"))
            );
        }, 2, TimeUnit.SECONDS);
        
        // Naplánování skutečného restartu
        scheduler.schedule(() -> {
            System.out.println("Shutting down server...");
            server.halt(false);
        }, 5, TimeUnit.SECONDS);
        
        lastRestartTime = System.currentTimeMillis();
    }

    public void scheduleManualRestart(int minutes) {
        scheduleRestart(minutes);
    }
    
    public void cancelManualRestart() {
        // Zruš všechny naplánované úlohy
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Vytvoř nový scheduler
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Naplánuj znovu automatické restarty
        scheduleNextRestart();
        
        System.out.println("Manual restart cancelled and automatic restarts rescheduled");
    }
    

    public String getNextRestartInfo() {
        RestartConfig config = RestartConfig.getInstance();
        if (config.getRestartType() == RestartConfig.RestartType.FIXED_TIME) {
            java.time.LocalTime now = java.time.LocalTime.now();
            
            // Najdi nejbližší čas restartu - seřazený podle vzdálenosti od aktuálního času
            java.time.LocalTime next = config.getFixedRestartTimes().stream()
                .filter(time -> time.isAfter(now))
                .min((t1, t2) -> Long.compare(now.until(t1, java.time.temporal.ChronoUnit.MINUTES), now.until(t2, java.time.temporal.ChronoUnit.MINUTES)))
                .orElse(
                    // Pokud žádný čas není po aktuálním čase, vezmi nejbližší čas zítra
                    config.getFixedRestartTimes().stream()
                        .min((t1, t2) -> t1.compareTo(t2))
                        .orElse(config.getFixedRestartTimes().get(0))
                );
            
            long minutesUntil = now.until(next, java.time.temporal.ChronoUnit.MINUTES);
            if (minutesUntil <= 0) minutesUntil += 24 * 60;
            
            long hours = minutesUntil / 60;
            long minutes = minutesUntil % 60;
            return next.toString() + " (in " + hours + "h " + minutes + "m)";
        } else if (config.getRestartType() == RestartConfig.RestartType.INTERVAL) {
            long minutesLeft = (lastRestartTime + (config.getIntervalHours() * 60L * 60L * 1000L) - System.currentTimeMillis()) / (60L * 1000L);
            minutesLeft = Math.max(0, minutesLeft);
            long hours = minutesLeft / 60;
            long minutes = minutesLeft % 60;
            return "in " + hours + "h " + minutes + "m";
        } else { // DELAY
            long minutesLeft = (serverStartTime + (config.getDelayMinutes() * 60L * 1000L) - System.currentTimeMillis()) / (60L * 1000L);
            minutesLeft = Math.max(0, minutesLeft);
            long hours = minutesLeft / 60;
            long minutes = minutesLeft % 60;
            return "in " + hours + "h " + minutes + "m (delay restart)";
        }
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
