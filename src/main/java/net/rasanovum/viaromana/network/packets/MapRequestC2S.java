package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.rasanovum.viaromana.map.MapInfo;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;
import net.rasanovum.viaromana.util.VersionUtils;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import commonnetwork.api.Dispatcher;

import java.util.List;
import java.util.UUID;

/*
 * Request the server to generate or update a map for the specified network and area.
 * The server will respond with a MapResponseS2C packet containing the map data.
 */
//? if <1.21 {
/*public record MapRequestC2S(MapInfo mapInfo) {
*///?} else {
public record MapRequestC2S(MapInfo mapInfo) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("via_romana:map_request_c2s");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<MapRequestC2S> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("via_romana:map_request_c2s"));

    public static final StreamCodec<FriendlyByteBuf, MapRequestC2S> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MapRequestC2S decode(FriendlyByteBuf buffer) {
            return new MapRequestC2S(MapInfo.readFromBuffer(buffer));
        }

        @Override
        public void encode(FriendlyByteBuf buffer, MapRequestC2S packet) {
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

    public static void encode(FriendlyByteBuf buf, MapRequestC2S packet) {
        packet.mapInfo.writeToBuffer(buf);
    }

    public static MapRequestC2S decode(FriendlyByteBuf buf) {
        return new MapRequestC2S(MapInfo.readFromBuffer(buf));
    }

    public static MapRequestC2S create(UUID networkId, BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes) {
        return new MapRequestC2S(MapInfo.request(networkId, minBounds, maxBounds, networkNodes));
    }

    // Getters
    public UUID getNetworkId() { return mapInfo.networkId(); }
    public List<NodeNetworkInfo> getNetworkNodes() { return mapInfo.networkNodes(); }

    public MapInfo getMapInfo() { return mapInfo; }

    public static void handle(PacketContext<MapRequestC2S> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            net.minecraft.server.level.ServerLevel level = ctx.sender().serverLevel();
            UUID networkId = ctx.message().mapInfo().networkId();
            
            ServerMapCache.generateMapIfNeeded(networkId, level)
                .thenAccept(mapInfo -> {
                    if (mapInfo != null) {
                        MapResponseS2C response = new MapResponseS2C(mapInfo);
                        Dispatcher.sendToClient(response, ctx.sender());
                    }
                });
        }
    }
}
