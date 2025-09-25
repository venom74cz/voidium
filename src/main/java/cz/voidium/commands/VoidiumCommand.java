package cz.voidium.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import cz.voidium.config.VoidiumConfig;
import cz.voidium.config.RestartConfig;
import cz.voidium.config.AnnouncementConfig;
import cz.voidium.config.GeneralConfig;
import cz.voidium.server.RestartManager;
import cz.voidium.server.AnnouncementManager;
import cz.voidium.server.SkinRestorer;
import cz.voidium.vote.VoteManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

public class VoidiumCommand {
    private static RestartManager restartManager;
    private static AnnouncementManager announcementManager;
    private static SkinRestorer skinRestorer;
    private static VoteManager voteManager;
    
    public static void setManagers(RestartManager restart, AnnouncementManager announcement) {
        restartManager = restart;
        announcementManager = announcement;
    }
    public static void setSkinRestorer(SkinRestorer restorer) { skinRestorer = restorer; }
    public static void setVoteManager(VoteManager manager) { voteManager = manager; }
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("voidium")
            .requires(source -> source.hasPermission(2))
            .executes(VoidiumCommand::showHelp)
            .then(Commands.literal("reload")
                .executes(VoidiumCommand::reload))
            .then(Commands.literal("restart")
                .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 60))
                    .executes(VoidiumCommand::forceRestart)))
            .then(Commands.literal("announce")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(VoidiumCommand::announce)))
            .then(Commands.literal("players")
                .executes(VoidiumCommand::players))
            .then(Commands.literal("memory")
                .executes(VoidiumCommand::memory))
            .then(Commands.literal("cancel")
                .executes(VoidiumCommand::cancel))
            .then(Commands.literal("config")
                .executes(VoidiumCommand::config))
            .then(Commands.literal("gui")
                .executes(VoidiumCommand::gui)
                .then(Commands.literal("restart")
                    .executes(VoidiumCommand::guiRestart))
                .then(Commands.literal("announce")
                    .executes(VoidiumCommand::guiAnnounce))
                .then(Commands.literal("general")
                    .executes(VoidiumCommand::guiGeneral)))
            .then(Commands.literal("skin")
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(VoidiumCommand::skinRefresh)))
        );
        
        // Status příkaz pro všechny
        dispatcher.register(Commands.literal("voidium")
            .then(Commands.literal("status")
                .executes(VoidiumCommand::status))
        );
    }
    
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §fAvailable commands:"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium reload §7- Reload configuration"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium restart <minutes> §7- Force server restart"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium announce <message> §7- Send announcement to all players"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium players §7- List online players"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium memory §7- Server memory usage"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium cancel §7- Cancel scheduled restart"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium config §7- Configuration settings"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium gui §7- Interactive settings"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium skin <player> §7- Try refresh player skin (offline mode)"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium status §7- Show mod status (everyone)"), false);
        return 1;
    }

    private static int skinRefresh(CommandContext<CommandSourceStack> context) {
        String targetName = StringArgumentType.getString(context, "player");
        if (skinRestorer == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cSkinRestorer disabled"));
            return 0;
        }
        net.minecraft.server.MinecraftServer server = context.getSource().getServer();
        var player = server.getPlayerList().getPlayerByName(targetName);
        if (player == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cPlayer not online"));
            return 0;
        }
        boolean scheduled = skinRestorer.manualRefresh(player);
        if (scheduled) {
            context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §aSkin refresh scheduled for §e" + targetName), false);
        } else {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cRefresh failed to schedule"));
        }
        return 1;
    }
    
    private static int reload(CommandContext<CommandSourceStack> context) {
        try {
            VoidiumConfig.init(FMLPaths.CONFIGDIR.get());
            if (voteManager != null) {
                voteManager.reload();
            }
            context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §aConfiguration reloaded successfully!"), false);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cError loading configuration: " + e.getMessage()));
        }
        return 1;
    }
    
    private static int forceRestart(CommandContext<CommandSourceStack> context) {
        int minutes = IntegerArgumentType.getInteger(context, "minutes");
        if (restartManager != null) {
            restartManager.scheduleManualRestart(minutes);
            context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §eForced restart scheduled in " + minutes + " minutes!"), true);
        } else {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cRestartManager is not available!"));
        }
        return 1;
    }
    
    private static int announce(CommandContext<CommandSourceStack> context) {
        String message = StringArgumentType.getString(context, "message");
        if (announcementManager != null) {
            announcementManager.broadcastMessage(message);
            context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §aAnnouncement sent!"), false);
        } else {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cAnnouncementManager is not available!"));
        }
        return 1;
    }
    
    private static int status(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §fServer status:"), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Server name: §e" + context.getSource().getServer().getMotd()), false);
    context.getSource().sendSuccess(() -> Component.literal("§7Version: §e1.2.6"), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Mod count: §e" + net.neoforged.fml.ModList.get().size()), false);
        
        // TPS informace
        net.minecraft.server.MinecraftServer server = context.getSource().getServer();
        long[] tickTimes = server.getTickTimesNanos();
        double totalTime = 0;
        for (int i = 0; i < Math.min(100, tickTimes.length); i++) {
            totalTime += tickTimes[i];
        }
        final double avgTickTime = totalTime / Math.min(100, tickTimes.length) / 1000000.0;
        final double tps = Math.min(20.0, 1000.0 / avgTickTime);
        context.getSource().sendSuccess(() -> Component.literal("§7TPS: §e" + String.format("%.2f", tps)), false);
        context.getSource().sendSuccess(() -> Component.literal("§7MSPT: §e" + String.format("%.2f", avgTickTime)), false);
        
        RestartConfig restartConfig = RestartConfig.getInstance();
        if (restartConfig.getRestartType() == RestartConfig.RestartType.FIXED_TIME) {
            context.getSource().sendSuccess(() -> Component.literal("§7Restart type: §eFixed times"), false);
            StringBuilder times = new StringBuilder("§7Restart times: §e");
            for (int i = 0; i < restartConfig.getFixedRestartTimes().size(); i++) {
                if (i > 0) times.append(", ");
                times.append(restartConfig.getFixedRestartTimes().get(i));
            }
            context.getSource().sendSuccess(() -> Component.literal(times.toString()), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§7Restart type: §eInterval"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7Interval: §e" + restartConfig.getIntervalHours() + " hours"), false);
        }
        
        if (restartManager != null) {
            String nextRestart = restartManager.getNextRestartInfo();
            context.getSource().sendSuccess(() -> Component.literal("§7Next restart: §e" + nextRestart), false);
        }
        
        AnnouncementConfig announcementConfig = AnnouncementConfig.getInstance();
        context.getSource().sendSuccess(() -> Component.literal("§7Announcements every: §e" + announcementConfig.getAnnouncementIntervalMinutes() + " minutes"), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Announcement count: §e" + announcementConfig.getAnnouncements().size()), false);
        return 1;
    }
    
    private static int players(CommandContext<CommandSourceStack> context) {
        net.minecraft.server.MinecraftServer server = context.getSource().getServer();
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §fOnline players (§e" + server.getPlayerCount() + "§f):"), false);
        server.getPlayerList().getPlayers().forEach(player -> {
            int ping = player.connection.latency();
            context.getSource().sendSuccess(() -> Component.literal("§7- §e" + player.getName().getString() + " §7(ping: §e" + ping + "ms§7)"), false);
        });
        return 1;
    }
    
    private static int memory(CommandContext<CommandSourceStack> context) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §fServer memory:"), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Used: §e" + usedMemory + "MB §7/ §e" + maxMemory + "MB"), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Free: §e" + freeMemory + "MB"), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Usage: §e" + String.format("%.1f", (double)usedMemory / maxMemory * 100) + "%"), false);
        return 1;
    }
    
    private static int cancel(CommandContext<CommandSourceStack> context) {
        if (restartManager != null) {
            restartManager.cancelManualRestart();
            context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §aAll manual restarts have been cancelled!"), false);
        } else {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cRestartManager is not available!"));
        }
        return 1;
    }
    
    private static int config(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §fConfiguration is stored in: §econfig/voidium/"), false);
        context.getSource().sendSuccess(() -> Component.literal("§7- restart.json - restart settings"), false);
        context.getSource().sendSuccess(() -> Component.literal("§7- announcements.json - announcements"), false);
        context.getSource().sendSuccess(() -> Component.literal("§7- general.json - general settings"), false);
        context.getSource().sendSuccess(() -> Component.literal("§7After editing use: §e/voidium reload"), false);
        return 1;
    }
    
    private static int gui(CommandContext<CommandSourceStack> context) {
        showMainMenu(context.getSource());
        return 1;
    }
    
    private static void showMainMenu(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§8[§bVoidium GUI§8] §fMain menu:"), false);
        
        // Restart settings
        Component restartButton = Component.literal("§a[§eRestart Settings§a] ")
            .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/voidium gui restart")));
        source.sendSuccess(() -> restartButton, false);
        
        // Announcements settings
        Component announceButton = Component.literal("§a[§eAnnouncements§a] ")
            .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/voidium gui announce")));
        source.sendSuccess(() -> announceButton, false);
        
        // General settings
        Component generalButton = Component.literal("§a[§eGeneral Settings§a] ")
            .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/voidium gui general")));
        source.sendSuccess(() -> generalButton, false);
    }
    
    private static int guiRestart(CommandContext<CommandSourceStack> context) {
        RestartConfig config = RestartConfig.getInstance();
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§8[§bRestart GUI§8] §fComplete settings:"), false);
        source.sendSuccess(() -> Component.literal("§7Restart type: §e" + config.getRestartType()), false);
        
        if (config.getRestartType() == RestartConfig.RestartType.FIXED_TIME) {
            source.sendSuccess(() -> Component.literal("§7Fixed restart times:"), false);
            for (int i = 0; i < config.getFixedRestartTimes().size(); i++) {
                final int index = i;
                source.sendSuccess(() -> Component.literal("§7  " + (index+1) + ". §e" + config.getFixedRestartTimes().get(index)), false);
            }
        } else {
            source.sendSuccess(() -> Component.literal("§7Restart interval: §e" + config.getIntervalHours() + " hours"), false);
        }
        
        Component backButton = Component.literal("§c[§fBack§c] ")
            .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/voidium gui")));
        source.sendSuccess(() -> backButton, false);
        return 1;
    }
    
    private static int guiAnnounce(CommandContext<CommandSourceStack> context) {
        AnnouncementConfig config = AnnouncementConfig.getInstance();
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§8[§bAnnouncement GUI§8] §fComplete settings:"), false);
        source.sendSuccess(() -> Component.literal("§7Announcement interval: §e" + config.getAnnouncementIntervalMinutes() + " minutes"), false);
        source.sendSuccess(() -> Component.literal("§7Message prefix: §e" + config.getPrefix().replace("&", "§")), false);
        source.sendSuccess(() -> Component.literal("§7Message count: §e" + config.getAnnouncements().size()), false);
        source.sendSuccess(() -> Component.literal("§7Message list:"), false);
        
        for (int i = 0; i < config.getAnnouncements().size(); i++) {
            final int index = i;
            final String message = config.getAnnouncements().get(i).replace("&", "§");
            source.sendSuccess(() -> Component.literal("§7  " + (index+1) + ". §f" + message), false);
        }
        
        Component backButton = Component.literal("§c[§fBack§c] ")
            .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/voidium gui")));
        source.sendSuccess(() -> backButton, false);
        return 1;
    }
    
    private static int guiGeneral(CommandContext<CommandSourceStack> context) {
        GeneralConfig config = GeneralConfig.getInstance();
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§8[§bGeneral GUI§8] §fComplete settings:"), false);
        source.sendSuccess(() -> Component.literal("§7Mod enabled: §e" + (config.isEnableMod() ? "§aYES" : "§cNO")), false);
        source.sendSuccess(() -> Component.literal("§7Auto restarts: §e" + (config.isEnableRestarts() ? "§aYES" : "§cNO")), false);
        source.sendSuccess(() -> Component.literal("§7Auto announcements: §e" + (config.isEnableAnnouncements() ? "§aYES" : "§cNO")), false);
        source.sendSuccess(() -> Component.literal("§7Boss bar on restart: §e" + (config.isEnableBossBar() ? "§aYES" : "§cNO")), false);
        source.sendSuccess(() -> Component.literal("§7Mod prefix: §e" + config.getModPrefix().replace("&", "§")), false);
        
        Component backButton = Component.literal("§c[§fBack§c] ")
            .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/voidium gui")));
        source.sendSuccess(() -> backButton, false);
        return 1;
    }
}