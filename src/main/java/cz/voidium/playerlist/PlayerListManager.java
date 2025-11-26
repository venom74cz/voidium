package cz.voidium.playerlist;

import cz.voidium.config.PlayerListConfig;
import cz.voidium.config.RanksConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import net.minecraft.stats.Stats;

public class PlayerListManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("voidium-playerlist");
    private static PlayerListManager instance;
    
    private MinecraftServer server;
    private ScheduledExecutorService executor;

    private PlayerListManager() {}

    public static PlayerListManager getInstance() {
        if (instance == null) {
            instance = new PlayerListManager();
        }
        return instance;
    }

    public void start(MinecraftServer server) {
        this.server = server;
        stop();
        
        PlayerListConfig config = PlayerListConfig.getInstance();
        if (!config.isEnableCustomPlayerList()) {
            LOGGER.info("Custom player list is disabled");
            return;
        }

        int interval = Math.max(3, config.getUpdateIntervalSeconds()); // Minimum 3 seconds for performance
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::updatePlayerList, 0, interval, TimeUnit.SECONDS);
        
        LOGGER.info("Player list manager started (update interval: {}s)", interval);
    }

    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void updatePlayerList() {
        if (server == null) return;
        PlayerListConfig config = PlayerListConfig.getInstance();
        if (!config.isEnableCustomPlayerList()) return;

        try {
            // Build header/footer for each player (for playtime)
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                int ping = player.connection.latency();
                String header = buildTextWithPlayer(config.getHeaderLine1(), config.getHeaderLine2(), config.getHeaderLine3(), player, ping);
                String footer = buildTextWithPlayer(config.getFooterLine1(), config.getFooterLine2(), config.getFooterLine3(), player, ping);

                player.connection.send(new net.minecraft.network.protocol.game.ClientboundTabListPacket(
                    Component.literal(header), Component.literal(footer)
                ));

                // Update player team (for colored names and prefix/suffix)
                if (config.isEnableCustomNames()) {
                    updatePlayerTeam(player);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error updating player list", e);
        }
    }

    // New: Build text with player-specific placeholders
    private String buildTextWithPlayer(String line1, String line2, String line3, ServerPlayer player, int ping) {
        if (server == null) return "";
        int online = server.getPlayerList().getPlayerCount();
        int max = server.getPlayerList().getMaxPlayers();
        double tps = getCurrentTPS();
        StringBuilder sb = new StringBuilder();
        if (!line1.isEmpty()) {
            sb.append(replacePlaceholdersFull(line1, online, max, tps, ping, player));
        }
        if (!line2.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(replacePlaceholdersFull(line2, online, max, tps, ping, player));
        }
        if (!line3.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(replacePlaceholdersFull(line3, online, max, tps, ping, player));
        }
        return sb.toString();
    }

    // New: Replace all placeholders including %playtime% and %time%
    private String replacePlaceholdersFull(String text, int online, int max, double tps, int ping, ServerPlayer player) {
        // Playtime in hours (rounded to 1 decimal)
        double playtimeHours = 0.0;
        if (player != null) {
            int ticksPlayed = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            playtimeHours = ticksPlayed / 20.0 / 3600.0;
        }
        // Prague time
        ZonedDateTime pragueTime = ZonedDateTime.now(ZoneId.of("Europe/Prague"));
        String formattedTime = pragueTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        // Memory usage (used/total MB)
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

    private void updatePlayerTeam(ServerPlayer player) {
        try {
            if (server == null) return;
            
            PlayerListConfig config = PlayerListConfig.getInstance();
            RanksConfig ranksConfig = RanksConfig.getInstance();
            
            Scoreboard scoreboard = server.getScoreboard();
            String playerName = player.getScoreboardName();
            
            // Collect all prefixes and suffixes from different sources
            StringBuilder allPrefixes = new StringBuilder();
            StringBuilder allSuffixes = new StringBuilder();
            ChatFormatting primaryColor = ChatFormatting.WHITE;
            boolean hasColor = false;
            boolean combineMultiple = config.isCombineMultipleRanks();
            
            // 0. Inherit from existing team (if not voidium team) to respect other mods
            PlayerTeam currentTeam = player.getTeam();
            if (currentTeam != null && !currentTeam.getName().startsWith("voidium_")) {
                String existingPrefix = currentTeam.getPlayerPrefix().getString();
                String existingSuffix = currentTeam.getPlayerSuffix().getString();
                
                if (!existingPrefix.isEmpty()) {
                    allPrefixes.append(existingPrefix);
                    if (!existingPrefix.endsWith(" ")) allPrefixes.append(" ");
                }
                if (!existingSuffix.isEmpty()) {
                    if (!existingSuffix.startsWith(" ")) allSuffixes.append(" ");
                    allSuffixes.append(existingSuffix);
                }
                
                if (currentTeam.getColor() != ChatFormatting.RESET && currentTeam.getColor() != ChatFormatting.WHITE) {
                    primaryColor = currentTeam.getColor();
                    hasColor = true;
                }
            }
            
            // 1. Collect Discord role-based prefixes/suffixes from DiscordConfig.rolePrefixes
            cz.voidium.config.DiscordConfig discordConfig = cz.voidium.config.DiscordConfig.getInstance();
            if (discordConfig != null && discordConfig.isEnableDiscord() && discordConfig.getRolePrefixes() != null) {
                try {
                    List<String> playerRoleIds = cz.voidium.discord.DiscordManager.getInstance().getPlayerDiscordRoles(player.getUUID());
                    
                    if (!playerRoleIds.isEmpty()) {
                        // Get ALL matching roles sorted by priority (highest first)
                        List<java.util.Map.Entry<String, cz.voidium.config.DiscordConfig.RoleStyle>> matchingRoles = new java.util.ArrayList<>();
                        for (String roleId : playerRoleIds) {
                            if (discordConfig.getRolePrefixes().containsKey(roleId)) {
                                cz.voidium.config.DiscordConfig.RoleStyle roleStyle = discordConfig.getRolePrefixes().get(roleId);
                                matchingRoles.add(new java.util.AbstractMap.SimpleEntry<>(roleId, roleStyle));
                            }
                        }
                        
                        // Sort by priority (highest first)
                        matchingRoles.sort((a, b) -> Integer.compare(b.getValue().priority, a.getValue().priority));
                        
                        // Append prefixes/suffixes
                        int rolesProcessed = 0;
                        for (java.util.Map.Entry<String, cz.voidium.config.DiscordConfig.RoleStyle> entry : matchingRoles) {
                            String roleId = entry.getKey();
                            cz.voidium.config.DiscordConfig.RoleStyle roleStyle = entry.getValue();
                            
                            // Fetch Role info from Discord if needed for defaults
                            net.dv8tion.jda.api.entities.Role discordRole = cz.voidium.discord.DiscordManager.getInstance().getRole(roleId);
                            
                            String prefix = roleStyle.prefix;
                            String suffix = roleStyle.suffix;
                            String color = roleStyle.color;
                            
                            // Auto-generate prefix if missing but role exists
                            if ((prefix == null || prefix.isEmpty()) && discordRole != null) {
                                String roleName = discordRole.getName();
                                String hexColor = "";
                                
                                // Try to get color from config or role
                                if (color != null && !color.isEmpty()) {
                                    // Config color takes precedence
                                } else {
                                    java.awt.Color roleColor = discordRole.getColor();
                                    if (roleColor != null) {
                                        hexColor = String.format("&#%02x%02x%02x", roleColor.getRed(), roleColor.getGreen(), roleColor.getBlue());
                                    }
                                }
                                
                                if (!hexColor.isEmpty()) {
                                    prefix = hexColor + "[" + roleName + "]&r";
                                } else {
                                    prefix = "&7[" + roleName + "]&r";
                                }
                            }

                            if (prefix != null && !prefix.isEmpty()) {
                                String translatedPrefix = translateColorCodes(prefix);
                                allPrefixes.append(translatedPrefix);
                                if (!translatedPrefix.endsWith(" ")) {
                                    allPrefixes.append(" ");
                                }
                            }
                            if (suffix != null && !suffix.isEmpty()) {
                                String translatedSuffix = translateColorCodes(suffix);
                                if (!translatedSuffix.startsWith(" ")) {
                                    allSuffixes.append(" ");
                                }
                                allSuffixes.append(translatedSuffix);
                            }
                            // Use color from highest priority role
                            if (!hasColor && color != null && !color.isEmpty()) {
                                primaryColor = parseColorCode(color);
                                hasColor = true;
                            }
                            
                            rolesProcessed++;
                            // If not combining multiple, stop after first role
                            if (!combineMultiple && rolesProcessed >= 1) break;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("[PlayerList] Error fetching Discord roles for {}: {}", playerName, e.getMessage());
                }
            }
            
            // 2. Add time-based ranks from RanksConfig
            if (ranksConfig != null && ranksConfig.getRanks() != null) {
                // TODO: Check player's actual playtime when playtime tracking is implemented
                // For now, we can apply all ranks or skip this
                // Example: if player has X hours, add corresponding rank prefix/suffix
            }
            
            // 3. Default prefix/suffix (applied if nothing else)
            if (allPrefixes.length() == 0 && config.getDefaultPrefix() != null && !config.getDefaultPrefix().isEmpty()) {
                String defaultPrefix = translateColorCodes(config.getDefaultPrefix());
                allPrefixes.append(defaultPrefix);
                if (!defaultPrefix.endsWith(" ")) {
                    allPrefixes.append(" ");
                }
            }
            if (allSuffixes.length() == 0 && config.getDefaultSuffix() != null && !config.getDefaultSuffix().isEmpty()) {
                String defaultSuffix = translateColorCodes(config.getDefaultSuffix());
                if (!defaultSuffix.startsWith(" ")) {
                    allSuffixes.append(" ");
                }
                allSuffixes.append(defaultSuffix);
            }
            
            // Create or get team
            String teamName = "voidium_" + playerName;
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            
            if (team == null) {
                team = scoreboard.addPlayerTeam(teamName);
            }
            
            // Set team properties with combined prefixes/suffixes
            team.setPlayerPrefix(Component.literal(allPrefixes.toString()));
            team.setPlayerSuffix(Component.literal(allSuffixes.toString()));
            team.setColor(primaryColor);
            
            // Add player to team
            scoreboard.addPlayerToTeam(playerName, team);
            
        } catch (Exception e) {
            LOGGER.error("Error updating player team for " + player.getName().getString(), e);
        }
    }
    
    private String translateColorCodes(String text) {
        if (text == null) return "";
        
        // Handle Hex colors: &#RRGGBB -> §x§R§R§G§G§B§B
        java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
        java.util.regex.Matcher matcher = hexPattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        text = sb.toString();

        // Convert & color codes to § for Minecraft
        return text.replace("&", "§");
    }
    
    private ChatFormatting parseColorCode(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) return ChatFormatting.WHITE;
        
        // Remove & or § prefix
        String code = colorCode.replace("&", "").replace("§", "").toLowerCase();
        
        // Map color codes to ChatFormatting
        return switch (code.substring(0, 1)) {
            case "0" -> ChatFormatting.BLACK;
            case "1" -> ChatFormatting.DARK_BLUE;
            case "2" -> ChatFormatting.DARK_GREEN;
            case "3" -> ChatFormatting.DARK_AQUA;
            case "4" -> ChatFormatting.DARK_RED;
            case "5" -> ChatFormatting.DARK_PURPLE;
            case "6" -> ChatFormatting.GOLD;
            case "7" -> ChatFormatting.GRAY;
            case "8" -> ChatFormatting.DARK_GRAY;
            case "9" -> ChatFormatting.BLUE;
            case "a" -> ChatFormatting.GREEN;
            case "b" -> ChatFormatting.AQUA;
            case "c" -> ChatFormatting.RED;
            case "d" -> ChatFormatting.LIGHT_PURPLE;
            case "e" -> ChatFormatting.YELLOW;
            case "f" -> ChatFormatting.WHITE;
            default -> ChatFormatting.WHITE;
        };
    }

    private String buildText(String line1, String line2, String line3) {
        if (server == null) return "";
        
        int online = server.getPlayerList().getPlayerCount();
        int max = server.getPlayerList().getMaxPlayers();
        double tps = getCurrentTPS();
        
        StringBuilder sb = new StringBuilder();
        
        if (!line1.isEmpty()) {
            sb.append(replacePlaceholders(line1, online, max, tps, 0));
        }
        if (!line2.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(replacePlaceholders(line2, online, max, tps, 0));
        }
        if (!line3.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(replacePlaceholders(line3, online, max, tps, 0));
        }
        
        return sb.toString();
    }

    private String replacePlaceholders(String text, int online, int max, double tps, int ping) {
        return text
            .replace("%online%", String.valueOf(online))
            .replace("%max%", String.valueOf(max))
            .replace("%tps%", String.format("%.1f", tps))
            .replace("%ping%", String.valueOf(ping));
    }

    private double getCurrentTPS() {
        if (server == null) return 20.0;
        
        try {
            // Get average tick time from server using reflection
            java.lang.reflect.Field tickTimesField = server.getClass().getDeclaredField("tickTimes");
            tickTimesField.setAccessible(true);
            long[] tickTimes = (long[]) tickTimesField.get(server);
            
            if (tickTimes == null || tickTimes.length == 0) return 20.0;
            
            long sum = 0;
            for (long time : tickTimes) {
                sum += time;
            }
            double avgTickNanos = (double) sum / tickTimes.length;
            double avgTickMillis = avgTickNanos * 1.0E-6D;
            
            // TPS = 1000ms / avg tick time (capped at 20)
            double tps = Math.min(1000.0 / avgTickMillis, 20.0);
            return Math.max(0.0, tps); // Ensure non-negative
        } catch (Exception e) {
            // Field might not exist in this version, return perfect TPS
            return 20.0;
        }
    }
    
    public void onPlayerJoin(ServerPlayer player) {
        // Setup player team when they join
        PlayerListConfig config = PlayerListConfig.getInstance();
        if (config.isEnableCustomNames()) {
            updatePlayerTeam(player);
        }
        // Trigger immediate update for new player
        updatePlayerList();
    }
    
    public void onPlayerLeave(ServerPlayer player) {
        // Clean up player's team when they leave
        try {
            if (server != null) {
                Scoreboard scoreboard = server.getScoreboard();
                String teamName = "voidium_" + player.getScoreboardName();
                PlayerTeam team = scoreboard.getPlayerTeam(teamName);
                if (team != null) {
                    scoreboard.removePlayerTeam(team);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error cleaning up player team", e);
        }
    }
}
