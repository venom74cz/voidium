package cz.voidium.server.chat;

import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

public class ChatHistoryManager {
    private static ChatHistoryManager instance;
    private final LinkedList<String> globalChatHistory = new LinkedList<>();
    private final LinkedList<ChatEntry> chatEntries = new LinkedList<>();
    private final int MAX_HISTORY = 100;

    public static class ChatEntry {
        public final long timestamp;
        public final String sender;
        public final String message;

        public ChatEntry(long timestamp, String sender, String message) {
            this.timestamp = timestamp;
            this.sender = sender;
            this.message = message;
        }
    }

    private ChatHistoryManager() {}

    public static synchronized ChatHistoryManager getInstance() {
        if (instance == null) {
            instance = new ChatHistoryManager();
        }
        return instance;
    }

    public void addGlobalMessage(String message) {
        synchronized (globalChatHistory) {
            if (globalChatHistory.size() >= MAX_HISTORY) {
                globalChatHistory.removeFirst();
            }
            globalChatHistory.add(message);
        }
    }

    public void addEntry(String sender, String message) {
        String flattened = sender + ": " + message;
        addGlobalMessage(flattened);
        synchronized (chatEntries) {
            if (chatEntries.size() >= MAX_HISTORY) {
                chatEntries.removeFirst();
            }
            chatEntries.add(new ChatEntry(System.currentTimeMillis(), sender, message));
        }
    }

    public List<String> getGlobalHistory() {
        synchronized (globalChatHistory) {
            return new LinkedList<>(globalChatHistory);
        }
    }

    public List<ChatEntry> getEntries() {
        synchronized (chatEntries) {
            return new LinkedList<>(chatEntries);
        }
    }
}
