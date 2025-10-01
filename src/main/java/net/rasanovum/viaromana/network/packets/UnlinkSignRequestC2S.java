package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.core.LinkHandler;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;

/*
 * Request the server to unlink the sign at the given position.
 */
public record UnlinkSignRequestC2S(BlockPos signPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UnlinkSignRequestC2S> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("via_romana:unlink_sign_request"));

    public static final StreamCodec<FriendlyByteBuf, UnlinkSignRequestC2S> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, UnlinkSignRequestC2S::signPos,
        UnlinkSignRequestC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketContext<UnlinkSignRequestC2S> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            net.minecraft.server.level.ServerLevel level = ctx.sender().serverLevel();
            BlockPos signPos = ctx.message().signPos();
            
            LinkHandler.unlinkSignFromNode(level, signPos);
        }
    }
}