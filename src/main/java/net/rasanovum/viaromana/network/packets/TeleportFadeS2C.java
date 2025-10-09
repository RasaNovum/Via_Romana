package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.client.FadeManager;
import net.rasanovum.viaromana.util.VersionUtils;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;

/**
 * Server-to-client packet to initiate the teleport fade effect on the client.
 */
//? if <1.21 {
/*public record TeleportFadeS2C(int fadeUpTicks, int holdTicks, int fadeDownTicks, int footstepInterval) {
*///?} else {
public record TeleportFadeS2C(int fadeUpTicks, int holdTicks, int fadeDownTicks, int footstepInterval) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("via_romana:teleport_fade_s2c");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<TeleportFadeS2C> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("via_romana:teleport_fade_s2c"));

    public static final StreamCodec<FriendlyByteBuf, TeleportFadeS2C> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TeleportFadeS2C decode(FriendlyByteBuf buffer) { return TeleportFadeS2C.decode(buffer); }

        @Override
        public void encode(FriendlyByteBuf buffer, TeleportFadeS2C packet) { TeleportFadeS2C.encode(buffer, packet); }
    };
    //?}

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buf, TeleportFadeS2C packet) {
        buf.writeInt(packet.fadeUpTicks);
        buf.writeInt(packet.holdTicks);
        buf.writeInt(packet.fadeDownTicks);
        buf.writeInt(packet.footstepInterval);
    }

    public static TeleportFadeS2C decode(FriendlyByteBuf buf) {
        return new TeleportFadeS2C(
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt()
        );
    }

    public static void handle(PacketContext<TeleportFadeS2C> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            FadeManager.startFade(
                ctx.message().fadeUpTicks(),
                ctx.message().holdTicks(),
                ctx.message().fadeDownTicks(),
                ctx.message().footstepInterval()
            );
        }
    }
}
