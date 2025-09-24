package net.rasanovum.viaromana.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenLinkSignScreenS2C() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenLinkSignScreenS2C> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("via_romana:open_link_sign_screen_s2c"));

    public static final StreamCodec<FriendlyByteBuf, OpenLinkSignScreenS2C> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OpenLinkSignScreenS2C decode(FriendlyByteBuf buffer) {
            return new OpenLinkSignScreenS2C();
        }

        @Override
        public void encode(FriendlyByteBuf buffer, OpenLinkSignScreenS2C packet) {
            // nothing
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}