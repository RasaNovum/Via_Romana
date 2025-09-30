package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.teleport.ServerTeleportHandler;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;

/*
 * Request the server to teleport the player from the origin sign position to the destination position.
 */
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

    public static void handle(PacketContext<TeleportRequestC2S> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            ServerTeleportHandler.handleTeleportRequest(ctx.message(), ctx.sender());
        }
    }
}
