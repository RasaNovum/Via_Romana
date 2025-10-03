package net.rasanovum.viaromana.map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;
import net.rasanovum.viaromana.path.PathGraph;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

public class MapBakeWorker {

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

        // 3. Render Image for the chunk area covering padded bounds
        long renderStartTime = System.nanoTime();
        BufferedImage chunkAreaImg = new BufferedImage(chunkAreaWidth / scaleFactor, chunkAreaHeight / scaleFactor, BufferedImage.TYPE_INT_ARGB);
        processChunkPngs(chunkAreaImg, level, minChunk, maxChunk, allowedChunks, scaleFactor);
        long renderTime = System.nanoTime() - renderStartTime;

        // 4. Crop to the exact padded bounds (which are the desired image bounds)
        int cropOffsetX = paddedMin.getX() - (minChunk.x * 16);
        int cropOffsetZ = paddedMin.getZ() - (minChunk.z * 16);
        BufferedImage finalImg = chunkAreaImg;
        if (cropOffsetX != 0 || cropOffsetZ != 0 || exactWidth != chunkAreaWidth || exactHeight != chunkAreaHeight) {
            int scaledCropX = cropOffsetX / scaleFactor;
            int scaledCropZ = cropOffsetZ / scaleFactor;
            if (scaledCropX + finalImgW > chunkAreaImg.getWidth() || scaledCropZ + finalImgH > chunkAreaImg.getHeight()) {
                throw new IllegalStateException("Crop bounds exceed chunk area image dimensions");
            }
            finalImg = chunkAreaImg.getSubimage(scaledCropX, scaledCropZ, finalImgW, finalImgH);
        }

        // 5. Encode to PNG
        long encodeStartTime = System.nanoTime();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(finalImg, "PNG", outputStream);
            byte[] pngData = outputStream.toByteArray();
            long encodeTime = System.nanoTime() - encodeStartTime;
            long totalBakeTime = System.nanoTime() - bakeStartTime;
            
            ViaRomana.LOGGER.info("[PERF] Map bake completed for network {}: total={}ms, render={}ms, encode={}ms, " +
                "dimensions={}x{}, scale={}, pngSize={}KB, chunks={}", 
                networkId, totalBakeTime / 1_000_000.0, renderTime / 1_000_000.0, encodeTime / 1_000_000.0,
                finalImgW, finalImgH, scaleFactor, pngData.length / 1024.0, allowedChunks.size());
            
