package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.map.MapInfo;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import commonnetwork.api.Dispatcher;

import java.util.List;
import java.util.UUID;

/*
 * Request the server to generate or update a map for the specified network and area.
 * The server will respond with a MapResponseS2C packet containing the map data.
 */
public record MapRequestC2S(MapInfo mapInfo) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MapRequestC2S> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("via_romana:map_request_c2s"));

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static MapRequestC2S create(UUID networkId, BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes) {
        return new MapRequestC2S(MapInfo.request(networkId, minBounds, maxBounds, networkNodes));
    }

    // Getters
    public UUID getNetworkId() { return mapInfo.networkId(); }
    public BlockPos getMinBounds() { return mapInfo.minBounds(); }
    public BlockPos getMaxBounds() { return mapInfo.maxBounds(); }
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
