package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.storage.IPathStorage;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import commonnetwork.api.Dispatcher;

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
    
    public static void handle(PacketContext<SignValidationRequestC2S> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            net.minecraft.server.level.ServerLevel level = ctx.sender().serverLevel();
            BlockPos nodePos = ctx.message().nodePos();
            
            IPathStorage storage = IPathStorage.get(level);
            boolean isValid = storage.graph().getNodeAt(nodePos).isPresent();
            
            SignValidationResponseS2C response = new SignValidationResponseS2C(nodePos, isValid);
            Dispatcher.sendToClient(response, ctx.sender());
        }
    }
}
