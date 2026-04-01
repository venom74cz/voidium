package cz.voidium.playerlist;

import cz.voidium.config.PlayerListConfig;
import cz.voidium.config.RanksConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerListManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("voidium-playerlist");
    private static final String DEFAULT_PLAYER_NAME_FORMAT = "%rank_prefix%%player_name%%rank_suffix%";
    private static PlayerListManager instance;

    private MinecraftServer server;
    private ScheduledExecutorService executor;
    private boolean registeredEvents;

    private PlayerListManager() {
    }

    public static PlayerListManager getInstance() {
        if (instance == null) {
            instance = new PlayerListManager();
        }
        return instance;
    }

    public void start(MinecraftServer server) {
        stop();
        this.server = server;

        PlayerListConfig config = PlayerListConfig.getInstance();
        if (!config.isEnableCustomPlayerList()) {
            LOGGER.info("Custom player list is disabled");
            return;
        }

        if (config.isEnableCustomNames()) {
            NeoForge.EVENT_BUS.register(this);
            registeredEvents = true;
            refreshPlayerNames();
        }

        int interval = Math.max(3, config.getUpdateIntervalSeconds());
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::updatePlayerList, 0, interval, TimeUnit.SECONDS);

        LOGGER.info("Player list manager started (update interval: {}s)", interval);
    }

    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        if (registeredEvents) {
            NeoForge.EVENT_BUS.unregister(this);
            registeredEvents = false;
        }

        refreshPlayerNames();
    }

    private void updatePlayerList() {
        if (server == null) {
            return;
        }

        PlayerListConfig config = PlayerListConfig.getInstance();
        if (!config.isEnableCustomPlayerList()) {
            return;
        }

        runOnServerThread(this::updatePlayerListOnServerThread);
    }

    private void updatePlayerListOnServerThread() {
        if (server == null) {
            return;
        }

        PlayerListConfig config = PlayerListConfig.getInstance();
        try {
            List<ServerPlayer> players = List.copyOf(server.getPlayerList().getPlayers());
            for (ServerPlayer player : players) {
                int ping = player.connection.latency();
                String header = buildTextWithPlayer(config.getHeaderLine1(), config.getHeaderLine2(),
                        config.getHeaderLine3(), player, ping);
                String footer = buildTextWithPlayer(config.getFooterLine1(), config.getFooterLine2(),
                        config.getFooterLine3(), player, ping);

                player.connection.send(new net.minecraft.network.protocol.game.ClientboundTabListPacket(
                        Component.literal(header), Component.literal(footer)));

                if (config.isEnableCustomNames()) {
                    player.refreshDisplayName();
                    player.refreshTabListName();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error updating player list", e);
        }
    }

    @SubscribeEvent
    public void onNameFormat(PlayerEvent.NameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isCustomNameFormattingEnabled()) {
            return;
        }

        event.setDisplayname(buildFormattedPlayerName(player, event.getDisplayname()));
    }

    @SubscribeEvent
    public void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isCustomNameFormattingEnabled()) {
            return;
        }

        Component baseName = event.getDisplayName() != null ? event.getDisplayName() : player.getName().copy();
        event.setDisplayName(buildFormattedPlayerName(player, baseName));
    }

    private boolean isCustomNameFormattingEnabled() {
        if (server == null) {
            return false;
        }

        cz.voidium.config.GeneralConfig generalConfig = cz.voidium.config.GeneralConfig.getInstance();
        PlayerListConfig config = PlayerListConfig.getInstance();
        return generalConfig != null
                && generalConfig.isEnablePlayerList()
                && config.isEnableCustomPlayerList()
                && config.isEnableCustomNames();
    }

    private Component buildFormattedPlayerName(ServerPlayer player, Component baseName) {
        NameDecoration decoration = collectNameDecoration(player);
        Component playerNameComponent = baseName.copy();

        if (decoration.playerNameColor() != null) {
            playerNameComponent = playerNameComponent.copy()
                    .withStyle(style -> style.withColor(decoration.playerNameColor()));
        }

        return applyNameFormat(PlayerListConfig.getInstance().getPlayerNameFormat(), decoration.prefixComponent(),
                playerNameComponent, decoration.suffixComponent());
    }

    private NameDecoration collectNameDecoration(ServerPlayer player) {
        PlayerListConfig config = PlayerListConfig.getInstance();
        MutableComponent prefixComponent = Component.empty();
        MutableComponent suffixComponent = Component.empty();
        TextColor playerNameColor = null;
        boolean hasPrefix = false;
        boolean hasSuffix = false;
        boolean combineMultiple = config.isCombineMultipleRanks();

        cz.voidium.config.DiscordConfig discordConfig = cz.voidium.config.DiscordConfig.getInstance();
        if (discordConfig != null
                && discordConfig.isEnableDiscord()
                && discordConfig.getRolePrefixes() != null
                && !discordConfig.getRolePrefixes().isEmpty()) {
            try {
                List<String> playerRoleIds = cz.voidium.discord.DiscordManager.getInstance()
                        .getPlayerDiscordRoles(player.getUUID());

                if (!playerRoleIds.isEmpty()) {
                    List<Map.Entry<String, cz.voidium.config.DiscordConfig.RoleStyle>> matchingRoles = new ArrayList<>();
                    for (String roleId : playerRoleIds) {
                        cz.voidium.config.DiscordConfig.RoleStyle roleStyle = discordConfig.getRolePrefixes().get(roleId);
                        if (roleStyle != null) {
                            matchingRoles.add(Map.entry(roleId, roleStyle));
                        }
                    }

                    matchingRoles.sort(Comparator.comparingInt(
                            (Map.Entry<String, cz.voidium.config.DiscordConfig.RoleStyle> entry) -> entry.getValue().priority)
                            .reversed());

                    int processedRoles = 0;
                    for (Map.Entry<String, cz.voidium.config.DiscordConfig.RoleStyle> entry : matchingRoles) {
                        cz.voidium.config.DiscordConfig.RoleStyle roleStyle = entry.getValue();

                        if (roleStyle.prefix != null && !roleStyle.prefix.isBlank()) {
                            prefixComponent.append(Component.literal(ensureTrailingSpace(translateColorCodes(roleStyle.prefix))));
                            hasPrefix = true;
                        }

                        if (roleStyle.suffix != null && !roleStyle.suffix.isBlank()) {
                            suffixComponent.append(Component.literal(ensureLeadingSpace(translateColorCodes(roleStyle.suffix))));
                            hasSuffix = true;
                        }

                        if (playerNameColor == null && roleStyle.color != null && !roleStyle.color.isBlank()) {
                            playerNameColor = parseTextColor(roleStyle.color);
                        }

                        processedRoles++;
                        if (!combineMultiple && processedRoles >= 1) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[PlayerList] Error fetching Discord roles for {}: {}", player.getScoreboardName(),
                        e.getMessage());
            }
        }

        Component rankPrefixComponent = null;
        Component rankSuffixComponent = null;
        cz.voidium.config.GeneralConfig generalConfig = cz.voidium.config.GeneralConfig.getInstance();
        RanksConfig ranksConfig = RanksConfig.getInstance();
        if (generalConfig != null
                && generalConfig.isEnableRanks()
                && ranksConfig != null
                && ranksConfig.isEnableAutoRanks()
                && ranksConfig.getRanks() != null) {
            int ticksPlayed = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            double hoursPlayed = ticksPlayed / 20.0 / 3600.0;

            RanksConfig.RankDefinition bestTimePrefix = null;
            RanksConfig.RankDefinition bestTimeSuffix = null;

            for (RanksConfig.RankDefinition rank : ranksConfig.getRanks()) {
                if (hoursPlayed < rank.hours) {
                    continue;
                }

                boolean meetsConditions = true;
                if (rank.customConditions != null && !rank.customConditions.isEmpty()) {
                    String uuid = player.getUUID().toString();
                    for (RanksConfig.CustomCondition condition : rank.customConditions) {
                        if (!cz.voidium.ranks.ProgressTracker.getInstance()
                                .meetsCondition(uuid, condition.type, condition.count)) {
                            meetsConditions = false;
                            break;
                        }
                    }
                }
                if (!meetsConditions) {
                    continue;
                }

                if ("PREFIX".equalsIgnoreCase(rank.type)) {
                    if (bestTimePrefix == null || rank.hours > bestTimePrefix.hours) {
                        bestTimePrefix = rank;
                    }
                } else if ("SUFFIX".equalsIgnoreCase(rank.type)) {
                    if (bestTimeSuffix == null || rank.hours > bestTimeSuffix.hours) {
                        bestTimeSuffix = rank;
                    }
                }
            }

            if (bestTimePrefix != null) {
                String prefix = ensureTrailingSpace(translateColorCodes(bestTimePrefix.value));
                String playedText = ranksConfig.getTooltipPlayed().replace("%hours%", String.format("%.1f", hoursPlayed));
                String requiredText = ranksConfig.getTooltipRequired().replace("%hours%", String.valueOf(bestTimePrefix.hours));
                Component tooltip = Component.literal(playedText).append(Component.literal("\n" + requiredText));
                rankPrefixComponent = Component.literal(prefix)
                        .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip)));
            }

            if (bestTimeSuffix != null) {
                String suffix = ensureLeadingSpace(translateColorCodes(bestTimeSuffix.value));
                String playedText = ranksConfig.getTooltipPlayed().replace("%hours%", String.format("%.1f", hoursPlayed));
                String requiredText = ranksConfig.getTooltipRequired().replace("%hours%", String.valueOf(bestTimeSuffix.hours));
                Component tooltip = Component.literal(playedText).append(Component.literal("\n" + requiredText));
                rankSuffixComponent = Component.literal(suffix)
                        .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip)));
            }
        }

        if (!hasPrefix && rankPrefixComponent == null && config.getDefaultPrefix() != null
                && !config.getDefaultPrefix().isEmpty()) {
            prefixComponent.append(Component.literal(ensureTrailingSpace(translateColorCodes(config.getDefaultPrefix()))));
        }

        if (!hasSuffix && rankSuffixComponent == null && config.getDefaultSuffix() != null
                && !config.getDefaultSuffix().isEmpty()) {
            suffixComponent.append(Component.literal(ensureLeadingSpace(translateColorCodes(config.getDefaultSuffix()))));
        }

        if (rankPrefixComponent != null) {
            prefixComponent.append(rankPrefixComponent);
        }

        if (rankSuffixComponent != null) {
            suffixComponent.append(rankSuffixComponent);
        }

        return new NameDecoration(prefixComponent, suffixComponent, playerNameColor);
    }

    private Component applyNameFormat(String rawFormat, Component prefixComponent, Component playerNameComponent,
            Component suffixComponent) {
        String format = rawFormat == null || rawFormat.isBlank() ? DEFAULT_PLAYER_NAME_FORMAT : rawFormat;
        MutableComponent result = Component.empty();
        int cursor = 0;
        boolean insertedPlayerName = false;

        while (cursor < format.length()) {
            int nextIndex = format.length();
            String placeholder = null;

            for (String candidate : new String[] { "%rank_prefix%", "%player_name%", "%rank_suffix%" }) {
                int candidateIndex = format.indexOf(candidate, cursor);
                if (candidateIndex >= 0 && candidateIndex < nextIndex) {
                    nextIndex = candidateIndex;
                    placeholder = candidate;
                }
            }

            if (placeholder == null) {
                if (cursor < format.length()) {
                    result.append(Component.literal(translateColorCodes(format.substring(cursor))));
                }
                break;
            }

            if (nextIndex > cursor) {
                result.append(Component.literal(translateColorCodes(format.substring(cursor, nextIndex))));
            }

            if ("%rank_prefix%".equals(placeholder)) {
                result.append(prefixComponent.copy());
            } else if ("%player_name%".equals(placeholder)) {
                result.append(playerNameComponent.copy());
                insertedPlayerName = true;
            } else if ("%rank_suffix%".equals(placeholder)) {
                result.append(suffixComponent.copy());
            }

            cursor = nextIndex + placeholder.length();
        }

        if (!insertedPlayerName) {
            result.append(playerNameComponent.copy());
        }

        return result;
    }

    private void refreshPlayerNames() {
        if (server == null) {
            return;
        }

        runOnServerThread(() -> {
            List<ServerPlayer> players = List.copyOf(server.getPlayerList().getPlayers());
            for (ServerPlayer onlinePlayer : players) {
                onlinePlayer.refreshDisplayName();
                onlinePlayer.refreshTabListName();
            }
        });
    }

    private void runOnServerThread(Runnable action) {
        if (server == null) {
            return;
        }

        if (server.isSameThread()) {
            action.run();
        } else {
            server.execute(action);
        }
    }

    private String buildTextWithPlayer(String line1, String line2, String line3, ServerPlayer player, int ping) {
        if (server == null)
            return "";
        int online = server.getPlayerList().getPlayerCount();
        int max = server.getPlayerList().getMaxPlayers();
        double tps = getCurrentTPS();
        StringBuilder stringBuilder = new StringBuilder();
        if (!line1.isEmpty()) {
            stringBuilder.append(replacePlaceholdersFull(line1, online, max, tps, ping, player));
        }
        if (!line2.isEmpty()) {
            if (stringBuilder.length() > 0)
                stringBuilder.append("\n");
            stringBuilder.append(replacePlaceholdersFull(line2, online, max, tps, ping, player));
        }
        if (!line3.isEmpty()) {
            if (stringBuilder.length() > 0)
                stringBuilder.append("\n");
            stringBuilder.append(replacePlaceholdersFull(line3, online, max, tps, ping, player));
        }
        return stringBuilder.toString();
    }

    private String replacePlaceholdersFull(String text, int online, int max, double tps, int ping,
            ServerPlayer player) {
        double playtimeHours = 0.0;
        if (player != null) {
            int ticksPlayed = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            playtimeHours = ticksPlayed / 20.0 / 3600.0;
        }

        ZonedDateTime pragueTime = ZonedDateTime.now(ZoneId.of("Europe/Prague"));
        String formattedTime = pragueTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        long maxMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        String memoryString = usedMem + " / " + maxMem + " MB";

        return text
                .replace("%online%", String.valueOf(online))
                .replace("%max%", String.valueOf(max))
                .replace("%tps%", String.format("%.1f", tps))
                .replace("%ping%", String.valueOf(ping))
                .replace("%playtime%", String.format("%.1f", playtimeHours))
                .replace("%time%", formattedTime)
                .replace("%memory%", memoryString);
    }

    private String translateColorCodes(String text) {
        if (text == null) {
            return "";
        }

        java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
        java.util.regex.Matcher matcher = hexPattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char character : hex.toCharArray()) {
                replacement.append("§").append(character);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        return buffer.toString().replace("&", "§");
    }

    private TextColor parseTextColor(String colorCode) {
        if (colorCode == null || colorCode.isBlank()) {
            return null;
        }

        String normalized = colorCode.trim();
        if (normalized.startsWith("<#") && normalized.endsWith(">")) {
            normalized = normalized.substring(2, normalized.length() - 1);
        }
        if (normalized.startsWith("&#")) {
            normalized = normalized.substring(2);
        }
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.matches("(?i)[0-9a-f]{6}")) {
            return TextColor.fromRgb(Integer.parseInt(normalized, 16));
        }

        normalized = normalized.replace("&", "").replace("§", "").toLowerCase();
        if (normalized.isEmpty()) {
            return null;
        }

        return switch (normalized.substring(0, 1)) {
            case "0" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.BLACK);
            case "1" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.DARK_BLUE);
            case "2" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.DARK_GREEN);
            case "3" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.DARK_AQUA);
            case "4" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.DARK_RED);
            case "5" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.DARK_PURPLE);
            case "6" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.GOLD);
            case "7" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.GRAY);
            case "8" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.DARK_GRAY);
            case "9" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.BLUE);
            case "a" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.GREEN);
            case "b" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.AQUA);
            case "c" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.RED);
            case "d" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.LIGHT_PURPLE);
            case "e" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.YELLOW);
            case "f" -> TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.WHITE);
            default -> null;
        };
    }

    private String ensureTrailingSpace(String value) {
        if (value == null || value.isEmpty() || value.endsWith(" ")) {
            return value;
        }
        return value + " ";
    }

    private String ensureLeadingSpace(String value) {
        if (value == null || value.isEmpty() || value.startsWith(" ")) {
            return value;
        }
        return " " + value;
    }

    private double getCurrentTPS() {
        return cz.voidium.util.TpsTracker.getInstance().getTPS();
    }

    public void onPlayerJoin(ServerPlayer player) {
        refreshPlayerNames();
        updatePlayerList();
    }

    public void onPlayerLeave(ServerPlayer player) {
        updatePlayerList();
    }

    private record NameDecoration(Component prefixComponent, Component suffixComponent, TextColor playerNameColor) {
    }
}