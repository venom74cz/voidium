package cz.voidium.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import cz.voidium.config.DiscordConfig;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LinkManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static LinkManager instance;
    private final Path dataPath;
    
    // UUID string -> Discord ID (Long)
    private Map<String, Long> linkedAccounts = new ConcurrentHashMap<>();
    
    // Verification codes: Code (String) -> UUID (String)
    private Map<String, String> pendingCodes = new ConcurrentHashMap<>();
    
    // Code expiry tracking: Code (String) -> Expiry timestamp (Long)
    private Map<String, Long> codeExpiry = new ConcurrentHashMap<>();

    private LinkManager() {
        this.dataPath = FMLPaths.CONFIGDIR.get().resolve("voidium").resolve("links.json");
        load();
    }

    public static synchronized LinkManager getInstance() {
        if (instance == null) {
            instance = new LinkManager();
        }
        return instance;
    }

    public void load() {
        if (Files.exists(dataPath)) {
            try (Reader reader = Files.newBufferedReader(dataPath)) {
                Map<String, Long> data = GSON.fromJson(reader, new TypeToken<Map<String, Long>>(){}.getType());
                if (data != null) {
                    linkedAccounts.putAll(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(dataPath)) {
            GSON.toJson(linkedAccounts, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String generateCode(UUID playerUuid) {
        // Clean up expired codes first (older than 10 minutes)
        cleanupExpiredCodes();
        
        // Generate a simple 6-digit code
        String code = String.format("%06d", new Random().nextInt(999999));
        pendingCodes.put(code, playerUuid.toString());
        codeExpiry.put(code, System.currentTimeMillis() + 600000); // 10 minutes expiry
        return code;
    }
    
    private void cleanupExpiredCodes() {
        long now = System.currentTimeMillis();
        codeExpiry.entrySet().removeIf(entry -> {
            if (entry.getValue() < now) {
                pendingCodes.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    public boolean verifyCode(String code, long discordId) {
        String uuidStr = pendingCodes.remove(code);
        codeExpiry.remove(code); // Clean up expiry entry
        if (uuidStr != null) {
            // Check limit
            long count = linkedAccounts.values().stream().filter(id -> id == discordId).count();
            if (count >= DiscordConfig.getInstance().getMaxAccountsPerDiscord()) {
                return false; // Limit reached
            }
            
            linkedAccounts.put(uuidStr, discordId);
            save();
            return true;
        }
        return false;
    }
    
    public UUID getPlayerFromCode(String code) {
        // Check if code is expired
        Long expiry = codeExpiry.get(code);
        if (expiry != null && expiry < System.currentTimeMillis()) {
            pendingCodes.remove(code);
            codeExpiry.remove(code);
            return null;
        }
        
        String uuidStr = pendingCodes.get(code);
        return uuidStr != null ? UUID.fromString(uuidStr) : null;
    }

    public boolean isLinked(UUID playerUuid) {
        return linkedAccounts.containsKey(playerUuid.toString());
    }

    public Long getDiscordId(UUID playerUuid) {
        return linkedAccounts.get(playerUuid.toString());
    }
    
    public void unlink(UUID playerUuid) {
        linkedAccounts.remove(playerUuid.toString());
        save();
    }

    public void unlinkDiscordId(long discordId) {
        linkedAccounts.entrySet().removeIf(entry -> entry.getValue() == discordId);
        save();
    }
    
    public List<UUID> getUuids(long discordId) {
        List<UUID> uuids = new ArrayList<>();
        for (Map.Entry<String, Long> entry : linkedAccounts.entrySet()) {
            if (entry.getValue() == discordId) {
                uuids.add(UUID.fromString(entry.getKey()));
            }
        }
        return uuids;
    }
}
