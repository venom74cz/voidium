package cz.voidium.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import cz.voidium.discord.LinkManager;
import cz.voidium.discord.TicketManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TicketCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ticket")
            .then(Commands.argument("reason", StringArgumentType.word())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(context -> {
                        String reason = StringArgumentType.getString(context, "reason");
                        String message = StringArgumentType.getString(context, "message");
                        var source = context.getSource();
                        var player = source.getPlayerOrException();
                        
                        Long discordId = LinkManager.getInstance().getDiscordId(player.getUUID());
                        if (discordId == null) {
                            source.sendFailure(Component.literal("§cMusíš mít propojený Discord účet! Použij /link"));
                            return 0;
                        }
                        
                        TicketManager.getInstance().createTicket(discordId, reason, message, player);
                        return 1;
                    })))
        );
    }
}
