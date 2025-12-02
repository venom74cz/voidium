package cz.voidium.server;

import cz.voidium.config.VoidiumConfig;
import cz.voidium.config.RestartConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RestartManager {
    private final MinecraftServer server;
    private ScheduledExecutorService scheduler;
    private long serverStartTime;
    
    // Soubor pro uložení posledního restartu
    private static final String LAST_RESTART_FILE = "config/voidium/last_restart.txt";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Poslední úspěšný restart (načtený ze souboru nebo null)
    private LocalDateTime lastSuccessfulRestart;

    public RestartManager(MinecraftServer server) {
        this.server = server;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.serverStartTime = System.currentTimeMillis();
        
        // Načti poslední restart ze souboru
        this.lastSuccessfulRestart = loadLastRestartTime();
        if (lastSuccessfulRestart != null) {
            System.out.println("[Restart] Last successful restart was at: " + lastSuccessfulRestart.format(DATE_TIME_FORMAT));
        } else {
            System.out.println("[Restart] No previous restart record found.");
        }
        
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
            System.err.println("[Restart] RestartConfig is null! Cannot schedule restart.");
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
            System.err.println("[Restart] No fixed restart times configured!");
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Najdi nejbližší čas restartu, který ještě neproběhl
        LocalDateTime nextRestartDateTime = null;
        long minSecondsUntil = Long.MAX_VALUE;
        
        for (LocalTime time : config.getFixedRestartTimes()) {
            LocalDateTime candidate = findNextOccurrence(now, time);
            
            // Pokud tento čas už dnes proběhl (máme záznam), přeskoč ho
            if (wasAlreadyExecutedToday(time)) {
                System.out.println("[Restart] Skipping " + time + " - already executed today.");
                // Posuň na zítřek
                candidate = LocalDateTime.of(now.toLocalDate().plusDays(1), time);
            }
            
            long secondsUntil = now.until(candidate, ChronoUnit.SECONDS);
            if (secondsUntil > 0 && secondsUntil < minSecondsUntil) {
                minSecondsUntil = secondsUntil;
                nextRestartDateTime = candidate;
            }
        }
        
        if (nextRestartDateTime == null) {
            System.err.println("[Restart] Could not determine next restart time!");
            return;
        }
        
        // Převeď sekundy na minuty (zaokrouhleno nahoru, minimálně 1)
        long delayMinutes = Math.max(1, (minSecondsUntil + 59) / 60);
        
        System.out.println("[Restart] Next restart scheduled at " + nextRestartDateTime.format(DATE_TIME_FORMAT) + 
                           " (in " + delayMinutes + " minutes)");
        scheduleRestart(delayMinutes, nextRestartDateTime);
    }
    
    /**
     * Zjistí, zda daný čas už dnes proběhl jako restart.
     */
    private boolean wasAlreadyExecutedToday(LocalTime time) {
        if (lastSuccessfulRestart == null) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate lastRestartDate = lastSuccessfulRestart.toLocalDate();
        
        // Pokud poslední restart nebyl dnes, nic nepřeskakuj
        if (!lastRestartDate.equals(today)) {
            return false;
        }
        
        // Poslední restart byl dnes - zkontroluj jestli to byl tento čas
        LocalTime lastRestartTime = lastSuccessfulRestart.toLocalTime();
        
        // Tolerance 5 minut - pokud restart proběhl v rozmezí +-5 minut od plánovaného času
        long diffMinutes = Math.abs(ChronoUnit.MINUTES.between(lastRestartTime, time));
        return diffMinutes <= 5;
    }
    
    /**
     * Najde nejbližší výskyt daného času (dnes nebo zítra).
     */
    private LocalDateTime findNextOccurrence(LocalDateTime now, LocalTime targetTime) {
        LocalDateTime todayAtTarget = LocalDateTime.of(now.toLocalDate(), targetTime);
        
        if (todayAtTarget.isAfter(now)) {
            return todayAtTarget;
        } else {
            // Čas už prošel, vrať zítřejší
            return LocalDateTime.of(now.toLocalDate().plusDays(1), targetTime);
        }
    }

    private void scheduleIntervalRestart() {
        int intervalHours = RestartConfig.getInstance().getIntervalHours();
        if (intervalHours <= 0) {
            System.err.println("[Restart] Invalid interval restart time: " + intervalHours + " hours. Must be > 0.");
            return;
        }
        // Počítáme od startu serveru - každých X hodin
        long elapsedMinutes = (System.currentTimeMillis() - serverStartTime) / (60L * 1000L);
        long intervalMinutes = intervalHours * 60L;
        long nextRestartIn = intervalMinutes - (elapsedMinutes % intervalMinutes);
        if (nextRestartIn <= 0) {
            nextRestartIn = intervalMinutes;
        }
        System.out.println("[Restart] Scheduling interval restart in " + nextRestartIn + " minutes (every " + intervalHours + " hours)");
        scheduleRestart(nextRestartIn, null);
    }
    
    private void scheduleDelayRestart() {
        int delayMinutes = RestartConfig.getInstance().getDelayMinutes();
        if (delayMinutes <= 0) {
            System.err.println("[Restart] Invalid delay restart time: " + delayMinutes + " minutes. Must be > 0.");
            return;
        }
        System.out.println("[Restart] Scheduling delay restart in " + delayMinutes + " minutes from server start");
        scheduleRestart(delayMinutes, null);
    }

    private void scheduleRestart(long delayMinutes, LocalDateTime scheduledTime) {
        if (delayMinutes <= 0) {
            System.err.println("[Restart] Invalid delay for restart: " + delayMinutes + " minutes");
            return;
        }
        
        System.out.println("[Restart] Scheduling restart in " + delayMinutes + " minutes");
        
        // Plánování oznámení před restartem
        long[] warningTimes = {60, 30, 15, 10, 5, 3, 2, 1}; // minuty
        
        for (long warningTime : warningTimes) {
            if (delayMinutes > warningTime) {
                scheduler.schedule(
                    () -> broadcastRestartWarning(warningTime),
                    delayMinutes - warningTime,
                    TimeUnit.MINUTES
                );
                System.out.println("[Restart] Scheduled warning for " + warningTime + " minutes before restart");
            }
        }

        // Plánování samotného restartu
        scheduler.schedule(() -> performRestart(scheduledTime), delayMinutes, TimeUnit.MINUTES);
        System.out.println("[Restart] Restart scheduled successfully for " + delayMinutes + " minutes");
    }

    private void broadcastRestartWarning(long minutesLeft) {
        RestartConfig config = RestartConfig.getInstance();
        String message = config.getWarningMessage().replace("%minutes%", String.valueOf(minutesLeft));
        server.getPlayerList().getPlayers().forEach(player -> 
            player.sendSystemMessage(VoidiumConfig.formatMessage(message))
        );
    }

    private void performRestart(LocalDateTime scheduledTime) {
        System.out.println("[Restart] Performing server restart...");
        
        // Ulož čas restartu PŘED samotným restartem
        LocalDateTime restartTime = scheduledTime != null ? scheduledTime : LocalDateTime.now();
        saveLastRestartTime(restartTime);
        System.out.println("[Restart] Saved restart time: " + restartTime.format(DATE_TIME_FORMAT));
        
        // Finální zpráva hráčům
        RestartConfig config = RestartConfig.getInstance();
        server.getPlayerList().getPlayers().forEach(player -> 
            player.sendSystemMessage(VoidiumConfig.formatMessage(config.getRestartingNowMessage()))
        );
        
        // Kick hráčů s důvodem RESTART
        scheduler.schedule(() -> {
            server.getPlayerList().getPlayers().forEach(player -> 
                player.connection.disconnect(VoidiumConfig.formatMessage(config.getKickMessage()))
            );
        }, 2, TimeUnit.SECONDS);
        
        // Naplánování skutečného restartu
        scheduler.schedule(() -> {
            System.out.println("[Restart] Shutting down server...");
            server.halt(false);
        }, 5, TimeUnit.SECONDS);
    }
    
    // === Persistence pro last restart time ===
    
    private LocalDateTime loadLastRestartTime() {
        Path path = Paths.get(LAST_RESTART_FILE);
        if (!Files.exists(path)) {
            return null;
        }
        
        try {
            String content = Files.readString(path).trim();
            if (content.isEmpty()) {
                return null;
            }
            return LocalDateTime.parse(content, DATE_TIME_FORMAT);
        } catch (IOException e) {
            System.err.println("[Restart] Failed to read last restart time: " + e.getMessage());
            return null;
        } catch (DateTimeParseException e) {
            System.err.println("[Restart] Failed to parse last restart time: " + e.getMessage());
            return null;
        }
    }
    
    private void saveLastRestartTime(LocalDateTime time) {
        Path path = Paths.get(LAST_RESTART_FILE);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, time.format(DATE_TIME_FORMAT));
        } catch (IOException e) {
            System.err.println("[Restart] Failed to save last restart time: " + e.getMessage());
        }
    }

    public void scheduleManualRestart(int minutes) {
        scheduleRestart(minutes, null);
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
        
        System.out.println("[Restart] Manual restart cancelled and automatic restarts rescheduled");
    }
    

    public String getNextRestartInfo() {
        RestartConfig config = RestartConfig.getInstance();
        if (config.getRestartType() == RestartConfig.RestartType.FIXED_TIME) {
            if (config.getFixedRestartTimes().isEmpty()) {
                return "No restart times configured";
            }
            
            LocalDateTime now = LocalDateTime.now();
            
            // Najdi nejbližší čas (stejná logika jako v scheduleFixedTimeRestart)
            LocalDateTime nextRestartDateTime = null;
            long minSecondsUntil = Long.MAX_VALUE;
            
            for (LocalTime time : config.getFixedRestartTimes()) {
                LocalDateTime candidate = findNextOccurrence(now, time);
                
                if (wasAlreadyExecutedToday(time)) {
                    candidate = LocalDateTime.of(now.toLocalDate().plusDays(1), time);
                }
                
                long secondsUntil = now.until(candidate, ChronoUnit.SECONDS);
                if (secondsUntil > 0 && secondsUntil < minSecondsUntil) {
                    minSecondsUntil = secondsUntil;
                    nextRestartDateTime = candidate;
                }
            }
            
            if (nextRestartDateTime == null) {
                return "No restart times configured";
            }
            
            long minutesUntil = Math.max(1, (minSecondsUntil + 59) / 60);
            
            long hours = minutesUntil / 60;
            long minutes = minutesUntil % 60;
            return nextRestartDateTime.toLocalTime().toString() + " (in " + hours + "h " + minutes + "m)";
        } else if (config.getRestartType() == RestartConfig.RestartType.INTERVAL) {
            int intervalHours = config.getIntervalHours();
            if (intervalHours <= 0) {
                return "Invalid interval configuration";
            }
            long elapsedMinutes = (System.currentTimeMillis() - serverStartTime) / (60L * 1000L);
            long intervalMinutes = intervalHours * 60L;
            long minutesLeft = intervalMinutes - (elapsedMinutes % intervalMinutes);
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
