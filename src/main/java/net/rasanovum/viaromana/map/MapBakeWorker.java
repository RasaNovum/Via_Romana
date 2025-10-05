package net.rasanovum.viaromana.map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;
import net.rasanovum.viaromana.path.PathGraph;

import java.util.*;

public class MapBakeWorker {
    private static final int[] COLOR_LOOKUP = new int[256]; // ARGB color lookup table
    public record PixelResult(byte[] pixels, int cacheIncrement, int renderIncrement) {}
    
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
        int scaleFactor = calculateScaleFactor(fullChunkWidth, fullChunkHeight);

        // 3. Render chunk region to pixel array
        long renderStartTime = System.nanoTime();
        int fullPixelWidth = fullChunkWidth / scaleFactor;
        int fullPixelHeight = fullChunkHeight / scaleFactor;
        byte[] fullPixels = new byte[fullPixelWidth * fullPixelHeight];
        int chunksWithData = processChunkPixels(fullPixels, level, mapChunks, bakeChunks, scaleFactor, fullPixelWidth, fullPixelHeight, bakeMinChunk);
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
    private int processChunkPixels(byte[] fullPixels, ServerLevel level, Set<ChunkPos> allChunks, Set<ChunkPos> renderedChunks, int scaleFactor, int pixelWidth, int pixelHeight, ChunkPos minChunk) {
        int chunksWithData = 0;
        int chunksFromCache = 0;
        int chunksRendered = 0;
        int chunksFromBiome = 0;
        int scaledChunkSize = 16 / scaleFactor;

        long lastLogTime = System.nanoTime();
        int processedCount = 0;
        int totalChunks = allChunks.size();

        ViaRomana.LOGGER.info("Total chunks={}, renderable={}", totalChunks, renderedChunks.size());

        for (ChunkPos chunkToProcess : allChunks) {
            processedCount++;
            long now = System.nanoTime();
            if (processedCount % 500 == 0 || (now - lastLogTime) > 2_000_000_000L) {
                ViaRomana.LOGGER.info("[PERF] Baking progress: {}/{} chunks ({} cached, {} rendered, {} biome)",
                        processedCount, totalChunks, chunksFromCache, chunksRendered, chunksFromBiome);
                lastLogTime = now;
            }

            byte[] chunkPixels = null;
            boolean isRenderable = renderedChunks.contains(chunkToProcess);

            if (isRenderable) {
                PixelResult result = getOrRenderChunkPixels(level, chunkToProcess);
                if (result.pixels() != null && result.pixels().length == 256) {
                    chunkPixels = result.pixels();
                    chunksFromCache += result.cacheIncrement();
                    chunksRendered += result.renderIncrement();
                }
            }

            if (chunkPixels == null) {
                chunkPixels = ChunkPixelUtil.generateBiomeFallbackPixels(level, chunkToProcess);
                chunksFromBiome++;
            }

            if (chunkPixels.length != 256) {
                ViaRomana.LOGGER.warn("Failed to generate pixels (render or fallback) for chunk {}", chunkToProcess);
                continue;
            }

            int baseX = (chunkToProcess.x - minChunk.x) * scaledChunkSize;
            int baseZ = (chunkToProcess.z - minChunk.z) * scaledChunkSize;
            if (baseX < 0 || baseZ < 0 || baseX + scaledChunkSize > pixelWidth || baseZ + scaledChunkSize > pixelHeight) {
                ViaRomana.LOGGER.warn("Chunk {} out of pixel bounds (baseX={}, baseZ={}, width={}, height={}), skipping",
                        chunkToProcess, baseX, baseZ, pixelWidth, pixelHeight);
                continue;
            }

            if (scaleFactor > 1) {
                chunkPixels = ChunkPixelUtil.scalePixels(chunkPixels, scaleFactor);
            }

            copyChunkToFull(chunkPixels, chunkToProcess, minChunk, scaledChunkSize, pixelWidth, fullPixels);
            chunksWithData++;
        }

        ViaRomana.LOGGER.info("Render attempt complete: {}/{} chunks had pixel data ({} cached, {} freshly rendered, {} biome fallbacks)",
                chunksWithData, totalChunks, chunksFromCache, chunksRendered, chunksFromBiome);

        int missingCount = totalChunks - chunksWithData;
        if (chunksWithData == 0 && totalChunks > 0) {
            ViaRomana.LOGGER.warn("Map Bake: No chunk pixel data was available for the requested map area. The map may appear blank.");
        } else if (missingCount > 0) {
            ViaRomana.LOGGER.warn("Map Bake: {} chunks were allowed but had no pixel data (missing chunks)", missingCount);
        }

        return chunksWithData;
    }

    /**
     * Returns pixel result along with cache/render increment
     */
    private PixelResult getOrRenderChunkPixels(ServerLevel level, ChunkPos chunkPos) {
        Optional<byte[]> cached = ChunkPixelUtil.getPixelBytes(level, chunkPos);
        if (cached.isPresent()) {
            byte[] pixels = cached.get();
            if (pixels.length == 256) {  // Valid full chunk
                return new PixelResult(pixels, 1, 0);
            }
        }

        level.getChunk(chunkPos.x, chunkPos.z); // Force-load chunk if no cache present

        byte[] pixels = ChunkPixelUtil.renderChunkPixels(level, chunkPos);
        if (pixels.length == 256) {
            ChunkPixelUtil.setPixelBytes(level, chunkPos, pixels);
            return new PixelResult(pixels, 0, 1);
        }

        ViaRomana.LOGGER.warn("Chunk {} failed to render (invalid size: {}), skipping", chunkPos, pixels.length);
        return new PixelResult(null, 0, 0);
    }

    /**
     * Copy scaled chunk row-by-row to full array
     */
    private void copyChunkToFull(byte[] chunkPixels, ChunkPos chunkPos, ChunkPos min, int scaledChunkSize, int pixelWidth, byte[] fullPixels) {
        int baseX = (chunkPos.x - min.x) * scaledChunkSize;
        int baseZ = (chunkPos.z - min.z) * scaledChunkSize;
        for (int dz = 0; dz < scaledChunkSize; dz++) {
            int srcIdx = dz * scaledChunkSize;
            int dstIdx = (baseZ + dz) * pixelWidth + baseX;
            System.arraycopy(chunkPixels, srcIdx, fullPixels, dstIdx, scaledChunkSize);
        }
    }
}