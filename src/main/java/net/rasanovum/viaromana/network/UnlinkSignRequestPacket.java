package net.rasanovum.viaromana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server to request unlinking a sign from a node.
 */
public record UnlinkSignRequestPacket(BlockPos signPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UnlinkSignRequestPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("via_romana:unlink_sign_request"));

    public static final StreamCodec<FriendlyByteBuf, UnlinkSignRequestPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, UnlinkSignRequestPacket::signPos,
        UnlinkSignRequestPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}