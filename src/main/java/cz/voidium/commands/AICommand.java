package cz.voidium.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import cz.voidium.ai.AIService;
import cz.voidium.config.AIConfig;
import cz.voidium.config.WebConfig;
import cz.voidium.discord.DiscordManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import java.util.List;

public final class AICommand {
    private AICommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ai")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(AICommand::ask)));
    }

    private static int ask(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal(localize("§8[§bVoidium§8] §cOnly players can use /ai.", "§8[§bVoidium§8] §cPříkaz /ai mohou použít jen hráči.")));
            return 0;
        }

        AIConfig config = AIConfig.getInstance();
        if (config == null || !config.isEnablePlayerChat()) {
            context.getSource().sendFailure(Component.literal(localize("§8[§bVoidium§8] §cPlayer AI chat is disabled.", "§8[§bVoidium§8] §cHráčské AI je vypnuté.")));
            return 0;
        }

        String accessError = validatePlayerAccess(player, config);
        if (accessError != null) {
            context.getSource().sendFailure(Component.literal(accessError));
            return 0;
        }

        // World restriction
        String worldKey = player.level().dimension().location().toString();
        if (config.getDisabledWorlds() != null && config.getDisabledWorlds().stream().anyMatch(worldKey::contains)) {
            context.getSource().sendFailure(Component.literal(localize("§8[§bVoidium§8] §cAI is disabled in this world.", "§8[§bVoidium§8] §cAI je v tomto světě vypnuté.")));
            return 0;
        }

        // Game mode restriction
        String gameMode = player.gameMode.getGameModeForPlayer().getName();
        if (config.getDisabledGameModes() != null && config.getDisabledGameModes().stream().anyMatch(m -> m.equalsIgnoreCase(gameMode))) {
            context.getSource().sendFailure(Component.literal(localize("§8[§bVoidium§8] §cAI is disabled in your current game mode.", "§8[§bVoidium§8] §cAI je v tomto herním módu vypnuté.")));
            return 0;
        }

        String message = StringArgumentType.getString(context, "message");
        player.sendSystemMessage(Component.literal(localize("§8[§bVoidium AI§8] §7Thinking...", "§8[§bVoidium AI§8] §7Přemýšlím...")));

        AIService.getInstance().askPlayer(player, message)
                .whenComplete((response, throwable) -> {
                    player.getServer().execute(() -> {
                        if (throwable != null) {
                            String error = throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage();
                            player.sendSystemMessage(Component.literal("§8[§bVoidium AI§8] §c" + sanitize(error)));
                            return;
                        }
                        for (String chunk : splitForChat(sanitize(response), 220)) {
                            player.sendSystemMessage(Component.literal("§8[§bVoidium AI§8] §f" + chunk));
                        }
                    });
                });
        return 1;
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "No response.";
        }
        return value.replace("\n", " ").trim();
    }

    private static java.util.List<String> splitForChat(String text, int maxLength) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        String remaining = text;
        while (remaining.length() > maxLength) {
            int splitAt = remaining.lastIndexOf(' ', maxLength);
            if (splitAt <= 0) {
                splitAt = maxLength;
            }
            parts.add(remaining.substring(0, splitAt).trim());
            remaining = remaining.substring(splitAt).trim();
        }
        if (!remaining.isBlank()) {
            parts.add(remaining);
        }
        return parts.isEmpty() ? java.util.List.of("No response.") : parts;
    }

    private static String validatePlayerAccess(ServerPlayer player, AIConfig config) {
        String mode = config.getPlayerAccessMode();
        if (mode == null || "ALL".equalsIgnoreCase(mode)) {
            return null;
        }

        double playedHours = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) / 20.0 / 3600.0;
        boolean meetsPlaytime = playedHours >= config.getPlayerAccessMinHours();
        List<String> requiredRoles = config.getPlayerAccessDiscordRoleIds();
        boolean roleGateConfigured = requiredRoles != null && !requiredRoles.isEmpty();
        boolean meetsDiscordRole = false;
        if (roleGateConfigured) {
            try {
                List<String> roles = DiscordManager.getInstance().getPlayerDiscordRoles(player.getUUID());
                meetsDiscordRole = roles.stream().anyMatch(requiredRoles::contains);
            } catch (Exception ignored) {
                meetsDiscordRole = false;
            }
        }

        boolean allowed = switch (mode) {
            case "PLAYTIME" -> meetsPlaytime;
            case "DISCORD_ROLE" -> roleGateConfigured && meetsDiscordRole;
            case "PLAYTIME_OR_DISCORD_ROLE" -> meetsPlaytime || (roleGateConfigured && meetsDiscordRole);
            case "PLAYTIME_AND_DISCORD_ROLE" -> meetsPlaytime && (!roleGateConfigured || meetsDiscordRole);
            default -> true;
        };
        if (allowed) {
            return null;
        }

        return switch (mode) {
            case "PLAYTIME" -> localize(
                    "§8[§bVoidium§8] §cPlayer AI requires at least " + config.getPlayerAccessMinHours() + " played hours.",
                    "§8[§bVoidium§8] §cHráčské AI vyžaduje alespoň " + config.getPlayerAccessMinHours() + " odehraných hodin.");
            case "DISCORD_ROLE" -> localize(
                    "§8[§bVoidium§8] §cPlayer AI requires one of the configured Discord roles.",
                    "§8[§bVoidium§8] §cHráčské AI vyžaduje jednu z nastavených Discord rolí.");
            case "PLAYTIME_OR_DISCORD_ROLE" -> localize(
                    "§8[§bVoidium§8] §cPlayer AI requires enough played hours or an allowed Discord role.",
                    "§8[§bVoidium§8] §cHráčské AI vyžaduje dost hodin nebo povolenou Discord roli.");
            case "PLAYTIME_AND_DISCORD_ROLE" -> localize(
                    "§8[§bVoidium§8] §cPlayer AI requires enough played hours and an allowed Discord role.",
                    "§8[§bVoidium§8] §cHráčské AI vyžaduje dost hodin i povolenou Discord roli.");
            default -> null;
        };
    }

    private static String localize(String english, String czech) {
        return WebConfig.getInstance() != null && "cz".equalsIgnoreCase(WebConfig.getInstance().getLanguage()) ? czech : english;
    }
}