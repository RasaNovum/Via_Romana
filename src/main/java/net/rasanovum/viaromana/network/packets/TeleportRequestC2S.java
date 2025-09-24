package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TeleportRequestC2S(BlockPos originSignPos, BlockPos destinationPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TeleportRequestC2S> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("viaromana:teleport_request_c2s"));

    public static final StreamCodec<FriendlyByteBuf, TeleportRequestC2S> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, TeleportRequestC2S::originSignPos,
        BlockPos.STREAM_CODEC, TeleportRequestC2S::destinationPos,
        TeleportRequestC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
