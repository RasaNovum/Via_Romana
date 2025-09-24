package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenChartingScreenS2C() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenChartingScreenS2C> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("via_romana:open_charting_screen_s2c"));

    public static final StreamCodec<FriendlyByteBuf, OpenChartingScreenS2C> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OpenChartingScreenS2C decode(FriendlyByteBuf buffer) {
            return new OpenChartingScreenS2C();
        }

        @Override
        public void encode(FriendlyByteBuf buffer, OpenChartingScreenS2C packet) {
            // nothing
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}