package net.rasanovum.viaromana.map;

import net.minecraft.world.level.ChunkPos;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ServerMapUtils {
    private ServerMapUtils() {}

    public static final float MAP_BOUNDS_PADDING_PERCENTAGE = 0.1f;
    public static final int MAP_BOUNDS_MIN_PADDING = 16;
    
    /**
     * Calculates uniform padding based on the larger dimension of the content bounds.
     */
    public static int calculateUniformPadding(int contentWidth, int contentHeight) {
        ViaRomana.LOGGER.info("Calculating uniform padding for content bounds: {}x{}", contentWidth, contentHeight);
        int maxDim = Math.max(contentWidth, contentHeight);
        ViaRomana.LOGGER.info("Max dimension: {}", maxDim);
        return Math.max(MAP_BOUNDS_MIN_PADDING, (int) (maxDim * MAP_BOUNDS_PADDING_PERCENTAGE));
    }

    public static Set<ChunkPos> calculateFogOfWarChunks(List<DestinationResponseS2C.NodeNetworkInfo> networkNodes, ChunkPos minChunk, ChunkPos maxChunk) {
        Set<ChunkPos> allowedChunks = new HashSet<>();
        if (networkNodes == null || networkNodes.isEmpty()) {
            ViaRomana.LOGGER.warn("No nodes provided for Fog of War calculation, returning empty set");
            return allowedChunks;
        }
        ViaRomana.LOGGER.info("Bounds: {}, {}", minChunk, maxChunk);
        final int FOG_OF_WAR_DISTANCE = CommonConfig.fog_of_war_distance;
        final int radiusSq = FOG_OF_WAR_DISTANCE * FOG_OF_WAR_DISTANCE;
        for (DestinationResponseS2C.NodeNetworkInfo node : networkNodes) {
            ChunkPos nodeChunk = new ChunkPos(node.position);
            for (int dx = -FOG_OF_WAR_DISTANCE; dx <= FOG_OF_WAR_DISTANCE; dx++) {
                for (int dz = -FOG_OF_WAR_DISTANCE; dz <= FOG_OF_WAR_DISTANCE; dz++) {
                    if (dx * dx + dz * dz > radiusSq) continue;
                    int tx = nodeChunk.x + dx;
                    int tz = nodeChunk.z + dz;
                    if (tx >= minChunk.x && tx <= maxChunk.x && tz >= minChunk.z && tz <= maxChunk.z) {
                        allowedChunks.add(new ChunkPos(tx, tz));
                    }
                }
            }
        }
        return allowedChunks;
    }
}
