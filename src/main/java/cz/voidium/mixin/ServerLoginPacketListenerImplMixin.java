package cz.voidium.mixin;

import com.mojang.authlib.GameProfile;
import cz.voidium.skin.EarlySkinInjector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects early skin property fetch in offline mode so player joins already with skin.
 */
@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {
    @Shadow @Final private MinecraftServer server;
    @Shadow private GameProfile gameProfile;

    @Inject(method = "handleHello", at = @At("TAIL"))
    private void voidium$earlySkin(ServerboundHelloPacket packet, CallbackInfo ci) {
        // Only offline mode & config enabled.
        if (server.usesAuthentication()) return;
        if (!cz.voidium.config.GeneralConfig.getInstance().isEnableSkinRestorer()) return;
        if (this.gameProfile == null) return;
        System.out.println("[Voidium] Mixin(handleHello) executing for name=" + this.gameProfile.getName() + ", pre-textures=" + this.gameProfile.getProperties().get("textures").size());
        // Fetch & inject
        EarlySkinInjector.fetchAndApply(this.gameProfile);
        System.out.println("[Voidium] Mixin(handleHello) post-inject textures=" + this.gameProfile.getProperties().get("textures").size());
    }
}
