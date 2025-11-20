package cz.voidium.ranks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RankStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static RankStorage instance;
    private final Path dataPath;
    
    // UUID -> Set of Rank Names
    private Map<String, Set<String>> playerRanks = new ConcurrentHashMap<>();

    private RankStorage() {
        this.dataPath = FMLPaths.CONFIGDIR.get().resolve("voidium").resolve("voidium_ranks_data.json");
        load();
    }

    public static synchronized RankStorage getInstance() {
        if (instance == null) {
            instance = new RankStorage();
        }
        return instance;
    }

    public void load() {
        if (Files.exists(dataPath)) {
            try (Reader reader = Files.newBufferedReader(dataPath)) {
                Map<String, Set<String>> data = GSON.fromJson(reader, new TypeToken<Map<String, Set<String>>>(){}.getType());
                if (data != null) {
                    playerRanks.putAll(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(dataPath)) {
            GSON.toJson(playerRanks, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasRank(UUID uuid, String rank) {
        Set<String> ranks = playerRanks.get(uuid.toString());
        return ranks != null && ranks.contains(rank);
    }

    public void addRank(UUID uuid, String rank) {
        playerRanks.computeIfAbsent(uuid.toString(), k -> new HashSet<>()).add(rank);
        save();
    }
}
