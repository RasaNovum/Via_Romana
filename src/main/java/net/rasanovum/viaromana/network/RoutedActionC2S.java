package net.rasanovum.viaromana.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S action packet
 */
public record RoutedActionC2S(Operation op) implements CustomPacketPayload {

    public enum Operation {
        SEVER_NEAREST_NODE,
        REMOVE_BRANCH
    }

    public static final CustomPacketPayload.Type<RoutedActionC2S> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("viaromana:action_request_c2s"));

    public static final StreamCodec<FriendlyByteBuf, RoutedActionC2S> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(i -> Operation.values()[i], Operation::ordinal), RoutedActionC2S::op,
        RoutedActionC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
