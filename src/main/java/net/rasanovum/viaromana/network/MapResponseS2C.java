package net.rasanovum.viaromana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.map.MapInfo;
import net.rasanovum.viaromana.network.DestinationResponsePacket.NodeNetworkInfo;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Packet for sending a map texture response.
 */
public record MapResponseS2C(MapInfo mapInfo) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MapResponseS2C> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("via_romana:map_response_s2c"));

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Factory methods for easier construction
    public static MapResponseS2C create(UUID networkId, BlockPos minBounds, BlockPos maxBounds, byte[] pngData, int bakeScaleFactor) {
        return new MapResponseS2C(MapInfo.response(networkId, minBounds, maxBounds, new ArrayList<>(), pngData, bakeScaleFactor));
    }

    public static MapResponseS2C create(UUID networkId, BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes, byte[] pngData, int bakeScaleFactor) {
        return new MapResponseS2C(MapInfo.response(networkId, minBounds, maxBounds, networkNodes, pngData, bakeScaleFactor));
    }

    // Delegate getters to mapInfo
    public UUID getNetworkId() { return mapInfo.networkId(); }
    public BlockPos getMinBounds() { return mapInfo.minBounds(); }
    public BlockPos getMaxBounds() { return mapInfo.maxBounds(); }
    public byte[] getPngData() { return mapInfo.pngData() != null ? mapInfo.pngData().clone() : null; }
    public int getBakeScaleFactor() { return mapInfo.bakeScaleFactor(); }
    public List<NodeNetworkInfo> getNetworkNodes() { return mapInfo.networkNodes(); }

    // Expose the underlying MapInfo for more advanced usage
    public MapInfo getMapInfo() { return mapInfo; }
}
