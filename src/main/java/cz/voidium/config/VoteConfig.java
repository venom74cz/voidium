package cz.voidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class VoteConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SHARED_SECRET_LENGTH = 16;
    private static final char[] SECRET_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom SECRET_RANDOM = new SecureRandom();
    private static VoteConfig instance;
    private transient Path configPath;
    private transient Path configDir;

    private boolean enabled = true;
    private String host = "0.0.0.0";
    private int port = 8192;
    private String rsaPrivateKeyPath = "votifier_rsa.pem";
    private String rsaPublicKeyPath = "votifier_rsa_public.pem";
    private String sharedSecret = generateSharedSecret();
    private boolean announceVotes = true;
    private String announcementMessage = "&b%PLAYER% &7hlasoval pro server a získal odměnu!";
    private int announcementCooldown = 300; // seconds
    private int maxVoteAgeHours = 24;
    private List<String> commands = new ArrayList<>(List.of(
            "tellraw %PLAYER% {\"text\":\"Děkujeme za hlasování!\",\"color\":\"green\"}",
            "give %PLAYER% diamond 1"
    ));
    private Logging logging = new Logging();

    public static class Logging {
        private boolean voteLog = true;
        private String voteLogFile = "votes.log";
        private boolean archiveJson = true;
        private String archivePath = "votes-history.ndjson";
        private boolean notifyOpsOnError = true;
        private String pendingQueueFile = "pending-votes.json";
        private String pendingVoteMessage = "&8[&bVoidium&8] &aPaid out &e%COUNT% &apending votes!";

        public boolean isVoteLog() { return voteLog; }
        public String getVoteLogFile() { return voteLogFile; }
        public boolean isArchiveJson() { return archiveJson; }
        public String getArchivePath() { return archivePath; }
        public boolean isNotifyOpsOnError() { return notifyOpsOnError; }
        public String getPendingQueueFile() { return pendingQueueFile; }
        public String getPendingVoteMessage() { return pendingVoteMessage; }
    }

    public VoteConfig(Path configPath) {
        this.configPath = configPath;
        this.configDir = configPath != null ? configPath.getParent() : null;
    }

    public static VoteConfig getInstance() {
        return instance;
    }

    public static void init(Path configDir) {
        Path configPath = configDir.resolve("votes.json");
        instance = load(configPath);
    }

    private static VoteConfig load(Path configPath) {
        VoteConfig config = new VoteConfig(configPath);

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                VoteConfig loaded = GSON.fromJson(reader, VoteConfig.class);
                if (loaded != null) {
                    loaded.configPath = configPath;
                    loaded.configDir = configPath.getParent();
                    if (loaded.sharedSecret == null || loaded.sharedSecret.isBlank()) {
                        loaded.sharedSecret = generateSharedSecret();
                        loaded.save();
                        System.out.println("Generated new vote shared secret (16 chars) because it was missing in config.");
                    }
                    if (loaded.logging == null) {
                        loaded.logging = new Logging();
                    }
                    return loaded;
                }
            } catch (Exception e) {
                System.err.println("Failed to load vote configuration: " + e.getMessage());
            }
        }

        config.save();
        return config;
    }

    public void save() {
        try {
            Path parent = configPath != null ? configPath.getParent() : null;
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                writer.write("// === VOTE CONFIGURATION ===\n");
                writer.write("// enabled: master switch for votifier listener\n");
                writer.write("// host: interface to bind\n");
                writer.write("// port: listener port\n");
                writer.write("// rsaPrivateKeyPath: path (relative to this config folder) to PKCS#8 PEM private key\n");
                writer.write("// rsaPublicKeyPath: path (relative) where public key will be written for vote sites\n");
                writer.write("// sharedSecret: optional HMAC secret for NuVotifier V2 token validation (auto-generated 16 chars; leave blank for legacy-only mode)\n");
                writer.write("// commands: list of server commands executed for every vote (%PLAYER% placeholder)\n");
                writer.write("// logging: voteLog stores plaintext records, archiveJson appends NDJSON, pendingQueueFile stores offline votes\n\n");
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to save vote configuration: " + e.getMessage());
        }
    }

    public boolean isEnabled() { return enabled; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getRsaPrivateKeyPath() { return rsaPrivateKeyPath; }
    public String getRsaPublicKeyPath() { return rsaPublicKeyPath; }
    public String getSharedSecret() { return sharedSecret; }
    public boolean isAnnounceVotes() { return announceVotes; }
    public String getAnnouncementMessage() { return announcementMessage; }
    public int getAnnouncementCooldown() { return announcementCooldown; }
    public int getMaxVoteAgeHours() { return maxVoteAgeHours; }
    public List<String> getCommands() { return commands; }
    public Logging getLogging() { return logging; }

    public void applyLocale(String locale) {
        java.util.Map<String, String> messages = LocalePresets.getVoteMessages(locale);
        this.announcementMessage = messages.get("announcementMessage");
        save();
    }

    private Path resolveConfigPath(String value) {
        if (value == null || value.isBlank()) {
            return getConfigDir().resolve(".");
        }
        Path path = Path.of(value);
        if (path.isAbsolute()) {
            return path;
        }
        if (path.getNameCount() > 1) {
            return path.normalize();
        }
        return getConfigDir().resolve(path);
    }

    private Path getConfigDir() {
        if (configDir == null) {
            configDir = configPath != null ? configPath.getParent() : null;
        }
        return configDir != null ? configDir : Path.of(".");
    }

    public Path getResolvedPrivateKeyPath() {
        return resolveConfigPath(rsaPrivateKeyPath);
    }

    public Path getResolvedPublicKeyPath() {
        return resolveConfigPath(rsaPublicKeyPath);
    }

    public Path getResolvedVoteLogFile() {
        return StorageHelper.resolve(logging.voteLogFile);
    }

    public Path getResolvedArchivePath() {
        return StorageHelper.resolve(logging.archivePath);
    }

    public Path getResolvedPendingQueueFile() {
        return StorageHelper.resolve(logging.pendingQueueFile);
    }

    private static String generateSharedSecret() {
        char[] buffer = new char[SHARED_SECRET_LENGTH];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = SECRET_ALPHABET[SECRET_RANDOM.nextInt(SECRET_ALPHABET.length)];
        }
        return new String(buffer);
    }
}
