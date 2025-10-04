package net.rasanovum.viaromana.map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;
import net.rasanovum.viaromana.path.PathGraph;

import java.util.*;

public class MapBakeWorker {
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
        
        // 1. Get FoW data and full chunk bounds
        PathGraph graph = PathGraph.getInstance(level);
        if (graph == null) throw new IllegalStateException("PathGraph is null");
        PathGraph.NetworkCache network = graph.getNetworkCache(networkId);
        PathGraph.FoWCache fowCache = graph.getOrComputeFoWCache(network);
        if (fowCache == null) throw new IllegalStateException("FoW cache is null");

        ChunkPos minChunk = fowCache.minChunk();
        ChunkPos maxChunk = fowCache.maxChunk();
        Set<ChunkPos> allowedChunks = fowCache.allowedChunks();

        // 2. Calculate scale factor for FULL chunk region
        int fullChunkWidth = (maxChunk.x - minChunk.x + 1) * 16;
        int fullChunkHeight = (maxChunk.z - minChunk.z + 1) * 16;
        int scaleFactor = calculateScaleFactor(fullChunkWidth, fullChunkHeight);

        // 3. Render ENTIRE chunk region to pixel array
        long renderStartTime = System.nanoTime();
        int fullPixelWidth = fullChunkWidth / scaleFactor;
        int fullPixelHeight = fullChunkHeight / scaleFactor;
        byte[] fullPixels = new byte[fullPixelWidth * fullPixelHeight];
        int chunksWithData = processChunkPixels(fullPixels, level, minChunk, maxChunk, allowedChunks, scaleFactor, fullPixelWidth);
        long renderTime = System.nanoTime() - renderStartTime;

        // 4. Use FoW region directly
        int finalWorldMinX = minChunk.x * 16;
        int finalWorldMinZ = minChunk.z * 16;
        int finalWorldMaxX = maxChunk.x * 16 + 15;
        int finalWorldMaxZ = maxChunk.z * 16 + 15;
        
        // Calculate padding for logging
        int contentWidth = maxBounds.getX() - minBounds.getX();
        int contentHeight = maxBounds.getZ() - minBounds.getZ();
        int padding = ServerMapUtils.calculateUniformPadding(contentWidth, contentHeight);

        // 5. Return with chunk-aligned world coordinates from FoW
        long totalBakeTime = System.nanoTime() - bakeStartTime;
        
        ViaRomana.LOGGER.info("[PERF] Map bake completed for network {}: total={}ms, render={}ms, " +
            "dimensions={}x{}, scale={}, rawSize={}KB, chunks={}/{}, contentPadding={}blocks", 
            networkId, totalBakeTime / 1_000_000.0, renderTime / 1_000_000.0, 
            fullPixelWidth, fullPixelHeight, scaleFactor, fullPixels.length / 1024.0, 
            chunksWithData, allowedChunks.size(), padding);
        
        return MapInfo.fromServer(networkId, fullPixels, fullPixelWidth, fullPixelHeight, scaleFactor,
            finalWorldMinX, finalWorldMinZ, finalWorldMaxX, finalWorldMaxZ,
            new java.util.ArrayList<>(allowedChunks), networkNodes);
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
        
        ChunkPos currentMinChunk = fowCache.minChunk();
        ChunkPos currentMaxChunk = fowCache.maxChunk();
        
        int currentWorldMinX = currentMinChunk.x * 16;
        int currentWorldMinZ = currentMinChunk.z * 16;
        int currentWorldMaxX = currentMaxChunk.x * 16 + 15;
        int currentWorldMaxZ = currentMaxChunk.z * 16 + 15;
        
