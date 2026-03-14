package cz.voidium.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.voidium.Voidium;
import cz.voidium.config.AIConfig;
import cz.voidium.config.GeneralConfig;
import cz.voidium.config.WebConfig;
import cz.voidium.discord.DiscordManager;
import cz.voidium.stats.StatsManager;
import cz.voidium.vote.VoteManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;

public class AIService {
    private static final Gson GSON = new GsonBuilder().create();
    private static final AIService INSTANCE = new AIService();
    private static final Set<String> REDACT_KEYS = Set.of(
            "apikey", "apiKey", "botToken", "sharedSecret", "adminToken", "token", "chatWebhookUrl");
    private static final int MAX_HISTORY_TURNS = 8;
    private static final Pattern BLOCKED_PATTERN = Pattern.compile(
            "(?i)(\\b(?:hack|exploit|dupe|cheat|crash\\s*server|ddos|dos\\s*attack|grief|xray|x-ray|bypass|injection|leaked|dump\\s*database)\\b)");
    private static final int MAX_RESPONSE_LENGTH = 4000;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Map<UUID, Instant> playerCooldowns = new ConcurrentHashMap<>();
        private final Map<String, List<ConversationTurn>> conversationHistory = new ConcurrentHashMap<>();

    private AIService() {
    }

    public static AIService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<String> askPlayer(ServerPlayer player, String message) {
        AIConfig config = AIConfig.getInstance();
        if (config == null || !config.isEnablePlayerChat()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Player AI chat is disabled."));
        }
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Message is empty."));
        }
        if (trimmed.length() > config.getPlayerPromptMaxLength()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Message is too long. Max " + config.getPlayerPromptMaxLength() + " characters."));
        }
        Instant until = playerCooldowns.get(player.getUUID());
        if (until != null && until.isAfter(Instant.now())) {
            long seconds = Duration.between(Instant.now(), until).toSeconds();
            return CompletableFuture.failedFuture(new IllegalStateException("AI cooldown active for " + seconds + " more seconds."));
        }
        playerCooldowns.put(player.getUUID(), Instant.now().plusSeconds(config.getPlayerCooldownSeconds()));

        if (BLOCKED_PATTERN.matcher(trimmed).find()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Your message was blocked by the moderation filter."));
        }

