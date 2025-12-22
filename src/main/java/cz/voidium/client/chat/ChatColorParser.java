package cz.voidium.client.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses chat messages for RGB/Hex color codes and Discord emojis.
 * 
 * Supported color formats:
 * - &#RRGGBB or &#RGB (short form) - Standard hex format
 * - <#RRGGBB> or <#RGB> - Bracketed hex format
 * - &x&R&R&G&G&B&B - Spigot/Bukkit format
 * - Minecraft color codes (&0-9, &a-f, &l, &m, &n, &o, &r)
 * 
 * Emoji format:
 * - :emoji_name: - Discord-style emoji (rendered as textures via EmojiManager)
 */
public class ChatColorParser {

    // Pattern for &#RRGGBB or &#RGB format
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})");

    // Pattern for <#RRGGBB> or <#RGB> format
    private static final Pattern BRACKETED_HEX_PATTERN = Pattern.compile("<#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})>");

    // Pattern for &x&R&R&G&G&B&B Spigot format
    private static final Pattern SPIGOT_HEX_PATTERN = Pattern.compile("&x(&[0-9a-fA-F]){6}", Pattern.CASE_INSENSITIVE);

    // Pattern for Minecraft color codes &0-9, &a-f, &k-o, &r
    private static final Pattern MINECRAFT_COLOR_PATTERN = Pattern.compile("&([0-9a-fk-or])", Pattern.CASE_INSENSITIVE);

    // Pattern for Discord-style emoji :name:
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-zA-Z0-9_]+):");

    // Minecraft color code mapping
    private static final int[] MINECRAFT_COLORS = {
            0x000000, // 0 - Black
            0x0000AA, // 1 - Dark Blue
            0x00AA00, // 2 - Dark Green
            0x00AAAA, // 3 - Dark Aqua
            0xAA0000, // 4 - Dark Red
            0xAA00AA, // 5 - Dark Purple
            0xFFAA00, // 6 - Gold
            0xAAAAAA, // 7 - Gray
            0x555555, // 8 - Dark Gray
            0x5555FF, // 9 - Blue
            0x55FF55, // a - Green
            0x55FFFF, // b - Aqua
            0xFF5555, // c - Red
            0xFF55FF, // d - Light Purple
            0xFFFF55, // e - Yellow
            0xFFFFFF // f - White
    };

    /**
     * Parse a Component and transform any color codes and emojis.
     * Returns the original component if no transformation needed.
     */
    public static Component parseMessage(Component original) {
        String plainText = original.getString();

        // Quick check - if no special characters, return original
        if (!containsColorCodes(plainText) && !containsEmoji(plainText)) {
            return original;
        }

        // Parse and build new component
        return buildStyledComponent(plainText);
    }

    /**
     * Check if text contains any color code patterns.
     */
    private static boolean containsColorCodes(String text) {
        return text.contains("&#") ||
                text.contains("<#") ||
                text.contains("&x") ||
                MINECRAFT_COLOR_PATTERN.matcher(text).find();
    }

    /**
     * Check if text contains emoji patterns.
     */
    private static boolean containsEmoji(String text) {
        return text.contains(":") && EMOJI_PATTERN.matcher(text).find();
    }

    /**
     * Build a styled MutableComponent from text with color codes and emojis.
     */
    private static MutableComponent buildStyledComponent(String text) {
        // First, normalize all hex formats to a unified internal format
        text = normalizeHexFormats(text);

        // Parse into segments with their styles
        List<StyledSegment> segments = parseSegments(text);

        // Build the final component
        MutableComponent result = Component.empty();
        for (StyledSegment segment : segments) {
            if (segment.isEmoji) {
                // For emoji, we keep the :name: format - rendering is handled by EmojiManager
                // in the font renderer mixin
                result.append(Component.literal(segment.text).withStyle(segment.style));
            } else {
                result.append(Component.literal(segment.text).withStyle(segment.style));
            }
        }

        return result;
    }

    /**
     * Normalize different hex formats to internal &#RRGGBB format.
     */
    private static String normalizeHexFormats(String text) {
        // Convert <#RRGGBB> to &#RRGGBB
        Matcher bracketMatcher = BRACKETED_HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (bracketMatcher.find()) {
            String hex = expandShortHex(bracketMatcher.group(1));
            bracketMatcher.appendReplacement(sb, "&#" + hex);
        }
        bracketMatcher.appendTail(sb);
        text = sb.toString();

        // Convert &x&R&R&G&G&B&B to &#RRGGBB
        Matcher spigotMatcher = SPIGOT_HEX_PATTERN.matcher(text);
        sb = new StringBuffer();
        while (spigotMatcher.find()) {
            String spigotHex = spigotMatcher.group();
            String hex = spigotHex.replaceAll("&[xX]|&", "");
            spigotMatcher.appendReplacement(sb, "&#" + hex);
        }
        spigotMatcher.appendTail(sb);
        text = sb.toString();

        // Expand short hex &#RGB to &#RRGGBB
        Matcher shortHexMatcher = Pattern.compile("&#([0-9a-fA-F]{3})(?![0-9a-fA-F])").matcher(text);
        sb = new StringBuffer();
        while (shortHexMatcher.find()) {
            String expanded = expandShortHex(shortHexMatcher.group(1));
            shortHexMatcher.appendReplacement(sb, "&#" + expanded);
        }
        shortHexMatcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Expand #RGB to #RRGGBB format.
     */
    private static String expandShortHex(String hex) {
        if (hex.length() == 3) {
            char r = hex.charAt(0);
            char g = hex.charAt(1);
            char b = hex.charAt(2);
            return "" + r + r + g + g + b + b;
        }
        return hex;
    }

    /**
     * Parse text into styled segments.
     */
    private static List<StyledSegment> parseSegments(String text) {
        List<StyledSegment> segments = new ArrayList<>();
        Style currentStyle = Style.EMPTY;
        StringBuilder currentText = new StringBuilder();

        int i = 0;
        while (i < text.length()) {
            // Check for &#RRGGBB hex code
            if (i + 8 <= text.length() && text.charAt(i) == '&' && text.charAt(i + 1) == '#') {
                String hexPart = text.substring(i + 2, Math.min(i + 8, text.length()));
                if (hexPart.length() == 6 && hexPart.matches("[0-9a-fA-F]{6}")) {
                    // Save current segment
                    if (currentText.length() > 0) {
                        segments.add(new StyledSegment(currentText.toString(), currentStyle, false));
                        currentText = new StringBuilder();
                    }
                    // Apply new color
                    int color = Integer.parseInt(hexPart, 16);
                    currentStyle = currentStyle.withColor(TextColor.fromRgb(color));
                    i += 8;
                    continue;
                }
            }

            // Check for Minecraft color codes &X
            if (i + 1 < text.length() && text.charAt(i) == '&') {
                char code = Character.toLowerCase(text.charAt(i + 1));
                Style newStyle = applyMinecraftCode(currentStyle, code);
                if (newStyle != currentStyle) {
                    // Save current segment
                    if (currentText.length() > 0) {
                        segments.add(new StyledSegment(currentText.toString(), currentStyle, false));
                        currentText = new StringBuilder();
                    }
                    currentStyle = newStyle;
                    i += 2;
                    continue;
                }
            }

            // Check for emoji :name:
            if (text.charAt(i) == ':') {
                Matcher emojiMatcher = EMOJI_PATTERN.matcher(text.substring(i));
                if (emojiMatcher.lookingAt()) {
                    String emojiName = emojiMatcher.group(1);
                    // Save current segment
                    if (currentText.length() > 0) {
                        segments.add(new StyledSegment(currentText.toString(), currentStyle, false));
                        currentText = new StringBuilder();
                    }
                    // Add emoji segment (keeping :name: format for rendering)
                    segments.add(new StyledSegment(":" + emojiName + ":", currentStyle, true));
                    i += emojiMatcher.end();
                    continue;
                }
            }

            // Regular character
            currentText.append(text.charAt(i));
            i++;
        }

        // Add remaining text
        if (currentText.length() > 0) {
            segments.add(new StyledSegment(currentText.toString(), currentStyle, false));
        }

        return segments;
    }

    /**
     * Apply Minecraft color/format code to style.
     */
    private static Style applyMinecraftCode(Style style, char code) {
        // Colors 0-9
        if (code >= '0' && code <= '9') {
            int colorIndex = code - '0';
            return Style.EMPTY.withColor(TextColor.fromRgb(MINECRAFT_COLORS[colorIndex]));
        }
        // Colors a-f
        if (code >= 'a' && code <= 'f') {
            int colorIndex = 10 + (code - 'a');
            return Style.EMPTY.withColor(TextColor.fromRgb(MINECRAFT_COLORS[colorIndex]));
        }
        // Formatting codes
        switch (code) {
            case 'k':
                return style.withObfuscated(true);
            case 'l':
                return style.withBold(true);
            case 'm':
                return style.withStrikethrough(true);
            case 'n':
                return style.withUnderlined(true);
            case 'o':
                return style.withItalic(true);
            case 'r':
                return Style.EMPTY; // Reset
            default:
                return style;
        }
    }

    /**
     * Represents a segment of text with its style.
     */
    private static class StyledSegment {
        final String text;
        final Style style;
        final boolean isEmoji;

        StyledSegment(String text, Style style, boolean isEmoji) {
            this.text = text;
            this.style = style;
            this.isEmoji = isEmoji;
        }
    }
}
