package cz.voidium.network;

import cz.voidium.Voidium;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;

public record PacketTicketMessage(String ticketId, String sender, String message) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketTicketMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Voidium.MOD_ID, "ticket_message"));

    public static final StreamCodec<FriendlyByteBuf, PacketTicketMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        PacketTicketMessage::ticketId,
        ByteBufCodecs.STRING_UTF8,
        PacketTicketMessage::sender,
        ByteBufCodecs.STRING_UTF8,
        PacketTicketMessage::message,
        PacketTicketMessage::new
    );

    @Override
    public CustomPacketPayload.Type<PacketTicketMessage> type() {
        return TYPE;
    }

    public static void handle(PacketTicketMessage payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side handling
            cz.voidium.client.chat.ChatChannelManager.getInstance().getOrCreateChannel(payload.ticketId(), "Ticket");
            
            // Format message similar to Discord
            Component msgInfo = Component.literal("§d[Ticket] §b" + payload.sender() + "§f: " + payload.message());
            
            cz.voidium.client.chat.ChatChannelManager.getInstance().addMessage(payload.ticketId(), msgInfo);
            
            // Optional: Notification sound or visual cue
        });
    }
}
