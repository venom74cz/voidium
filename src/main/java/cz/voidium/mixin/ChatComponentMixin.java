package cz.voidium.mixin;

import cz.voidium.client.chat.ChatColorParser;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into ChatComponent to intercept and transform chat messages
 * before they are displayed. This handles RGB/Hex colors and emoji parsing.
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    // Thread-local flag to prevent infinite recursion
    @Unique
    private static final ThreadLocal<Boolean> voidium$isProcessing = ThreadLocal.withInitial(() -> false);

    /**
     * Intercept addMessage to parse color codes and emojis.
     */
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"), cancellable = true)
    private void voidium$onAddMessage(Component message, MessageSignature signature, GuiMessageTag tag,
            CallbackInfo ci) {
        // Prevent infinite recursion
        if (voidium$isProcessing.get()) {
            return;
        }

        try {
            voidium$isProcessing.set(true);

            // Parse the message for RGB colors and emojis
            Component parsed = ChatColorParser.parseMessage(message);

            // If parsing changed the message, cancel and re-add with parsed content
            if (parsed != message) {
                ci.cancel();
                ((ChatComponent) (Object) this).addMessage(parsed, signature, tag);
            }
        } finally {
            voidium$isProcessing.set(false);
        }
    }
}
