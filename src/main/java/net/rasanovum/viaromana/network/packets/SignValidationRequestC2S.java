package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.util.VersionUtils;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import commonnetwork.api.Dispatcher;

/*
 * Request the server to validate the sign at the given position.
 * The server will respond with a SignValidationResponseS2C packet.
 */
//? if <1.21 {
/*public record SignValidationRequestC2S(BlockPos nodePos) {
*///?} else {
public record SignValidationRequestC2S(BlockPos nodePos) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("viaromana:sign_validation_c2s");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<SignValidationRequestC2S> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("viaromana:sign_validation_c2s"));

    public static final StreamCodec<FriendlyByteBuf, SignValidationRequestC2S> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SignValidationRequestC2S::nodePos,
        SignValidationRequestC2S::new
    );
    //?}

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buf, SignValidationRequestC2S packet) {
        buf.writeBlockPos(packet.nodePos);
    }

    public static SignValidationRequestC2S decode(FriendlyByteBuf buf) {
        return new SignValidationRequestC2S(buf.readBlockPos());
    }
    
    public static void handle(PacketContext<SignValidationRequestC2S> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            net.minecraft.server.level.ServerLevel level = ctx.sender().serverLevel();
            BlockPos nodePos = ctx.message().nodePos();
            
            PathGraph graph = PathGraph.getInstance(level);
            boolean isValid = graph.getNodeAt(nodePos).isPresent();
            
            SignValidationResponseS2C response = new SignValidationResponseS2C(nodePos, isValid);
            Dispatcher.sendToClient(response, ctx.sender());
        }
    }
}
