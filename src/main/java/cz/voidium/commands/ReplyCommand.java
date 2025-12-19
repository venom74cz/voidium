package cz.voidium.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import cz.voidium.discord.TicketManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ReplyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("reply")
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(context -> {
                    String message = StringArgumentType.getString(context, "message");
                    var source = context.getSource();
                    var player = source.getPlayerOrException();
                    
                    TicketManager.getInstance().replyToTicket(player, message);
                    return 1;
                }))
        );
    }
}
