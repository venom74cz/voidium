package cz.voidium.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import cz.voidium.config.VoidiumConfig;
import cz.voidium.config.RestartConfig;
import cz.voidium.config.AnnouncementConfig;
import cz.voidium.server.RestartManager;
import cz.voidium.server.AnnouncementManager;
import cz.voidium.server.SkinRestorer;
import cz.voidium.vote.VoteManager;
import cz.voidium.discord.DiscordManager;
import cz.voidium.entitycleaner.EntityCleaner;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.Path;

public class VoidiumCommand {
    private static RestartManager restartManager;
    private static AnnouncementManager announcementManager;
    private static SkinRestorer skinRestorer;
    private static VoteManager voteManager;
    private static EntityCleaner entityCleaner;
    
    public static void setManagers(RestartManager restart, AnnouncementManager announcement) {
        restartManager = restart;
        announcementManager = announcement;
    }
    public static void setSkinRestorer(SkinRestorer restorer) { skinRestorer = restorer; }
    public static void setVoteManager(VoteManager manager) { voteManager = manager; }
    public static void setEntityCleaner(EntityCleaner cleaner) { entityCleaner = cleaner; }
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("voidium")
            .then(Commands.literal("status")
                .executes(VoidiumCommand::status))
            .then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(VoidiumCommand::reload))
            .then(Commands.literal("restart")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 60))
                    .executes(VoidiumCommand::forceRestart)))
            .then(Commands.literal("announce")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(VoidiumCommand::announce)))
            .then(Commands.literal("players")
                .requires(source -> source.hasPermission(2))
                .executes(VoidiumCommand::players))
            .then(Commands.literal("memory")
                .requires(source -> source.hasPermission(2))
                .executes(VoidiumCommand::memory))
            .then(Commands.literal("cancel")
                .requires(source -> source.hasPermission(2))
                .executes(VoidiumCommand::cancel))
            .then(Commands.literal("config")
                .requires(source -> source.hasPermission(2))
                .executes(VoidiumCommand::config))
            .then(Commands.literal("skin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(VoidiumCommand::skinRefresh)))
            .then(Commands.literal("votes")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("pending")
                    .executes(VoidiumCommand::votesPending)
                    .then(Commands.argument("player", StringArgumentType.word())
                        .executes(VoidiumCommand::votesPendingPlayer)))
                .then(Commands.literal("clear")
                    .executes(VoidiumCommand::votesClear)))
            .then(Commands.literal("web")
                .requires(source -> source.hasPermission(2))
                .executes(VoidiumCommand::web))
            .then(Commands.literal("clear")
                .requires(source -> source.hasPermission(2))
                .executes(VoidiumCommand::clearAll)
                .then(Commands.literal("items")
                    .executes(VoidiumCommand::clearItems))
                .then(Commands.literal("mobs")
                    .executes(VoidiumCommand::clearMobs))
                .then(Commands.literal("xp")
                    .executes(VoidiumCommand::clearXp))
                .then(Commands.literal("arrows")
                    .executes(VoidiumCommand::clearArrows))
                .then(Commands.literal("preview")
                    .executes(VoidiumCommand::clearPreview)))
            .executes(VoidiumCommand::showHelp)
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
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium skin <player> §7- Try refresh player skin (offline mode)"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium votes pending [player] §7- Show pending votes"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium votes clear §7- Clear all pending votes"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium status §7- Show mod status (everyone)"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium web §7- Show Web Control URL"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium clear §7- Clear all entities (items, mobs, xp, arrows)"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium clear items|mobs|xp|arrows §7- Clear specific type"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/voidium clear preview §7- Preview what would be cleared"), false);
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
            Path configDir = FMLPaths.CONFIGDIR.get();
            Path voidiumDir = configDir.resolve("voidium");
            
            // Reload all configs with voidium directory
            VoidiumConfig.init(voidiumDir);
            cz.voidium.config.DiscordConfig.init(voidiumDir);
            cz.voidium.config.WebConfig.init(voidiumDir);
            cz.voidium.config.StatsConfig.init(voidiumDir);
            cz.voidium.config.RanksConfig.init(voidiumDir);
            cz.voidium.config.TicketConfig.init(voidiumDir);
            cz.voidium.config.PlayerListConfig.init(voidiumDir);
            cz.voidium.config.GeneralConfig.init(voidiumDir);
            cz.voidium.config.RestartConfig.init(voidiumDir);
            cz.voidium.config.AnnouncementConfig.init(voidiumDir);
            cz.voidium.config.EntityCleanerConfig.init(voidiumDir);
            
            if (voteManager != null) {
                voteManager.reload();
            }
            
            if (entityCleaner != null) {
                entityCleaner.reload();
            }
            
            DiscordManager.getInstance().setServer(context.getSource().getServer());
            DiscordManager.getInstance().reload();
            
            // Reload player list manager
            if (cz.voidium.playerlist.PlayerListManager.getInstance() != null) {
                cz.voidium.playerlist.PlayerListManager.getInstance().stop();
                cz.voidium.playerlist.PlayerListManager.getInstance().start(context.getSource().getServer());
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
    context.getSource().sendSuccess(() -> Component.literal("§7Version: §e2.1.0"), false);
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
    
    private static int votesPending(CommandContext<CommandSourceStack> context) {
        if (voteManager == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cVote system not available"));
            return 0;
        }
        
        var queue = voteManager.getPendingQueue();
        if (queue == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cPending queue not initialized"));
            return 0;
        }
        
        int total = queue.getTotalPending();
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §fTotal pending votes: §e" + total), false);
        return 1;
    }
    
    private static int votesPendingPlayer(CommandContext<CommandSourceStack> context) {
        if (voteManager == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cVote system not available"));
            return 0;
        }
        
        var queue = voteManager.getPendingQueue();
        if (queue == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cPending queue not initialized"));
            return 0;
        }
        
        String playerName = StringArgumentType.getString(context, "player");
        int count = queue.getPendingCount(playerName);
        
        if (count == 0) {
            context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §7Player §e" + playerName + "§7 has no pending votes"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §aPlayer §e" + playerName + "§a has §e" + count + "§a pending vote" + (count > 1 ? "s" : "")), false);
        }
        return 1;
    }
    
    private static int votesClear(CommandContext<CommandSourceStack> context) {
        if (voteManager == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cVote system not available"));
            return 0;
        }
        
        var queue = voteManager.getPendingQueue();
        if (queue == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cPending queue not initialized"));
            return 0;
        }
        
        int total = queue.getTotalPending();
        queue.clear();
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §aCleared §e" + total + "§a pending vote" + (total != 1 ? "s" : "")), false);
        return 1;
    }
    
    private static int web(CommandContext<CommandSourceStack> context) {
        String url = cz.voidium.web.WebManager.getInstance().getWebUrl();
        context.getSource().sendSuccess(() -> Component.literal("§aWeb Control Interface: §b" + url), false);
        return 1;
    }
    
    // === Entity Cleaner Commands ===
    
    private static int clearAll(CommandContext<CommandSourceStack> context) {
        if (entityCleaner == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cEntityCleaner is not available!"));
            return 0;
        }
        EntityCleaner.CleanupResult result = entityCleaner.forceCleanup(true, true, true, true);
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §aCleared: §e" + result.items + "§a items, §e" + result.mobs + "§a mobs, §e" + result.xpOrbs + "§a XP orbs, §e" + result.arrows + "§a arrows"), true);
        return 1;
    }
    
    private static int clearItems(CommandContext<CommandSourceStack> context) {
        if (entityCleaner == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cEntityCleaner is not available!"));
            return 0;
        }
        EntityCleaner.CleanupResult result = entityCleaner.forceCleanup(true, false, false, false);
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §aCleared §e" + result.items + "§a dropped items"), true);
        return 1;
    }
    
    private static int clearMobs(CommandContext<CommandSourceStack> context) {
        if (entityCleaner == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cEntityCleaner is not available!"));
            return 0;
        }
        EntityCleaner.CleanupResult result = entityCleaner.forceCleanup(false, true, false, false);
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §aCleared §e" + result.mobs + "§a mobs"), true);
        return 1;
    }
    
    private static int clearXp(CommandContext<CommandSourceStack> context) {
        if (entityCleaner == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cEntityCleaner is not available!"));
            return 0;
        }
        EntityCleaner.CleanupResult result = entityCleaner.forceCleanup(false, false, true, false);
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §aCleared §e" + result.xpOrbs + "§a XP orbs"), true);
        return 1;
    }
    
    private static int clearArrows(CommandContext<CommandSourceStack> context) {
        if (entityCleaner == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cEntityCleaner is not available!"));
            return 0;
        }
        EntityCleaner.CleanupResult result = entityCleaner.forceCleanup(false, false, false, true);
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §aCleared §e" + result.arrows + "§a arrows"), true);
        return 1;
    }
    
    private static int clearPreview(CommandContext<CommandSourceStack> context) {
        if (entityCleaner == null) {
            context.getSource().sendFailure(Component.literal("§8[§bVoidium§8] §cEntityCleaner is not available!"));
            return 0;
        }
        EntityCleaner.CleanupResult result = entityCleaner.previewCleanup();
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §fWould clear: §e" + result.items + "§f items, §e" + result.mobs + "§f mobs, §e" + result.xpOrbs + "§f XP orbs, §e" + result.arrows + "§f arrows"), false);
        context.getSource().sendSuccess(() -> Component.literal("§8[§bVoidium§8] §fTotal: §e" + result.total() + "§f entities"), false);
        return 1;
    }
}