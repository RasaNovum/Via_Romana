package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DestinationRequestC2S(BlockPos sourceSignPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DestinationRequestC2S> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("viaromana:destination_request_c2s"));

    public static final StreamCodec<FriendlyByteBuf, DestinationRequestC2S> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, DestinationRequestC2S::sourceSignPos,
        DestinationRequestC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
