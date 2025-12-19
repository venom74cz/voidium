package cz.voidium.network;

import cz.voidium.Voidium;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.ByteBufCodecs;

public record PacketTicketCreated(String ticketId, String displayName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketTicketCreated> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Voidium.MOD_ID, "ticket_created"));

    public static final StreamCodec<FriendlyByteBuf, PacketTicketCreated> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        PacketTicketCreated::ticketId,
        ByteBufCodecs.STRING_UTF8,
        PacketTicketCreated::displayName,
        PacketTicketCreated::new
    );

    @Override
    public CustomPacketPayload.Type<PacketTicketCreated> type() {
        return TYPE;
    }

    public static void handle(PacketTicketCreated payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side handling
            cz.voidium.client.chat.ChatChannelManager.getInstance().getOrCreateChannel(payload.ticketId(), payload.displayName());
            // Optional: Auto-switch or notification
            // cz.voidium.client.chat.ChatChannelManager.getInstance().setActiveChannel(payload.ticketId());
        });
    }
}
