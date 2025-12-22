package cz.voidium.network;

import cz.voidium.Voidium;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.ByteBufCodecs;
import java.util.ArrayList;
import java.util.List;

public record PacketSyncChatHistory(List<String> messages) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSyncChatHistory> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Voidium.MOD_ID, "sync_chat_history"));

    public static final StreamCodec<FriendlyByteBuf, PacketSyncChatHistory> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            PacketSyncChatHistory::messages,
            PacketSyncChatHistory::new);

    @Override
    public CustomPacketPayload.Type<PacketSyncChatHistory> type() {
        return TYPE;
    }

    public static void handle(PacketSyncChatHistory payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side handling - send history to vanilla chat
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                for (String msg : payload.messages()) {
                    // Parse through ChatColorParser for RGB support
                    net.minecraft.network.chat.Component component = cz.voidium.client.chat.ChatColorParser
                            .parseMessage(
                                    net.minecraft.network.chat.Component.literal(msg));
                    mc.player.displayClientMessage(component, false);
                }
            }
        });
    }
}
