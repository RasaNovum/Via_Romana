package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.map.MapInfo;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;

import java.util.List;
import java.util.UUID;

/**
 * Packet for requesting a map texture for a specific world region.
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

    // Factory method for easier construction
    public static MapRequestC2S create(UUID networkId, BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes) {
        return new MapRequestC2S(MapInfo.request(networkId, minBounds, maxBounds, networkNodes));
    }

    // Delegate getters to mapInfo
    public UUID getNetworkId() { return mapInfo.networkId(); }
    public BlockPos getMinBounds() { return mapInfo.minBounds(); }
    public BlockPos getMaxBounds() { return mapInfo.maxBounds(); }
    public List<NodeNetworkInfo> getNetworkNodes() { return mapInfo.networkNodes(); }

    // Expose the underlying MapInfo for more advanced usage
    public MapInfo getMapInfo() { return mapInfo; }
}
