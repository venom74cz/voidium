package cz.voidium.skin;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple persistent skin cache: maps lowercase player name -> CachedEntry (uuid, value, signature, timestamp).
 * Stored as JSON at config/voidium/skin-cache.json.
 */
public final class SkinCache {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, CachedEntry> CACHE = new ConcurrentHashMap<>();
    private static Path filePath;
    private static long maxAgeSeconds = 86400; // 24h default
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("voidium-skincache");

    private SkinCache() {}

    public static void init(Path configDir) {
        try {
            Path dir = configDir.resolve("voidium");
            Files.createDirectories(dir);
            filePath = dir.resolve("skin-cache.json");
            load();
        } catch (IOException e) {
            LOGGER.warn("SkinCache init failed: {}", e.getMessage());
        }
    }

    public static void setMaxAgeSeconds(long seconds) { maxAgeSeconds = seconds; }

    public static CachedEntry get(String name) {
        CachedEntry ce = CACHE.get(name.toLowerCase());
        if (ce == null) return null;
        if (Instant.now().getEpochSecond() - ce.cachedAt > maxAgeSeconds) {
            CACHE.remove(name.toLowerCase());
            return null;
        }
        return ce;
    }

    public static void put(String name, UUID uuid, String value, String signature) {
        CACHE.put(name.toLowerCase(), new CachedEntry(uuid, value, signature, Instant.now().getEpochSecond()));
        saveAsync();
    }

    private static void load() {
        if (filePath == null || !Files.exists(filePath)) return;
        try (Reader r = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(r);
            if (!root.isJsonObject()) return;
            JsonObject obj = root.getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                try {
                    JsonObject ce = e.getValue().getAsJsonObject();
                    UUID uuid = UUID.fromString(ce.get("uuid").getAsString());
                    String value = ce.get("value").getAsString();
                    String sig = ce.get("signature").getAsString();
                    long ts = ce.get("cachedAt").getAsLong();
                    CACHE.put(e.getKey().toLowerCase(), new CachedEntry(uuid, value, sig, ts));
                } catch (Exception ignore) {}
            }
            LOGGER.info("SkinCache loaded entries={}", CACHE.size());
        } catch (Exception ex) {
            LOGGER.warn("SkinCache load failed: {}", ex.getMessage());
        }
    }

    private static void saveAsync() {
        // Lightweight fire-and-forget thread, infrequent writes.
        new Thread(SkinCache::save, "Voidium-SkinCache-Save").start();
    }

    private static synchronized void save() {
        if (filePath == null) return;
        JsonObject root = new JsonObject();
        for (Map.Entry<String, CachedEntry> e : CACHE.entrySet()) {
            CachedEntry ce = e.getValue();
            JsonObject o = new JsonObject();
            o.addProperty("uuid", ce.uuid.toString());
            o.addProperty("value", ce.value);
            o.addProperty("signature", ce.signature);
            o.addProperty("cachedAt", ce.cachedAt);
            root.add(e.getKey(), o);
        }
        try (Writer w = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        } catch (IOException e) {
            LOGGER.warn("SkinCache save failed: {}", e.getMessage());
        }
    }

    public record CachedEntry(UUID uuid, String value, String signature, long cachedAt) {}
}
