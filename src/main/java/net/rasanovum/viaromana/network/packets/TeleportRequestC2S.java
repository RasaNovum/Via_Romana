package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.rasanovum.viaromana.teleport.ServerTeleportHandler;
import net.rasanovum.viaromana.util.VersionUtils;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;

/*
 * Request the server to teleport the player from the origin sign position to the destination position.
 */
//? if <1.21 {
/*public record TeleportRequestC2S(BlockPos originSignPos, BlockPos destinationPos) {
*///?} else {
public record TeleportRequestC2S(BlockPos originSignPos, BlockPos destinationPos) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("viaromana:teleport_request_c2s");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<TeleportRequestC2S> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("viaromana:teleport_request_c2s"));

    public static final StreamCodec<FriendlyByteBuf, TeleportRequestC2S> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, TeleportRequestC2S::originSignPos,
        BlockPos.STREAM_CODEC, TeleportRequestC2S::destinationPos,
        TeleportRequestC2S::new
    );
    //?}

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buf, TeleportRequestC2S packet) {
        buf.writeBlockPos(packet.originSignPos);
        buf.writeBlockPos(packet.destinationPos);
    }

    public static TeleportRequestC2S decode(FriendlyByteBuf buf) {
        BlockPos origin = buf.readBlockPos();
        BlockPos destination = buf.readBlockPos();
        return new TeleportRequestC2S(origin, destination);
    }

    public static void handle(PacketContext<TeleportRequestC2S> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            ServerTeleportHandler.handleTeleportRequest(ctx.message(), ctx.sender());
        }
    }
}
