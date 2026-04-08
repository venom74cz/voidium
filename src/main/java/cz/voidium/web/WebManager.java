package cz.voidium.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import cz.voidium.Voidium;
import cz.voidium.ai.AIService;
import cz.voidium.config.EntityCleanerConfig;
import cz.voidium.config.GeneralConfig;
import cz.voidium.config.StatsConfig;
import cz.voidium.config.TicketConfig;
import cz.voidium.config.WebConfig;
import cz.voidium.discord.DiscordManager;
import cz.voidium.discord.LinkManager;
import cz.voidium.discord.TicketManager;
import cz.voidium.server.AnnouncementManager;
import cz.voidium.server.RestartManager;
import cz.voidium.server.chat.ChatHistoryManager;
import cz.voidium.stats.StatsManager;
import cz.voidium.vote.VoteManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Voidium-Web");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String SESSION_COOKIE = "voidium_session"; 
    private static final String ADMIN_AI_CONVERSATION_ID = "admin:web";
    private static final DateTimeFormatter VOTE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final URI GITHUB_LATEST_RELEASE_URI = URI.create("https://api.github.com/repos/venom74cz/voidium/releases/latest");
    private static final Duration RELEASE_CHECK_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration RELEASE_CHECK_FAILURE_TTL = Duration.ofMinutes(5);
    private static final Duration RELEASE_CHECK_TIMEOUT = Duration.ofSeconds(2);
    private static final Pattern VERSION_NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final int RATE_LIMIT_MAX_REQUESTS = 120;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;
    // Keep the singleton below timeout constants because instance field initialization uses them.
    private static final WebManager INSTANCE = new WebManager();

    private final Map<String, Instant> bootstrapTokens = new ConcurrentHashMap<>();
    private final Map<String, Instant> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> rateLimits = new ConcurrentHashMap<>();
    private final HttpClient releaseHttpClient = HttpClient.newBuilder()
            .connectTimeout(RELEASE_CHECK_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private volatile HttpServer httpServer;
    private volatile MinecraftServer server;
    private volatile Instant startedAt;
    private volatile java.util.concurrent.ScheduledExecutorService sessionCleaner;
    private volatile WebConsoleAppender consoleAppender;
    private volatile ReleaseInfo cachedReleaseInfo = ReleaseInfo.empty();
    private volatile Instant releaseInfoFetchedAt = Instant.EPOCH;

    private WebManager() {
    }

    public static WebManager getInstance() {
        return INSTANCE;
    }

    public synchronized void setServer(MinecraftServer server) {
        this.server = server;
    }

    public synchronized void start() {
        if (server == null) {
            LOGGER.warn("WebManager start requested before MinecraftServer was attached");
            return;
        }
        if (httpServer != null) {
            return;
        }

        try {
            HttpServer created = HttpServer.create(resolveBindAddress(WebConfig.getInstance()), 0);
            created.createContext("/", new RootHandler());
            created.createContext("/assets/", new StaticAssetHandler());
            created.createContext("/api/dashboard", new DashboardHandler());
            created.createContext("/api/feeds", new FeedsHandler());
            created.createContext("/api/action", new ActionHandler());
            created.createContext("/api/ai/admin", rateLimited(new AdminAiHandler()));
            created.createContext("/api/ai/admin/suggest", rateLimited(new AdminAiSuggestHandler()));
            created.createContext("/api/ai/players", rateLimited(new AiPlayerHistoryHandler()));
            created.createContext("/api/config/schema", new ConfigSchemaHandler());
            created.createContext("/api/config/values", new ConfigValuesHandler());
            created.createContext("/api/config/defaults", new ConfigDefaultsHandler());
            created.createContext("/api/config/locale", new ConfigLocaleHandler());
            created.createContext("/api/config/preview", new ConfigPreviewHandler());
            created.createContext("/api/config/diff", new ConfigDiffHandler());
            created.createContext("/api/config/apply", new ConfigApplyHandler());
            created.createContext("/api/config/rollback", new ConfigRollbackHandler());
            created.createContext("/api/config/reload", new ConfigReloadHandler());
            created.createContext("/api/console/execute", new ConsoleExecuteHandler());
            created.createContext("/api/discord/roles", new DiscordRolesHandler());
            created.createContext("/api/logout", new LogoutHandler());
            created.createContext("/api/events", new SseHandler());
            created.createContext("/api/config/schema/export", new ConfigSchemaExportHandler());
            created.createContext("/api/server-icon", new ServerIconHandler());
            created.createContext("/api/server-properties", new ServerPropertiesHandler());
            created.setExecutor(Executors.newCachedThreadPool());
            created.start();

            httpServer = created;
            startedAt = Instant.now();
            attachConsoleAppender();

            // Periodically clean expired sessions (every 5 minutes)
            sessionCleaner = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Voidium-SessionCleaner");
                t.setDaemon(true);
                return t;
            });
            sessionCleaner.scheduleAtFixedRate(this::cleanupExpiredTokens, 5, 5, java.util.concurrent.TimeUnit.MINUTES);

            LOGGER.info("Voidium Web Manager listening on {}", getBaseUrl());
        } catch (IOException e) {
            LOGGER.error("Failed to start Voidium Web Manager", e);
        }
    }

    public synchronized void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
            LOGGER.info("Voidium Web Manager stopped");
        }
        if (sessionCleaner != null) {
            sessionCleaner.shutdownNow();
            sessionCleaner = null;
        }
        detachConsoleAppender();
        sessions.clear();
        bootstrapTokens.clear();
    }

    public boolean isRunning() {
        return httpServer != null;
    }

    public String issueAccessUrl() {
        cleanupExpiredTokens();
        String token = UUID.randomUUID().toString().replace("-", "");
        bootstrapTokens.put(token, Instant.now().plus(Duration.ofMinutes(10)));
        return getBaseUrl() + "/?token=" + token;
    }

    public String getPersistentAccessUrl() {
        return getBaseUrl() + "/?token=" + WebConfig.getInstance().getAdminToken();
    }

    public String getBaseUrl() {
        WebConfig config = WebConfig.getInstance();
        return "http://" + resolvePublicHost(config) + ":" + config.getPort();
    }

    private InetSocketAddress resolveBindAddress(WebConfig config) throws IOException {
        String bindAddress = config.getBindAddress();
        if (bindAddress == null || bindAddress.isBlank() || "0.0.0.0".equals(bindAddress)) {
            return new InetSocketAddress(config.getPort());
        }
        return new InetSocketAddress(InetAddress.getByName(bindAddress), config.getPort());
    }

    private String resolvePublicHost(WebConfig config) {
        String host = config.getPublicHostname();
        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        if (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("0.0.0.0")) {
            // Try MC server-ip from server.properties first
            MinecraftServer mc = server;
            if (mc != null) {
                String serverIp = mc.getLocalIp();
                if (serverIp != null && !serverIp.isBlank()) {
                    return serverIp;
                }
            }
            return "localhost";
        }
        return host;
    }

    private void cleanupExpiredTokens() {
        Instant now = Instant.now();
        bootstrapTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        sessions.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        long currentMs = System.currentTimeMillis();
        rateLimits.entrySet().removeIf(entry -> currentMs - entry.getValue()[1] > RATE_LIMIT_WINDOW_MS * 5);
    }

    private boolean isAuthenticated(HttpExchange exchange) {
        cleanupExpiredTokens();
        Map<String, String> query = parseParams(exchange.getRequestURI().getRawQuery());
        String queryToken = query.get("token");
        if (queryToken != null && !queryToken.isBlank()) {
            if (matchesAdminToken(queryToken) || consumeBootstrapToken(queryToken)) {
                attachSessionCookie(exchange);
                return true;
            }
        }

        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return false;
        }

        for (String cookie : cookieHeader.split(";")) {
            String[] parts = cookie.trim().split("=", 2);
            if (parts.length == 2 && SESSION_COOKIE.equals(parts[0])) {
                Instant expires = sessions.get(parts[1]);
                if (expires != null && expires.isAfter(Instant.now())) {
                    sessions.put(parts[1], Instant.now().plus(Duration.ofMinutes(WebConfig.getInstance().getSessionTtlMinutes())));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesAdminToken(String token) {
        String adminToken = WebConfig.getInstance().getAdminToken();
        return adminToken != null && !adminToken.isBlank() && adminToken.equals(token);
    }

    private boolean consumeBootstrapToken(String token) {
        Instant expires = bootstrapTokens.remove(token);
        return expires != null && expires.isAfter(Instant.now());
    }

    private void attachSessionCookie(HttpExchange exchange) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        long maxAgeSeconds = Duration.ofMinutes(WebConfig.getInstance().getSessionTtlMinutes()).toSeconds();
        sessions.put(sessionId, Instant.now().plusSeconds(maxAgeSeconds));
        exchange.getResponseHeaders().add("Set-Cookie", SESSION_COOKIE + "=" + sessionId + "; Max-Age=" + maxAgeSeconds + "; HttpOnly; SameSite=Lax; Path=/");
    }

    private void attachConsoleAppender() {
        if (consoleAppender != null) {
            return;
        }
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        consoleAppender = new WebConsoleAppender("VoidiumWebConsole", null, PatternLayout.createDefaultLayout());
        consoleAppender.start();
        configuration.addAppender(consoleAppender);
        LoggerConfig rootLogger = configuration.getRootLogger();
        rootLogger.addAppender(consoleAppender, null, null);
        context.updateLoggers();
    }

    private void detachConsoleAppender() {
        if (consoleAppender == null) {
            return;
        }
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        configuration.getRootLogger().removeAppender("VoidiumWebConsole");
        consoleAppender.stop();
        consoleAppender = null;
        context.updateLoggers();
    }

    private Map<String, String> parseParams(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : raw.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            String[] parts = pair.split("=", 2);
            result.put(urlDecode(parts[0]), parts.length > 1 ? urlDecode(parts[1]) : "");
        }
        return result;
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private JsonObject readJsonBody(HttpExchange exchange) throws IOException {
        return GSON.fromJson(readRequestBody(exchange), JsonObject.class);
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private void sendJson(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] body = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private void sendText(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private record ReleaseInfo(String latestVersion, boolean updateAvailable, String updateUrl) {
        private static ReleaseInfo empty() {
            return new ReleaseInfo(null, false, null);
        }
    }

    private Map<String, Object> buildDashboardPayload() {
        MinecraftServer currentServer = server;
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        String installedVersion = resolveInstalledVersion();
        ReleaseInfo releaseInfo = resolveReleaseInfo(installedVersion);

        return mapOf(
                entry("serverName", currentServer != null ? currentServer.getMotd() : "Voidium Server"),
                entry("baseUrl", getBaseUrl()),
                entry("publicAccessUrl", getPersistentAccessUrl()),
            entry("version", installedVersion),
            entry("latestVersion", releaseInfo.latestVersion()),
            entry("updateAvailable", releaseInfo.updateAvailable()),
            entry("updateUrl", releaseInfo.updateUrl()),
            entry("serverIconUrl", resolveServerIconUrl()),
                entry("onlinePlayers", currentServer != null ? currentServer.getPlayerCount() : 0),
                entry("maxPlayers", currentServer != null ? currentServer.getPlayerList().getMaxPlayers() : 0),
                entry("tps", round(currentTps())),
                entry("mspt", round(currentMspt())),
                entry("uptime", formatDuration(startedAt == null ? Duration.ZERO : Duration.between(startedAt, Instant.now()))),
                entry("memoryUsedMb", usedMemory),
                entry("memoryMaxMb", maxMemory),
                entry("memoryUsagePercent", maxMemory == 0 ? 0 : round((usedMemory * 100.0) / maxMemory)),
                entry("nextRestart", resolveNextRestart()),
                entry("maintenanceMode", GeneralConfig.getInstance().isMaintenanceMode()),
                entry("timers", buildTimerPayload()),
                entry("tickets", buildTicketPayload()),
                entry("voteQueue", buildVoteQueuePayload()),
                entry("players", currentServer == null ? List.of() : buildPlayerPayload(currentServer)),
                entry("modules", buildModulePayload()),
                entry("alerts", buildAlerts()),
                entry("history", StatsManager.getInstance().getHistory()),
                entry("ai", AIService.getInstance().getAdminCapabilities()),
                entry("chatFeed", ChatHistoryManager.getInstance().getEntries()),
                entry("consoleFeed", WebConsoleFeed.getInstance().snapshot()),
                entry("auditFeed", WebAuditLog.getInstance().snapshot()),
                entry("systemInfo", buildSystemInfo()));
    }

    private String resolveInstalledVersion() {
        return ModList.get().getModContainerById(Voidium.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    private String resolveServerIconUrl() {
        return Files.exists(Path.of("server-icon.png")) ? "/api/server-icon" : null;
    }

    private ReleaseInfo resolveReleaseInfo(String installedVersion) {
        Instant now = Instant.now();
        ReleaseInfo cached = cachedReleaseInfo;
        Duration ttl = cached.latestVersion() == null ? RELEASE_CHECK_FAILURE_TTL : RELEASE_CHECK_CACHE_TTL;
        if (!releaseInfoFetchedAt.equals(Instant.EPOCH) && Duration.between(releaseInfoFetchedAt, now).compareTo(ttl) < 0) {
            return cached;
        }

        synchronized (this) {
            cached = cachedReleaseInfo;
            ttl = cached.latestVersion() == null ? RELEASE_CHECK_FAILURE_TTL : RELEASE_CHECK_CACHE_TTL;
            if (!releaseInfoFetchedAt.equals(Instant.EPOCH) && Duration.between(releaseInfoFetchedAt, now).compareTo(ttl) < 0) {
                return cached;
            }

            try {
                cachedReleaseInfo = fetchLatestRelease(installedVersion);
            } catch (Exception e) {
                LOGGER.debug("Failed to resolve latest VOIDIUM release info: {}", e.getMessage());
                if (cached.latestVersion() == null) {
                    cachedReleaseInfo = ReleaseInfo.empty();
                }
            }
            releaseInfoFetchedAt = now;
            return cachedReleaseInfo;
        }
    }

    private ReleaseInfo fetchLatestRelease(String installedVersion) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(GITHUB_LATEST_RELEASE_URI)
                .timeout(RELEASE_CHECK_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Voidium-WebPanel")
                .GET()
                .build();

        HttpResponse<String> response = releaseHttpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            return ReleaseInfo.empty();
        }

        JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
        if (json == null) {
            return ReleaseInfo.empty();
        }

        String latestVersion = sanitizeVersion(json.has("tag_name") ? json.get("tag_name").getAsString() : null);
        String releaseUrl = json.has("html_url") ? json.get("html_url").getAsString() : "https://github.com/venom74cz/voidium/releases/latest";
        if (latestVersion == null || latestVersion.isBlank()) {
            return ReleaseInfo.empty();
        }

        return new ReleaseInfo(latestVersion, isNewerVersion(installedVersion, latestVersion), releaseUrl);
    }

    private String sanitizeVersion(String rawVersion) {
        if (rawVersion == null) {
            return null;
        }
        String normalized = rawVersion.trim();
        while (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private boolean isNewerVersion(String currentVersion, String latestVersion) {
        List<Integer> currentParts = extractVersionParts(sanitizeVersion(currentVersion));
        List<Integer> latestParts = extractVersionParts(sanitizeVersion(latestVersion));
        if (currentParts.isEmpty() || latestParts.isEmpty()) {
            return false;
        }
        int maxParts = Math.max(currentParts.size(), latestParts.size());
        for (int index = 0; index < maxParts; index++) {
            int current = index < currentParts.size() ? currentParts.get(index) : 0;
            int latest = index < latestParts.size() ? latestParts.get(index) : 0;
            if (latest > current) {
                return true;
            }
            if (latest < current) {
                return false;
            }
        }
        return false;
    }

    private List<Integer> extractVersionParts(String version) {
        if (version == null || version.isBlank()) {
            return List.of();
        }
        List<Integer> parts = new ArrayList<>();
        Matcher matcher = VERSION_NUMBER_PATTERN.matcher(version);
        while (matcher.find()) {
            parts.add(Integer.parseInt(matcher.group()));
        }
        return parts;
    }

    private Map<String, Object> buildSystemInfo() {
        try {
            var osBean = ManagementFactory.getOperatingSystemMXBean();
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("osName", osBean.getName());
            info.put("osArch", osBean.getArch());
            info.put("availableProcessors", osBean.getAvailableProcessors());
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
                long totalPhysical = sunBean.getTotalMemorySize() / 1024 / 1024;
                long freePhysical = sunBean.getFreeMemorySize() / 1024 / 1024;
                info.put("cpuLoad", round(sunBean.getCpuLoad() * 100));
                info.put("processCpuLoad", round(sunBean.getProcessCpuLoad() * 100));
                info.put("systemRamTotalMb", totalPhysical);
                info.put("systemRamUsedMb", totalPhysical - freePhysical);
                info.put("systemRamPercent", totalPhysical == 0 ? 0 : round(((totalPhysical - freePhysical) * 100.0) / totalPhysical));
            }
            Path serverRoot = Path.of(".").toAbsolutePath();
            var store = Files.getFileStore(serverRoot);
            long diskTotalGb = store.getTotalSpace() / 1024 / 1024 / 1024;
            long diskFreeGb = store.getUsableSpace() / 1024 / 1024 / 1024;
            long diskUsedGb = diskTotalGb - diskFreeGb;
            info.put("diskTotalGb", diskTotalGb);
            info.put("diskUsedGb", diskUsedGb);
            info.put("diskPercent", diskTotalGb == 0 ? 0 : round((diskUsedGb * 100.0) / diskTotalGb));
            return info;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> buildPlayerPayload(MinecraftServer currentServer) {
        List<Map<String, Object>> players = new ArrayList<>();
        LinkManager linkManager = LinkManager.getInstance();
        for (ServerPlayer player : currentServer.getPlayerList().getPlayers()) {
            Long discordId = linkManager.getDiscordId(player.getUUID());
            players.add(mapOf(
                    entry("name", player.getName().getString()),
                    entry("uuid", player.getUUID().toString()),
                    entry("ping", player.connection.latency()),
                    entry("linked", discordId != null),
                    entry("discordId", discordId != null ? String.valueOf(discordId) : null)));
        }
        return players;
    }

    private List<Map<String, Object>> buildModulePayload() {
        GeneralConfig general = GeneralConfig.getInstance();
        VoteManager voteManager = Voidium.getInstance().getVoteManager();
        TicketConfig ticketConfig = TicketConfig.getInstance();
        EntityCleanerConfig cleanerConfig = EntityCleanerConfig.getInstance();
        List<Map<String, Object>> modules = new ArrayList<>();
        modules.add(module(tr("Web", "Web"), general.isEnableWeb(), isRunning() ? tr("live", "ĹľivÄ›") : tr("stopped", "zastaveno")));
        modules.add(module(tr("Discord", "Discord"), general.isEnableDiscord(), DiscordManager.getInstance().getJda() != null ? tr("connected", "pĹ™ipojeno") : tr("offline", "offline")));
        modules.add(module(tr("Stats", "Statistiky"), general.isEnableStats(), general.isEnableStats() ? tr("tracking", "sbÄ›r aktivnĂ­") : tr("disabled", "vypnuto")));
        modules.add(module(tr("Ranks", "Ranky"), general.isEnableRanks(), general.isEnableRanks() ? tr("active", "aktivnĂ­") : tr("disabled", "vypnuto")));
        modules.add(module(tr("Vote", "Vote"), general.isEnableVote(), voteManager != null && voteManager.getPendingQueue() != null ? voteManager.getPendingQueue().getTotalPending() + tr(" pending", " ÄŤekĂˇ") : tr("idle", "neÄŤinnĂ©")));
        modules.add(module(tr("PlayerList", "PlayerList"), general.isEnablePlayerList(), general.isEnablePlayerList() ? tr("enabled", "zapnuto") : tr("disabled", "vypnuto")));
        modules.add(module(tr("Tickets", "Tikety"), ticketConfig != null && ticketConfig.isEnableTickets(), TicketManager.getInstance().snapshotOpenTickets().size() + tr(" open", " otevreno")));
        modules.add(module(tr("Announcements", "OznĂˇmenĂ­"), general.isEnableAnnouncements(), general.isEnableAnnouncements() ? tr("scheduled", "naplĂˇnovĂˇno") : tr("disabled", "vypnuto")));
        modules.add(module(tr("Restarts", "Restarty"), general.isEnableRestarts(), general.isEnableRestarts() ? resolveNextRestart() : tr("disabled", "vypnuto")));
        modules.add(module(tr("EntityCleaner", "EntityCleaner"), cleanerConfig != null && cleanerConfig.isEnabled(), cleanerConfig != null && cleanerConfig.isEnabled() ? tr("Every ", "Kazdych ") + cleanerConfig.getCleanupIntervalSeconds() + "s" : tr("disabled", "vypnuto")));
        modules.add(module(tr("AI", "AI"), true, tr("player + admin assistants", "hrĂˇÄŤskĂ˝ + admin asistent")));
        return modules;
    }

    private List<Map<String, Object>> buildTimerPayload() {
        List<Map<String, Object>> timers = new ArrayList<>();

        RestartManager restartManager = Voidium.getInstance().getRestartManager();
        if (restartManager != null && GeneralConfig.getInstance().isEnableRestarts()) {
            long remainingSeconds = restartManager.getSecondsUntilNextRestart();
            if (remainingSeconds >= 0) {
                timers.add(mapOf(
                        entry("id", "restart"),
                        entry("title", tr("Next restart", "DalĹˇĂ­ restart")),
                        entry("subtitle", resolveNextRestart()),
                        entry("remainingSeconds", remainingSeconds),
                        entry("totalSeconds", restartManager.getRestartCycleSeconds()),
                        entry("tone", "danger")));
            }
        }

        var cleaner = Voidium.getInstance().getEntityCleaner();
        if (cleaner != null && EntityCleanerConfig.getInstance() != null && EntityCleanerConfig.getInstance().isEnabled()) {
            timers.add(mapOf(
                    entry("id", "entitycleaner"),
                    entry("title", tr("Next entity cleanup", "DalĹˇĂ­ entity cleanup")),
                    entry("subtitle", EntityCleanerConfig.getInstance().isProtectBosses() ? tr("Boss protection enabled", "Ochrana bossĹŻ zapnuta") : tr("Boss protection disabled", "Ochrana bossĹŻ vypnuta")),
                    entry("remainingSeconds", Math.max(0, cleaner.getSecondsUntilCleanup())),
                    entry("totalSeconds", Math.max(0, EntityCleanerConfig.getInstance().getCleanupIntervalSeconds())),
                    entry("tone", "accent")));
        }

        StatsConfig statsConfig = StatsConfig.getInstance();
        if (statsConfig != null && statsConfig.isEnableStats()) {
            long remainingSeconds = secondsUntilStatsReport(statsConfig);
            timers.add(mapOf(
                    entry("id", "stats-report"),
                    entry("title", tr("Next stats report", "DalĹˇĂ­ stats report")),
                    entry("subtitle", tr("Scheduled for ", "NaplĂˇnovĂˇno na ") + statsConfig.getReportTime()),
                    entry("remainingSeconds", remainingSeconds),
                    entry("totalSeconds", 24L * 3600L),
                    entry("tone", "mint")));
        }

        return timers;
    }

    private Map<String, Object> buildVoteQueuePayload() {
        VoteManager voteManager = Voidium.getInstance().getVoteManager();
        if (voteManager == null || voteManager.getPendingQueue() == null) {
            return mapOf(entry("total", 0), entry("players", List.of()));
        }

        Map<String, List<cz.voidium.vote.PendingVoteQueue.PendingVote>> grouped = new LinkedHashMap<>();
        for (var vote : voteManager.getPendingQueue().snapshot()) {
            grouped.computeIfAbsent(vote.getUsername(), key -> new ArrayList<>()).add(vote);
        }

        List<Map<String, Object>> players = grouped.entrySet().stream()
                .map(entry -> {
                    List<cz.voidium.vote.PendingVoteQueue.PendingVote> votes = entry.getValue();
                    votes.sort(Comparator.comparingLong(cz.voidium.vote.PendingVoteQueue.PendingVote::getQueuedAt).reversed());
                    var latest = votes.get(0);
                    boolean online = server != null && server.getPlayerList().getPlayerByName(entry.getKey()) != null;
                    return mapOf(
                            entry("player", entry.getKey()),
                            entry("count", votes.size()),
                            entry("latestService", latest.getServiceName()),
                            entry("latestQueuedAt", formatVoteTimestamp(latest.getQueuedAt())),
                            entry("latestVoteAt", formatVoteTimestamp(latest.getTimestamp())),
                            entry("online", online));
                })
                .sorted((left, right) -> Integer.compare(((Number) right.get("count")).intValue(), ((Number) left.get("count")).intValue()))
                .toList();

        return mapOf(entry("total", voteManager.getPendingQueue().getTotalPending()), entry("players", players));
    }

    private Map<String, Object> buildTicketPayload() {
        List<Map<String, Object>> tickets = TicketManager.getInstance().snapshotOpenTickets().stream()
                .map(snapshot -> mapOf(
                        entry("channelId", snapshot.channelId()),
                        entry("player", snapshot.playerName()),
                        entry("cachedMessages", snapshot.cachedMessages()),
                        entry("previewLines", snapshot.previewLines())))
                .toList();
        return mapOf(entry("open", tickets.size()), entry("items", tickets));
    }

    private String formatVoteTimestamp(long epochMillis) {
        if (epochMillis <= 0) {
            return tr("Unknown", "NeznĂˇmĂ©");
        }
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(VOTE_TIME_FORMAT);
    }

    private long secondsUntilStatsReport(StatsConfig statsConfig) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = LocalDateTime.of(now.toLocalDate(), statsConfig.getReportTime());
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Math.max(0, Duration.between(now, next).getSeconds());
    }

    private Map<String, Object> module(String name, boolean enabled, String detail) {
        return mapOf(entry("name", name), entry("enabled", enabled), entry("detail", detail));
    }

    private List<String> buildAlerts() {
        List<String> alerts = new ArrayList<>();
        if (currentTps() < 18.5) {
            alerts.add(tr("TPS dropped below the healthy range.", "TPS kleslo pod zdravĂ˝ rozsah."));
        }
        if (buildMemoryUsagePercent() > 85.0) {
            alerts.add(tr("Memory usage is above 85%.", "VyuĹľitĂ­ pamÄ›ti je nad 85 %."));
        }
        if (DiscordManager.getInstance().getJda() == null && GeneralConfig.getInstance().isEnableDiscord()) {
            alerts.add(tr("Discord integration is enabled but not connected.", "Discord integrace je zapnutĂˇ, ale nenĂ­ pĹ™ipojenĂˇ."));
        }
        if (alerts.isEmpty()) {
            alerts.add(tr("All major systems look stable.", "VĹˇechny hlavnĂ­ systĂ©my vypadajĂ­ stabilnÄ›."));
        }
        return alerts;
    }

    private double buildMemoryUsagePercent() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        return maxMemory == 0 ? 0 : round((usedMemory * 100.0) / maxMemory);
    }

    private String resolveNextRestart() {
        RestartManager restartManager = Voidium.getInstance().getRestartManager();
        return restartManager == null ? tr("Not scheduled", "NenĂ­ naplĂˇnovĂˇno") : restartManager.getNextRestartInfo();
    }

    private double currentMspt() {
        MinecraftServer currentServer = server;
        if (currentServer == null) {
            return 0.0;
        }
        long[] tickTimes = currentServer.getTickTimesNanos();
        int sampleSize = Math.min(100, tickTimes.length);
        if (sampleSize == 0) {
            return 0.0;
        }
        double total = 0.0;
        for (int index = 0; index < sampleSize; index++) {
            total += tickTimes[index];
        }
        return total / sampleSize / 1_000_000.0;
    }

    private double currentTps() {
        double mspt = currentMspt();
        if (mspt <= 0.0) {
            return 20.0;
        }
        return Math.min(20.0, 1000.0 / mspt);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String formatDuration(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        return days + "d " + hours + "h " + minutes + "m";
    }

    private void reloadConfigsFromDisk() {
        try {
            var voidiumDir = FMLPaths.CONFIGDIR.get().resolve("voidium");
            cz.voidium.config.GeneralConfig.init(voidiumDir);
            cz.voidium.config.WebConfig.init(voidiumDir);
            cz.voidium.config.DiscordConfig.init(voidiumDir);
            cz.voidium.config.AnnouncementConfig.init(voidiumDir);
            cz.voidium.config.RestartConfig.init(voidiumDir);
            cz.voidium.config.RanksConfig.init(voidiumDir);
            cz.voidium.config.PlayerListConfig.init(voidiumDir);
            cz.voidium.config.StatsConfig.init(voidiumDir);
            cz.voidium.config.VoteConfig.init(voidiumDir);
            cz.voidium.config.TicketConfig.init(voidiumDir);
            cz.voidium.config.EntityCleanerConfig.init(voidiumDir);
            cz.voidium.config.AIConfig.init(voidiumDir);
        } catch (Exception e) {
            LOGGER.error("Failed to reload config singletons from disk", e);
        }
    }

    private byte[] readWebResource(String resourcePath) {
        // Try classpath first (production JAR), then fall back to disk (dev mode)
        try (InputStream is = WebManager.class.getResourceAsStream("/web/" + resourcePath)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (IOException ignored) {
        }
        // Dev fallback: read from src/main/resources/web/
        Path devPath = Path.of("src", "main", "resources", "web", resourcePath);
        if (Files.exists(devPath)) {
            try {
                return Files.readAllBytes(devPath);
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private String guessContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".ico")) return "image/x-icon";
        if (path.endsWith(".woff2")) return "font/woff2";
        if (path.endsWith(".woff")) return "font/woff";
        return "application/octet-stream";
    }

    private AIService.AdminRequest buildAdminRequest(JsonObject payload) {
        List<String> configFiles = new ArrayList<>();
        if (payload.has("configFiles") && payload.get("configFiles").isJsonArray()) {
            payload.getAsJsonArray("configFiles").forEach(item -> configFiles.add(item.getAsString()));
        }
        return new AIService.AdminRequest(
                server,
                payload.has("includeServer") && payload.get("includeServer").getAsBoolean(),
                payload.has("includePlayers") && payload.get("includePlayers").getAsBoolean(),
                payload.has("includeModules") && payload.get("includeModules").getAsBoolean(),
                payload.has("includeConfigs") && payload.get("includeConfigs").getAsBoolean(),
                configFiles,
                ADMIN_AI_CONVERSATION_ID);
    }

    private String normalizeCommand(String raw) {
        String command = raw == null ? "" : raw.trim();
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        return command;
    }

    private String uiLocale() {
        return WebConfig.getInstance() != null && "cz".equalsIgnoreCase(WebConfig.getInstance().getLanguage()) ? "cz" : "en";
    }

    private String tr(String english, String czech) {
        return "cz".equals(uiLocale()) ? czech : english;
    }

    private boolean isAllowedConsoleCommand(String command) {
        if (command.isBlank() || command.length() > 160 || command.contains("\n") || command.contains("\r")) {
            return false;
        }
        String root = command.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return resolveConsoleCommandFamily(root) != null;
    }

    private String resolveConsoleCommandFamily(String root) {
        if (Set.of("say", "me", "msg", "tell", "tellraw", "title").contains(root)) {
            return "messaging";
        }
        if (Set.of("tp", "teleport", "spreadplayers").contains(root)) {
            return "teleport";
        }
        if (Set.of("time", "weather", "gamerule", "difficulty", "list", "playsound").contains(root)) {
            return "server-utility";
        }
        if (Set.of("give", "clear", "effect", "xp", "experience", "kick", "ban", "pardon", "banlist").contains(root)) {
            return "player-control";
        }
        if ("voidium".equals(root)) {
            return "voidium-admin";
        }
        return null;
    }

    private Map.Entry<String, Object> entry(String key, Object value) {
        return Map.entry(key, value);
    }

    @SafeVarargs
    private final Map<String, Object> mapOf(Map.Entry<String, Object>... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private HttpHandler rateLimited(HttpHandler delegate) {
        return exchange -> {
            if (handleRateLimit(exchange)) return;
            delegate.handle(exchange);
        };
    }

    private boolean handleRateLimit(HttpExchange exchange) throws IOException {
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        long now = System.currentTimeMillis();
        long[] bucket = rateLimits.compute(ip, (k, v) -> {
            if (v == null || now - v[1] > RATE_LIMIT_WINDOW_MS) return new long[]{1, now};
            v[0]++;
            return v;
        });
        if (bucket[0] > RATE_LIMIT_MAX_REQUESTS) {
            exchange.getResponseHeaders().set("Retry-After", "60");
            sendJson(exchange, 429, Map.of("message", "Rate limit exceeded. Try again later."));
            return true;
        }
        return false;
    }

    private final class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }
            if (isAuthenticated(exchange)) {
                URI uri = exchange.getRequestURI();
                if (uri.getRawQuery() != null && uri.getRawQuery().contains("token=")) {
                    redirect(exchange, "/");
                    return;
                }
                byte[] data = readWebResource("index.html");
                if (data != null) {
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, data.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(data);
                    }
                } else {
                    sendText(exchange, 500, "text/plain", "index.html not found");
                }
                return;
            }
            sendText(exchange, 401, "text/html", "<html><body style=\"font-family:Segoe UI,sans-serif;background:#07131b;color:#edfafd;padding:40px\"><h1>Voidium Web access required</h1><p>Use <strong>/voidium web</strong> on the server or supply a valid admin token.</p></body></html>");
        }
    }

    private final class StaticAssetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            // Sanitize: only allow paths under /assets/ with no traversal
            if (path.contains("..") || !path.startsWith("/assets/")) {
                sendText(exchange, 400, "text/plain", "Bad request");
                return;
            }
            String resourcePath = path.substring(1); // strip leading /
            byte[] data = readWebResource(resourcePath);
            if (data == null) {
                sendText(exchange, 404, "text/plain", "Not found");
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", guessContentType(path));
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=31536000, immutable");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }
    }

    private final class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            sendJson(exchange, 200, buildDashboardPayload());
        }
    }

    private final class FeedsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            sendJson(exchange, 200, Map.of("chatFeed", ChatHistoryManager.getInstance().getEntries(), "consoleFeed", WebConsoleFeed.getInstance().snapshot()));
        }
    }

    private final class ActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("message", "Method Not Allowed"));
                return;
            }

            Map<String, String> params = parseParams(readRequestBody(exchange));
            String action = params.getOrDefault("action", "").toLowerCase(Locale.ROOT);
            try {
                switch (action) {
                    case "announce" -> {
                        String message = params.getOrDefault("message", "").trim();
                        if (message.isEmpty()) {
                            sendJson(exchange, 400, Map.of("message", "Announcement message is required."));
                            return;
                        }
                        AnnouncementManager manager = Voidium.getInstance().ensureAnnouncementManager(server);
                        if (manager == null) {
                            sendJson(exchange, 400, Map.of("message", "Announcement manager is not available."));
                            return;
                        }
                        server.execute(() -> manager.broadcastMessage(message));
                        sendJson(exchange, 200, Map.of("message", "Announcement sent."));
                    }
                    case "restart" -> {
                        RestartManager manager = Voidium.getInstance().getRestartManager();
                        if (manager == null) {
                            sendJson(exchange, 400, Map.of("message", "Restart manager is not available."));
                            return;
                        }
                        manager.scheduleManualRestart(5);
                        sendJson(exchange, 200, Map.of("message", "Restart scheduled in 5 minutes."));
                    }
                    case "reload" -> {
                        if (server == null) {
                            sendJson(exchange, 500, Map.of("message", "Server is not attached."));
                            return;
                        }
                        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "voidium reload"));
                        sendJson(exchange, 200, Map.of("message", "Reload requested."));
                    }
                    case "entitycleaner_preview" -> {
                        var cleaner = Voidium.getInstance().getEntityCleaner();
                        if (cleaner == null) {
                            sendJson(exchange, 400, Map.of("message", tr("EntityCleaner is not available.", "EntityCleaner nenĂ­ dostupnĂ˝.")));
                            return;
                        }
                        var result = cleaner.previewCleanup();
                        sendJson(exchange, 200, Map.of(
                                "message", formatEntityCleanerPreview(result),
                                "result", mapOf(
                                        entry("items", result.items),
                                        entry("mobs", result.mobs),
                                        entry("xpOrbs", result.xpOrbs),
                                        entry("arrows", result.arrows),
                                        entry("total", result.total()))));
                    }
                    case "entitycleaner_all" -> sendEntityCleanerRun(exchange, true, true, true, true);
                    case "entitycleaner_items" -> sendEntityCleanerRun(exchange, true, false, false, false);
                    case "entitycleaner_mobs" -> sendEntityCleanerRun(exchange, false, true, false, false);
                    case "entitycleaner_xp" -> sendEntityCleanerRun(exchange, false, false, true, false);
                    case "entitycleaner_arrows" -> sendEntityCleanerRun(exchange, false, false, false, true);
                    case "vote_clear_all" -> {
                        VoteManager manager = Voidium.getInstance().getVoteManager();
                        if (manager == null || manager.getPendingQueue() == null) {
                            sendJson(exchange, 400, Map.of("message", tr("Vote queue is not available.", "Vote queue nenĂ­ dostupnĂˇ.")));
                            return;
                        }
                        int total = manager.getPendingQueue().getTotalPending();
                        manager.getPendingQueue().clear();
                        sendJson(exchange, 200, Map.of("message", tr("Cleared ", "SmazĂˇno ") + total + tr(" pending vote(s).", " ÄŤekajĂ­cĂ­ch hlasĹŻ.")));
                    }
                    case "vote_payout_player" -> {
                        String player = params.getOrDefault("player", "").trim();
                        if (player.isEmpty()) {
                            sendJson(exchange, 400, Map.of("message", tr("Player is required.", "HrĂˇÄŤ je povinnĂ˝.")));
                            return;
                        }
                        VoteManager manager = Voidium.getInstance().getVoteManager();
                        if (manager == null || manager.getPendingQueue() == null) {
                            sendJson(exchange, 400, Map.of("message", tr("Vote queue is not available.", "Vote queue nenĂ­ dostupnĂˇ.")));
                            return;
                        }
                        int delivered = manager.deliverPendingVotesForPlayer(player);
                        if (delivered < 0) {
                            sendJson(exchange, 400, Map.of("message", tr("Player must be online for manual payout.", "HrĂˇÄŤ musĂ­ bĂ˝t online pro ruÄŤnĂ­ payout.")));
                            return;
                        }
                        sendJson(exchange, 200, Map.of("message", delivered == 0
                                ? tr("No pending votes for that player.", "Tento hrĂˇÄŤ nemĂˇ ĹľĂˇdnĂ© ÄŤekajĂ­cĂ­ hlasy.")
                                : tr("Queued payout for ", "NaplĂˇnovĂˇn payout pro ") + player + tr(": ", ": ") + delivered + tr(" vote(s).", " hlasĹŻ.")));
                    }
                    case "vote_clear_player" -> {
                        String player = params.getOrDefault("player", "").trim();
                        if (player.isEmpty()) {
                            sendJson(exchange, 400, Map.of("message", tr("Player is required.", "HrĂˇÄŤ je povinnĂ˝.")));
                            return;
                        }
                        VoteManager manager = Voidium.getInstance().getVoteManager();
                        if (manager == null || manager.getPendingQueue() == null) {
                            sendJson(exchange, 400, Map.of("message", tr("Vote queue is not available.", "Vote queue nenĂ­ dostupnĂˇ.")));
                            return;
                        }
                        int removed = manager.clearPendingVotesForPlayer(player);
                        sendJson(exchange, 200, Map.of("message", removed == 0
                                ? tr("No pending votes removed.", "Nebyly odebrĂˇny ĹľĂˇdnĂ© ÄŤekajĂ­cĂ­ hlasy.")
                                : tr("Removed ", "OdebrĂˇno ") + removed + tr(" pending vote(s) for ", " ÄŤekajĂ­cĂ­ch hlasĹŻ pro ") + player + "."));
                    }
                    case "player_kick" -> {
                        if (server == null) {
                            sendJson(exchange, 500, Map.of("message", tr("Server is not attached.", "Server není připojen.")));
                            return;
                        }
                        String playerName = params.getOrDefault("player", "").trim();
                        String reason = params.getOrDefault("reason", tr("Removed by web panel", "Odebrán přes web panel")).trim();
                        if (playerName.isEmpty()) {
                            sendJson(exchange, 400, Map.of("message", tr("Player is required.", "Hráč je povinný.")));
                            return;
                        }
                        if (server.getPlayerList().getPlayerByName(playerName) == null) {
                            sendJson(exchange, 400, Map.of("message", tr("Player must be online for kick.", "Hráč musí být online pro kick.")));
                            return;
                        }
                        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kick " + playerName + " " + reason));
                        sendJson(exchange, 200, Map.of("message", tr("Kick requested for ", "Kick byl vyzadan pro ") + playerName + "."));
                    }
                    case "player_ban" -> {
                        if (server == null) {
                            sendJson(exchange, 500, Map.of("message", tr("Server is not attached.", "Server není připojen.")));
                            return;
                        }
                        String playerName = params.getOrDefault("player", "").trim();
                        String reason = params.getOrDefault("reason", tr("Banned by web panel", "Zabanován přes web panel")).trim();
                        if (playerName.isEmpty()) {
                            sendJson(exchange, 400, Map.of("message", tr("Player is required.", "Hráč je povinný.")));
                            return;
                        }
                        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "ban " + playerName + " " + reason));
                        sendJson(exchange, 200, Map.of("message", tr("Ban requested for ", "Ban byl vyzadan pro ") + playerName + "."));
                    }
                    case "player_unlink" -> {
                        String uuid = params.getOrDefault("uuid", "").trim();
                        String playerName = params.getOrDefault("player", "").trim();
                        if (uuid.isEmpty()) {
                            sendJson(exchange, 400, Map.of("message", tr("Player UUID is required.", "UUID hráče je povinné.")));
                            return;
                        }
                        LinkManager.getInstance().unlink(UUID.fromString(uuid));
                        sendJson(exchange, 200, Map.of("message", tr("Discord link removed for ", "Discord propojení odstraněno pro ") + (playerName.isBlank() ? uuid : playerName) + "."));
                    }
                    case "ticket_note" -> {
                        String channelId = params.getOrDefault("channelId", "").trim();
                        String message = params.getOrDefault("message", "").trim();
                        if (channelId.isEmpty() || message.isEmpty()) {
                            sendJson(exchange, 400, Map.of("message", tr("Ticket channel and message are required.", "Ticket channel a zprĂˇva jsou povinnĂ©.")));
                            return;
                        }
                        TicketManager.getInstance().sendWebNote(channelId, message);
                        sendJson(exchange, 200, Map.of("message", tr("Ticket note sent.", "PoznĂˇmka do ticketu byla odeslĂˇna.")));
                    }
                    case "ticket_close" -> {
                        String channelId = params.getOrDefault("channelId", "").trim();
                        if (channelId.isEmpty()) {
                            sendJson(exchange, 400, Map.of("message", tr("Ticket channel is required.", "Ticket channel je povinnĂ˝.")));
                            return;
                        }
                        TicketManager.getInstance().closeTicketFromWeb(channelId);
                        sendJson(exchange, 200, Map.of("message", tr("Ticket close requested.", "Uzavreni ticketu bylo vyzadano.")));
                    }
                    case "maintenance_on" -> {
                        GeneralConfig.getInstance().setMaintenanceMode(true);
                        GeneralConfig.getInstance().save();
                        sendJson(exchange, 200, Map.of("message", tr("Maintenance mode enabled.", "ReĹľim ĂşdrĹľby zapnutĂ˝.")));
                    }
                    case "maintenance_off" -> {
                        GeneralConfig.getInstance().setMaintenanceMode(false);
                        GeneralConfig.getInstance().save();
                        sendJson(exchange, 200, Map.of("message", tr("Maintenance mode disabled.", "ReĹľim ĂşdrĹľby vypnutĂ˝.")));
                    }
                    case "vote_payout_all_online" -> {
                        VoteManager manager = Voidium.getInstance().getVoteManager();
                        if (manager == null || manager.getPendingQueue() == null) {
                            sendJson(exchange, 400, Map.of("message", tr("Vote queue is not available.", "Vote queue nenĂ­ dostupnĂˇ.")));
                            return;
                        }
                        int delivered = manager.deliverAllOnlinePending();
                        sendJson(exchange, 200, Map.of("message", delivered == 0
                                ? tr("No pending votes to deliver.", "Ĺ˝ĂˇdnĂ© ÄŤekajĂ­cĂ­ hlasy k doruÄŤenĂ­.")
                                : tr("Delivered ", "DoruÄŤeno ") + delivered + tr(" vote(s) to all online players.", " hlasĹŻ vĹˇem online hrĂˇÄŤĹŻm.")));
                    }
                    case "entitycleaner_preview_dimensions" -> {
                        var cleaner = Voidium.getInstance().getEntityCleaner();
                        if (cleaner == null) {
                            sendJson(exchange, 400, Map.of("message", tr("EntityCleaner is not available.", "EntityCleaner nenĂ­ dostupnĂ˝.")));
                            return;
                        }
                        var byDim = cleaner.previewCleanupByDimension();
                        Map<String, Object> dimMap = new LinkedHashMap<>();
                        for (var e : byDim.entrySet()) {
                            dimMap.put(e.getKey(), mapOf(
                                    entry("items", e.getValue().items),
                                    entry("mobs", e.getValue().mobs),
                                    entry("xpOrbs", e.getValue().xpOrbs),
                                    entry("arrows", e.getValue().arrows),
                                    entry("total", e.getValue().total())));
                        }
                        sendJson(exchange, 200, Map.of("dimensions", dimMap));
                    }
                    case "ticket_transcript" -> {
                        String channelId = params.getOrDefault("channelId", "").trim();
                        if (channelId.isEmpty()) {
                            sendJson(exchange, 400, Map.of("message", tr("Ticket channel is required.", "Ticket channel je povinnĂ˝.")));
                            return;
                        }
                        var lines = TicketManager.getInstance().getTranscriptLines(channelId);
                        String player = TicketManager.getInstance().getPlayerNameForTicket(channelId);
                        sendJson(exchange, 200, Map.of("player", player != null ? player : "unknown", "lines", lines));
                    }
                    default -> sendJson(exchange, 400, Map.of("message", "Unknown action: " + action));
                }
            } catch (Exception e) {
                LOGGER.error("Web action failed: {}", action, e);
                sendJson(exchange, 500, Map.of("message", "Action failed: " + e.getMessage()));
            }
        }
    }

    private void sendEntityCleanerRun(HttpExchange exchange, boolean items, boolean mobs, boolean xp, boolean arrows) throws IOException {
        var cleaner = Voidium.getInstance().getEntityCleaner();
        if (cleaner == null) {
            sendJson(exchange, 400, Map.of("message", tr("EntityCleaner is not available.", "EntityCleaner nenĂ­ dostupnĂ˝.")));
            return;
        }
        var result = cleaner.forceCleanup(items, mobs, xp, arrows);
        sendJson(exchange, 200, Map.of(
                "message", formatEntityCleanerRun(result),
                "result", mapOf(
                        entry("items", result.items),
                        entry("mobs", result.mobs),
                        entry("xpOrbs", result.xpOrbs),
                        entry("arrows", result.arrows),
                        entry("total", result.total()))));
    }

    private String formatEntityCleanerPreview(cz.voidium.entitycleaner.EntityCleaner.CleanupResult result) {
        return tr("Would clear: ", "VyÄŤistilo by se: ")
                + result.items + tr(" items, ", " itemĹŻ, ")
                + result.mobs + tr(" mobs, ", " mobĹŻ, ")
                + result.xpOrbs + tr(" XP orbs, ", " XP orbĹŻ, ")
                + result.arrows + tr(" arrows", " ĹˇĂ­pĹŻ")
                + ". " + tr("Total: ", "Celkem: ") + result.total();
    }

    private String formatEntityCleanerRun(cz.voidium.entitycleaner.EntityCleaner.CleanupResult result) {
        return tr("Cleared: ", "VyÄŤiĹˇtÄ›no: ")
                + result.items + tr(" items, ", " itemĹŻ, ")
                + result.mobs + tr(" mobs, ", " mobĹŻ, ")
                + result.xpOrbs + tr(" XP orbs, ", " XP orbĹŻ, ")
                + result.arrows + tr(" arrows", " ĹˇĂ­pĹŻ")
                + ". " + tr("Total: ", "Celkem: ") + result.total();
    }

    private final class AdminAiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("message", "Method Not Allowed"));
                return;
            }
            try {
                JsonObject payload = readJsonBody(exchange);
                if (payload == null) {
                    sendJson(exchange, 400, Map.of("message", "Missing JSON payload."));
                    return;
                }
                String answer = AIService.getInstance().askAdmin(payload.has("message") ? payload.get("message").getAsString() : "", buildAdminRequest(payload));
                WebAuditLog.getInstance().record("ai.admin.ask", payload.has("message") ? payload.get("message").getAsString() : "", String.valueOf(exchange.getRemoteAddress()), true);
                sendJson(exchange, 200, Map.of("answer", answer));
            } catch (Exception e) {
                LOGGER.error("Admin AI request failed", e);
                WebAuditLog.getInstance().record("ai.admin.ask", e.getMessage(), String.valueOf(exchange.getRemoteAddress()), false);
                sendJson(exchange, 500, Map.of("message", e.getMessage()));
            }
        }
    }

    private final class AdminAiSuggestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("message", "Method Not Allowed"));
                return;
            }
            try {
                JsonObject payload = readJsonBody(exchange);
                if (payload == null) {
                    sendJson(exchange, 400, Map.of("message", "Missing JSON payload."));
                    return;
                }
                Map<String, Object> aiResult = AIService.getInstance().suggestAdminChanges(payload.has("message") ? payload.get("message").getAsString() : "", buildAdminRequest(payload));
                Map<String, Object> result = new LinkedHashMap<>(aiResult);
                result.putAll(ConfigStudioService.getInstance().stageAiSuggestion(GSON.toJsonTree(aiResult).getAsJsonObject()));
                WebAuditLog.getInstance().record("ai.admin.suggest", payload.has("message") ? payload.get("message").getAsString() : "", String.valueOf(exchange.getRemoteAddress()), true);
                sendJson(exchange, 200, result);
            } catch (Exception e) {
                LOGGER.error("Admin AI suggestion request failed", e);
                WebAuditLog.getInstance().record("ai.admin.suggest", e.getMessage(), String.valueOf(exchange.getRemoteAddress()), false);
                sendJson(exchange, 500, Map.of("message", e.getMessage()));
            }
        }
    }

    private final class AiPlayerHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            sendJson(exchange, 200, AIService.getInstance().getPlayerConversations());
        }
    }

    private final class ConfigSchemaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            sendJson(exchange, 200, ConfigStudioService.getInstance().getSchema());
        }
    }

    private final class ConfigValuesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            sendJson(exchange, 200, ConfigStudioService.getInstance().getValues());
        }
    }

    private final class ConfigDefaultsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("message", "Method Not Allowed"));
                return;
            }
            sendJson(exchange, 200, ConfigStudioService.getInstance().restoreDefaults(readJsonBody(exchange)));
        }
    }

    private final class ConfigLocaleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("message", "Method Not Allowed"));
                return;
            }
            sendJson(exchange, 200, ConfigStudioService.getInstance().applyLocalePreset(readJsonBody(exchange)));
        }
    }

    private final class ConfigPreviewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("message", "Method Not Allowed"));
                return;
            }
            sendJson(exchange, 200, ConfigStudioService.getInstance().preview(readJsonBody(exchange)));
        }
    }

    private final class ConfigDiffHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("message", "Method Not Allowed"));
                return;
            }
            sendJson(exchange, 200, ConfigStudioService.getInstance().previewDiff(readJsonBody(exchange)));
        }
    }

    private final class ConfigApplyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("message", "Method Not Allowed"));
                return;
            }
            try {
                JsonObject payload = readJsonBody(exchange);
                Map<String, Object> result = ConfigStudioService.getInstance().apply(payload);
                String source = payload != null && payload.has("source") ? payload.get("source").getAsString() : "manual";
                WebAuditLog.getInstance().record("config.apply", source + ": " + String.valueOf(result.get("message")), String.valueOf(exchange.getRemoteAddress()), Boolean.TRUE.equals(result.get("applied")));
                sendJson(exchange, 200, result);
            } catch (Exception e) {
                LOGGER.error("Failed to apply config changes", e);
                WebAuditLog.getInstance().record("config.apply", e.getMessage(), String.valueOf(exchange.getRemoteAddress()), false);
                sendJson(exchange, 500, Map.of("message", e.getMessage()));
            }
        }
    }

    private final class ConfigRollbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("message", "Method Not Allowed"));
                return;
            }
            try {
                Map<String, Object> result = ConfigStudioService.getInstance().rollbackLatest();
                reloadConfigsFromDisk();
                WebAuditLog.getInstance().record("config.rollback", String.valueOf(result.get("message")), String.valueOf(exchange.getRemoteAddress()), Boolean.TRUE.equals(result.get("rolledBack")));
                sendJson(exchange, 200, result);
            } catch (Exception e) {
                LOGGER.error("Failed to rollback config changes", e);
                WebAuditLog.getInstance().record("config.rollback", e.getMessage(), String.valueOf(exchange.getRemoteAddress()), false);
                sendJson(exchange, 500, Map.of("message", e.getMessage()));
            }
        }
    }

    private final class ConfigReloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("message", "Method Not Allowed"));
                return;
            }
            if (server == null) {
                sendJson(exchange, 500, Map.of("message", "Server is not attached."));
                return;
            }
            server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "voidium reload"));
            WebAuditLog.getInstance().record("config.reload", "voidium reload", String.valueOf(exchange.getRemoteAddress()), true);
            sendJson(exchange, 200, Map.of("message", "Reload requested."));
        }
    }

    private final class ConsoleExecuteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("message", "Method Not Allowed"));
                return;
            }
            if (server == null) {
                sendJson(exchange, 500, Map.of("message", "Server is not attached."));
                return;
            }

            JsonObject payload = readJsonBody(exchange);
            String command = normalizeCommand(payload != null && payload.has("command") ? payload.get("command").getAsString() : "");
            String family = resolveConsoleCommandFamily(command.split("\\s+", 2)[0].toLowerCase(Locale.ROOT));
            if (!isAllowedConsoleCommand(command)) {
                WebAuditLog.getInstance().record("console.execute", command, String.valueOf(exchange.getRemoteAddress()), false);
                sendJson(exchange, 400, Map.of("message", "Command blocked by web console safety policy."));
                return;
            }

            server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
            WebAuditLog.getInstance().record("console.execute", family + ": " + command, String.valueOf(exchange.getRemoteAddress()), true);
            sendJson(exchange, 200, Map.of("message", "Command queued [" + family + "]: " + command));
        }
    }

    private final class DiscordRolesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            sendJson(exchange, 200, Map.of("roles", DiscordManager.getInstance().getGuildRolesData()));
        }
    }

    private final class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
            if (cookieHeader != null) {
                for (String cookie : cookieHeader.split(";")) {
                    String[] parts = cookie.trim().split("=", 2);
                    if (parts.length == 2 && SESSION_COOKIE.equals(parts[0])) {
                        sessions.remove(parts[1]);
                    }
                }
            }
            exchange.getResponseHeaders().add("Set-Cookie", SESSION_COOKIE + "=; Max-Age=0; HttpOnly; SameSite=Lax; Path=/");
            sendJson(exchange, 200, Map.of("message", "Logged out"));
        }
    }

    private final class SseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            try {
                writeSseEvent(os, "dashboard", GSON.toJson(buildDashboardPayload()));
                while (httpServer != null) {
                    Thread.sleep(3000);
                    writeSseEvent(os, "dashboard", GSON.toJson(buildDashboardPayload()));
                }
            } catch (InterruptedException | IOException e) {
                // Client disconnected
            } finally {
                try { os.close(); } catch (IOException ignored) {}
            }
        }

        private void writeSseEvent(OutputStream os, String event, String data) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("event: ").append(event).append('\n');
            for (String line : data.split("\n", -1)) {
                sb.append("data: ").append(line).append('\n');
            }
            sb.append('\n');
            os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    private final class ConfigSchemaExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            sendJson(exchange, 200, ConfigStudioService.getInstance().exportJsonSchema());
        }
    }

    private final class ServerIconHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendText(exchange, 401, "text/plain", "Unauthorized");
                return;
            }
            Path iconPath = Path.of("server-icon.png");
            if (!Files.exists(iconPath)) {
                sendText(exchange, 404, "text/plain", "No server icon found");
                return;
            }
            byte[] data = Files.readAllBytes(iconPath);
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=300");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }
    }

    private final class ServerPropertiesHandler implements HttpHandler {
        private static final Set<String> SENSITIVE_KEYS = Set.of("rcon.password", "server-ip");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                return;
            }
            Path propsPath = Path.of("server.properties");
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                if (!Files.exists(propsPath)) {
                    sendJson(exchange, 404, Map.of("message", "server.properties not found"));
                    return;
                }
                Map<String, String> props = new LinkedHashMap<>();
                for (String line : Files.readAllLines(propsPath, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    int eq = trimmed.indexOf('=');
                    if (eq > 0) {
                        String key = trimmed.substring(0, eq).trim();
                        String value = trimmed.substring(eq + 1).trim();
                        props.put(key, SENSITIVE_KEYS.contains(key) ? "***" : value);
                    }
                }
                sendJson(exchange, 200, Map.of("properties", props));
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                JsonObject payload = readJsonBody(exchange);
                if (payload == null || !payload.has("properties")) {
                    sendJson(exchange, 400, Map.of("message", "Missing properties payload."));
                    return;
                }
                if (!Files.exists(propsPath)) {
                    sendJson(exchange, 404, Map.of("message", "server.properties not found"));
                    return;
                }
                Map<String, String> changes = new LinkedHashMap<>();
                payload.getAsJsonObject("properties").entrySet().forEach(e -> {
                    if (!SENSITIVE_KEYS.contains(e.getKey())) {
                        changes.put(e.getKey(), e.getValue().getAsString());
                    }
                });
                List<String> lines = Files.readAllLines(propsPath, StandardCharsets.UTF_8);
                List<String> output = new ArrayList<>();
                Set<String> written = new java.util.HashSet<>();
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        int eq = trimmed.indexOf('=');
                        if (eq > 0) {
                            String key = trimmed.substring(0, eq).trim();
                            if (changes.containsKey(key)) {
                                output.add(key + "=" + changes.get(key));
                                written.add(key);
                                continue;
                            }
                        }
                    }
                    output.add(line);
                }
                for (var entry : changes.entrySet()) {
                    if (!written.contains(entry.getKey())) {
                        output.add(entry.getKey() + "=" + entry.getValue());
                    }
                }
                Path backup = propsPath.resolveSibling("server.properties.bak");
                Files.copy(propsPath, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Files.write(propsPath, output, StandardCharsets.UTF_8);
                WebAuditLog.getInstance().record("server-properties.edit", changes.size() + " properties changed", String.valueOf(exchange.getRemoteAddress()), true);
                sendJson(exchange, 200, Map.of("message", tr("server.properties updated. Restart the server to apply changes.", "server.properties aktualizován. Restartujte server pro aplikaci změn."), "changedKeys", new ArrayList<>(changes.keySet())));
                return;
            }
            sendJson(exchange, 405, Map.of("message", "Method Not Allowed"));
        }
    }
}
