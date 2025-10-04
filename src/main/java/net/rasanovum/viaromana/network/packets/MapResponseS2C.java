package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.rasanovum.viaromana.map.MapInfo;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;
import net.rasanovum.viaromana.util.VersionUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/*
 * Response from the server containing the generated map data for the requested area and network.
 */
//? if <1.21 {
/*public record MapResponseS2C(MapInfo mapInfo) {
*///?} else {
public record MapResponseS2C(MapInfo mapInfo) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("via_romana:map_response_s2c");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<MapResponseS2C> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("via_romana:map_response_s2c"));

    public static final StreamCodec<FriendlyByteBuf, MapResponseS2C> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MapResponseS2C decode(FriendlyByteBuf buffer) {
            return new MapResponseS2C(MapInfo.readFromBuffer(buffer));
        }

        @Override
        public void encode(FriendlyByteBuf buffer, MapResponseS2C packet) {
            packet.mapInfo.writeToBuffer(buffer);
        }
    };
    //?}

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buf, MapResponseS2C packet) {
        packet.mapInfo.writeToBuffer(buf);
    }

    public static MapResponseS2C decode(FriendlyByteBuf buf) {
        return new MapResponseS2C(MapInfo.readFromBuffer(buf));
    }

    public MapInfo getMapInfo() { return mapInfo; }

    public static void handle(commonnetwork.networking.data.PacketContext<MapResponseS2C> ctx) {
        if (commonnetwork.networking.data.Side.CLIENT.equals(ctx.side())) {
            net.rasanovum.viaromana.client.MapClient.handleMapResponse(ctx.message());
        }
    }
}
