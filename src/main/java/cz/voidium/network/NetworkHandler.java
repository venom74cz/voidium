package cz.voidium.network;

import cz.voidium.Voidium;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Voidium.MOD_ID);
        
        registrar.playToClient(
            PacketSyncChatHistory.TYPE,
            PacketSyncChatHistory.STREAM_CODEC,
            PacketSyncChatHistory::handle
        );
        
        registrar.playToClient(
            PacketSyncEmojis.TYPE,
            PacketSyncEmojis.STREAM_CODEC,
            PacketSyncEmojis::handle
        );
        
        registrar.playToClient(
            PacketTicketCreated.TYPE,
            PacketTicketCreated.STREAM_CODEC,
            PacketTicketCreated::handle
        );
        
        registrar.playToClient(
            PacketTicketMessage.TYPE,
            PacketTicketMessage.STREAM_CODEC,
            PacketTicketMessage::handle
        );
    }
}
