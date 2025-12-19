package cz.voidium.network;

import cz.voidium.Voidium;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Packet to sync Discord emojis from server to client.
 * Format: Map<EmojiName, EmojiURL>
 */
public record PacketSyncEmojis(Map<String, String> emojis) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSyncEmojis> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Voidium.MOD_ID, "sync_emojis"));

    public static final StreamCodec<FriendlyByteBuf, PacketSyncEmojis> STREAM_CODEC = 
        StreamCodec.of(PacketSyncEmojis::encode, PacketSyncEmojis::decode);

    private static void encode(FriendlyByteBuf buf, PacketSyncEmojis packet) {
        buf.writeVarInt(packet.emojis.size());
        for (Map.Entry<String, String> entry : packet.emojis.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    private static PacketSyncEmojis decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, String> emojis = new HashMap<>();
        for (int i = 0; i < size; i++) {
            emojis.put(buf.readUtf(), buf.readUtf());
        }
        return new PacketSyncEmojis(emojis);
    }

    @Override
    public CustomPacketPayload.Type<PacketSyncEmojis> type() {
        return TYPE;
    }

    public static void handle(PacketSyncEmojis payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            cz.voidium.client.media.EmojiManager.getInstance().syncEmojis(payload.emojis());
        });
    }
}
