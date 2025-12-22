package cz.voidium.network;

import cz.voidium.Voidium;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.ByteBufCodecs;

public record PacketTicketCreated(String ticketId, String displayName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketTicketCreated> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Voidium.MOD_ID, "ticket_created"));

    public static final StreamCodec<FriendlyByteBuf, PacketTicketCreated> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            PacketTicketCreated::ticketId,
            ByteBufCodecs.STRING_UTF8,
            PacketTicketCreated::displayName,
            PacketTicketCreated::new);

    @Override
    public CustomPacketPayload.Type<PacketTicketCreated> type() {
        return TYPE;
    }

    public static void handle(PacketTicketCreated payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side handling - show ticket created notification in vanilla chat
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                // Format ticket creation message with colors
                String formatted = "&#9b59b6[Ticket] &#00d4aaTvůj ticket " + payload.displayName() + " byl vytvořen!";
                net.minecraft.network.chat.Component component = cz.voidium.client.chat.ChatColorParser.parseMessage(
                        net.minecraft.network.chat.Component.literal(formatted));
                mc.player.displayClientMessage(component, false);
            }
        });
    }
}
