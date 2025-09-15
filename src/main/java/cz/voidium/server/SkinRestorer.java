package cz.voidium.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.voidium.config.GeneralConfig;
import net.minecraft.server.MinecraftServer;
import cz.voidium.skin.SkinCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Offline-mode skin restorer (post-login fallback) – now simplified because early mixin handles most cases.
 */
public class SkinRestorer {
    private final MinecraftServer server;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final ConcurrentMap<String, SkinData> skinCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> uuidCache = new ConcurrentHashMap<>();
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("voidium-skinrestorer");

    public SkinRestorer(MinecraftServer server) {
        this.server = server;
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        LOGGER.info("SkinRestorer enabled (offline mode skin fetching)");
    }

    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (server.isSingleplayerOwner(player.getGameProfile())) return;
        if (server.usesAuthentication()) return; // only offline
        if (!GeneralConfig.getInstance().isEnableSkinRestorer()) return;
        final String name = player.getGameProfile().getName();
        LOGGER.debug("SkinRestorer login event for {}", name);
        executor.submit(() -> fetchAndApply(player, name));
    }

    private void fetchAndApply(ServerPlayer player, String name) {
        try {
            // Already has textures (early mixin) – just ensure one ADD broadcast for any late clients.
            if (!player.getGameProfile().getProperties().get("textures").isEmpty()) {
                server.execute(() -> {
                    try {
                        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, player));
                    } catch (Exception ignored) {}
                });
                return;
            }

            // Try persistent cache first by player name
            SkinCache.CachedEntry disk = SkinCache.get(name);
            UUID officialUuid;
            SkinData skin;
            if (disk != null) {
                officialUuid = disk.uuid();
                skin = new SkinData(disk.value(), disk.signature());
                LOGGER.debug("Disk cache hit for {}", name);
            } else {
                officialUuid = uuidCache.computeIfAbsent(name.toLowerCase(), k -> {
                    try { return fetchOfficialUUID(name); } catch (IOException e) { return null; }
                });
                if (officialUuid == null) { LOGGER.debug("UUID not found for name={}", name); return; }
                skin = skinCache.computeIfAbsent(officialUuid.toString(), k -> {
                    try { return fetchSkin(officialUuid); } catch (IOException e) { return null; }
                });
                if (skin == null) { LOGGER.debug("Skin not found for uuid={}", officialUuid); return; }
                // Persist newly fetched skin
                SkinCache.put(name, officialUuid, skin.value, skin.signature);
            }

            server.execute(() -> {
                try {
                    var profile = player.getGameProfile();
                    int before = profile.getProperties().get("textures").size();
                    profile.getProperties().removeAll("textures");
                    profile.getProperties().put("textures", new com.mojang.authlib.properties.Property("textures", skin.value, skin.signature));
                    int after = profile.getProperties().get("textures").size();
                    LOGGER.debug("Applied textures (before={}, after={}) for {}", before, after, name);
                    // Minimal refresh: remove then add once.
                    try { server.getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(java.util.List.of(player.getUUID()))); } catch (Exception ignore) {}
                    server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, player));
                } catch (Exception e) {
                    LOGGER.warn("Failed applying skin for {}: {}", name, e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.warn("Skin restore error for {}: {}", name, e.getMessage());
        }
    }

    private UUID fetchOfficialUUID(String name) throws IOException {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + name;
        String json = httpGet(url);
        if (json == null || json.isEmpty()) return null;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (!obj.has("id")) return null;
        String raw = obj.get("id").getAsString();
        return UUID.fromString(raw.replaceFirst(
                "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                "$1-$2-$3-$4-$5"));
    }

    private SkinData fetchSkin(UUID uuid) throws IOException {
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false";
        String json = httpGet(url);
        if (json == null || json.isEmpty()) return null;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (!obj.has("properties")) return null;
        var arr = obj.getAsJsonArray("properties");
        for (var el : arr) {
            JsonObject p = el.getAsJsonObject();
            if ("textures".equals(p.get("name").getAsString())) {
                return new SkinData(p.get("value").getAsString(), p.get("signature").getAsString());
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private String httpGet(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
    LOGGER.trace("HTTP GET {} -> {}", urlStr, code);
        if (code == 204) return null; // username not found
        if (code != 200) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line; while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    public void shutdown() { executor.shutdownNow(); }

    public boolean manualRefresh(ServerPlayer player) {
        if (server.usesAuthentication()) return false;
        executor.submit(() -> fetchAndApply(player, player.getGameProfile().getName()));
        return true;
    }

    private record SkinData(String value, String signature) {}
}
