package cz.voidium.mixin;

import cz.voidium.client.media.EmojiManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin for ChatScreen to add emoji autocomplete suggestions.
 * When user types ':' followed by characters, shows matching emoji names.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow
    protected EditBox input;

    @Unique
    private List<String> voidium$suggestions = new ArrayList<>();

    @Unique
    private int voidium$selectedIndex = 0;

    @Unique
    private String voidium$currentQuery = "";

    @Unique
    private boolean voidium$showingSuggestions = false;

    @Unique
    private static final int MAX_SUGGESTIONS = 8;

    @Unique
    private static final int SUGGESTION_HEIGHT = 12;

    @Unique
    private static final int SUGGESTION_PADDING = 2;

    @Unique
    private boolean voidium$suppressed = false;

    /**
     * Called after rendering to draw emoji suggestions popup.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void voidium$onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!voidium$showingSuggestions || voidium$suggestions.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int inputX = input.getX();
        int inputY = input.getY();

        // Calculate popup position (above the input box)
        int popupWidth = 150;
        int popupHeight = Math.min(voidium$suggestions.size(), MAX_SUGGESTIONS) * SUGGESTION_HEIGHT
                + SUGGESTION_PADDING * 2;
        int popupX = inputX + getColonPosition() * 6; // Approximate char width
        int popupY = inputY - popupHeight - 2;

        // Draw background
        guiGraphics.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xDD000000);
        guiGraphics.renderOutline(popupX, popupY, popupWidth, popupHeight, 0xFF555555);

        // Draw suggestions
        int y = popupY + SUGGESTION_PADDING;
        int displayCount = Math.min(voidium$suggestions.size(), MAX_SUGGESTIONS);

        for (int i = 0; i < displayCount; i++) {
            String suggestion = voidium$suggestions.get(i);
            boolean selected = (i == voidium$selectedIndex);

            // Highlight selected
            if (selected) {
                guiGraphics.fill(popupX + 1, y - 1, popupX + popupWidth - 1, y + SUGGESTION_HEIGHT - 1, 0x55FFFFFF);
            }

            // Draw emoji name
            String displayText = ":" + suggestion + ":";
            int color = selected ? 0xFFFFFF00 : 0xFFFFFFFF;
            guiGraphics.drawString(mc.font, displayText, popupX + SUGGESTION_PADDING + 2, y, color, false);

            y += SUGGESTION_HEIGHT;
        }
    }

    /**
     * Intercept key presses for Tab/Enter/Escape handling.
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void voidium$onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!voidium$showingSuggestions || voidium$suggestions.isEmpty()) {
            return;
        }

        // Tab or Enter = select
        if (keyCode == 258 || keyCode == 257) { // Tab = 258, Enter = 257
            voidium$applySuggestion();
            cir.setReturnValue(true);
            return;
        }

        // Escape = close suggestions
        if (keyCode == 256) { // Escape
            voidium$suppressedQuery = voidium$currentQuery;
            voidium$closeSuggestions();
            voidium$suppressed = true;
            cir.setReturnValue(true);
            return;
        }

        // Arrow Up
        if (keyCode == 265) {
            voidium$selectedIndex = Math.max(0, voidium$selectedIndex - 1);
            cir.setReturnValue(true);
            return;
        }

        // Arrow Down
        if (keyCode == 264) {
            voidium$selectedIndex = Math.min(voidium$suggestions.size() - 1, voidium$selectedIndex + 1);
            cir.setReturnValue(true);
            return;
        }
    }

    /**
     * Update suggestions when input changes.
     */
    @Unique
    private String voidium$suppressedQuery = "";

    @Inject(method = "tick", at = @At("TAIL"))
    private void voidium$onTick(CallbackInfo ci) {
        if (input == null)
            return;

        String text = input.getValue();
        String query = voidium$extractEmojiQuery(text, input.getCursorPosition());

        if (query != null && query.length() >= 1) {
            // Check if suppressed for this exact query (ESC was pressed)
            if (voidium$suppressed && query.equals(voidium$suppressedQuery)) {
                // Stay suppressed, don't show suggestions
                return;
            }

            // Query changed from suppressed one - reset suppression
            if (voidium$suppressed && !query.equals(voidium$suppressedQuery)) {
                voidium$suppressed = false;
                voidium$suppressedQuery = "";
            }

            // Only update if query changed, enabling navigation
            if (!query.equals(voidium$currentQuery)) {
                voidium$updateSuggestions(query);
            } else if (!voidium$showingSuggestions) {
                // If not showing (but valid query), try showing (rare case,
                // maybe after returning from menu)
                voidium$updateSuggestions(query);
            }
        } else {
            // Invalid query, reset everything
            voidium$closeSuggestions();
            voidium$suppressed = false;
            voidium$suppressedQuery = "";
        }
    }

    /**
     * Extract the current emoji query (text after last ':' before cursor).
     */
    @Unique
    private String voidium$extractEmojiQuery(String text, int cursorPos) {
        if (cursorPos > text.length())
            cursorPos = text.length();

        String beforeCursor = text.substring(0, cursorPos);
        int colonIndex = beforeCursor.lastIndexOf(':');

        if (colonIndex == -1)
            return null;

        // Check if there's a space between colon and cursor (not in emoji mode)
        String afterColon = beforeCursor.substring(colonIndex + 1);
        if (afterColon.contains(" "))
            return null;

        // Check if it's a completed emoji (has closing colon)
        if (afterColon.contains(":"))
            return null;

        return afterColon;
    }

    /**
     * Update suggestions list based on query.
     */
    @Unique
    private void voidium$updateSuggestions(String query) {
        voidium$currentQuery = query;
        voidium$suggestions.clear();

        String lowerQuery = query.toLowerCase();

        for (String emojiName : EmojiManager.getInstance().getEmojiNames()) {
            if (emojiName.toLowerCase().startsWith(lowerQuery)) {
                voidium$suggestions.add(emojiName);
                if (voidium$suggestions.size() >= MAX_SUGGESTIONS * 2)
                    break; // Get a bit more for sorting
            }
        }

        // Also check contains (not just starts with)
        if (voidium$suggestions.size() < MAX_SUGGESTIONS) {
            for (String emojiName : EmojiManager.getInstance().getEmojiNames()) {
                if (emojiName.toLowerCase().contains(lowerQuery) && !voidium$suggestions.contains(emojiName)) {
                    voidium$suggestions.add(emojiName);
                    if (voidium$suggestions.size() >= MAX_SUGGESTIONS * 2)
                        break;
                }
            }
        }

        // Sort by relevance (starts with first, then alphabetically)
        voidium$suggestions.sort((a, b) -> {
            boolean aStarts = a.toLowerCase().startsWith(lowerQuery);
            boolean bStarts = b.toLowerCase().startsWith(lowerQuery);
            if (aStarts && !bStarts)
                return -1;
            if (!aStarts && bStarts)
                return 1;
            return a.compareToIgnoreCase(b);
        });

        // Limit to MAX_SUGGESTIONS
        if (voidium$suggestions.size() > MAX_SUGGESTIONS) {
            voidium$suggestions = new ArrayList<>(voidium$suggestions.subList(0, MAX_SUGGESTIONS));
        }

        voidium$showingSuggestions = !voidium$suggestions.isEmpty();
        voidium$selectedIndex = 0;
    }

    /**
     * Apply the selected suggestion to the input.
     */
    @Unique
    private void voidium$applySuggestion() {
        if (voidium$suggestions.isEmpty() || voidium$selectedIndex >= voidium$suggestions.size()) {
            return;
        }

        String selected = voidium$suggestions.get(voidium$selectedIndex);
        String text = input.getValue();
        int cursorPos = input.getCursorPosition();

        // Find the colon position
        String beforeCursor = text.substring(0, cursorPos);
        int colonIndex = beforeCursor.lastIndexOf(':');

        if (colonIndex == -1)
            return;

        // Replace `:query` with `:emoji:`
        String before = text.substring(0, colonIndex);
        String after = text.substring(cursorPos);
        String newText = before + ":" + selected + ":" + after;

        input.setValue(newText);
        input.setCursorPosition(colonIndex + selected.length() + 2); // Position after closing ':'

        voidium$closeSuggestions();
        voidium$suppressed = false;
    }

    /**
     * Close the suggestions popup.
     */
    @Unique
    private void voidium$closeSuggestions() {
        voidium$showingSuggestions = false;
        voidium$suggestions.clear();
        voidium$currentQuery = "";
        voidium$selectedIndex = 0;
    }

    /**
     * Get approximate position of the colon for popup positioning.
     */
    @Unique
    private int getColonPosition() {
        String text = input.getValue();
        int cursorPos = input.getCursorPosition();
        if (cursorPos > text.length())
            cursorPos = text.length();

        String beforeCursor = text.substring(0, cursorPos);
        int colonIndex = beforeCursor.lastIndexOf(':');
        return colonIndex >= 0 ? colonIndex : 0;
    }
}
