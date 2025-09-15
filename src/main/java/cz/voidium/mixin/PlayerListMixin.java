package cz.voidium.mixin;

import com.mojang.authlib.GameProfile;
import cz.voidium.skin.EarlySkinInjector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Secondary safeguard: right before PlayerList finishes adding the player, ensure textures present.
 * If early login mixin missed (API timing or timeout), we still try here on main thread.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow @Final private MinecraftServer server;

    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V", shift = At.Shift.BEFORE), cancellable = false)
    private void voidium$ensureSkin(net.minecraft.network.Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        if (server.usesAuthentication()) return;
        if (!cz.voidium.config.GeneralConfig.getInstance().isEnableSkinRestorer()) return;
        GameProfile gp = player.getGameProfile();
        if (gp.getProperties().get("textures").isEmpty()) {
            System.out.println("[Voidium] PlayerListMixin: missing textures pre-broadcast, attempting fetch for " + gp.getName());
            EarlySkinInjector.fetchAndApply(gp);
            System.out.println("[Voidium] PlayerListMixin: textures after attempt=" + gp.getProperties().get("textures").size());
        }
    }
}
