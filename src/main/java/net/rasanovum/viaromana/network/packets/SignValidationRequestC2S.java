package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/*
 * Request the server to validate the sign at the given position.
 * The server will respond with a SignValidationResponseS2C packet.
 */
public record SignValidationRequestC2S(BlockPos nodePos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SignValidationRequestC2S> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("viaromana:sign_validation_c2s"));

    public static final StreamCodec<FriendlyByteBuf, SignValidationRequestC2S> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SignValidationRequestC2S::nodePos,
        SignValidationRequestC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
