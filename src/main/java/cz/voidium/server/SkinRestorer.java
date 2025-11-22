package cz.voidium.server;

import cz.voidium.config.GeneralConfig;
import cz.voidium.skin.SkinCache;
import cz.voidium.skin.SkinData;
import cz.voidium.skin.SkinFetcher;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Offline-mode skin restorer (post-login fallback) – now simplified because early mixin handles most cases.
 */
public class SkinRestorer {
    private final MinecraftServer server;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("voidium-skinrestorer");

    public SkinRestorer(MinecraftServer server) {
        this.server = server;
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        LOGGER.info("SkinRestorer enabled (offline mode skin fetching)");
    }

    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (server.isSingleplayerOwner(player.getGameProfile())) return;
        if (server.usesAuthentication()) return; // only offline
        if (!GeneralConfig.getInstance().isEnableSkinRestorer()) return;
        final String name = player.getGameProfile().getName();
        LOGGER.debug("SkinRestorer login event for {}", name);
        executor.submit(() -> fetchAndApply(player, name));
    }

    private void fetchAndApply(ServerPlayer player, String name) {
        try {
            // Already has textures (early mixin) – just ensure one ADD broadcast for any late clients.
            if (!player.getGameProfile().getProperties().get("textures").isEmpty()) {
                server.execute(() -> {
                    try {
                        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, player));
                    } catch (Exception ignored) {}
                });
                return;
            }

            // Try persistent cache first by player name
            SkinCache.CachedEntry disk = SkinCache.get(name);
            UUID officialUuid;
            SkinData skin;
            if (disk != null) {
                officialUuid = disk.uuid();
                skin = new SkinData(disk.value(), disk.signature());
                LOGGER.debug("Disk cache hit for {}", name);
            } else {
                try {
                    officialUuid = SkinFetcher.fetchOfficialUUID(name);
                    if (officialUuid == null) { LOGGER.debug("UUID not found for name={}", name); return; }
                    
                    skin = SkinFetcher.fetchSkin(officialUuid);
                    if (skin == null) { LOGGER.debug("Skin not found for uuid={}", officialUuid); return; }
                    
                    // Persist newly fetched skin
                    SkinCache.put(name, officialUuid, skin.value(), skin.signature());
                } catch (IOException e) {
                    LOGGER.warn("Failed to fetch skin for {}: {}", name, e.getMessage());
                    return;
                }
            }

            SkinData finalSkin = skin;
            server.execute(() -> {
                try {
                    var profile = player.getGameProfile();
                    int before = profile.getProperties().get("textures").size();
                    profile.getProperties().removeAll("textures");
                    profile.getProperties().put("textures", new com.mojang.authlib.properties.Property("textures", finalSkin.value(), finalSkin.signature()));
                    int after = profile.getProperties().get("textures").size();
                    LOGGER.debug("Applied textures (before={}, after={}) for {}", before, after, name);
                    // Minimal refresh: remove then add once.
                    try { server.getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(java.util.List.of(player.getUUID()))); } catch (Exception ignore) {}
                    server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, player));
                } catch (Exception e) {
                    LOGGER.warn("Failed applying skin for {}: {}", name, e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.warn("Skin restore error for {}: {}", name, e.getMessage());
        }
    }

    public void shutdown() { executor.shutdownNow(); }

    public boolean manualRefresh(ServerPlayer player) {
        if (server.usesAuthentication()) return false;
        executor.submit(() -> fetchAndApply(player, player.getGameProfile().getName()));
        return true;
    }
}
