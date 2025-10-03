package net.rasanovum.viaromana.map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Record for map data that can represent both requests (with null image data)
 * and responses (with populated image data).
 */
public record MapInfo(
    UUID networkId,
    BlockPos minBounds,
    BlockPos maxBounds,
    List<NodeNetworkInfo> networkNodes,
    byte[] pngData, // null for requests, populated for responses
    int bakeScaleFactor,
    Long createdAtMs, // null for requests, populated with System.currentTimeMillis() for responses
    List<ChunkPos> allowedChunks, // optional persisted fog-of-war chunk list
    byte[] fullPixels, // raw pixel array for incremental updates (null if not cached)
    int pixelWidth,    // width of fullPixels array
    int pixelHeight    // height of fullPixels array
) {
    
    /**
     * Creates a map request (no image data).
     */
    public static MapInfo request(UUID networkId, BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes) {
        return new MapInfo(networkId, minBounds, maxBounds, 
                          networkNodes != null ? new ArrayList<>(networkNodes) : new ArrayList<>(), 
                          null, 1, null, null, null, 0, 0);
    }
    
    /**
     * Creates a map response (with raw pixel data).
     */
    public static MapInfo response(UUID networkId, BlockPos minBounds, BlockPos maxBounds, 
                                 List<NodeNetworkInfo> networkNodes, byte[] fullPixels, int pixelWidth, int pixelHeight, int bakeScaleFactor) {
        return new MapInfo(networkId, minBounds, maxBounds, 
                          networkNodes != null ? new ArrayList<>(networkNodes) : new ArrayList<>(), 
                          null, // pngData is null - client will generate it
                          bakeScaleFactor, System.currentTimeMillis(), null, 
                          fullPixels != null ? fullPixels.clone() : null, pixelWidth, pixelHeight);
    }
    
    /**
     * Creates a map response from server cache data (with optional fullPixels for incremental updates).
     */
    public static MapInfo fromServerCache(UUID networkId, BlockPos minBounds, BlockPos maxBounds, 
                                        List<NodeNetworkInfo> networkNodes, byte[] pngData, int bakeScaleFactor, List<ChunkPos> allowedChunks,
                                        byte[] fullPixels, int pixelWidth, int pixelHeight) {
        return new MapInfo(networkId, minBounds, maxBounds, 
                          networkNodes != null ? new ArrayList<>(networkNodes) : new ArrayList<>(), 
                          pngData != null ? pngData.clone() : null, bakeScaleFactor, System.currentTimeMillis(), 
                          allowedChunks != null ? new ArrayList<>(allowedChunks) : null,
                          fullPixels != null ? fullPixels.clone() : null, pixelWidth, pixelHeight);
    }
    
    // Helper methods
    public boolean hasImageData() {
        return fullPixels != null && pixelWidth > 0 && pixelHeight > 0;
    }
    
    public boolean isRequest() {
        return fullPixels == null;
    }
    
    public boolean isResponse() {
        return fullPixels != null && pixelWidth > 0 && pixelHeight > 0;
    }
    
    public int getWorldWidth() {
        return maxBounds.getX() - minBounds.getX() + 1;
    }
    
    public int getWorldHeight() {
        return maxBounds.getZ() - minBounds.getZ() + 1;
    }
    
    public boolean hasTimestamp() {
        return createdAtMs != null;
    }
    
    public long getAgeMs() {
        return createdAtMs != null ? System.currentTimeMillis() - createdAtMs : 0L;
    }
    
    public String getFormattedAge() {
        if (createdAtMs == null) return "no timestamp";
        long ageMs = getAgeMs();
        if (ageMs < 1000) return ageMs + "ms ago";
        if (ageMs < 60000) return (ageMs / 1000) + "s ago";
        return (ageMs / 60000) + "m ago";
    }
    
    // Network serialization
    public void writeToBuffer(FriendlyByteBuf buffer) {
        buffer.writeUUID(networkId);
        buffer.writeBlockPos(minBounds);
        buffer.writeBlockPos(maxBounds);
        
        // Write network nodes
        buffer.writeInt(networkNodes.size());
        for (NodeNetworkInfo node : networkNodes) {
            buffer.writeBlockPos(node.position);
            buffer.writeFloat(node.clearance);
            buffer.writeInt(node.connections.size());
            for (BlockPos connection : node.connections) {
                buffer.writeBlockPos(connection);
            }
        }
        
        // Write raw pixel data (if present) - client will convert to PNG
        if (fullPixels != null && pixelWidth > 0 && pixelHeight > 0) {
            buffer.writeBoolean(true);
            buffer.writeInt(fullPixels.length);
            buffer.writeBytes(fullPixels);
            buffer.writeInt(pixelWidth);
            buffer.writeInt(pixelHeight);
            buffer.writeInt(bakeScaleFactor);
            buffer.writeLong(createdAtMs != null ? createdAtMs : 0L);
        } else {
            buffer.writeBoolean(false);
        }

        // Write optional allowed chunks list
        if (allowedChunks != null) {
            buffer.writeBoolean(true);
            buffer.writeInt(allowedChunks.size());
            for (ChunkPos cp : allowedChunks) {
                buffer.writeInt(cp.x);
                buffer.writeInt(cp.z);
            }
        } else {
            buffer.writeBoolean(false);
        }
    }
    
    public static MapInfo readFromBuffer(FriendlyByteBuf buffer) {
        UUID networkId = buffer.readUUID();
        BlockPos minBounds = buffer.readBlockPos();
        BlockPos maxBounds = buffer.readBlockPos();
        
        // Read network nodes
        int nodeCount = buffer.readInt();
        List<NodeNetworkInfo> networkNodes = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            BlockPos nodePos = buffer.readBlockPos();
            float clearance = buffer.readFloat();
            int connectionCount = buffer.readInt();
            List<BlockPos> connections = new ArrayList<>(connectionCount);
            for (int j = 0; j < connectionCount; j++) {
                connections.add(buffer.readBlockPos());
            }
            networkNodes.add(new NodeNetworkInfo(nodePos, clearance, connections));
        }
        
        // Read raw pixel data (if present) - client will convert to PNG
        boolean hasPixelData = buffer.readBoolean();
        byte[] fullPixels = null;
        int pixelWidth = 0;
        int pixelHeight = 0;
        int bakeScaleFactor = 1;
        Long createdAtMs = null;
        
        if (hasPixelData) {
            int length = buffer.readInt();
            fullPixels = new byte[length];
            buffer.readBytes(fullPixels);
            pixelWidth = buffer.readInt();
            pixelHeight = buffer.readInt();
            bakeScaleFactor = buffer.readInt();
            long ts = buffer.readLong();
            createdAtMs = ts == 0L ? null : ts;
        }

        // Read optional allowed chunks
        List<ChunkPos> allowed = null;
        boolean hasChunks = buffer.readBoolean();
        if (hasChunks) {
            int size = buffer.readInt();
            allowed = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int x = buffer.readInt();
                int z = buffer.readInt();
                allowed.add(new ChunkPos(x, z));
            }
        }
        
        // Network receives raw pixels (pngData will be generated client-side)
        return new MapInfo(networkId, minBounds, maxBounds, networkNodes, null, bakeScaleFactor, createdAtMs, allowed, fullPixels, pixelWidth, pixelHeight);
    }
    
    // Ensure defensive copying of mutable fields
    public MapInfo {
        networkNodes = networkNodes != null ? new ArrayList<>(networkNodes) : new ArrayList<>();
        pngData = pngData != null ? pngData.clone() : null;
        allowedChunks = allowedChunks != null ? new ArrayList<>(allowedChunks) : null;
        fullPixels = fullPixels != null ? fullPixels.clone() : null;
    }
}
