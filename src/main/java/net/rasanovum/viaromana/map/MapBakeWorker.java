package net.rasanovum.viaromana.map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;
import net.rasanovum.viaromana.path.PathGraph;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

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
        
        // Compute padded bounds based on server-side constants
        int widthW = maxBounds.getX() - minBounds.getX();
        int heightW = maxBounds.getZ() - minBounds.getZ();
        int padX = Math.max(ServerMapUtils.MAP_BOUNDS_MIN_PADDING, (int) (widthW * ServerMapUtils.MAP_BOUNDS_PADDING_PERCENTAGE));
        int padZ = Math.max(ServerMapUtils.MAP_BOUNDS_MIN_PADDING, (int) (heightW * ServerMapUtils.MAP_BOUNDS_PADDING_PERCENTAGE));
        BlockPos paddedMin = minBounds.offset(-padX, 0, -padZ);
        BlockPos paddedMax = maxBounds.offset(padX, 0, padZ);

        // 1. Dimension Calculations on padded bounds
        int exactWidth = paddedMax.getX() - paddedMin.getX() + 1;
        int exactHeight = paddedMax.getZ() - paddedMin.getZ() + 1;
        int scaleFactor = calculateScaleFactor(exactWidth, exactHeight);
        int finalImgW = exactWidth / scaleFactor;
        int finalImgH = exactHeight / scaleFactor;

        // 2. Data Fetching & Preparation
        PathGraph graph = PathGraph.getInstance(level);

        if (graph == null) throw new IllegalStateException("PathGraph is null");

        PathGraph.NetworkCache network = graph.getNetworkCache(networkId);
        PathGraph.FoWCache fowCache = graph.getOrComputeFoWCache(network);

        if (fowCache == null) throw new IllegalStateException("FoW cache is null");

        ChunkPos minChunk = fowCache.minChunk();
        ChunkPos maxChunk = fowCache.maxChunk();

        int chunkAreaWidth = (maxChunk.x - minChunk.x + 1) * 16;
        int chunkAreaHeight = (maxChunk.z - minChunk.z + 1) * 16;

        Set<ChunkPos> allowedChunks = (fowCache != null) ? fowCache.allowedChunks() : ServerMapUtils.calculateFogOfWarChunks(networkNodes, minChunk, maxChunk);

        // 3. Render raw pixels for the chunk area covering padded bounds
        long renderStartTime = System.nanoTime();
        int pixelWidth = chunkAreaWidth / scaleFactor;
        int pixelHeight = chunkAreaHeight / scaleFactor;
        byte[] fullPixels = new byte[pixelWidth * pixelHeight];
        int chunksWithData = processChunkPixels(fullPixels, level, minChunk, maxChunk, allowedChunks, scaleFactor, pixelWidth);
        long renderTime = System.nanoTime() - renderStartTime;

        // 4. Crop raw pixels to exact padded bounds if needed
        byte[] finalPixels = fullPixels;
        int finalPixelWidth = pixelWidth;
        int finalPixelHeight = pixelHeight;
        
        int cropOffsetX = paddedMin.getX() - (minChunk.x * 16);
        int cropOffsetZ = paddedMin.getZ() - (minChunk.z * 16);
        
        if (cropOffsetX != 0 || cropOffsetZ != 0 || exactWidth != chunkAreaWidth || exactHeight != chunkAreaHeight) {
            int scaledCropX = cropOffsetX / scaleFactor;
            int scaledCropZ = cropOffsetZ / scaleFactor;
            finalPixelWidth = finalImgW;
            finalPixelHeight = finalImgH;
            finalPixels = new byte[finalPixelWidth * finalPixelHeight];
            
            // Copy cropped region
            for (int dz = 0; dz < finalPixelHeight; dz++) {
                int srcIdx = (scaledCropZ + dz) * pixelWidth + scaledCropX;
                int dstIdx = dz * finalPixelWidth;
                System.arraycopy(fullPixels, srcIdx, finalPixels, dstIdx, finalPixelWidth);
            }
        }

        // 5. Convert pixels to image and encode to PNG
        long convertStartTime = System.nanoTime();
        int[] argbPixels = new int[finalPixelWidth * finalPixelHeight];
        for (int i = 0; i < finalPixels.length; i++) {
            int packedId = finalPixels[i] & 0xFF;
            argbPixels[i] = COLOR_LOOKUP[packedId];
        }
        long convertTime = System.nanoTime() - convertStartTime;
        
        long encodeStartTime = System.nanoTime();
        try {
            BufferedImage finalImg = new BufferedImage(finalPixelWidth, finalPixelHeight, BufferedImage.TYPE_INT_ARGB);
            finalImg.getRaster().setDataElements(0, 0, finalPixelWidth, finalPixelHeight, argbPixels);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(finalPixelWidth * finalPixelHeight * 4); // Pre-size buffer
            javax.imageio.ImageIO.write(finalImg, "PNG", outputStream);
            byte[] pngData = outputStream.toByteArray();
            long encodeTime = System.nanoTime() - encodeStartTime;
            long totalBakeTime = System.nanoTime() - bakeStartTime;
            
            ViaRomana.LOGGER.info("[PERF] Map bake completed for network {}: total={}ms, render={}ms, convert={}ms, encode={}ms, " +
                "dimensions={}x{}, scale={}, pngSize={}KB, rawSize={}KB, chunks={}/{}", 
                networkId, totalBakeTime / 1_000_000.0, renderTime / 1_000_000.0, convertTime / 1_000_000.0, 
                encodeTime / 1_000_000.0, finalImgW, finalImgH, scaleFactor, pngData.length / 1024.0, 
                finalPixels.length / 1024.0, chunksWithData, allowedChunks.size());
            
            return MapInfo.fromServerCache(networkId, paddedMin, paddedMax, networkNodes, pngData, scaleFactor, 
                new java.util.ArrayList<>(allowedChunks), finalPixels, finalPixelWidth, finalPixelHeight);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert map to PNG", e);
        }
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
        
        // Check if network bounds changed (requires full rebake)
        if (!previousResult.minBounds().equals(network.getMin()) || !previousResult.maxBounds().equals(network.getMax())) {
            ViaRomana.LOGGER.info("Network bounds changed, performing full rebake instead of incremental");
            PathGraph graph = PathGraph.getInstance(level);
            return bake(previousResult.networkId(), level, network.getMin(), network.getMax(), graph.getNodesAsInfo(network));
        }
        
        ViaRomana.LOGGER.info("[PERF] Starting incremental update: {} dirty chunks", dirtyChunks.size());
        
        long spliceStartTime = System.nanoTime();
        byte[] updatedPixels = previousResult.fullPixels().clone();
        int scaleFactor = previousResult.bakeScaleFactor();
        int pixelWidth = previousResult.pixelWidth();
        int scaledChunkSize = 16 / scaleFactor;
        
        BlockPos paddedMin = previousResult.minBounds();
        ChunkPos minChunk = new ChunkPos(paddedMin);
        
        int chunksUpdated = 0;
        for (ChunkPos dirtyPos : dirtyChunks) {
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
            
            for (int dz = 0; dz < scaledChunkSize; dz++) {
                int srcIdx = dz * scaledChunkSize;
                int dstIdx = (baseZ + dz) * pixelWidth + baseX;
                System.arraycopy(chunkPixels, srcIdx, updatedPixels, dstIdx, scaledChunkSize);
            }
            chunksUpdated++;
        }
        long spliceTime = System.nanoTime() - spliceStartTime;
        
        // Convert to ARGB and encode PNG
        long convertStartTime = System.nanoTime();
        int[] argbPixels = new int[updatedPixels.length];
        for (int i = 0; i < updatedPixels.length; i++) {
            argbPixels[i] = COLOR_LOOKUP[updatedPixels[i] & 0xFF];
        }
        long convertTime = System.nanoTime() - convertStartTime;
        
        long encodeStartTime = System.nanoTime();
        try {
            BufferedImage finalImg = new BufferedImage(pixelWidth, previousResult.pixelHeight(), BufferedImage.TYPE_INT_ARGB);
            finalImg.getRaster().setDataElements(0, 0, pixelWidth, previousResult.pixelHeight(), argbPixels);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(pixelWidth * previousResult.pixelHeight() * 4);
            ImageIO.write(finalImg, "PNG", outputStream);
            byte[] pngData = outputStream.toByteArray();
            long encodeTime = System.nanoTime() - encodeStartTime;
            long totalUpdateTime = System.nanoTime() - updateStartTime;
            
            ViaRomana.LOGGER.info("[PERF] Incremental update completed: total={}ms, splice={}ms ({}chunks), convert={}ms, encode={}ms, pngSize={}KB", 
                totalUpdateTime / 1_000_000.0, spliceTime / 1_000_000.0, chunksUpdated, 
                convertTime / 1_000_000.0, encodeTime / 1_000_000.0, pngData.length / 1024.0);
            
            return MapInfo.fromServerCache(previousResult.networkId(), previousResult.minBounds(), previousResult.maxBounds(),
                previousResult.networkNodes(), pngData, scaleFactor, previousResult.allowedChunks(),
                updatedPixels, pixelWidth, previousResult.pixelHeight());
        } catch (IOException e) {
            ViaRomana.LOGGER.error("Failed incremental update, falling back to full rebake", e);
            PathGraph graph = PathGraph.getInstance(level);
            return bake(previousResult.networkId(), level, network.getMin(), network.getMax(), graph.getNodesAsInfo(network));
        }
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
        int scaledChunkSize = 16 / scaleFactor;

        for (ChunkPos chunkPos : allowedChunks) {
            // Skip chunks outside bounds
            if (chunkPos.x < min.x || chunkPos.x > max.x || chunkPos.z < min.z || chunkPos.z > max.z) {
                continue;
            }

            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            if (chunk == null) {
                ViaRomana.LOGGER.warn("Failed to load chunk {} for pixel composite", chunkPos);
                continue;
            }

            Optional<byte[]> optPixels = ChunkPixelUtil.getPixelBytes(level, chunkPos);
            byte[] chunkPixels;
            
            if (optPixels.isEmpty()) {
                chunkPixels = ChunkPixelUtil.renderChunkPixels(level, chunkPos);
                if (chunkPixels.length == 256) {
                    ChunkPixelUtil.setPixelBytes(level, chunkPos, chunkPixels);
                } else {
                    ViaRomana.LOGGER.warn("Chunk {} failed to render (invalid size: {}), skipping", chunkPos, chunkPixels.length);
                    continue;
                }
            } else {
                chunkPixels = optPixels.get();
                if (chunkPixels.length != 256) {
                    ViaRomana.LOGGER.warn("Chunk {} has invalid cached pixel data (size: {}), re-rendering", chunkPos, chunkPixels.length);
                    chunkPixels = ChunkPixelUtil.renderChunkPixels(level, chunkPos);
                    ChunkPixelUtil.setPixelBytes(level, chunkPos, chunkPixels);
                }
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

        ViaRomana.LOGGER.info("Render attempt complete: {}/{} chunks had pixel data", chunksWithData, allowedChunks.size());
        
        if (chunksWithData == 0 && !allowedChunks.isEmpty()) {
            ViaRomana.LOGGER.warn("Map Bake: No chunk pixel data was available for the requested map area. The map may appear blank.");
        } else if (chunksWithData < allowedChunks.size()) {
            int missingCount = allowedChunks.size() - chunksWithData;
            ViaRomana.LOGGER.warn("Map Bake: {} chunks were allowed but had no pixel data (missing chunks)", missingCount);
        }
        
        return chunksWithData;
    }
}