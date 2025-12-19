package cz.voidium.client.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;

public class ChatChannelManager {
    private static ChatChannelManager instance;
    private final Map<String, ChatChannel> channels = new ConcurrentHashMap<>();
    private String activeChannelId = "main";
    private boolean isGlobalChatFocus = false;

    private ChatChannelManager() {
        // Initialize default main channel
        channels.put("main", new ChatChannel("main", "General", false));
    }

    public static synchronized ChatChannelManager getInstance() {
        if (instance == null) {
            instance = new ChatChannelManager();
        }
        return instance;
    }

    public void receiveHistory(List<String> messages) {
        ChatChannel main = channels.get("main");
        if (main != null) {
            main.clear(); // Clear existing to avoid duplicates on re-sync
            for (String msg : messages) {
                main.addMessage(Component.literal(msg)); 
            }
        }
    }

    public void addMessage(String channelId, Component message) {
        getOrCreateChannel(channelId, channelId).addMessage(message);
    }
    
    public ChatChannel getOrCreateChannel(String id, String displayName) {
        return channels.computeIfAbsent(id, k -> new ChatChannel(k, displayName, true));
    }
    
    public ChatChannel getActiveChannel() {
        return channels.get(activeChannelId);
    }
    
    public void setActiveChannel(String channelId) {
        if (channels.containsKey(channelId)) {
            this.activeChannelId = channelId;
        }
    }

    public void removeChannel(String channelId) {
        ChatChannel ch = channels.get(channelId);
        if (ch != null && ch.isRemovable) {
            channels.remove(channelId);
            if (activeChannelId.equals(channelId)) {
                activeChannelId = "main";
            }
        }
    }

    public List<ChatChannel> getAllChannels() {
        return new ArrayList<>(channels.values());
    }

    public static class ChatChannel {
        private final String id;
        private final String displayName; 
        private final boolean isRemovable;
        private final List<Component> history = new ArrayList<>();

        public ChatChannel(String id, String displayName, boolean isRemovable) {
            this.id = id;
            this.displayName = displayName;
            this.isRemovable = isRemovable;
        }

        public void addMessage(Component message) {
            history.add(message);
            // Limit history client-side too
            if (history.size() > 100) {
                history.remove(0);
            }
        }
        
        public void clear() {
            history.clear();
        }

        public List<Component> getHistory() {
            return new ArrayList<>(history);
        }
        
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public boolean isRemovable() { return isRemovable; }
    }
}
