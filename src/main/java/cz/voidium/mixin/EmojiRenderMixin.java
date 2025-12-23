package cz.voidium.mixin;

import cz.voidium.client.media.EmojiManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mixin into GuiGraphics to render Discord emojis as textures inline with text.
 * Covers drawString for String, Component and FormattedCharSequence.
 */
@Mixin(GuiGraphics.class)
public abstract class EmojiRenderMixin {

    @Shadow
    public abstract void blit(ResourceLocation texture, int x, int y, int blitOffset, float uOffset, float vOffset,
            int width, int height, int textureWidth, int textureHeight);

    @Shadow
    public abstract PoseStack pose();

    @Unique
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-zA-Z0-9_]+):");

    @Unique
    private static final ThreadLocal<Boolean> voidium$isRendering = ThreadLocal.withInitial(() -> false);

    // 1. String Target (INT coordinates)
    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", at = @At("HEAD"), cancellable = true)
    public void drawStringString(Font font, String text, int x, int y, int color, boolean dropShadow,
            CallbackInfoReturnable<Integer> cir) {
        if (!voidium$tryRender(font, text, x, y, color, dropShadow, cir)) {
            // Let original run if render failed or no emoji
        }
    }

    // 2. Component Target (INT coordinates)
    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", at = @At("HEAD"), cancellable = true)
    public void drawStringComponent(Font font, Component text, int x, int y, int color, boolean dropShadow,
            CallbackInfoReturnable<Integer> cir) {
        if (text != null) {
            // Note: getString() removes style, so emojis in chat might lose color
            // formatting on the same line.
            // This is a trade-off to get emojis working without complex FontRenderer
            // hooking.
            if (!voidium$tryRender(font, text.getString(), x, y, color, dropShadow, cir)) {

            }
        }
    }

    // 3. FormattedCharSequence Target (INT coordinates) - Used by Chat History
    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I", at = @At("HEAD"), cancellable = true)
    public void drawStringSequence(Font font, FormattedCharSequence text, int x, int y, int color, boolean dropShadow,
            CallbackInfoReturnable<Integer> cir) {
        if (text != null) {
            String extracted = voidium$extractText(text);
            if (!voidium$tryRender(font, extracted, x, y, color, dropShadow, cir)) {

            }
        }
    }

    @Unique
    private boolean voidium$tryRender(Font font, String text, int x, int y, int color, boolean dropShadow,
            CallbackInfoReturnable<Integer> cir) {
        if (voidium$isRendering.get() || text == null || text.isEmpty()) {
            return false;
        }

        Matcher matcher = EMOJI_PATTERN.matcher(text);
        if (!matcher.find()) {
            return false;
        }

        // Prevent recursion
        voidium$isRendering.set(true);

        try {
            GuiGraphics gfx = (GuiGraphics) (Object) this;
            int currentX = x;
            int lastEnd = 0;
            int emojiSize = EmojiManager.getInstance().getEmojiSize();

            // Reset matcher to start
            matcher.reset();

            while (matcher.find()) {
                // Render text before emoji
                String beforeText = text.substring(lastEnd, matcher.start());
                if (!beforeText.isEmpty()) {
                    currentX = gfx.drawString(font, beforeText, currentX, y, color, dropShadow);
                }

                String emojiName = matcher.group(1);
                ResourceLocation emojiTexture = EmojiManager.getInstance().getEmojiTexture(emojiName);

                if (emojiTexture != null) {
                    // Render emoji texture
                    RenderSystem.enableBlend();
                    pose().pushPose();

                    // Scale 72x72 texture down to emojiSize (9px)
                    // Translate to position first
                    pose().translate(currentX, y - 1, 0); // y-1 for slight centering
                    float scale = (float) emojiSize / 72.0f;
                    pose().scale(scale, scale, 1.0f);

                    // Draw full 72x72 texture at (0,0) relative to translated/scaled origin
                    // blit(texture, x, y, blitOffset, u, v, width, height, texW, texH)
                    blit(emojiTexture, 0, 0, 0, 0, 0, 72, 72, 72, 72);

                    pose().popPose();
                    RenderSystem.disableBlend();
                    currentX += emojiSize + 1;
                } else {
                    // Emoji not loaded, render as text
                    String emojiText = ":" + emojiName + ":";
                    currentX = gfx.drawString(font, emojiText, currentX, y, color, dropShadow);
                }

                lastEnd = matcher.end();
            }

            // Render remaining text
            if (lastEnd < text.length()) {
                String remaining = text.substring(lastEnd);
                currentX = gfx.drawString(font, remaining, currentX, y, color, dropShadow);
            }

            cir.setReturnValue(currentX);
            return true;
        } finally {
            voidium$isRendering.set(false);
        }
    }

    @Unique
    private String voidium$extractText(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }
}
