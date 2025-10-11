package cz.voidium.vote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe queue for votes received when player is offline.
 * Persists pending votes to JSON file.
 */
public class PendingVoteQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger("voidium-vote-queue");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Path queueFile;
    private final Lock lock = new ReentrantLock();
    private List<PendingVote> queue;

    public static class PendingVote {
        private final String username;
        private final String serviceName;
        private final String address;
        private final long timestamp;
        private final long queuedAt;

        public PendingVote(String username, String serviceName, String address, long timestamp, long queuedAt) {
            this.username = username;
            this.serviceName = serviceName;
            this.address = address;
            this.timestamp = timestamp;
            this.queuedAt = queuedAt;
        }

        public String getUsername() { return username; }
        public String getServiceName() { return serviceName; }
        public String getAddress() { return address; }
        public long getTimestamp() { return timestamp; }
        public long getQueuedAt() { return queuedAt; }
    }

    public PendingVoteQueue(Path queueFile) {
        this.queueFile = queueFile;
        this.queue = new ArrayList<>();
        load();
    }

    /**
     * Add vote to pending queue for offline player
     */
    public void enqueue(VoteEvent event) {
        lock.lock();
        try {
            PendingVote pending = new PendingVote(
                event.username(),
                event.serviceName(),
                event.address(),
                event.timestamp(),
                System.currentTimeMillis()
            );
            queue.add(pending);
            save();
            LOGGER.info("Queued vote for offline player: {} from {}", event.username(), event.serviceName());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get and remove all pending votes for a player
     */
    public List<PendingVote> dequeueForPlayer(String username) {
        lock.lock();
        try {
            List<PendingVote> playerVotes = new ArrayList<>();
            queue.removeIf(vote -> {
                if (vote.username.equalsIgnoreCase(username)) {
                    playerVotes.add(vote);
                    return true;
                }
                return false;
            });
            
            if (!playerVotes.isEmpty()) {
                save();
                LOGGER.info("Dequeued {} pending vote(s) for player: {}", playerVotes.size(), username);
            }
            
            return playerVotes;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if player has pending votes
     */
    public boolean hasPendingVotes(String username) {
        lock.lock();
        try {
            return queue.stream().anyMatch(vote -> vote.username.equalsIgnoreCase(username));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get count of pending votes for player
     */
    public int getPendingCount(String username) {
        lock.lock();
        try {
            return (int) queue.stream().filter(vote -> vote.username.equalsIgnoreCase(username)).count();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get total pending votes count
     */
    public int getTotalPending() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Load pending votes from disk
     */
    private void load() {
        lock.lock();
        try {
            if (!Files.exists(queueFile)) {
                queue = new ArrayList<>();
                return;
            }

            try (Reader reader = Files.newBufferedReader(queueFile, StandardCharsets.UTF_8)) {
                List<PendingVote> loaded = GSON.fromJson(reader, new TypeToken<List<PendingVote>>(){}.getType());
                queue = loaded != null ? loaded : new ArrayList<>();
                LOGGER.info("Loaded {} pending vote(s) from queue file", queue.size());
            } catch (Exception e) {
                LOGGER.error("Failed to load pending vote queue", e);
                queue = new ArrayList<>();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Save pending votes to disk
     */
    private void save() {
        try {
            Path parent = queueFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(queueFile, StandardCharsets.UTF_8)) {
                GSON.toJson(queue, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save pending vote queue", e);
        }
    }

    /**
     * Clear all pending votes (admin command)
     */
    public void clear() {
        lock.lock();
        try {
            int count = queue.size();
            queue.clear();
            save();
            LOGGER.info("Cleared {} pending vote(s)", count);
        } finally {
            lock.unlock();
        }
    }
}
