package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.rasanovum.viaromana.client.gui.WarpBlockScreen;
import net.rasanovum.viaromana.util.VersionUtils;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;

/*
 * Instruct the client to open the warp block screen for the block at the given position.
 */
//? if <1.21 {
/*public record OpenWarpBlockScreenS2C(BlockPos blockPos) {
*///?} else {
public record OpenWarpBlockScreenS2C(BlockPos blockPos) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("via_romana:open_warp_block_screen_s2c");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<OpenWarpBlockScreenS2C> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("via_romana:open_warp_block_screen_s2c"));

    public static final StreamCodec<FriendlyByteBuf, OpenWarpBlockScreenS2C> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OpenWarpBlockScreenS2C decode(FriendlyByteBuf buffer) {
            return new OpenWarpBlockScreenS2C(buffer.readBlockPos());
        }

        @Override
        public void encode(FriendlyByteBuf buffer, OpenWarpBlockScreenS2C packet) {
            buffer.writeBlockPos(packet.blockPos());
        }
    };
    //?}

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buf, OpenWarpBlockScreenS2C packet) {
        buf.writeBlockPos(packet.blockPos);
    }

    public static OpenWarpBlockScreenS2C decode(FriendlyByteBuf buf) {
        return new OpenWarpBlockScreenS2C(buf.readBlockPos());
    }
}