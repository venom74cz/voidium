package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigFileHelper {
    private ConfigFileHelper() {
    }

    public static <T> T loadJson(Path path, Gson gson, Class<T> type) throws IOException {
        String sanitized = sanitizeJson(Files.readString(path, StandardCharsets.UTF_8));
        try (Reader reader = new StringReader(sanitized)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            return gson.fromJson(jsonReader, type);
        }
    }

    public static String sanitizeJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        StringBuilder sanitized = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        for (int index = 0; index < raw.length(); index++) {
            char current = raw.charAt(index);
            char next = index + 1 < raw.length() ? raw.charAt(index + 1) : '\0';
            if (!inString && current == '/' && next == '/') {
                while (index < raw.length() && raw.charAt(index) != '\n') {
                    index++;
                }
                if (index < raw.length()) {
                    sanitized.append('\n');
                }
                continue;
            }
            sanitized.append(current);
            if (current == '"' && !escaped) {
                inString = !inString;
            }
            escaped = current == '\\' && !escaped;
            if (current != '\\') {
                escaped = false;
            }
        }
        String text = sanitized.toString().trim();
        if (text.isBlank()) {
            return "{}";
        }
        return text;
    }
}
