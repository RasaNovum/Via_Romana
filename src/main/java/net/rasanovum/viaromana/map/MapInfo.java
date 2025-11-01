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
    byte[] biomePixels,                 // biome pixel array (base layer)
    byte[] chunkPixels,                 // chunk pixel array (overlay layer, 16x16 per chunk at 1x scale, scaled down if scaleFactor > 1)
    int pixelWidth,
    int pixelHeight,
    int scaleFactor,
    int worldMinX,                      // world X coordinate of the top-left pixel
    int worldMinZ,                      // world Z coordinate of the top-left pixel
    int worldMaxX,                      // world X coordinate of the bottom-right pixel
    int worldMaxZ,                      // world Z coordinate of the bottom-right pixel
    Long createdAtMs,
    List<ChunkPos> allowedChunks,       // fog-of-war chunk list (server-side only)
    List<NodeNetworkInfo> networkNodes  // network nodes for spline rendering
) {

    /**
     * Creates a map request.
     */
    public static MapInfo request(UUID networkId, BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes) {
        return new MapInfo(networkId, null, null, 0, 0, 1,
                          minBounds.getX(), minBounds.getZ(), maxBounds.getX(), maxBounds.getZ(),
                          null, null,
                          networkNodes != null ? new ArrayList<>(networkNodes) : new ArrayList<>());
    }

    /**
     * Creates a map from server (with full pixel data and world coordinates).
     */
    public static MapInfo fromServer(UUID networkId, byte[] biomePixels, byte[] chunkPixels, int pixelWidth, int pixelHeight, int scaleFactor,
                                     int worldMinX, int worldMinZ, int worldMaxX, int worldMaxZ,
                                     List<ChunkPos> allowedChunks, List<NodeNetworkInfo> networkNodes) {
        return new MapInfo(networkId,
                          biomePixels != null ? biomePixels.clone() : null,
                          chunkPixels != null ? chunkPixels.clone() : null, pixelWidth, pixelHeight, scaleFactor,
                          worldMinX, worldMinZ, worldMaxX, worldMaxZ,
                          System.currentTimeMillis(),
                          allowedChunks != null ? new ArrayList<>(allowedChunks) : null,
                          networkNodes != null ? new ArrayList<>(networkNodes) : new ArrayList<>());
    }

    public boolean hasImageData() {
        return (biomePixels != null || chunkPixels != null) && pixelWidth > 0 && pixelHeight > 0;
    }

    // Network serialization
    public void writeToBuffer(FriendlyByteBuf buffer) {
        buffer.writeUUID(networkId);

        buffer.writeInt(networkNodes.size());
        for (NodeNetworkInfo node : networkNodes) {
            buffer.writeBlockPos(node.position);
            buffer.writeFloat(node.clearance);
            buffer.writeInt(node.connections.size());
            for (BlockPos connection : node.connections) {
                buffer.writeBlockPos(connection);
            }
        }

        if (hasImageData()) {
            buffer.writeBoolean(true);
            if (biomePixels != null) {
                buffer.writeBoolean(true);
                buffer.writeInt(biomePixels.length);
                buffer.writeBytes(biomePixels);
            } else {
                buffer.writeBoolean(false);
            }

            if (chunkPixels != null) {
                buffer.writeBoolean(true);
                buffer.writeInt(chunkPixels.length);
                buffer.writeBytes(chunkPixels);
            } else {
                buffer.writeBoolean(false);
            }
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
        byte[] biomePixels = null;
        byte[] chunkPixels = null;
        int pixelWidth = 0;
        int pixelHeight = 0;
        int scaleFactor = 1;
        int worldMinX = 0;
        int worldMinZ = 0;
        int worldMaxX = 0;
        int worldMaxZ = 0;
        Long createdAtMs = null;

        if (hasPixelData) {
            // Read biome pixels
            boolean hasBiomePixels = buffer.readBoolean();
            if (hasBiomePixels) {
                int biomeLength = buffer.readInt();
                biomePixels = new byte[biomeLength];
                buffer.readBytes(biomePixels);
            }
            // Read chunk pixels
            boolean hasChunkPixels = buffer.readBoolean();
            if (hasChunkPixels) {
                int chunkLength = buffer.readInt();
                chunkPixels = new byte[chunkLength];
                buffer.readBytes(chunkPixels);
            }
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

        return new MapInfo(networkId, biomePixels, chunkPixels, pixelWidth, pixelHeight, scaleFactor, worldMinX, worldMinZ, worldMaxX, worldMaxZ, createdAtMs, null, networkNodes);
    }

    public MapInfo {
        networkNodes = networkNodes != null ? new ArrayList<>(networkNodes) : new ArrayList<>();
        allowedChunks = allowedChunks != null ? new ArrayList<>(allowedChunks) : null;
        biomePixels = biomePixels != null ? biomePixels.clone() : null;
        chunkPixels = chunkPixels != null ? chunkPixels.clone() : null;
    }
}
