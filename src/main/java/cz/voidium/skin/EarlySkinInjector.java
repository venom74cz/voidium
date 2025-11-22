package cz.voidium.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.util.UUID;

/**
 * Lightweight synchronous skin fetcher used early in player placement.
 * Timeouts kept short to not stall login pipeline.
 */
public final class EarlySkinInjector {
    private EarlySkinInjector() {}
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("voidium-earlyskin");

    public static void fetchAndApply(GameProfile profile) {
        String name = profile.getName();
        try {
            // Cache hit?
            var cached = SkinCache.get(name);
            if (cached != null) {
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", new Property("textures", cached.value(), cached.signature()));
                LOGGER.debug("EarlySkinInjector cache hit for {}", name);
                return;
            }
            UUID official = SkinFetcher.fetchOfficialUUID(name);
            if (official == null) return;
            SkinData skin = SkinFetcher.fetchSkin(official);
            if (skin == null) return;
            profile.getProperties().removeAll("textures");
            profile.getProperties().put("textures", new Property("textures", skin.value(), skin.signature()));
            SkinCache.put(name, official, skin.value(), skin.signature());
            LOGGER.debug("EarlySkinInjector applied skin for {}", name);
        } catch (Exception e) {
            LOGGER.debug("EarlySkinInjector failed for {}: {}", name, e.getMessage());
        }
    }
}
