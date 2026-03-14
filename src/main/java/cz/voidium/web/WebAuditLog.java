package cz.voidium.web;

import cz.voidium.config.StorageHelper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.neoforged.fml.loading.FMLPaths;

public class WebAuditLog {
    private static final WebAuditLog INSTANCE = new WebAuditLog();
    private static final int MAX_ENTRIES = 200;
    private final CopyOnWriteArrayList<Map<String, Object>> entries = new CopyOnWriteArrayList<>();

    private WebAuditLog() {
    }

    public static WebAuditLog getInstance() {
        return INSTANCE;
    }

    public void record(String action, String detail, String source, boolean success) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        entry.put("action", action);
        entry.put("detail", detail);
        entry.put("source", source);
        entry.put("success", success);
        entries.add(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
        appendToDisk(entry);
    }

    public List<Map<String, Object>> snapshot() {
        return new ArrayList<>(entries);
    }

    private void appendToDisk(Map<String, Object> entry) {
        try {
            Path root = StorageHelper.getStorageDir() != null
                    ? StorageHelper.getStorageDir()
                    : FMLPaths.CONFIGDIR.get().resolve("voidium").resolve("storage");
            Files.createDirectories(root);
            Path file = root.resolve("web-audit.log");
            String line = entry.get("timestamp") + " | " + entry.get("action") + " | " + entry.get("source") + " | " + entry.get("success") + " | " + entry.get("detail") + System.lineSeparator();
            Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
