package net.rasanovum.viaromana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenWarpBlockScreenS2C(BlockPos blockPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenWarpBlockScreenS2C> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("via_romana:open_warp_block_screen_s2c"));

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}