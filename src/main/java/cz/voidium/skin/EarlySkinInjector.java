package cz.voidium.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Lightweight synchronous skin fetcher used early in player placement.
 * Timeouts kept short to not stall login pipeline.
 */
public final class EarlySkinInjector {
    private EarlySkinInjector() {}
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("voidium-earlyskin");

    public static void fetchAndApply(GameProfile profile) {
        String name = profile.getName();
        try {
            // Cache hit?
            var cached = SkinCache.get(name);
            if (cached != null) {
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", new Property("textures", cached.value(), cached.signature()));
                LOGGER.debug("EarlySkinInjector cache hit for {}", name);
                return;
            }
            UUID official = fetchOfficialUUID(name);
            if (official == null) return;
            SkinData skin = fetchSkin(official);
            if (skin == null) return;
            profile.getProperties().removeAll("textures");
            profile.getProperties().put("textures", new Property("textures", skin.value, skin.signature));
            SkinCache.put(name, official, skin.value, skin.signature);
            LOGGER.debug("EarlySkinInjector applied skin for {}", name);
        } catch (Exception e) {
            LOGGER.debug("EarlySkinInjector failed for {}: {}", name, e.getMessage());
        }
    }

    private static UUID fetchOfficialUUID(String name) throws IOException {
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

    private static SkinData fetchSkin(UUID uuid) throws IOException {
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false";
        String json = httpGet(url);
        if (json == null || json.isEmpty()) return null;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (!obj.has("properties")) return null;
        for (var el : obj.getAsJsonArray("properties")) {
            JsonObject p = el.getAsJsonObject();
            if ("textures".equals(p.get("name").getAsString())) {
                return new SkinData(p.get("value").getAsString(), p.get("signature").getAsString());
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static String httpGet(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
        if (code != 200) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line; while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private record SkinData(String value, String signature) {}
}
