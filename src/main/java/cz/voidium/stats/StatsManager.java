package cz.voidium.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import cz.voidium.config.StatsConfig;
import cz.voidium.discord.DiscordManager;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import cz.voidium.config.StorageHelper;

public class StatsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Stats");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static StatsManager instance;
    
    private final Path dataPath;
    private MinecraftServer server;
    private ScheduledExecutorService scheduler;
    
    // Date -> List of player counts (recorded every minute)
    private Map<String, List<Integer>> dailySamples = new HashMap<>();
    
    // Live History data for Web Panel
    private final List<DataPoint> history = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY_POINTS = 1440; // 24 hours at 1 min interval

    public static class DataPoint {
        public long timestamp;
        public int players;
        public double tps;
        
        public DataPoint(long timestamp, int players, double tps) {
            this.timestamp = timestamp;
            this.players = players;
            this.tps = tps;
        }
    }
    
    private StatsManager() {
        this.dataPath = StorageHelper.resolve("voidium_stats_data.json");
        loadData();
    }

    public static synchronized StatsManager getInstance() {
        if (instance == null) {
            instance = new StatsManager();
        }
        return instance;
    }

    public void start(MinecraftServer server) {
        this.server = server;
        if (!StatsConfig.getInstance().isEnableStats()) return;

        scheduler = Executors.newScheduledThreadPool(1);
        
        // Record stats every minute
        scheduler.scheduleAtFixedRate(this::recordStats, 1, 1, TimeUnit.MINUTES);
        
        // Schedule report check every minute (to see if it's time to report)
        scheduler.scheduleAtFixedRate(this::checkReportTime, 1, 1, TimeUnit.MINUTES);
        
        LOGGER.info("Stats Manager started.");
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        saveData();
    }

    public void reload() {
        stop();
        start(server);
    }

    private void recordStats() {
        if (server == null) return;
        int playerCount = server.getPlayerList().getPlayerCount();
        String today = LocalDate.now().toString();
        
        dailySamples.computeIfAbsent(today, k -> new ArrayList<>()).add(playerCount);
        
        // Update live history
        double tps = getTps();
        history.add(new DataPoint(System.currentTimeMillis(), playerCount, tps));
        while (history.size() > MAX_HISTORY_POINTS) {
            history.remove(0);
        }
        
        // Save occasionally? Or just on stop? Let's save on stop or maybe every hour.
        // For safety, let's save every time for now or maybe every 10 mins. 
        // Actually, let's just keep it in memory and save on stop, but if crash happens we lose data.
        // Better to save periodically.
        saveData(); 
    }
    
    private double getTps() {
        if (server == null) return 20.0;
        
        try {
            // Get average tick time from server using reflection
            java.lang.reflect.Field tickTimesField = server.getClass().getDeclaredField("tickTimes");
            tickTimesField.setAccessible(true);
            long[] tickTimes = (long[]) tickTimesField.get(server);
            
            if (tickTimes == null || tickTimes.length == 0) return 20.0;
            
            long sum = 0;
            for (long time : tickTimes) {
                sum += time;
            }
            double avgTickNanos = (double) sum / tickTimes.length;
            double avgTickMillis = avgTickNanos * 1.0E-6D;
            
            // TPS = 1000ms / avg tick time (capped at 20)
            double tps = Math.min(1000.0 / avgTickMillis, 20.0);
            return Math.max(0.0, tps); // Ensure non-negative
        } catch (Exception e) {
            // Field might not exist in this version or access failed
            return 20.0;
        }
    }

    public List<DataPoint> getHistory() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    private void checkReportTime() {
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalTime reportTime = StatsConfig.getInstance().getReportTime();
        
        LOGGER.debug("Checking report time: now={}, reportTime={}", now, reportTime);
        
        if (now.equals(reportTime)) {
            LOGGER.info("Report time matched! Sending daily report...");
            sendDailyReport();
        }
    }

    public void sendDailyReport() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        List<Integer> samples = dailySamples.get(yesterday);
        
        LOGGER.info("Sending daily report for {}, samples size: {}", yesterday, samples != null ? samples.size() : 0);
        
        if (samples == null || samples.isEmpty()) {
            LOGGER.info("No stats data for yesterday ({}), skipping report.", yesterday);
            return;
        }
        
        int peak = Collections.max(samples);
        double average = samples.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        
        LOGGER.info("Stats for {}: peak={}, average={:.2f}", yesterday, peak, average);
        
        // Send to Discord
        DiscordManager.getInstance().sendStatsReport(yesterday, peak, average);
        
        // Cleanup old data (older than 2 days)
        cleanupOldData();
    }
    
    private void cleanupOldData() {
        LocalDate cutoff = LocalDate.now().minusDays(2);
        dailySamples.entrySet().removeIf(entry -> LocalDate.parse(entry.getKey()).isBefore(cutoff));
        saveData();
    }

    private void loadData() {
        if (Files.exists(dataPath)) {
            try (Reader reader = Files.newBufferedReader(dataPath)) {
                Map<String, List<Integer>> data = GSON.fromJson(reader, new TypeToken<Map<String, List<Integer>>>(){}.getType());
                if (data != null) {
                    dailySamples = data;
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load stats data", e);
            }
        }
    }

    private void saveData() {
        try (Writer writer = Files.newBufferedWriter(dataPath)) {
            GSON.toJson(dailySamples, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save stats data", e);
        }
    }
}
