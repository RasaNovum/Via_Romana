package net.rasanovum.viaromana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.rasanovum.viaromana.map.MapInfo;
import net.rasanovum.viaromana.network.DestinationResponsePacket.NodeNetworkInfo;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Packet for sending a map texture response.
 */
public class MapResponseS2C {
    private final MapInfo mapInfo;
    
    public MapResponseS2C(UUID networkId, BlockPos minBounds, BlockPos maxBounds, byte[] pngData, int bakeScaleFactor) {
        this.mapInfo = MapInfo.response(networkId, minBounds, maxBounds, new ArrayList<>(), pngData, bakeScaleFactor);
    }
    
    public MapResponseS2C(UUID networkId, BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes, byte[] pngData, int bakeScaleFactor) {
        this.mapInfo = MapInfo.response(networkId, minBounds, maxBounds, networkNodes, pngData, bakeScaleFactor);
    }
    
    public MapResponseS2C(FriendlyByteBuf buffer) {
        this.mapInfo = MapInfo.readFromBuffer(buffer);
    }
    
    public void write(FriendlyByteBuf buffer) {
        mapInfo.writeToBuffer(buffer);
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
