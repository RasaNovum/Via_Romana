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

        // 3. Render chunk region to pixel array
        long renderStartTime = System.nanoTime();
        int fullPixelWidth = fullChunkWidth / scaleFactor;
        int fullPixelHeight = fullChunkHeight / scaleFactor;
        byte[] fullPixels = new byte[fullPixelWidth * fullPixelHeight];
        int chunksWithData = MapPixelAssembler.processChunkPixels(fullPixels, level, mapChunks, bakeChunks, scaleFactor, fullPixelWidth, fullPixelHeight, bakeMinChunk);
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
        
        byte[] croppedPixels = new byte[effectiveWidth * effectiveHeight];
        for (int z = 0; z < effectiveHeight; z++) {
            int srcZ = effectiveStartZ + z;
            if (srcZ >= fullPixelHeight) break;
            int srcIdx = srcZ * fullPixelWidth + effectiveStartX;
            int dstIdx = z * effectiveWidth;
            System.arraycopy(fullPixels, srcIdx, croppedPixels, dstIdx, Math.min(effectiveWidth, fullPixelWidth - effectiveStartX));
        }

        // 5. Return with chunk-aligned world coordinates from FoW
        long totalBakeTime = System.nanoTime() - bakeStartTime;
        
        ViaRomana.LOGGER.info("[PERF] Map bake completed for network {}: total={}ms, render={}ms, " +
            "dimensions={}x{}, scale={}, rawSize={}KB, chunks={}",
            networkId, totalBakeTime / 1_000_000.0, renderTime / 1_000_000.0, 
            effectiveWidth, effectiveHeight, scaleFactor, croppedPixels.length / 1024.0, 
            chunksWithData);
        
        return MapInfo.fromServer(networkId, croppedPixels, effectiveWidth, effectiveHeight, scaleFactor, desiredMinX, desiredMinZ, desiredMaxX, desiredMaxZ, new ArrayList<>(bakeChunks), networkNodes);
    }

    /**
     * Splices dirty chunks into existing pixel array.
     * Falls back to full rebake if no cached pixels available.
     */
    public MapInfo updateMap(MapInfo previousResult, Set<ChunkPos> dirtyChunks, ServerLevel level, PathGraph.NetworkCache network) {
        long updateStartTime = System.nanoTime();
        
        // If no cached pixels OR network bounds changed, do full rebake
        if (previousResult.fullPixels() == null || previousResult.pixelWidth() == 0) {
            ViaRomana.LOGGER.debug("No cached pixels, performing full rebake for {} dirty chunks", dirtyChunks.size());
            PathGraph graph = PathGraph.getInstance(level);
            return bake(previousResult.networkId(), level, network.getMin(), network.getMax(), graph.getNodesAsInfo(network));
        }
        
        // Check if FoW bounds changed
        PathGraph graph = PathGraph.getInstance(level);
        PathGraph.FoWCache fowCache = graph.getOrComputeFoWCache(network);
        if (fowCache == null) {
            ViaRomana.LOGGER.warn("FoW cache unavailable for incremental update, performing full rebake");
            return bake(previousResult.networkId(), level, network.getMin(), network.getMax(), graph.getNodesAsInfo(network));
        }
        
        // Check if network bounds changed (including padding)
        int padding = ServerMapUtils.calculateUniformPadding(network.getMax().getX() - network.getMin().getX(), network.getMax().getZ() - network.getMin().getZ());
        int currentDesiredMinX = network.getMin().getX() - padding;
        int currentDesiredMinZ = network.getMin().getZ() - padding;
        int currentDesiredMaxX = network.getMax().getX() + padding;
        int currentDesiredMaxZ = network.getMax().getZ() + padding;
        
        if (previousResult.worldMinX() != currentDesiredMinX || previousResult.worldMaxX() != currentDesiredMaxX ||
            previousResult.worldMinZ() != currentDesiredMinZ || previousResult.worldMaxZ() != currentDesiredMaxZ) {
            ViaRomana.LOGGER.info("Network bounds changed, performing full rebake instead of incremental");
            return bake(previousResult.networkId(), level, network.getMin(), network.getMax(), graph.getNodesAsInfo(network));
        }
        
        ViaRomana.LOGGER.info("[PERF] Starting incremental update: {} dirty chunks", dirtyChunks.size());
        
        long spliceStartTime = System.nanoTime();
        byte[] updatedPixels = previousResult.fullPixels().clone();
        int scaleFactor = previousResult.scaleFactor();
        int pixelWidth = previousResult.pixelWidth();
        int scaledChunkSize = 16 / scaleFactor;
        
        ChunkPos minChunk = previousResult.getMinChunk();
        ChunkPos maxChunk = previousResult.getMaxChunk();
        
        int bakedMinX = minChunk.x * 16;
        int bakedMinZ = minChunk.z * 16;
        int desiredMinX = previousResult.worldMinX();
        int desiredMinZ = previousResult.worldMinZ();
        int startPixelX = (desiredMinX - bakedMinX) / scaleFactor;
        int startPixelZ = (desiredMinZ - bakedMinZ) / scaleFactor;
        
        int chunksUpdated = 0;
        int chunksSkipped = 0;
        for (ChunkPos dirtyPos : dirtyChunks) {
            if (dirtyPos.x < minChunk.x || dirtyPos.x > maxChunk.x || 
                dirtyPos.z < minChunk.z || dirtyPos.z > maxChunk.z) {
                chunksSkipped++;
                continue;
            }
            
            // Render dirty chunk
            byte[] chunkPixels = ChunkPixelRenderer.renderChunkPixels(level, dirtyPos);
            if (chunkPixels.length != 256) continue;
            setPixelBytes(level, dirtyPos, chunkPixels);
            
            // Scale if needed
            if (scaleFactor > 1) {
                chunkPixels = ChunkPixelRenderer.scalePixels(chunkPixels, scaleFactor);
            }
            
            // Splice into full pixel array
            int baseX = (dirtyPos.x - minChunk.x) * scaledChunkSize;
            int baseZ = (dirtyPos.z - minChunk.z) * scaledChunkSize;
            int dstX = baseX - startPixelX;
            int dstZ = baseZ - startPixelZ;
            
            // Prevent ArrayIndexOutOfBounds
            if (dstX < 0 || dstZ < 0 || dstX + scaledChunkSize > pixelWidth || 
                dstZ + scaledChunkSize > previousResult.pixelHeight()) {
                ViaRomana.LOGGER.warn("Chunk {} out of splice bounds, skipping (dstX={}, dstZ={}, width={}, height={})", 
                    dirtyPos, dstX, dstZ, pixelWidth, previousResult.pixelHeight());
                chunksSkipped++;
                continue;
            }
            
            for (int dz = 0; dz < scaledChunkSize; dz++) {
                int srcIdx = dz * scaledChunkSize;
                int dstIdx = (dstZ + dz) * pixelWidth + dstX;
                System.arraycopy(chunkPixels, srcIdx, updatedPixels, dstIdx, scaledChunkSize);
            }
            chunksUpdated++;
        }
        
        if (chunksSkipped > 0) {
            ViaRomana.LOGGER.info("Skipped {} dirty chunks outside cached bounds (likely network resize)", chunksSkipped);
        }
        long spliceTime = System.nanoTime() - spliceStartTime;
        long totalUpdateTime = System.nanoTime() - updateStartTime;
        
        ViaRomana.LOGGER.info("[PERF] Incremental update completed: total={}ms, splice={}ms ({}chunks), rawSize={}KB", 
            totalUpdateTime / 1_000_000.0, spliceTime / 1_000_000.0, chunksUpdated, updatedPixels.length / 1024.0);
        
        return MapInfo.fromServer(previousResult.networkId(), updatedPixels, pixelWidth, previousResult.pixelHeight(), scaleFactor,
            previousResult.worldMinX(), previousResult.worldMinZ(), previousResult.worldMaxX(), previousResult.worldMaxZ(),
            previousResult.allowedChunks(), previousResult.networkNodes());
    }
}