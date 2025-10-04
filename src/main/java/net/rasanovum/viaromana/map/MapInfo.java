package net.rasanovum.viaromana.map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Map data record with direct world-to-pixel coordinate mapping.
 */
public record MapInfo(
    UUID networkId,
    byte[] fullPixels,                  // raw pixel array (16x16 per chunk at 1x scale, scaled down if scaleFactor > 1)
    int pixelWidth,
    int pixelHeight,
    int scaleFactor,
    int worldMinX,                      // chunk-aligned: chunkX * 16
    int worldMinZ,                      // chunk-aligned: chunkZ * 16
    int worldMaxX,                      // chunk-aligned: chunkX * 16 + 15
    int worldMaxZ,                      // chunk-aligned: chunkZ * 16 + 15
    Long createdAtMs,
    List<ChunkPos> allowedChunks,       // fog-of-war chunk list (server-side only)
    List<NodeNetworkInfo> networkNodes  // network nodes for spline rendering
) {
    
    /**
     * Creates a map request (no image data).
     */
    public static MapInfo request(UUID networkId, BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes) {
        return new MapInfo(networkId, null, 0, 0, 1, 
                          minBounds.getX(), minBounds.getZ(), maxBounds.getX(), maxBounds.getZ(),
                          null, null,
                          networkNodes != null ? new ArrayList<>(networkNodes) : new ArrayList<>());
    }
    
    /**
     * Creates a map from server (with full pixel data and world coordinates).
     */
    public static MapInfo fromServer(UUID networkId, byte[] fullPixels, int pixelWidth, int pixelHeight, int scaleFactor,
                                     int worldMinX, int worldMinZ, int worldMaxX, int worldMaxZ,
                                     List<ChunkPos> allowedChunks, List<NodeNetworkInfo> networkNodes) {
        return new MapInfo(networkId, 
                          fullPixels != null ? fullPixels.clone() : null, pixelWidth, pixelHeight, scaleFactor,
                          worldMinX, worldMinZ, worldMaxX, worldMaxZ,
                          System.currentTimeMillis(),
                          allowedChunks != null ? new ArrayList<>(allowedChunks) : null,
                          networkNodes != null ? new ArrayList<>(networkNodes) : new ArrayList<>());
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
        return worldMaxX - worldMinX + 1;
    }
    
    public int getWorldHeight() {
        return worldMaxZ - worldMinZ + 1;
    }
    
    /**
     * Get the ChunkPos corresponding to worldMinX/Z.
     */
    public ChunkPos getMinChunk() {
        return new ChunkPos(worldMinX >> 4, worldMinZ >> 4);
    }
    
    /**
     * Get the ChunkPos corresponding to worldMaxX/Z.
     */
    public ChunkPos getMaxChunk() {
        return new ChunkPos(worldMaxX >> 4, worldMaxZ >> 4);
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
        
        // Write raw pixel data and world coordinates
        if (fullPixels != null && pixelWidth > 0 && pixelHeight > 0) {
            buffer.writeBoolean(true);
            buffer.writeInt(fullPixels.length);
            buffer.writeBytes(fullPixels);
            buffer.writeInt(pixelWidth);
            buffer.writeInt(pixelHeight);
            buffer.writeInt(scaleFactor);
            buffer.writeInt(worldMinX);
            buffer.writeInt(worldMinZ);
            buffer.writeInt(worldMaxX);
            buffer.writeInt(worldMaxZ);
            buffer.writeLong(createdAtMs != null ? createdAtMs : 0L);
        } else {
            buffer.writeBoolean(false);
        }
    }
    
    public static MapInfo readFromBuffer(FriendlyByteBuf buffer) {
        UUID networkId = buffer.readUUID();
        
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
        
        // Read raw pixel data and world coordinates
        boolean hasPixelData = buffer.readBoolean();
        byte[] fullPixels = null;
        int pixelWidth = 0;
        int pixelHeight = 0;
        int scaleFactor = 1;
        int worldMinX = 0;
        int worldMinZ = 0;
        int worldMaxX = 0;
        int worldMaxZ = 0;
        Long createdAtMs = null;
        
        if (hasPixelData) {
            int length = buffer.readInt();
            fullPixels = new byte[length];
            buffer.readBytes(fullPixels);
            pixelWidth = buffer.readInt();
            pixelHeight = buffer.readInt();
            scaleFactor = buffer.readInt();
            worldMinX = buffer.readInt();
            worldMinZ = buffer.readInt();
            worldMaxX = buffer.readInt();
            worldMaxZ = buffer.readInt();
            long ts = buffer.readLong();
            createdAtMs = ts == 0L ? null : ts;
        }
        
        return new MapInfo(networkId, fullPixels, pixelWidth, pixelHeight, scaleFactor, worldMinX, worldMinZ, worldMaxX, worldMaxZ, createdAtMs, null, networkNodes);
    }
    
    // Ensure defensive copying of mutable fields
    public MapInfo {
        networkNodes = networkNodes != null ? new ArrayList<>(networkNodes) : new ArrayList<>();
        allowedChunks = allowedChunks != null ? new ArrayList<>(allowedChunks) : null;
        fullPixels = fullPixels != null ? fullPixels.clone() : null;
    }
}
