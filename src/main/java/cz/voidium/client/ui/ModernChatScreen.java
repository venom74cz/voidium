package cz.voidium.client.ui;

import cz.voidium.client.chat.ChatChannelManager;
import cz.voidium.client.media.MediaManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discord-style Modern Chat Screen (Compact Version)
 */
public class ModernChatScreen extends Screen {

    // Colors (Discord-inspired)
    private static final int BG_DARK = 0xEE202225; // Slightly opaque background
    private static final int BG_SECONDARY = 0xAA2F3136;
    private static final int BG_TERTIARY = 0xAA282B30; // Input background
    private static final int ACCENT = 0xFF5865F2; // Discord blurple
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFFB9BBBE;
    private static final int TEXT_MUTED = 0xFF72767D;
    private static final int GREEN = 0xFF57F287;
    private static final int YELLOW = 0xFFFEE75C;

    // Custom emoji pattern :emoji_name:
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-zA-Z0-9_]+):");

    // Layout constants
    private int panelX, panelY, panelWidth, panelHeight;
    private int inputHeight = 25;
    private int topBarHeight = 30; // Height for tabs
    private EditBox input;

    // Animation states
    private float animationTick = 0;
    private Map<String, Float> tabHoverProgress = new HashMap<>();

    // Wizard state (Private Room)
    private boolean showWizard = false;
    private EditBox wizardInput;

    // Command suggestions
    private net.minecraft.client.gui.components.CommandSuggestions commandSuggestions;

    // Autocomplete state
    private List<String> suggestions = new java.util.ArrayList<>();
    private int selectedSuggestion = 0;
    private boolean isSuggesting = false;
    private String suggestionQuery = "";

    public ModernChatScreen() {
        super(Component.literal(""));
    }

    @Override
    protected void init() {
        // Compact Design: 70% width, 60% height, centered
        // Max 400x300, or smaller if screen is tiny
        this.panelWidth = Math.min((int) (width * 0.85), 450);
        this.panelHeight = Math.min((int) (height * 0.85), 320);
        this.panelX = (width - panelWidth) / 2;
        this.panelY = (height - panelHeight) / 2;

        String oldValue = input != null ? input.getValue() : "";
        this.input = new EditBox(font, panelX + 10, panelY + panelHeight - 20, panelWidth - 40, 16,
                Component.literal(""));
        this.input.setMaxLength(256);
        this.input.setValue(oldValue);
        this.input.setBordered(false);
        this.input.setResponder(this::onInputChanged); // Hook for suggestions
        this.addWidget(this.input);

        // Init Command Suggestions (Vanilla)
        this.commandSuggestions = new net.minecraft.client.gui.components.CommandSuggestions(this.minecraft, this,
                this.input, this.font, false, true, 0, 7, true, Integer.MIN_VALUE);
        this.commandSuggestions.setAllowSuggestions(true);
        this.commandSuggestions.updateCommandInfo();

        // Wizard input
        this.wizardInput = new EditBox(font, panelX + panelWidth / 2 - 60, panelY + panelHeight / 2, 120, 16,
                Component.literal(""));
        this.wizardInput.setMaxLength(32);
        this.wizardInput.setBordered(true);
        this.wizardInput.setVisible(false);
        this.addWidget(this.wizardInput);

        // Focus input by default
        this.setFocused(this.input);
    }

    private void onInputChanged(String text) {
        if (this.commandSuggestions != null) {
            this.commandSuggestions.setAllowSuggestions(true);
            this.commandSuggestions.updateCommandInfo();
        }
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        if (this.commandSuggestions != null)
            this.commandSuggestions.updateCommandInfo();
    }

    @Override
    public void tick() {
        // Update emoji suggestions
        String val = input.getValue();
        int cursor = input.getCursorPosition(); // Assuming cursor at end for simplicity

        // Find if we are typing an emoji :something
        int lastColon = val.lastIndexOf(':', cursor - 1);
        if (lastColon >= 0) {
            String query = val.substring(lastColon + 1, cursor);
            // Must not contain spaces and typically logic suggests we are in emoji mode
            if (!query.contains(" ")) {
                this.isSuggesting = true;
                this.suggestionQuery = query.toLowerCase();

                // Filter
                suggestions.clear();
                Set<String> allNames = cz.voidium.client.media.EmojiManager.getInstance().getEmojiNames();
                List<String> startsWith = new java.util.ArrayList<>();
                List<String> contains = new java.util.ArrayList<>();

                for (String name : allNames) {
                    String sortName = name.toLowerCase();
                    if (sortName.equals(this.suggestionQuery)) {
                        startsWith.add(0, name); // Prioritize exact
                    } else if (sortName.startsWith(this.suggestionQuery)) {
                        startsWith.add(name);
                    } else if (sortName.contains(this.suggestionQuery)) {
                        contains.add(name);
                    }
                }

                // Sort by length then alpha
                java.util.Comparator<String> sorter = (a, b) -> {
                    int len = Integer.compare(a.length(), b.length());
                    if (len != 0)
                        return len;
                    return a.compareTo(b);
                };

                startsWith.sort(sorter);
                contains.sort(sorter);

                // Combine
                suggestions.addAll(startsWith);
                suggestions.addAll(contains);

                // Limit to 7
                if (suggestions.size() > 7)
                    suggestions = suggestions.subList(0, 7);
                if (suggestions.isEmpty())
                    isSuggesting = false;

                // Clamp selection
                if (selectedSuggestion >= suggestions.size())
                    selectedSuggestion = 0;
            } else {
                isSuggesting = false;
            }
        } else {
            isSuggesting = false;
        }

        animationTick++;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.commandSuggestions != null && this.commandSuggestions.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (showWizard) {
            this.wizardInput.mouseClicked(mouseX, mouseY, button);

            int w = 200;
            int h = 180;
            int x = panelX + panelWidth / 2 - w / 2;
            int y = panelY + panelHeight / 2 - h / 2;

            // Close wizard if clicked outside
            if (button == 0) {
                if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) {
                    showWizard = false;
                    wizardInput.setVisible(false);
                    setFocused(input);
                    return true;
                }

                // Create Button
                int btnY = y + h - 25;
                if (mouseX > x + 40 && mouseX < x + 160 && mouseY > btnY && mouseY < btnY + 15) {
                    confirmWizard();
                    return true;
                }

                // Player List Click
                int listX = x + 10;
                int listY = y + 65;
                int listW = w - 20;
                int listH = h - 95;

                if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
                    var connection = this.minecraft.getConnection();
                    if (connection != null) {
                        var players = new java.util.ArrayList<>(connection.getOnlinePlayers());
                        players.removeIf(
                                p -> p.getProfile().getName().equals(this.minecraft.player.getGameProfile().getName()));
                        players.sort(java.util.Comparator.comparing(p -> p.getProfile().getName()));

                        int relativeY = (int) mouseY - listY + wizardScroll;
                        int index = relativeY / 12;

                        if (index >= 0 && index < players.size()) {
                            String clickedName = players.get(index).getProfile().getName();
                            String current = wizardInput.getValue();
                            if (current.isEmpty()) {
                                wizardInput.setValue(clickedName);
                            } else {
                                // Toggle logic or append
                                if (current.contains(clickedName)) {
                                    // Remove (complex with commas, lazy fix: just clear if single, or ignore)
                                } else {
                                    wizardInput.setValue(current + "," + clickedName);
                                }
                            }
                        }
                    }
                }
            }
            return true;
        }

        // Tab switching logic
        if (button == 0) { // Left click
            var channels = ChatChannelManager.getInstance().getAllChannels();
            int x = panelX + 10;
            int y = panelY + 6;
            int tabHeight = 18;

            for (var ch : channels) {
                String name = ch.getDisplayName();
                int nameWidth = font.width(name);
                int w = nameWidth + 12;
                if (ch.isRemovable())
                    w += 10;

                // Active area for tab
                if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + tabHeight) {

                    // Check close button click
                    if (ch.isRemovable()) {
                        if (mouseX >= x + w - 12 && mouseX <= x + w - 2 && mouseY >= y + 4 && mouseY <= y + 14) {
                            ChatChannelManager.getInstance().removeChannel(ch.getId());
                            return true;
                        }
                    }

                    ChatChannelManager.getInstance().setActiveChannel(ch.getId());
                    return true;
                }
                x += w + 5;
            }

            // "+" Button click check
            if (mouseX >= x && mouseX <= x + 15 && mouseY >= y && mouseY <= y + 15) {
                showWizard = true;
                wizardInput.setValue("");
                wizardInput.setVisible(true);
                wizardInput.setFocused(true);
                setFocused(wizardInput);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.commandSuggestions != null && this.commandSuggestions.mouseScrolled(scrollY)) {
            return true;
        }

        if (showWizard) {
            wizardScroll -= scrollY * 12;
            if (wizardScroll < 0)
                wizardScroll = 0;
            // Max scroll could be calculated but simple limit is fine for now
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Command suggestions hooks
        if (this.commandSuggestions != null && this.commandSuggestions.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        // Emoji autocomplete hooks
        if (isSuggesting && !suggestions.isEmpty()) {
            if (keyCode == 265) { // Up
                selectedSuggestion--;
                if (selectedSuggestion < 0)
                    selectedSuggestion = suggestions.size() - 1;
                return true;
            }
            if (keyCode == 264) { // Down
                selectedSuggestion++;
                if (selectedSuggestion >= suggestions.size())
                    selectedSuggestion = 0;
                return true;
            }
            if (keyCode == 258 || keyCode == 257) { // Tab or Enter
                completeSuggestion();
                return true;
            }
        }

        if (showWizard) {
            if (keyCode == 257) { // Enter
                confirmWizard();
                return true;
            }
            if (keyCode == 256) { // Escape
                showWizard = false;
                wizardInput.setVisible(false);
                setFocused(input);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == 257 && !input.getValue().isEmpty()) { // Enter
            sendMessage();
            return true;
        }
        if (keyCode == 256) { // Escape
            this.minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void completeSuggestion() {
        if (suggestions.isEmpty())
            return;
        String completion = suggestions.get(selectedSuggestion);
        String val = input.getValue();
        int lastColon = val.lastIndexOf(':');

        String before = val.substring(0, lastColon);
        input.setValue(before + ":" + completion + ": ");
        input.setCursorPosition(input.getValue().length());
        isSuggesting = false;
    }

    private void confirmWizard() {
        String val = wizardInput.getValue().trim();
        if (!val.isEmpty()) {
            if (val.startsWith("ticket:")) {
                // Manual ticket shorthand if needed
            } else {
                // Check if multiple names (comma separated)
                String[] names = val.split(",");
                if (names.length > 1) {
                    // Group DM
                    String channelId = "dm-" + val.toLowerCase().replace(" ", "");
                    ChatChannelManager.getInstance().getOrCreateChannel(channelId, "@" + val); // Display name as comma
                                                                                               // list
                    ChatChannelManager.getInstance().setActiveChannel(channelId);
                } else {
                    // Single DM
                    String channelId = "dm-" + val.toLowerCase();
                    ChatChannelManager.getInstance().getOrCreateChannel(channelId, "@" + val);
                    ChatChannelManager.getInstance().setActiveChannel(channelId);
                }
            }
        }
        showWizard = false;
        wizardInput.setVisible(false);
        setFocused(input);
    }

    private void sendMessage() {
        String text = input.getValue().trim();
        if (text.isEmpty())
            return;

        var active = ChatChannelManager.getInstance().getActiveChannel();
        String activeId = active != null ? active.getId() : "main";

        if (activeId.startsWith("ticket-")) {
            this.minecraft.player.connection.sendCommand("reply " + text);
        } else if (activeId.startsWith("dm-")) {
            // Direct Message
            String target = activeId.substring(3); // remove "dm-"
            this.minecraft.player.connection.sendCommand("msg " + target + " " + text);
            // Local echo
            ChatChannelManager.getInstance().addMessage(activeId,
                    Component.literal("§7[You -> " + target + "] " + text));
        } else {
            // Check for commands
            if (text.startsWith("/")) {
                this.minecraft.player.connection.sendCommand(text.substring(1));
            } else {
                this.minecraft.player.connection.sendChat(text);
            }
        }

        input.setValue("");
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // No dark background overlay - float in center

        // 1. Main Panel Background (Glassmorphism)
        renderMainPanel(gfx);

        // 2. Top Tabs (Channels)
        renderTopTabs(gfx, mouseX, mouseY);

        // 3. Messages Area
        renderMessages(gfx, mouseX, mouseY);

        // 4. Input Area
        renderInputArea(gfx);

        // Render stylized input text
        int inputX = panelX + 30; // shifted for emoji button
        int inputY = panelY + panelHeight - 18;

        // Placeholder
        if (input.getValue().isEmpty() && !input.isFocused()) {
            gfx.drawString(font, "Message...", inputX, inputY, TEXT_MUTED, false);
        }

        // Input widget handles its own rendering (text only because bordered=false)
        input.setX(inputX);
        input.setY(inputY - 1);
        input.render(gfx, mouseX, mouseY, partialTick);

        // Command Suggestions
        if (this.commandSuggestions != null) {
            this.commandSuggestions.render(gfx, mouseX, mouseY);
        }

        // 5. Suggestions Overlay
        if (isSuggesting && !suggestions.isEmpty()) {
            renderSuggestions(gfx);
        }

        // 6. Wizard Overlay
        if (showWizard) {
            renderWizard(gfx, mouseX, mouseY);
        }
    }

    // Wizard Scroll
    private int wizardScroll = 0;

    private void renderWizard(GuiGraphics gfx, int mouseX, int mouseY) {
        int w = 200;
        int h = 180;
        int x = panelX + panelWidth / 2 - w / 2;
        int y = panelY + panelHeight / 2 - h / 2;

        // Background
        gfx.fill(x, y, x + w, y + h, 0xFF202225);
        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, ACCENT); // Border

        gfx.drawCenteredString(font, "New Private Message", x + w / 2, y + 10, TEXT_PRIMARY);
        gfx.drawString(font, "Recipients (comma separated):", x + 10, y + 25, TEXT_SECONDARY, false);

        // Adjust input position
        wizardInput.setX(x + 10);
        wizardInput.setY(y + 35);
        wizardInput.setWidth(w - 20);
        wizardInput.render(gfx, mouseX, mouseY, 0);

        // Player List Header
        gfx.drawString(font, "Select Players:", x + 10, y + 55, TEXT_SECONDARY, false);

        // Player List Area
        int listX = x + 10;
        int listY = y + 65;
        int listW = w - 20;
        int listH = h - 95; // Leave space for button

        gfx.fill(listX, listY, listX + listW, listY + listH, 0x40000000);

        // Fetch players
        var connection = this.minecraft.getConnection();
        if (connection != null) {
            var players = new java.util.ArrayList<>(connection.getOnlinePlayers());
            players.removeIf(p -> p.getProfile().getName().equals(this.minecraft.player.getGameProfile().getName()));

            // Sorting
            players.sort(java.util.Comparator.comparing(p -> p.getProfile().getName()));

            gfx.enableScissor(listX, listY, listX + listW, listY + listH);
            int itemY = listY + 2 - wizardScroll;

            for (var p : players) {
                String name = p.getProfile().getName();
                boolean isHovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= itemY
                        && mouseY <= itemY + 12;

                // Highlight if chosen
                boolean isChosen = wizardInput.getValue().contains(name);

                if (isChosen) {
                    gfx.fill(listX, itemY, listX + listW, itemY + 12, 0x405865F2);
                } else if (isHovered) {
                    gfx.fill(listX, itemY, listX + listW, itemY + 12, 0x20FFFFFF);
                }

                // Render Head (approximation or just name)
                // Head rendering is complex without easy access to skin location here, strictly
                // use text for now
                gfx.drawString(font, name, listX + 4, itemY + 2, TEXT_PRIMARY, false);

                itemY += 12;
            }
            gfx.disableScissor();
        }

        // Create Button
        int btnColor = 0xFF5865F2;
        int btnY = y + h - 25;
        boolean hover = mouseX > x + 40 && mouseX < x + 160 && mouseY > btnY && mouseY < btnY + 15;
        if (hover)
            btnColor = 0xFF4752C4;

        gfx.fill(x + 40, btnY, x + 160, btnY + 15, btnColor);
        gfx.drawCenteredString(font, "Create Group", x + 100, btnY + 4, TEXT_PRIMARY);
    }

    private void renderSuggestions(GuiGraphics gfx) {
        int w = 120;
        int h = suggestions.size() * 14 + 4;
        int x = panelX + 30;
        int y = panelY + panelHeight - 20 - h; // Above input

        // Background
        gfx.fill(x, y, x + w, y + h, 0xF0202225);
        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF5865F2); // Border

        int itemY = y + 2;
        for (int i = 0; i < suggestions.size(); i++) {
            String s = suggestions.get(i);
            boolean selected = i == selectedSuggestion;

            if (selected) {
                gfx.fill(x, itemY, x + w, itemY + 14, 0x405865F2);
            }

            // Render icon if available
            ResourceLocation tex = cz.voidium.client.media.EmojiManager.getInstance().getEmojiTexture(s);
            if (tex != null) {
                gfx.blit(tex, x + 4, itemY + 2, 0, 0, 9, 9, 9, 9);
            }

            gfx.drawString(font, ":" + s + ":", x + 16, itemY + 3, selected ? 0xFFFFFFFF : 0xFFB9BBBE, false);
            itemY += 14;
        }
    }

    private void renderMainPanel(GuiGraphics gfx) {
        // Main window body
        gfx.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, BG_DARK);

        // Top Bar background (slightly lighter)
        gfx.fill(panelX, panelY, panelX + panelWidth, panelY + topBarHeight, BG_SECONDARY);

        // Borders/Glow
        int glowColor = 0x40000000;
        gfx.fill(panelX - 1, panelY, panelX, panelY + panelHeight, glowColor);
        gfx.fill(panelX + panelWidth, panelY, panelX + panelWidth + 1, panelY + panelHeight, glowColor);
        gfx.fill(panelX, panelY + panelHeight, panelX + panelWidth, panelY + panelHeight + 1, glowColor);
        gfx.fill(panelX, panelY, panelX + panelWidth, panelY + 1, glowColor); // top
        gfx.fill(panelX, panelY + panelHeight, panelX + panelWidth, panelY + panelHeight + 1, glowColor); // bottom
    }

    private void renderTopTabs(GuiGraphics gfx, int mouseX, int mouseY) {
        var channels = ChatChannelManager.getInstance().getAllChannels();
        var activeChannel = ChatChannelManager.getInstance().getActiveChannel();

        int x = panelX + 10;
        int y = panelY + 6;
        int tabHeight = 18;

        for (var ch : channels) {
            String name = ch.getDisplayName();
            int nameWidth = font.width(name);
            int w = nameWidth + 12;

            // Extra width for close button if removable
            if (ch.isRemovable()) {
                w += 10;
            }

            boolean isActive = activeChannel != null && activeChannel.getId().equals(ch.getId());
            boolean isHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + tabHeight;

            // Animation logic
            float target = isHovered || isActive ? 1.0f : 0.0f;
            float current = tabHoverProgress.getOrDefault(ch.getId(), 0.0f);
            current = current + (target - current) * 0.2f;
            tabHoverProgress.put(ch.getId(), current);

            // Background (Pill shape)
            int alpha = (int) (current * 64);
            if (isActive)
                alpha = 100;

            if (alpha > 5) {
                // Draw pill
                gfx.fill(x, y, x + w, y + tabHeight, 0x40FFFFFF);
            }

            // Text
            int textColor = isActive ? 0xFFFFFFFF : 0xFFB9BBBE;
            gfx.drawString(font, name, x + 6, y + 5, textColor, false);

            // Close Button (X)
            if (ch.isRemovable()) {
                boolean closeHover = mouseX >= x + w - 12 && mouseX <= x + w - 2 && mouseY >= y + 4 && mouseY <= y + 14;
                int closeColor = closeHover ? 0xFFFF5555 : 0xFFB9BBBE;
                gfx.drawString(font, "x", x + w - 10, y + 5, closeColor, false);
            }

            x += w + 5;
        }

        // "+" Button for new channel/group
        gfx.drawString(font, "§a+", x + 4, y + 5, 0xFF55FF55, false);
    }

    private void renderMessages(GuiGraphics gfx, int mouseX, int mouseY) {
        int msgX = panelX + 10;
        int msgY = panelY + topBarHeight + 10;
        int msgWidth = panelWidth - 20;
        int msgHeight = panelHeight - topBarHeight - inputHeight - 15;

        // Enable scissor to clip overflow
        gfx.enableScissor(msgX, msgY, msgX + msgWidth, msgY + msgHeight);

        var activeChannel = ChatChannelManager.getInstance().getActiveChannel();
        if (activeChannel == null) {
            gfx.disableScissor();
            return;
        }

        List<Component> messages = activeChannel.getHistory();
        if (messages.isEmpty()) {
            gfx.drawCenteredString(font, "§7No messages yet...", panelX + panelWidth / 2, msgY + msgHeight / 2,
                    TEXT_MUTED);
            gfx.disableScissor();
            return;
        }

        // Start rendering from bottom
        int y = msgY + msgHeight;
        int lineHeight = 12;

        for (int i = messages.size() - 1; i >= 0; i--) {
            if (y < msgY - lineHeight)
                break; // Stop if fully clipped

            Component msg = messages.get(i);
            String text = msg.getString();

            // Word wrap the message
            List<String> wrappedLines = wrapText(text, msgWidth);

            // Render wrapped lines from bottom up
            for (int j = wrappedLines.size() - 1; j >= 0; j--) {
                y -= lineHeight;
                if (y >= msgY - lineHeight) {
                    renderStyledMessage(gfx, wrappedLines.get(j), msgX, y);
                }
            }
        }

        gfx.disableScissor();
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }

        // Simple word-level wrapping
        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (font.width(testLine) <= maxWidth) {
                if (currentLine.length() > 0)
                    currentLine.append(" ");
                currentLine.append(word);
            } else {
                // Line is too long, wrap
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // Single word is too long, force it anyway
                    lines.add(word);
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private void renderStyledMessage(GuiGraphics gfx, String text, int x, int y) {
        // Parse for custom emojis :emoji:
        Matcher emojiMatcher = EMOJI_PATTERN.matcher(text);
        int lastEnd = 0;
        int currentX = x;

        while (emojiMatcher.find()) {
            // Render text before emoji
            String before = text.substring(lastEnd, emojiMatcher.start());
            if (!before.isEmpty()) {
                gfx.drawString(font, before, currentX, y, TEXT_PRIMARY, false);
                currentX += font.width(before);
            }

            // Try to render emoji as texture
            String emojiName = emojiMatcher.group(1);
            ResourceLocation emojiTexture = cz.voidium.client.media.EmojiManager.getInstance()
                    .getEmojiTexture(emojiName);

            if (emojiTexture != null) {
                // Render texture
                int size = 9; // Slightly larger than font
                gfx.blit(emojiTexture, currentX, y, 0, 0, size, size, size, size);
                currentX += size + 1;
            } else {
                // Fallback to text if texture not loaded
                String emojiDisplay = ":" + emojiName + ":";
                gfx.drawString(font, emojiDisplay, currentX, y, YELLOW, false);
                currentX += font.width(emojiDisplay);
            }

            lastEnd = emojiMatcher.end();
        }

        // Render remaining text
        if (lastEnd < text.length()) {
            gfx.drawString(font, text.substring(lastEnd), currentX, y, TEXT_PRIMARY, false);
        }
    }

    private void renderInputArea(GuiGraphics gfx) {
        int inputY = panelY + panelHeight - inputHeight;

        // Input background
        gfx.fill(panelX, inputY, panelX + panelWidth, panelY + panelHeight, BG_TERTIARY);

        // Separator
        gfx.fill(panelX, inputY, panelX + panelWidth, inputY + 1, 0x30FFFFFF);

        // Emoji Button
        int btnX = panelX + 8;
        int btnY = inputY + 8;

        // Emoji icon
        gfx.drawString(font, "☻", btnX + 4, btnY, TEXT_SECONDARY, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
