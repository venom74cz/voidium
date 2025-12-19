package cz.voidium.server.chat;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;

public class ChatHistoryManager {
    private static ChatHistoryManager instance;
    private final LinkedList<String> globalChatHistory = new LinkedList<>();
    private final int MAX_HISTORY = 100;

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

    public List<String> getGlobalHistory() {
        synchronized (globalChatHistory) {
            return new LinkedList<>(globalChatHistory);
        }
    }
}