            return MapInfo.fromServerCache(networkId, paddedMin, paddedMax, networkNodes, pngData, scaleFactor, new java.util.ArrayList<>(allowedChunks));
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert map to PNG", e);
        }
    }

    /**
     * Performs an incremental update on an existing map image.
     */
    public MapInfo updateMap(MapInfo previousResult, Set<ChunkPos> dirtyChunks, ServerLevel level, PathGraph.NetworkCache network) {
        long updateStartTime = System.nanoTime();
        ViaRomana.LOGGER.info("[PERF] Starting incremental map update: {} dirty chunks for network {}", dirtyChunks.size(), previousResult.networkId());

        long decodeStartTime = System.nanoTime();
        try {
            BufferedImage mapImage = ImageIO.read(new ByteArrayInputStream(previousResult.pngData()));
            long decodeTime = System.nanoTime() - decodeStartTime;
            ViaRomana.LOGGER.debug("[PERF] Map PNG decoded in {}ms", decodeTime / 1_000_000.0);
            
            Graphics2D graphics = mapImage.createGraphics();

            BlockPos paddedMin = previousResult.minBounds();
            int scaleFactor = previousResult.bakeScaleFactor();
            ChunkPos minChunk = new ChunkPos(paddedMin);

            ViaRomana.LOGGER.debug("Map update: minBounds(padded)={}, scaleFactor={}, minChunk={}", paddedMin, scaleFactor, minChunk);

            int chunksUpdated = 0;
            int chunksSkippedNoData = 0;
            long chunkProcessStartTime = System.nanoTime();

            for (ChunkPos dirtyPos : dirtyChunks) {
                chunksSkippedNoData++;
                ViaRomana.LOGGER.debug("Re-rendering dirty chunk {} ", dirtyPos);

                int relBlockX = dirtyPos.x * 16 - paddedMin.getX();
                int relBlockZ = dirtyPos.z * 16 - paddedMin.getZ();
                int chunkImgX = relBlockX / scaleFactor;
                int chunkImgZ = relBlockZ / scaleFactor;

                ViaRomana.LOGGER.debug("Updating chunk {} at image position ({}, {})", dirtyPos, chunkImgX, chunkImgZ);

                // Re-render PNG for dirty chunk
                byte[] newBytes = ChunkPngUtil.renderChunkPngBytes(level, dirtyPos);
                ChunkPngUtil.setPngBytes(level, dirtyPos, newBytes);

                // Composite to map
                Optional<byte[]> optBytes = ChunkPngUtil.getPngBytes(level, dirtyPos);
                if (optBytes.isPresent()) {
                    BufferedImage chunkImage = ChunkPngUtil.loadPngFromBytes(optBytes.get());
                    if (chunkImage != null) {
                        int scaledChunkSize = 16 / scaleFactor;
                        if (scaledChunkSize <= 0) scaledChunkSize = 1;
                        if (scaleFactor > 1) {
                            BufferedImage scaled = new BufferedImage(scaledChunkSize, scaledChunkSize, BufferedImage.TYPE_INT_ARGB);
                            Graphics2D sg = scaled.createGraphics();
                            sg.drawImage(chunkImage, 0, 0, scaledChunkSize, scaledChunkSize, null);
                            sg.dispose();
                            chunkImage = scaled;
                        }
                        graphics.drawImage(chunkImage, chunkImgX, chunkImgZ, null);
                        chunksUpdated++;
                    }
                }
            }

            graphics.dispose();
            long chunkProcessTime = System.nanoTime() - chunkProcessStartTime;

            ViaRomana.LOGGER.debug("Map incremental update completed: {} chunks updated, {} chunks skipped (no data)", chunksUpdated, chunksSkippedNoData);

            long encodeStartTime = System.nanoTime();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(mapImage, "PNG", outputStream);
            byte[] pngData = outputStream.toByteArray();
            long encodeTime = System.nanoTime() - encodeStartTime;
            long totalUpdateTime = System.nanoTime() - updateStartTime;
            
            ViaRomana.LOGGER.info("[PERF] Incremental update completed: total={}ms, decode={}ms, chunkProcess={}ms, encode={}ms, " +
                "chunksUpdated={}, pngSize={}KB", 
                totalUpdateTime / 1_000_000.0, decodeTime / 1_000_000.0, chunkProcessTime / 1_000_000.0, 
                encodeTime / 1_000_000.0, chunksUpdated, pngData.length / 1024.0);
            
            return MapInfo.fromServerCache(previousResult.networkId(), previousResult.minBounds(), previousResult.maxBounds(),
                    previousResult.networkNodes(), pngData, scaleFactor, previousResult.allowedChunks());

        } catch (IOException e) {
            ViaRomana.LOGGER.error("Failed to update map incrementally, performing a full re-bake.", e);
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

    private void processChunkPngs(BufferedImage img, ServerLevel level, ChunkPos min, ChunkPos max, Set<ChunkPos> allowedChunks, int scaleFactor) {
        long startTime = System.nanoTime();
        ViaRomana.LOGGER.debug("Processing chunk PNGs: area from {} to {}, {} allowed chunks, scale factor {}", min, max, allowedChunks.size(), scaleFactor);
        int chunksWithData = attemptRender(img, level, min, max, allowedChunks, scaleFactor);
        long processTime = System.nanoTime() - startTime;
        ViaRomana.LOGGER.debug("[PERF] Chunk PNG processing: {}ms, {}/{} chunks had data", 
            processTime / 1_000_000.0, chunksWithData, allowedChunks.size());
        if (chunksWithData == 0 && !allowedChunks.isEmpty()) {
            ViaRomana.LOGGER.warn("Map Bake: No chunk PNG data was available for the requested map area. The map may appear blank.");
        }
    }

    private int attemptRender(BufferedImage img, ServerLevel level, ChunkPos min, ChunkPos max, Set<ChunkPos> allowedChunks, int scaleFactor) {
        int pngChunks = 0;
        int totalChecked = 0;
        Graphics2D g = img.createGraphics();

        for (int cx = min.x; cx <= max.x; cx++) {
            for (int cz = min.z; cz <= max.z; cz++) {
                ChunkPos chunkPos = new ChunkPos(cx, cz);
                if (!allowedChunks.contains(chunkPos)) continue;

                totalChecked++;
                LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
                if (chunk == null) {
                    ViaRomana.LOGGER.warn("Failed to load chunk {} for PNG composite", chunkPos);
                    continue;
                }

                Optional<byte[]> optBytes = ChunkPngUtil.getPngBytes(level, chunkPos);

                if (optBytes.isEmpty() || optBytes.get().length == 0) {
                    byte[] newBytes = ChunkPngUtil.renderChunkPngBytes(level, chunkPos);
                    if (newBytes.length > 0) {
                        ChunkPngUtil.setPngBytes(level, chunkPos, newBytes);
                        optBytes = Optional.of(newBytes);
                    } else {
                        continue;
                    }
                }

                byte[] bytes = optBytes.get();
                BufferedImage chunkImg = ChunkPngUtil.loadPngFromBytes(bytes);
                if (chunkImg == null) {
                    ViaRomana.LOGGER.warn("Failed to load PNG image for chunk {} (bytes: {})", chunkPos, bytes.length);
                    continue;
                }

                pngChunks++;
                int scaledSize = 16 / scaleFactor;
                if (scaleFactor > 1) {
                    BufferedImage scaled = new BufferedImage(scaledSize, scaledSize, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D sg = scaled.createGraphics();
                    sg.drawImage(chunkImg, 0, 0, scaledSize, scaledSize, null);
                    sg.dispose();
                    chunkImg = scaled;
                }

                int baseX = (cx - min.x) * scaledSize;
                int baseZ = (cz - min.z) * scaledSize;
                g.drawImage(chunkImg, baseX, baseZ, null);
            }
        }

        g.dispose();
        ViaRomana.LOGGER.info("Render attempt complete: {}/{} chunks had PNG data", pngChunks, totalChecked);
        return pngChunks;
    }
}