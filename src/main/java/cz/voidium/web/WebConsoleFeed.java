package cz.voidium.web;

import java.util.LinkedList;
import java.util.List;

public class WebConsoleFeed {
    private static final WebConsoleFeed INSTANCE = new WebConsoleFeed();
    private static final int MAX_ENTRIES = 250;
    private final LinkedList<ConsoleEntry> entries = new LinkedList<>();

    public static class ConsoleEntry {
        public final long timestamp;
        public final String level;
        public final String logger;
        public final String message;

        public ConsoleEntry(long timestamp, String level, String logger, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.logger = logger;
            this.message = message;
        }
    }

    private WebConsoleFeed() {
    }

    public static WebConsoleFeed getInstance() {
        return INSTANCE;
    }

    public void append(String level, String logger, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        synchronized (entries) {
            if (entries.size() >= MAX_ENTRIES) {
                entries.removeFirst();
            }
            entries.add(new ConsoleEntry(System.currentTimeMillis(), level, logger, message));
        }
    }

    public List<ConsoleEntry> snapshot() {
        synchronized (entries) {
            return new LinkedList<>(entries);
        }
    }
}