        if (previousResult.worldMinX() != currentWorldMinX || previousResult.worldMaxX() != currentWorldMaxX ||
            previousResult.worldMinZ() != currentWorldMinZ || previousResult.worldMaxZ() != currentWorldMaxZ) {
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
        
        int chunksUpdated = 0;
        int chunksSkipped = 0;
        for (ChunkPos dirtyPos : dirtyChunks) {
            if (dirtyPos.x < minChunk.x || dirtyPos.x > maxChunk.x || 
                dirtyPos.z < minChunk.z || dirtyPos.z > maxChunk.z) {
                chunksSkipped++;
                continue;
            }
            
            // Render dirty chunk
            byte[] chunkPixels = ChunkPixelUtil.renderChunkPixels(level, dirtyPos);
            if (chunkPixels.length != 256) continue;
            ChunkPixelUtil.setPixelBytes(level, dirtyPos, chunkPixels);
            
            // Scale if needed
            if (scaleFactor > 1) {
                chunkPixels = ChunkPixelUtil.scalePixels(chunkPixels, scaleFactor);
            }
            
            // Splice into full pixel array
            int baseX = (dirtyPos.x - minChunk.x) * scaledChunkSize;
            int baseZ = (dirtyPos.z - minChunk.z) * scaledChunkSize;
            
            // Prevent ArrayIndexOutOfBounds
            if (baseX < 0 || baseZ < 0 || baseX + scaledChunkSize > pixelWidth || 
                baseZ + scaledChunkSize > previousResult.pixelHeight()) {
                ViaRomana.LOGGER.warn("Chunk {} out of splice bounds, skipping (baseX={}, baseZ={}, width={}, height={})", 
                    dirtyPos, baseX, baseZ, pixelWidth, previousResult.pixelHeight());
                chunksSkipped++;
                continue;
            }
            
            for (int dz = 0; dz < scaledChunkSize; dz++) {
                int srcIdx = dz * scaledChunkSize;
                int dstIdx = (baseZ + dz) * pixelWidth + baseX;
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

    private int calculateScaleFactor(int width, int height) {
        int maxDim = Math.max(width, height);
        int MAX_DIM = CommonConfig.maximum_map_dimension;
        if (maxDim <= MAX_DIM) return 1;
        int requiredScale = (int) Math.ceil((double) maxDim / MAX_DIM);
        return Integer.highestOneBit(requiredScale - 1) << 1;
    }

    /**
     * Processes raw chunk pixels directly into a full pixel array.
     * Iterates over allowed chunks to avoid wasted bounding box iterations.
     */
    private int processChunkPixels(byte[] fullPixels, ServerLevel level, ChunkPos min, ChunkPos max, 
                                    Set<ChunkPos> allowedChunks, int scaleFactor, int pixelWidth) {
        int chunksWithData = 0;
        int chunksFromCache = 0;
        int chunksRendered = 0;
        int scaledChunkSize = 16 / scaleFactor;
        
        long lastLogTime = System.nanoTime();
        int processedCount = 0;
        int totalChunks = allowedChunks.size();

        for (ChunkPos chunkPos : allowedChunks) {
            // TODO: Remove this logging
            processedCount++;
            long now = System.nanoTime();
            if (processedCount % 500 == 0 || (now - lastLogTime) > 2_000_000_000L) {
                ViaRomana.LOGGER.info("[PERF] Baking progress: {}/{} chunks ({} cached, {} rendered)", 
                    processedCount, totalChunks, chunksFromCache, chunksRendered);
                lastLogTime = now;
            }
            
            // Skip chunks outside bounds
            if (chunkPos.x < min.x || chunkPos.x > max.x || chunkPos.z < min.z || chunkPos.z > max.z) {
                continue;
            }

            // First check cache to avoid rendering if data exists
            Optional<byte[]> cachedPixels = ChunkPixelUtil.getPixelBytes(level, chunkPos);
            if (cachedPixels.isPresent()) {
                byte[] chunkPixels = cachedPixels.get();
                chunksFromCache++;
                
                if (chunkPixels.length != 256) {
                    level.getChunk(chunkPos.x, chunkPos.z); // TODO: See if lazy loading can be used instead of chunk loading (unless that's what's already happening)
                    chunkPixels = ChunkPixelUtil.renderChunkPixels(level, chunkPos);
                    chunksRendered++;
                    chunksFromCache--;
                    ChunkPixelUtil.setPixelBytes(level, chunkPos, chunkPixels);
                }
                
                if (scaleFactor > 1) {
                    chunkPixels = ChunkPixelUtil.scalePixels(chunkPixels, scaleFactor);
                }
                
                int baseX = (chunkPos.x - min.x) * scaledChunkSize;
                int baseZ = (chunkPos.z - min.z) * scaledChunkSize;
                for (int dz = 0; dz < scaledChunkSize; dz++) {
                    int srcIdx = dz * scaledChunkSize;
                    int dstIdx = (baseZ + dz) * pixelWidth + baseX;
                    System.arraycopy(chunkPixels, srcIdx, fullPixels, dstIdx, scaledChunkSize);
                }
                chunksWithData++;
                continue;
            }
            
            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            if (chunk == null) {
                ViaRomana.LOGGER.warn("Failed to load chunk {} for pixel composite", chunkPos);
                continue;
            }
            
            byte[] chunkPixels = ChunkPixelUtil.renderChunkPixels(level, chunkPos);
            chunksRendered++;
            if (chunkPixels.length == 256) {
                ChunkPixelUtil.setPixelBytes(level, chunkPos, chunkPixels);
            } else {
                ViaRomana.LOGGER.warn("Chunk {} failed to render (invalid size: {}), skipping", chunkPos, chunkPixels.length);
                continue;
            }

            if (scaleFactor > 1) { // Scale if needed
                chunkPixels = ChunkPixelUtil.scalePixels(chunkPixels, scaleFactor);
            }

            // Copy chunk pixels to full pixel array
            int baseX = (chunkPos.x - min.x) * scaledChunkSize;
            int baseZ = (chunkPos.z - min.z) * scaledChunkSize;
            
            for (int dz = 0; dz < scaledChunkSize; dz++) {
                int srcIdx = dz * scaledChunkSize;
                int dstIdx = (baseZ + dz) * pixelWidth + baseX;
                System.arraycopy(chunkPixels, srcIdx, fullPixels, dstIdx, scaledChunkSize);
            }
            
            chunksWithData++;
        }

        ViaRomana.LOGGER.info("Render attempt complete: {}/{} chunks had pixel data ({} cached, {} freshly rendered)", 
            chunksWithData, allowedChunks.size(), chunksFromCache, chunksRendered);
        
        if (chunksWithData == 0 && !allowedChunks.isEmpty()) {
            ViaRomana.LOGGER.warn("Map Bake: No chunk pixel data was available for the requested map area. The map may appear blank.");
        } else if (chunksWithData < allowedChunks.size()) {
            int missingCount = allowedChunks.size() - chunksWithData;
            ViaRomana.LOGGER.warn("Map Bake: {} chunks were allowed but had no pixel data (missing chunks)", missingCount);
        }
        
        return chunksWithData;
    }
}