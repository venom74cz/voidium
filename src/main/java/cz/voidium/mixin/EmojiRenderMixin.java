package cz.voidium.mixin;

import cz.voidium.client.media.EmojiManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * This intercepts drawString calls and renders emoji textures where
 * :emoji_name: patterns are found.
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

    /**
     * Intercept drawString to render emojis as textures.
     */
    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", at = @At("HEAD"), cancellable = true)
    private void voidium$onDrawString(Font font, String text, int x, int y, int color, boolean dropShadow,
            CallbackInfoReturnable<Integer> cir) {
        // Check if text contains emoji patterns
        if (text == null || !text.contains(":") || voidium$isRendering.get()) {
            return;
        }

        Matcher matcher = EMOJI_PATTERN.matcher(text);
        if (!matcher.find()) {
            return;
        }

        // Reset matcher to start
        matcher.reset();

        try {
            voidium$isRendering.set(true);

            GuiGraphics gfx = (GuiGraphics) (Object) this;
            int currentX = x;
            int lastEnd = 0;
            int fontHeight = font.lineHeight;
            int emojiSize = fontHeight; // Match emoji size to font height

            while (matcher.find()) {
                // Render text before emoji
                String beforeText = text.substring(lastEnd, matcher.start());
                if (!beforeText.isEmpty()) {
                    int width = gfx.drawString(font, beforeText, currentX, y, color, dropShadow);
                    currentX = width;
                }

                String emojiName = matcher.group(1);
                ResourceLocation emojiTexture = EmojiManager.getInstance().getEmojiTexture(emojiName);

                if (emojiTexture != null) {
                    // Render emoji texture
                    pose().pushPose();
                    blit(emojiTexture, currentX, y - 1, 0, 0, 0, emojiSize, emojiSize, emojiSize, emojiSize);
                    pose().popPose();
                    currentX += emojiSize + 1;
                } else {
                    // Emoji not loaded, render as text
                    String emojiText = ":" + emojiName + ":";
                    int width = gfx.drawString(font, emojiText, currentX, y, color, dropShadow);
                    currentX = width;
                }

                lastEnd = matcher.end();
            }

            // Render remaining text after last emoji
            if (lastEnd < text.length()) {
                String afterText = text.substring(lastEnd);
                currentX = gfx.drawString(font, afterText, currentX, y, color, dropShadow);
            }

            cir.setReturnValue(currentX);
        } finally {
            voidium$isRendering.set(false);
        }
    }

    /**
     * Also handle Component-based drawString for formatted text.
     */
    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", at = @At("HEAD"), cancellable = true)
    private void voidium$onDrawStringComponent(Font font, Component component, int x, int y, int color,
            boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        String text = component.getString();

        // Quick check - if no emoji pattern, let vanilla handle it
        if (text == null || !text.contains(":") || voidium$isRendering.get()) {
            return;
        }

        if (!EMOJI_PATTERN.matcher(text).find()) {
            return;
        }

        // For components with emojis, we need special handling
        // The color parsing is already done by ChatComponentMixin, so we just need
        // emoji rendering
        try {
            voidium$isRendering.set(true);

            GuiGraphics gfx = (GuiGraphics) (Object) this;
            int result = voidium$renderComponentWithEmojis(gfx, font, component, x, y, color, dropShadow);
            cir.setReturnValue(result);
        } finally {
            voidium$isRendering.set(false);
        }
    }

    /**
     * Render a component with inline emoji textures.
     */
    @Unique
    private int voidium$renderComponentWithEmojis(GuiGraphics gfx, Font font, Component component, int x, int y,
            int color, boolean dropShadow) {
        String text = component.getString();
        Matcher matcher = EMOJI_PATTERN.matcher(text);

        int currentX = x;
        int lastEnd = 0;
        int fontHeight = font.lineHeight;
        int emojiSize = fontHeight;

        while (matcher.find()) {
            // Render text before emoji using vanilla rendering for proper styling
            String beforeText = text.substring(lastEnd, matcher.start());
            if (!beforeText.isEmpty()) {
                // Use basic color for now - styled components already have color applied
                int width = gfx.drawString(font, beforeText, currentX, y, color, dropShadow);
                currentX = width;
            }

            String emojiName = matcher.group(1);
            ResourceLocation emojiTexture = EmojiManager.getInstance().getEmojiTexture(emojiName);

            if (emojiTexture != null) {
                pose().pushPose();
                blit(emojiTexture, currentX, y - 1, 0, 0, 0, emojiSize, emojiSize, emojiSize, emojiSize);
                pose().popPose();
                currentX += emojiSize + 1;
            } else {
                String emojiText = ":" + emojiName + ":";
                int width = gfx.drawString(font, emojiText, currentX, y, color, dropShadow);
                currentX = width;
            }

            lastEnd = matcher.end();
        }

        // Render remaining text
        if (lastEnd < text.length()) {
            String afterText = text.substring(lastEnd);
            currentX = gfx.drawString(font, afterText, currentX, y, color, dropShadow);
        }

        return currentX;
    }
}
