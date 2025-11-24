package cz.voidium.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class SkinFetcher {
    private static final String MOJANG_PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MOJANG_SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";

    public static UUID fetchOfficialUUID(String name) throws IOException {
        String url = MOJANG_PROFILE_URL + name;
        String json = httpGet(url);
        if (json == null || json.isEmpty()) return null;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (!obj.has("id") || obj.get("id") == null || !obj.get("id").isJsonPrimitive()) return null;
        String raw = obj.get("id").getAsString();
        return UUID.fromString(raw.replaceFirst(
                "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                "$1-$2-$3-$4-$5"));
    }

    public static SkinData fetchSkin(UUID uuid) throws IOException {
        String url = String.format(MOJANG_SESSION_URL, uuid.toString().replace("-", ""));
        String json = httpGet(url);
        if (json == null || json.isEmpty()) return null;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (!obj.has("properties")) return null;
        for (var elem : obj.getAsJsonArray("properties")) {
            JsonObject prop = elem.getAsJsonObject();
            if (prop.has("name") && prop.get("name") != null && 
                "textures".equals(prop.get("name").getAsString())) {
                if (!prop.has("value") || prop.get("value") == null) continue;
                String val = prop.get("value").getAsString();
                String sig = prop.has("signature") && prop.get("signature") != null ? 
                    prop.get("signature").getAsString() : "";
                return new SkinData(val, sig);
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
        if (conn.getResponseCode() != 200) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
