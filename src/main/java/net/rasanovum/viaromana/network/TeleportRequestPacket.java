package net.rasanovum.viaromana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TeleportRequestPacket(BlockPos originSignPos, BlockPos destinationPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TeleportRequestPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("viaromana:teleport_request_c2s"));

    public static final StreamCodec<FriendlyByteBuf, TeleportRequestPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, TeleportRequestPacket::originSignPos,
        BlockPos.STREAM_CODEC, TeleportRequestPacket::destinationPos,
        TeleportRequestPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
