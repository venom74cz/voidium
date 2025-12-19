package cz.voidium.client.media;

import cz.voidium.Voidium;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Discord emoji textures.
 * Emojis are synced from server and rendered as inline textures.
 */
public class EmojiManager {
    private static EmojiManager instance;
    
    // Map of emoji name -> texture ResourceLocation
    private final Map<String, ResourceLocation> emojiTextures = new ConcurrentHashMap<>();
    // Map of emoji name -> URL (synced from server)
    private final Map<String, String> emojiUrls = new ConcurrentHashMap<>();
    // Loading state
    private final Map<String, Boolean> loading = new ConcurrentHashMap<>();
    
    private EmojiManager() {}
    
    public static synchronized EmojiManager getInstance() {
        if (instance == null) {
            instance = new EmojiManager();
        }
        return instance;
    }
    
    /**
     * Called when receiving emoji sync packet from server.
     */
    public void syncEmojis(Map<String, String> emojis) {
        this.emojiUrls.clear();
        this.emojiUrls.putAll(emojis);
        
        // Pre-load all emojis (both custom and standard provided by server)
        for (Map.Entry<String, String> entry : emojis.entrySet()) {
            loadEmoji(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Get the texture for an emoji by name.
     * Returns null if not loaded yet.
     */
    public ResourceLocation getEmojiTexture(String name) {
        // If not loaded but we have URL, try load (lazy load)
        if (!emojiTextures.containsKey(name) && emojiUrls.containsKey(name)) {
            loadEmoji(name, emojiUrls.get(name));
        }
        return emojiTextures.get(name);
    }
    
    /**
     * Check if emoji exists (was synced from server).
     */
    public boolean hasEmoji(String name) {
        return emojiUrls.containsKey(name);
    }
    
    /**
     * Get list of all available emoji names for autocomplete.
     */
    public java.util.Set<String> getEmojiNames() {
        return emojiUrls.keySet();
    }
    
    private void loadEmoji(String name, String urlString) {
        if (loading.containsKey(name) || emojiTextures.containsKey(name)) return;
        
        loading.put(name, true);
        
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Voidium-Mod");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                try (InputStream stream = conn.getInputStream()) {
                    NativeImage image = NativeImage.read(stream);
                    
                    Minecraft.getInstance().execute(() -> {
                        DynamicTexture texture = new DynamicTexture(image);
                        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(Voidium.MOD_ID, "emoji/" + name);
                        Minecraft.getInstance().getTextureManager().register(rl, texture);
                        emojiTextures.put(name, rl);
                        loading.remove(name);
                    });
                }
            } catch (Exception e) {
                loading.remove(name);
            }
        });
    }
    
    /**
     * Get emoji size for rendering (Discord emojis are typically 22x22 or 32x32).
     */
    public int getEmojiSize() {
        return 12; // Match font height
    }
}
