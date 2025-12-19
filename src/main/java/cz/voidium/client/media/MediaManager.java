package cz.voidium.client.media;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import cz.voidium.Voidium;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MediaManager {
    private static MediaManager instance;
    private final Map<String, ResourceLocation> textureCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> loadingCache = new ConcurrentHashMap<>();

    private MediaManager() {}

    public static synchronized MediaManager getInstance() {
        if (instance == null) {
            instance = new MediaManager();
        }
        return instance;
    }

    public ResourceLocation getTexture(String url) {
        if (textureCache.containsKey(url)) {
            return textureCache.get(url);
        }

        if (!loadingCache.containsKey(url)) {
            loadingCache.put(url, true);
            downloadImage(url);
        }
        
        return null; // Return null (or placeholder) while loading
    }

    private void downloadImage(String urlString) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Voidium-Mod");
                
                try (InputStream stream = connection.getInputStream()) {
                    NativeImage image = NativeImage.read(stream);
                    
                    // Schedule upload to GPU on main thread
                    Minecraft.getInstance().execute(() -> {
                        DynamicTexture texture = new DynamicTexture(image);
                        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(Voidium.MOD_ID, "media/" + Math.abs(urlString.hashCode()));
                        Minecraft.getInstance().getTextureManager().register(rl, texture);
                        textureCache.put(urlString, rl);
                        loadingCache.remove(urlString);
                    });
                }
            } catch (Exception e) {
                // Failed to load
                loadingCache.remove(urlString);
            }
        });
    }
}
