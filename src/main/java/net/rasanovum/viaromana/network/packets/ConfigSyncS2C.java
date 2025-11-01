package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.client.ClientConfigCache;
import net.rasanovum.viaromana.util.VersionUtils;
import net.rasanovum.viaromana.ViaRomana;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;

/**
 * Server-to-client packet to synchronize server config values to the client.
 */
//? if <1.21 {
/*public record ConfigSyncS2C(float pathQualityThreshold, int nodeDistanceMin, int nodeDistanceMax, int nodeUtilityDistance, int infrastructureCheckRadius) {
*///?} else {
public record ConfigSyncS2C(float pathQualityThreshold, int nodeDistanceMin, int nodeDistanceMax, int nodeUtilityDistance, int infrastructureCheckRadius) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("via_romana:config_sync_s2c");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<ConfigSyncS2C> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("via_romana:config_sync_s2c"));

    public static final StreamCodec<FriendlyByteBuf, ConfigSyncS2C> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ConfigSyncS2C decode(FriendlyByteBuf buffer) { return ConfigSyncS2C.decode(buffer); }

        @Override
        public void encode(FriendlyByteBuf buffer, ConfigSyncS2C packet) { ConfigSyncS2C.encode(buffer, packet); }
    };
    //?}

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buf, ConfigSyncS2C packet) {
        buf.writeFloat(packet.pathQualityThreshold);
        buf.writeInt(packet.nodeDistanceMin);
        buf.writeInt(packet.nodeDistanceMax);
        buf.writeInt(packet.nodeUtilityDistance);
        buf.writeInt(packet.infrastructureCheckRadius);
    }

    public static ConfigSyncS2C decode(FriendlyByteBuf buf) {
        return new ConfigSyncS2C(
            buf.readFloat(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()
        );
    }

    public static void handle(PacketContext<ConfigSyncS2C> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            ConfigSyncS2C packet = ctx.message();
            ClientConfigCache.updateFromServer(
                packet.pathQualityThreshold, packet.nodeDistanceMin, packet.nodeDistanceMax, packet.nodeUtilityDistance, packet.infrastructureCheckRadius
            );
            
            ViaRomana.LOGGER.debug("Received config sync from server: pathQuality={}, nodeMin={}, nodeMax={}, utility={}, infraRadius={}",
                packet.pathQualityThreshold, packet.nodeDistanceMin, packet.nodeDistanceMax, packet.nodeUtilityDistance, packet.infrastructureCheckRadius);
        }
    }
}

