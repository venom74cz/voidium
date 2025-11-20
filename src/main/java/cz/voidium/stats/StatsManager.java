package cz.voidium.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import cz.voidium.config.StatsConfig;
import cz.voidium.discord.DiscordManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;
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

public class StatsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Stats");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static StatsManager instance;
    
    private final Path dataPath;
    private MinecraftServer server;
    private ScheduledExecutorService scheduler;
    
    // Date -> List of player counts (recorded every minute)
    private Map<String, List<Integer>> dailySamples = new HashMap<>();
    
    private StatsManager() {
        this.dataPath = FMLPaths.CONFIGDIR.get().resolve("voidium").resolve("voidium_stats_data.json");
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
            scheduler.shutdown();
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
        
        // Save occasionally? Or just on stop? Let's save on stop or maybe every hour.
        // For safety, let's save every time for now or maybe every 10 mins. 
        // Actually, let's just keep it in memory and save on stop, but if crash happens we lose data.
        // Better to save periodically.
        saveData(); 
    }

    private void checkReportTime() {
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalTime reportTime = StatsConfig.getInstance().getReportTime();
        
        if (now.equals(reportTime)) {
            sendDailyReport();
        }
    }

    public void sendDailyReport() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        List<Integer> samples = dailySamples.get(yesterday);
        
        if (samples == null || samples.isEmpty()) {
            LOGGER.info("No stats data for yesterday ({}), skipping report.", yesterday);
            return;
        }
        
        int peak = Collections.max(samples);
        double average = samples.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        
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
