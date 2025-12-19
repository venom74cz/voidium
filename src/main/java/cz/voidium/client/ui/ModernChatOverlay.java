package cz.voidium.client.ui;

import cz.voidium.client.chat.ChatChannelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import java.util.List;

import net.minecraft.client.DeltaTracker;

public class ModernChatOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen instanceof ModernChatScreen) return;
        
        var channel = ChatChannelManager.getInstance().getActiveChannel(); 
        if (channel == null) return;

        // FADE LOGIC
        long now = System.currentTimeMillis();
        long lastMsg = cz.voidium.client.ClientSetup.lastChatTime;
        long timeSince = now - lastMsg;
        
        if (timeSince > 10000 && mc.screen == null) { // 10 seconds inactive AND chat not open
            return; // Hide
        }
        
        // Calculate fade
        int alpha = 0xFF; // Full opacity
        if (timeSince > 9000 && mc.screen == null) {
            // Fade out over last second
            float fade = 1.0f - ((timeSince - 9000) / 1000.0f);
            alpha = (int)(fade * 255);
            if (alpha < 5) return;
        }

        int color = (alpha << 24) | 0xFFFFFF; // White text with fading alpha
        
        List<Component> messages = channel.getHistory(); 
        int y = mc.getWindow().getGuiScaledHeight() - 40;
        int x = 10;
        
        // Render last 10 messages from bottom up
        int count = 0;
        for (int i = messages.size() - 1; i >= 0 && count < 10; i--) {
            Component msg = messages.get(i);
            renderStyledMessage(guiGraphics, mc, msg.getString(), x, y, color); // Pass color
            y -= 10;
            count++;
        }
    }
    
    private void renderStyledMessage(GuiGraphics gfx, Minecraft mc, String text, int x, int y, int color) {
        // Matches :emoji_name:
        java.util.regex.Pattern EMOJI_PATTERN = java.util.regex.Pattern.compile(":([a-zA-Z0-9_]+):");
        java.util.regex.Matcher emojiMatcher = EMOJI_PATTERN.matcher(text);
        int lastEnd = 0;
        int currentX = x;
        
        while (emojiMatcher.find()) {
            // Render text before emoji
            String before = text.substring(lastEnd, emojiMatcher.start());
            if (!before.isEmpty()) {
                gfx.drawString(mc.font, before, currentX, y, color, true); // Use dynamic color
                currentX += mc.font.width(before);
            }
            
            // Try to render emoji as texture
            String emojiName = emojiMatcher.group(1);
            net.minecraft.resources.ResourceLocation emojiTexture = cz.voidium.client.media.EmojiManager.getInstance().getEmojiTexture(emojiName);
            
            if (emojiTexture != null) {
                // Render texture - set alpha for blit if possible?
                // Standard blit doesn't easily support alpha without shader setup, skipping for now as textures usually stay opaque
                // But text needs to fade
                int size = 9; 
                
                // Fade check - if alpha is low, maybe don't render or simple render
                int alpha = (color >> 24) & 0xFF;
                if (alpha > 50) {
                     gfx.blit(emojiTexture, currentX, y, 0, 0, size, size, size, size);
                }
                
                currentX += size + 1;
            } else {
                 // Fallback to text
                String emojiDisplay = ":" + emojiName + ":";
                gfx.drawString(mc.font, emojiDisplay, currentX, y, color, true); // Use dynamic color
                currentX += mc.font.width(emojiDisplay);
            }
            
            lastEnd = emojiMatcher.end();
        }
        
        // Render remaining text
        if (lastEnd < text.length()) {
            gfx.drawString(mc.font, text.substring(lastEnd), currentX, y, color, true);
        }
    }
}