        String context = buildPlayerContext(player);
        String conversationId = "player:" + player.getUUID();
        return CompletableFuture.supplyAsync(() -> sendChatCompletion(config.getPlayerApi(), trimmed, context, conversationId), executor);
    }

    public String askAdmin(String message, AdminRequest request) {
        AIConfig config = AIConfig.getInstance();
        if (config == null || !config.isEnableAdminAssistant()) {
            throw new IllegalStateException("Admin AI assistant is disabled.");
        }
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Message is empty.");
        }
        if (trimmed.length() > config.getAdminPromptMaxLength()) {
            throw new IllegalArgumentException("Prompt is too long. Max " + config.getAdminPromptMaxLength() + " characters.");
        }
        String context = buildAdminContext(request, config);
        return sendChatCompletion(config.getAdminApi(), trimmed, context, request.conversationId());
    }

    public Map<String, Object> suggestAdminChanges(String message, AdminRequest request) {
        String augmentedPrompt = "Return JSON only with this structure: "
                + "{\"summary\":string,\"changes\":[{\"file\":string,\"path\":string,\"current\":string,\"proposed\":string,\"reason\":string}],\"warnings\":[string]}. "
                + "Focus on practical config improvements for Voidium based on the supplied context.\nUser request: " + message;
        String raw = sendChatCompletion(AIConfig.getInstance().getAdminApi(), augmentedPrompt,
                buildAdminContext(request, AIConfig.getInstance()), request.conversationId() + ":suggest");
        return normalizeSuggestionResponse(raw);
    }

    public Map<String, Object> getAdminCapabilities() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("voidium");
        List<String> configFiles = List.of();
        try {
            if (Files.exists(configDir)) {
                configFiles = Files.list(configDir)
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .sorted()
                        .collect(Collectors.toList());
            }
        } catch (IOException ignored) {
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", AIConfig.getInstance() != null && AIConfig.getInstance().isEnableAdminAssistant());
        data.put("configFiles", configFiles);
        data.put("redactsSensitiveValues", AIConfig.getInstance() != null && AIConfig.getInstance().isRedactSensitiveValues());
        data.put("history", getConversationSnapshot("admin:web"));
        return data;
    }

    public List<Map<String, Object>> getConversationSnapshot(String conversationId) {
        return conversationHistory.getOrDefault(conversationId, List.of()).stream()
                .sorted(Comparator.comparingLong(ConversationTurn::timestamp))
                .map(turn -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("role", turn.role());
                    row.put("content", turn.content());
                    row.put("timestamp", turn.timestamp());
                    return row;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getPlayerConversations() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> players = new ArrayList<>();
        for (Map.Entry<String, List<ConversationTurn>> entry : conversationHistory.entrySet()) {
            if (!entry.getKey().startsWith("player:")) continue;
            String uuid = entry.getKey().substring("player:".length());
            List<ConversationTurn> turns = entry.getValue();
            if (turns.isEmpty()) continue;
            Map<String, Object> playerData = new LinkedHashMap<>();
            playerData.put("uuid", uuid);
            playerData.put("turns", turns.size());
            playerData.put("lastActivity", turns.get(turns.size() - 1).timestamp());
            playerData.put("history", getConversationSnapshot(entry.getKey()));
            players.add(playerData);
        }
        players.sort((a, b) -> Long.compare((long) b.get("lastActivity"), (long) a.get("lastActivity")));
        result.put("conversations", players);
        return result;
    }

    private String buildPlayerContext(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return "Player is connected, but server context is unavailable.";
        }
        return "Player: " + player.getName().getString() + "\n"
                + "Dimension: " + player.level().dimension().location() + "\n"
                + "Online players: " + server.getPlayerCount() + "\n"
                + "TPS: " + StatsManager.getInstance().getHistory().stream().reduce((first, second) -> second)
                        .map(point -> point.tps).orElse(20.0);
    }

    private String buildAdminContext(AdminRequest request, AIConfig config) {
        List<String> parts = new ArrayList<>();
        MinecraftServer server = request.server();
        if (request.includeServer() && server != null) {
            parts.add(buildServerSnapshot(server));
        }
        if (request.includePlayers() && server != null) {
            parts.add(buildPlayerSnapshot(server));
        }
        if (request.includeModules()) {
            parts.add(buildModuleSnapshot());
        }
        if (request.includeConfigs()) {
            parts.add(buildConfigSnapshot(request.configFiles(), config));
        }
        String combined = String.join("\n\n", parts);
        if (combined.length() > config.getAdminContextMaxChars()) {
            return combined.substring(0, config.getAdminContextMaxChars()) + "\n\n[Context truncated due to limit]";
        }
        return combined;
    }

    private String buildServerSnapshot(MinecraftServer server) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        return "[Server Snapshot]\n"
                + "MOTD: " + server.getMotd() + "\n"
                + "Players: " + server.getPlayerCount() + "/" + server.getPlayerList().getMaxPlayers() + "\n"
                + "Memory: " + usedMemory + "MB / " + maxMemory + "MB\n"
                + "Web URL: " + WebConfig.getInstance().getPublicHostname() + ":" + WebConfig.getInstance().getPort();
    }

    private String buildPlayerSnapshot(MinecraftServer server) {
        String players = server.getPlayerList().getPlayers().stream()
                .map(player -> player.getName().getString() + " (ping " + player.connection.latency() + "ms)")
                .collect(Collectors.joining(", "));
        return "[Players]\n" + (players.isBlank() ? "No players online" : players);
    }

    private String buildModuleSnapshot() {
        VoteManager voteManager = Voidium.getInstance().getVoteManager();
        return "[Modules]\n"
                + "Discord: " + (DiscordManager.getInstance().getJda() != null ? "connected" : "offline") + "\n"
                + "Stats: " + (GeneralConfig.getInstance().isEnableStats() ? "enabled" : "disabled") + "\n"
                + "Vote: " + (voteManager != null && voteManager.getPendingQueue() != null
                        ? voteManager.getPendingQueue().getTotalPending() + " pending votes"
                        : "idle") + "\n"
                + "Web: " + (GeneralConfig.getInstance().isEnableWeb() ? "enabled" : "disabled");
    }

    private String buildConfigSnapshot(List<String> requestedFiles, AIConfig config) {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("voidium");
        List<String> selectedFiles = requestedFiles == null || requestedFiles.isEmpty()
                ? defaultConfigFiles(configDir)
                : requestedFiles;
        StringBuilder builder = new StringBuilder("[Config Snapshot]\n");
        for (String fileName : selectedFiles) {
            Path file = configDir.resolve(fileName).normalize();
            if (!file.startsWith(configDir) || !Files.exists(file) || !Files.isRegularFile(file)) {
                continue;
            }
            try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                if (config.isRedactSensitiveValues()) {
                    content = redactSecrets(content);
                }
                builder.append("--- ").append(fileName).append(" ---\n");
                builder.append(content).append("\n\n");
            } catch (IOException e) {
                builder.append("--- ").append(fileName).append(" ---\n");
                builder.append("[Unable to read file: ").append(e.getMessage()).append("]\n\n");
            }
        }
        return builder.toString();
    }

    private List<String> defaultConfigFiles(Path configDir) {
        try {
            if (!Files.exists(configDir)) {
                return List.of();
            }
            return Files.list(configDir)
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private String redactSecrets(String content) {
        String redacted = content;
        for (String key : REDACT_KEYS) {
            Pattern pattern = Pattern.compile("(?im)(\"" + Pattern.quote(key) + "\"\\s*:\\s*\")(.*?)(\")");
            redacted = pattern.matcher(redacted).replaceAll("$1***REDACTED***$3");
        }
        return redacted;
    }

    private String sendChatCompletion(AIConfig.EndpointProfile profile, String userPrompt, String context, String conversationId) {
        validateProfile(profile);
        JsonObject payload = new JsonObject();
        payload.addProperty("model", profile.getModel());
        payload.addProperty("temperature", profile.getTemperature());
        payload.addProperty("max_tokens", profile.getMaxTokens());
        JsonArray messages = new JsonArray();
        messages.add(message("system", profile.getSystemPrompt()));
        if (context != null && !context.isBlank()) {
            messages.add(message("system", "Context:\n" + context));
        }
        for (ConversationTurn turn : recentTurns(conversationId)) {
            messages.add(message(turn.role(), turn.content()));
        }
        messages.add(message("user", userPrompt));
        payload.add("messages", messages);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(profile.getEndpointUrl()))
                .timeout(Duration.ofSeconds(profile.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8));

        if (profile.getApiKey() != null && !profile.getApiKey().isBlank()
                && !"PUT_API_KEY_HERE".equals(profile.getApiKey())) {
            requestBuilder.header(profile.getAuthHeaderName(), profile.getAuthHeaderPrefix() + profile.getApiKey());
        }

        try {
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI API returned HTTP " + response.statusCode() + ": " + trim(response.body(), 400));
            }
            String answer = extractAssistantText(response.body());
            if (answer.length() > MAX_RESPONSE_LENGTH) {
                answer = answer.substring(0, MAX_RESPONSE_LENGTH) + "…";
            }
            rememberTurn(conversationId, "user", userPrompt);
            rememberTurn(conversationId, "assistant", answer);
            return answer;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI request failed: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("AI request failed: " + e.getMessage(), e);
        }
    }

    private List<ConversationTurn> recentTurns(String conversationId) {
        List<ConversationTurn> turns = conversationHistory.getOrDefault(conversationId, List.of());
        int fromIndex = Math.max(0, turns.size() - MAX_HISTORY_TURNS);
        return new ArrayList<>(turns.subList(fromIndex, turns.size()));
    }

    private void rememberTurn(String conversationId, String role, String content) {
        if (conversationId == null || conversationId.isBlank() || content == null || content.isBlank()) {
            return;
        }
        conversationHistory.compute(conversationId, (key, turns) -> {
            List<ConversationTurn> updated = turns == null ? new ArrayList<>() : new ArrayList<>(turns);
            updated.add(new ConversationTurn(role, trim(content, 4000), System.currentTimeMillis()));
            int overflow = updated.size() - (MAX_HISTORY_TURNS * 2);
            if (overflow > 0) {
                updated = new ArrayList<>(updated.subList(overflow, updated.size()));
            }
            return updated;
        });
    }

    private Map<String, Object> normalizeSuggestionResponse(String raw) {
        try {
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("summary", root.has("summary") ? root.get("summary").getAsString() : "No summary.");
            List<Map<String, String>> changes = new ArrayList<>();
            if (root.has("changes") && root.get("changes").isJsonArray()) {
                root.getAsJsonArray("changes").forEach(item -> {
                    if (!item.isJsonObject()) {
                        return;
                    }
                    JsonObject object = item.getAsJsonObject();
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("file", getJsonString(object, "file"));
                    row.put("path", getJsonString(object, "path"));
                    row.put("current", getJsonString(object, "current"));
                    row.put("proposed", getJsonString(object, "proposed"));
                    row.put("reason", getJsonString(object, "reason"));
                    changes.add(row);
                });
            }
            List<String> warnings = new ArrayList<>();
            if (root.has("warnings") && root.get("warnings").isJsonArray()) {
                root.getAsJsonArray("warnings").forEach(item -> warnings.add(item.getAsString()));
            }
            result.put("changes", changes);
            result.put("warnings", warnings);
            result.put("diffPreview", buildSyntheticDiff(changes));
            return result;
        } catch (Exception ignored) {
            return Map.of(
                    "summary", trim(raw, 1000),
                    "changes", List.of(),
                    "warnings", List.of("AI did not return valid JSON. Showing raw summary instead."),
                    "diffPreview", "No structured diff available.");
        }
    }

    private String buildSyntheticDiff(List<Map<String, String>> changes) {
        if (changes.isEmpty()) {
            return "No structured config changes suggested.";
        }
        StringBuilder builder = new StringBuilder();
        for (Map<String, String> change : changes) {
            builder.append(change.getOrDefault("file", "unknown")).append(" :: ")
                    .append(change.getOrDefault("path", "unknown")).append("\n")
                    .append("- ").append(change.getOrDefault("current", "(unknown) ")).append("\n")
                    .append("+ ").append(change.getOrDefault("proposed", "(none)")).append("\n\n");
        }
        return builder.toString().trim();
    }

    private String getJsonString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private void validateProfile(AIConfig.EndpointProfile profile) {
        if (profile == null) {
            throw new IllegalStateException("AI profile is missing.");
        }
        if (profile.getEndpointUrl() == null || profile.getEndpointUrl().isBlank()) {
            throw new IllegalStateException("AI endpoint URL is missing.");
        }
        if (profile.getModel() == null || profile.getModel().isBlank()) {
            throw new IllegalStateException("AI model is missing.");
        }
    }

    private JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content == null ? "" : content);
        return message;
    }

    private String extractAssistantText(String body) {
        JsonObject root = GSON.fromJson(body, JsonObject.class);
        if (root == null) {
            return "AI API returned an empty response.";
        }
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JsonObject first = choices.get(0).getAsJsonObject();
            JsonObject message = first.getAsJsonObject("message");
            if (message != null && message.has("content")) {
                return normalizeContent(message.get("content"));
            }
        }
        if (root.has("output_text")) {
            return root.get("output_text").getAsString();
        }
        return trim(body, 800);
    }

    private String normalizeContent(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (element.isJsonArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonObject() && item.getAsJsonObject().has("text")) {
                    builder.append(item.getAsJsonObject().get("text").getAsString());
                } else if (item.isJsonPrimitive()) {
                    builder.append(item.getAsString());
                }
            }
            return builder.toString();
        }
        return element.toString();
    }

    private String trim(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxChars ? value : value.substring(0, maxChars) + "...";
    }

    public record AdminRequest(
            MinecraftServer server,
            boolean includeServer,
            boolean includePlayers,
            boolean includeModules,
            boolean includeConfigs,
            List<String> configFiles,
            String conversationId) {
    }

        private record ConversationTurn(String role, String content, long timestamp) {
        }
}