package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/*
 * Response from the server indicating whether the sign at the given position is valid.
 */
public record SignValidationResponseS2C(BlockPos nodePos, boolean isValid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SignValidationResponseS2C> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("viaromana:sign_validation_s2c"));

    public static final StreamCodec<FriendlyByteBuf, SignValidationResponseS2C> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SignValidationResponseS2C::nodePos,
        ByteBufCodecs.BOOL, SignValidationResponseS2C::isValid,
        SignValidationResponseS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
