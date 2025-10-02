package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.rasanovum.viaromana.util.VersionUtils;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;

//? if <1.21 {
/*public record OpenLinkSignScreenS2C() {
*///?} else {
public record OpenLinkSignScreenS2C() implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("via_romana:open_link_sign_screen_s2c");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<OpenLinkSignScreenS2C> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("via_romana:open_link_sign_screen_s2c"));

    public static final StreamCodec<FriendlyByteBuf, OpenLinkSignScreenS2C> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OpenLinkSignScreenS2C decode(FriendlyByteBuf buffer) {
            return new OpenLinkSignScreenS2C();
        }

        @Override
        public void encode(FriendlyByteBuf buffer, OpenLinkSignScreenS2C packet) {
        }
    };
    //?}

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buf, OpenLinkSignScreenS2C packet) {
    }

    public static OpenLinkSignScreenS2C decode(FriendlyByteBuf buf) {
        return new OpenLinkSignScreenS2C();
    }
    
    public static void handle(PacketContext<OpenLinkSignScreenS2C> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            // Unused - screen opening is handled client-side via mixin
        }
    }
}