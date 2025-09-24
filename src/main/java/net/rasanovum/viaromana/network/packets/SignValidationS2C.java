package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SignValidationS2C(BlockPos nodePos, boolean isValid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SignValidationS2C> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("viaromana:sign_validation_s2c"));

    public static final StreamCodec<FriendlyByteBuf, SignValidationS2C> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SignValidationS2C::nodePos,
        ByteBufCodecs.BOOL, SignValidationS2C::isValid,
        SignValidationS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
