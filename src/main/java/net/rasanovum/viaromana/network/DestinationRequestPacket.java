package net.rasanovum.viaromana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DestinationRequestPacket(BlockPos sourceSignPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DestinationRequestPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("viaromana:destination_request_c2s"));

    public static final StreamCodec<FriendlyByteBuf, DestinationRequestPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, DestinationRequestPacket::sourceSignPos,
        DestinationRequestPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
