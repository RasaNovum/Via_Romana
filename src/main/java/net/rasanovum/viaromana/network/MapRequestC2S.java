package net.rasanovum.viaromana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.rasanovum.viaromana.map.MapInfo;
import net.rasanovum.viaromana.network.DestinationResponsePacket.NodeNetworkInfo;

import java.util.List;
import java.util.UUID;

/**
 * Packet for requesting a map texture for a specific world region.
 */
public class MapRequestC2S {
    private final MapInfo mapInfo;
    
    public MapRequestC2S(UUID networkId, BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes) {
        this.mapInfo = MapInfo.request(networkId, minBounds, maxBounds, networkNodes);
    }
    
    public MapRequestC2S(FriendlyByteBuf buffer) {
        this.mapInfo = MapInfo.readFromBuffer(buffer);
    }
    
    public void write(FriendlyByteBuf buffer) {
        mapInfo.writeToBuffer(buffer);
    }
    
    // Delegate getters to mapInfo
    public UUID getNetworkId() { return mapInfo.networkId(); }
    public BlockPos getMinBounds() { return mapInfo.minBounds(); }
    public BlockPos getMaxBounds() { return mapInfo.maxBounds(); }
    public List<NodeNetworkInfo> getNetworkNodes() { return mapInfo.networkNodes(); }
    
    // Expose the underlying MapInfo for more advanced usage
    public MapInfo getMapInfo() { return mapInfo; }
}
