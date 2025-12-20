package cz.voidium.server.chat;

import cz.voidium.discord.DiscordManager;
import cz.voidium.network.PacketSyncEmojis;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages syncing Discord server emojis to Minecraft clients.
 */
public class EmojiSyncService {
    private static EmojiSyncService instance;
    private Map<String, String> cachedEmojis = new HashMap<>();
    private long lastFetch = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

    private EmojiSyncService() {
    }

    public static synchronized EmojiSyncService getInstance() {
        if (instance == null) {
            instance = new EmojiSyncService();
        }
        return instance;
    }

    /**
     * Send emoji data to a player on login.
     */
    public void syncToPlayer(ServerPlayer player) {
        Map<String, String> emojis = getEmojis();
        if (!emojis.isEmpty()) {
            try {
                PacketDistributor.sendToPlayer(player, new PacketSyncEmojis(emojis));
            } catch (UnsupportedOperationException e) {
                // Client doesn't have Voidium mod installed, ignore
            }
        }
    }

    /**
     * Get cached emojis, refreshing if stale.
     */
    public Map<String, String> getEmojis() {
        long now = System.currentTimeMillis();
        if (cachedEmojis.isEmpty() || (now - lastFetch) > CACHE_DURATION) {
            refreshEmojis();
        }
        return cachedEmojis;
    }

    /**
     * Fetch emojis from Discord guild.
     */
    private void refreshEmojis() {
        try {
            var jda = DiscordManager.getInstance().getJda();
            if (jda == null)
                return;

            var guild = jda.getGuildById(cz.voidium.config.DiscordConfig.getInstance().getGuildId());
            if (guild == null)
                return;

            Map<String, String> newEmojis = new HashMap<>();

            // Get all custom emojis from the guild
            for (var emoji : guild.getEmojis()) {
                String name = emoji.getName();
                String url = emoji.getImageUrl();
                newEmojis.put(name, url);
            }

            // Add standard emojis (mapping common aliases to Twemoji URLs)
            // Dynamically fetch from iamcal/emoji-data
            fetchStandardEmojis(newEmojis);

            cachedEmojis = newEmojis;
            lastFetch = System.currentTimeMillis();

            System.out.println("[Voidium] Synced " + newEmojis.size() + " Discord emojis");
        } catch (Exception e) {
            System.err.println("[Voidium] Failed to sync Discord emojis: " + e.getMessage());
        }
    }

    private void fetchStandardEmojis(Map<String, String> map) {
        try {
            // Using a lighter version or a direct map would be faster, but parsing the
            // standard DB is most robust
            // We use a curated list or a transforming proxy if possible, but here we just
            // read the standard JSON
            // Source: https://raw.githubusercontent.com/iamcal/emoji-data/master/emoji.json
            // Note: This is a large file (10MB+). For a mod, we might want to cache this on
            // disk or use a smaller derived list.
            // For now, we'll try to use a slightly more optimized list provided by a CDN or
            // similar if available,
            // otherwise we stick to the plan. Let's use a simpler kv pair if possible.
            // Actually, let's parse the iamcal format, it's the standard.

            java.net.URL url = new java.net.URL(
                    "https://raw.githubusercontent.com/iamcal/emoji-data/master/emoji.json");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (java.io.InputStreamReader reader = new java.io.InputStreamReader(conn.getInputStream(),
                    java.nio.charset.StandardCharsets.UTF_8)) {
                com.google.gson.JsonElement root = com.google.gson.JsonParser.parseReader(reader);
                if (root.isJsonArray()) {
                    for (com.google.gson.JsonElement el : root.getAsJsonArray()) {
                        com.google.gson.JsonObject obj = el.getAsJsonObject();

                        if (!obj.has("short_name") || !obj.has("unified"))
                            continue;

                        String shortName = obj.get("short_name").getAsString();
                        String hex = obj.get("unified").getAsString().toLowerCase();

                        // Twemoji URL
                        String iconUrl = "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/" + hex
                                + ".png";
                        map.put(shortName, iconUrl);

                        // Also Add short_names array alternatives
                        if (obj.has("short_names")) {
                            for (com.google.gson.JsonElement alt : obj.getAsJsonArray("short_names")) {
                                map.put(alt.getAsString(), iconUrl);
                            }
                        }
                    }
                }
            }
            System.out.println("[Voidium] Fetched standard emoji library.");
        } catch (Exception e) {
            System.err.println("[Voidium] Failed to fetch standard emojis: " + e.getMessage());
            // Fallbacks
            map.put("smile", "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/1f604.png");
            map.put("heart", "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/2764.png");
        }
    }
}
