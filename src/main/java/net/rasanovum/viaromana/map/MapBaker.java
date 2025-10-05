package net.rasanovum.viaromana.map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;
import net.rasanovum.viaromana.path.PathGraph;

import java.util.*;

import static net.rasanovum.viaromana.storage.level.LevelDataManager.setPixelBytes;

public class MapBaker {
    private static final int[] COLOR_LOOKUP = new int[256]; // ARGB color lookup table
    
    static {
        COLOR_LOOKUP[0] = 0x00000000;
        for (int packedId = 1; packedId < 256; packedId++) {
            int mcColor = net.minecraft.world.level.material.MapColor.getColorFromPackedId(packedId);
            int r = (mcColor >> 16) & 0xFF;
            int g = (mcColor >> 8) & 0xFF;
            int b = mcColor & 0xFF;
            COLOR_LOOKUP[packedId] = 0xFF000000 | (b << 16) | (g << 8) | r;
        }
    }

    public MapInfo bake(UUID networkId, ServerLevel level, BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes) {
        long bakeStartTime = System.nanoTime();
        ViaRomana.LOGGER.info("[PERF] Starting full map bake for network {}", networkId);
        ViaRomana.LOGGER.info("Bounds: ({}), ({})", minBounds, maxBounds);
        
        // 1. Get FoW data and full chunk bounds
        PathGraph graph = PathGraph.getInstance(level);
        if (graph == null) throw new IllegalStateException("PathGraph is null");
        PathGraph.NetworkCache network = graph.getNetworkCache(networkId);
        PathGraph.FoWCache fowCache = graph.getOrComputeFoWCache(network);
        if (fowCache == null) throw new IllegalStateException("FoW cache is null");

        // Create list of all chunks in the bounding box
        Set<ChunkPos> bakeChunks = fowCache.allowedChunks();
        BlockPos desiredMinBlock = fowCache.minBlock();
        BlockPos desiredMaxBlock = fowCache.maxBlock();
        ChunkPos bakeMinChunk = fowCache.minChunk();
        ChunkPos bakeMaxChunk = fowCache.maxChunk();

        // Create list of all chunks within the map bounds
        Set<ChunkPos> mapChunks = new HashSet<>();
        for (int cx = bakeMinChunk.x; cx <= bakeMaxChunk.x; cx++) {
            for (int cz = bakeMinChunk.z; cz <= bakeMaxChunk.z; cz++) {
                mapChunks.add(new ChunkPos(cx, cz));
            }
        }

        // 2. Calculate scale factor for FULL chunk region
        int fullChunkWidth = (bakeMaxChunk.x - bakeMinChunk.x + 1) * 16;
        int fullChunkHeight = (bakeMaxChunk.z - bakeMinChunk.z + 1) * 16;
        int scaleFactor = MapPixelAssembler.calculateScaleFactor(fullChunkWidth, fullChunkHeight);

        // 3. Render chunk region to pixel arrays
        long renderStartTime = System.nanoTime();
        int fullPixelWidth = fullChunkWidth / scaleFactor;
        int fullPixelHeight = fullChunkHeight / scaleFactor;
        byte[] biomePixels = new byte[fullPixelWidth * fullPixelHeight];
        byte[] chunkPixels = new byte[fullPixelWidth * fullPixelHeight];
        int chunksWithData = MapPixelAssembler.processChunkPixels(biomePixels, chunkPixels, level, mapChunks, bakeChunks, scaleFactor, fullPixelWidth, fullPixelHeight, bakeMinChunk);
        long renderTime = System.nanoTime() - renderStartTime;
        
        int desiredMinX = desiredMinBlock.getX();
        int desiredMinZ = desiredMinBlock.getZ();
        int desiredMaxX = desiredMaxBlock.getX();
        int desiredMaxZ = desiredMaxBlock.getZ();
        
        int bakedMinX = bakeMinChunk.x * 16;
        int bakedMinZ = bakeMinChunk.z * 16;
        
        int startPixelX = (desiredMinX - bakedMinX) / scaleFactor;
        int startPixelZ = (desiredMinZ - bakedMinZ) / scaleFactor;
        int croppedPixelWidth = ((desiredMaxX - desiredMinX + 1) / scaleFactor);
        int croppedPixelHeight = ((desiredMaxZ - desiredMinZ + 1) / scaleFactor);
        
        // Clamp to valid ranges
        int effectiveStartX = Math.max(0, startPixelX);
        int effectiveStartZ = Math.max(0, startPixelZ);
        int effectiveWidth = Math.min(croppedPixelWidth, fullPixelWidth - effectiveStartX);
        int effectiveHeight = Math.min(croppedPixelHeight, fullPixelHeight - effectiveStartZ);
        
        byte[] croppedBiomePixels = new byte[effectiveWidth * effectiveHeight];
        byte[] croppedChunkPixels = new byte[effectiveWidth * effectiveHeight];
        for (int z = 0; z < effectiveHeight; z++) {
            int srcZ = effectiveStartZ + z;
            if (srcZ >= fullPixelHeight) break;
            int srcIdx = srcZ * fullPixelWidth + effectiveStartX;
            int dstIdx = z * effectiveWidth;
            int copyWidth = Math.min(effectiveWidth, fullPixelWidth - effectiveStartX);
            System.arraycopy(biomePixels, srcIdx, croppedBiomePixels, dstIdx, copyWidth);
            System.arraycopy(chunkPixels, srcIdx, croppedChunkPixels, dstIdx, copyWidth);
        }

        // 5. Return with chunk-aligned world coordinates from FoW
        long totalBakeTime = System.nanoTime() - bakeStartTime;
        
        ViaRomana.LOGGER.info("[PERF] Map bake completed for network {}: total={}ms, render={}ms, " +
            "dimensions={}x{}, scale={}, rawSize={}KB, chunks={}",
            networkId, totalBakeTime / 1_000_000.0, renderTime / 1_000_000.0, 
            effectiveWidth, effectiveHeight, scaleFactor, (croppedBiomePixels.length + croppedChunkPixels.length) / 1024.0, 
            chunksWithData);
        
        return MapInfo.fromServer(networkId, croppedBiomePixels, croppedChunkPixels, effectiveWidth, effectiveHeight, scaleFactor, desiredMinX, desiredMinZ, desiredMaxX, desiredMaxZ, new ArrayList<>(bakeChunks), networkNodes);
    }

    /**
     * Splices dirty chunks into existing pixel array.
     * Falls back to full rebake if no cached pixels available.
     * Note: With dual maps, incremental updates are complex, so we do full rebake for now.
     */
    public MapInfo updateMap(MapInfo previousResult, Set<ChunkPos> dirtyChunks, ServerLevel level, PathGraph.NetworkCache network) {
        ViaRomana.LOGGER.info("Incremental update requested for {} dirty chunks, performing full rebake due to dual map system", dirtyChunks.size());
        PathGraph graph = PathGraph.getInstance(level);
        return bake(previousResult.networkId(), level, network.getMin(), network.getMax(), graph.getNodesAsInfo(network));
    }
}