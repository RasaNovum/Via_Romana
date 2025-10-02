package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.rasanovum.viaromana.core.LinkHandler;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.rasanovum.viaromana.util.VersionUtils;

/*
 * Request the server to unlink the sign at the given position.
 */
//? if <1.21 {
/*public record UnlinkSignRequestC2S(BlockPos signPos) {
*///?} else {
public record SignUnlinkRequestC2S(BlockPos signPos) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("via_romana:unlink_sign_request");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<SignUnlinkRequestC2S> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("via_romana:unlink_sign_request"));

    public static final StreamCodec<FriendlyByteBuf, SignUnlinkRequestC2S> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SignUnlinkRequestC2S::signPos,
        SignUnlinkRequestC2S::new
    );
    //?}

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buf, SignUnlinkRequestC2S packet) {
        buf.writeBlockPos(packet.signPos);
    }

    public static SignUnlinkRequestC2S decode(FriendlyByteBuf buf) {
        return new SignUnlinkRequestC2S(buf.readBlockPos());
    }

    public static void handle(PacketContext<SignUnlinkRequestC2S> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            net.minecraft.server.level.ServerLevel level = ctx.sender().serverLevel();
            BlockPos signPos = ctx.message().signPos();
            
            LinkHandler.unlinkSignFromNode(level, signPos);
        }
    }
}