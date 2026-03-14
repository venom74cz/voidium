package cz.voidium.mixin;

import com.mojang.authlib.GameProfile;
import cz.voidium.config.DiscordConfig;
import cz.voidium.discord.DiscordManager;
import cz.voidium.discord.LinkManager;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.StoredUserList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StoredUserList.class)
public class UserBanListMixin {

    @Inject(method = "add", at = @At("HEAD"))
    @SuppressWarnings("unchecked")
    private void onBanAdd(StoredUserEntry<?> entry, CallbackInfo ci) {
        // Only act on UserBanList, not other StoredUserList subclasses (ops, whitelist)
        if (!((Object) this instanceof UserBanList)) return;
        if (!(entry instanceof UserBanListEntry banEntry)) return;
        if (!DiscordConfig.getInstance().isSyncBansMcToDiscord()) return;

        GameProfile profile = ((StoredUserEntryAccessor<GameProfile>) banEntry).getUser();
        if (profile == null || profile.getId() == null) return;

        Long discordId = LinkManager.getInstance().getDiscordId(profile.getId());
        if (discordId != null) {
            String reason = banEntry.getReason();
            if (reason == null) reason = "Banned on Minecraft Server";

            // Avoid loop if ban comes from Discord Sync
            if ("Discord Ban Sync".equals(banEntry.getSource())) return;

            DiscordManager.getInstance().banDiscordUser(discordId, reason);
        }
    }
}
