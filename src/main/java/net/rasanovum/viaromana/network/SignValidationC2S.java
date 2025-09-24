package net.rasanovum.viaromana.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SignValidationC2S(BlockPos nodePos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SignValidationC2S> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("viaromana:sign_validation_c2s"));

    public static final StreamCodec<FriendlyByteBuf, SignValidationC2S> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SignValidationC2S::nodePos,
        SignValidationC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
