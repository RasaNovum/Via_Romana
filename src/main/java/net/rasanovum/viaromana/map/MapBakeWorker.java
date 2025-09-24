package net.rasanovum.viaromana.map;

import folk.sisby.surveyor.terrain.ChunkSummary;
import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import folk.sisby.surveyor.util.RegistryPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.configuration.ViaRomanaConfig;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.surveyor.SurveyorUtil;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

public class MapBakeWorker {
    
    public MapInfo bake(UUID networkId, ServerLevel level, BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes) {
        // Compute padded bounds based on server-side constants (must match client intent)
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
        WorldTerrainSummary terrain = SurveyorUtil.getTerrain(level);

        if (terrain == null) throw new IllegalStateException("Surveyor terrain data is null");

        PathGraph graph = PathGraph.getInstance(level);

        if (graph == null) throw new IllegalStateException("PathGraph is null");

        PathGraph.NetworkCache network = graph.getNetworkCache(networkId);
        PathGraph.FoWCache fowCache = graph.getOrComputeFoWCache(network);

        if (fowCache == null) throw new IllegalStateException("Surveyor terrain data is null");
        
        ChunkPos minChunk = fowCache.minChunk();
        ChunkPos maxChunk = fowCache.maxChunk();

        int chunkAreaWidth = (maxChunk.x - minChunk.x + 1) * 16;
        int chunkAreaHeight = (maxChunk.z - minChunk.z + 1) * 16;

        Set<ChunkPos> allowedChunks = (fowCache != null) ? fowCache.allowedChunks() : ServerMapUtils.calculateFogOfWarChunks(networkNodes, minChunk, maxChunk);

        // 3. Render Image for the chunk area covering padded bounds
        BufferedImage chunkAreaImg = new BufferedImage(chunkAreaWidth / scaleFactor, chunkAreaHeight / scaleFactor, BufferedImage.TYPE_INT_ARGB);
        processSurveyorChunks(chunkAreaImg, level, terrain, minChunk, maxChunk, allowedChunks, scaleFactor);

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
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(finalImg, "PNG", outputStream);
            return MapInfo.fromServerCache(networkId, paddedMin, paddedMax, networkNodes, outputStream.toByteArray(), scaleFactor, new java.util.ArrayList<>(allowedChunks));
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert map to PNG", e);
        }
    }

    /**
     * Performs an incremental update on an existing map image.
     */
    public MapInfo updateMap(MapInfo previousResult, Set<ChunkPos> dirtyChunks, ServerLevel level, PathGraph.NetworkCache network) {
        ViaRomana.LOGGER.debug("MapBakeWorker.updateMap() called with {} dirty chunks for network {}", dirtyChunks.size(), previousResult.networkId());
        
        StringBuilder dirtyChunksList = new StringBuilder();
        for (ChunkPos pos : dirtyChunks) {
            dirtyChunksList.append(pos.toString()).append(" ");
        }
        ViaRomana.LOGGER.debug("Dirty chunks to update: {}", dirtyChunksList.toString().trim());
        
        try {
            BufferedImage mapImage = ImageIO.read(new ByteArrayInputStream(previousResult.pngData()));
            Graphics2D graphics = mapImage.createGraphics();

            WorldTerrainSummary terrain = SurveyorUtil.getTerrain(level);
            if (terrain == null) {
                ViaRomana.LOGGER.error("Cannot update map: Surveyor terrain is null.");
                return previousResult;
            }

            BlockPos paddedMin = previousResult.minBounds();
            int scaleFactor = previousResult.bakeScaleFactor();
            ChunkPos minChunk = new ChunkPos(paddedMin);
            
            ViaRomana.LOGGER.debug("Map update: minBounds(padded)={}, scaleFactor={}, minChunk={}", paddedMin, scaleFactor, minChunk);

            int chunksUpdated = 0;
            int chunksSkippedNoData = 0;
            
            for (ChunkPos dirtyPos : dirtyChunks) {
                ChunkSummary summary = terrain.get(dirtyPos);
                if (summary == null) {
                    chunksSkippedNoData++;
                    ViaRomana.LOGGER.warn("No Surveyor data for dirty chunk {}", dirtyPos);
                    continue;
                }

                int relBlockX = dirtyPos.x * 16 - paddedMin.getX();
                int relBlockZ = dirtyPos.z * 16 - paddedMin.getZ();
                int chunkImgX = relBlockX / scaleFactor;
                int chunkImgZ = relBlockZ / scaleFactor;

                ViaRomana.LOGGER.debug("Updating chunk {} at image position ({}, {})", dirtyPos, chunkImgX, chunkImgZ);

                int scaledChunkSize = 16 / scaleFactor;
                if (scaledChunkSize <= 0) scaledChunkSize = 1;
                BufferedImage chunkImage = new BufferedImage(scaledChunkSize, scaledChunkSize, BufferedImage.TYPE_INT_ARGB);
                
                renderSurveyorChunk(chunkImage, level, terrain, summary, dirtyPos, dirtyPos.x, dirtyPos.z, scaleFactor);
                
                graphics.drawImage(chunkImage, chunkImgX, chunkImgZ, null);
                chunksUpdated++;
            }

            graphics.dispose();
            
            ViaRomana.LOGGER.debug("Map incremental update completed: {} chunks updated, {} chunks skipped (no data)", chunksUpdated, chunksSkippedNoData);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(mapImage, "PNG", outputStream);
            return MapInfo.fromServerCache(previousResult.networkId(), previousResult.minBounds(), previousResult.maxBounds(), 
                                  previousResult.networkNodes(), outputStream.toByteArray(), scaleFactor, previousResult.allowedChunks());

        } catch (IOException e) {
            ViaRomana.LOGGER.error("Failed to update map incrementally, performing a full re-bake.", e);
            return bake(previousResult.networkId(), level, network.getMin(), network.getMax(), network.getNodesAsInfo());
        }
    }

    private int calculateScaleFactor(int width, int height) {
        int maxDim = Math.max(width, height);
        int MAX_DIM = ViaRomanaConfig.maximum_map_dimension;
        if (maxDim <= MAX_DIM) return 1;
        int requiredScale = (int) Math.ceil((double) maxDim / MAX_DIM);
        return Integer.highestOneBit(requiredScale - 1) << 1;
    }

    private void processSurveyorChunks(BufferedImage img, ServerLevel level, WorldTerrainSummary terrain, ChunkPos min, ChunkPos max, Set<ChunkPos> allowedChunks, int scaleFactor) {
        ViaRomana.LOGGER.debug("Processing Surveyor chunks: area from {} to {}, {} allowed chunks, scale factor {}", 
                             min, max, allowedChunks.size(), scaleFactor);
        int chunksWithData = attemptRender(img, level, terrain, min, max, allowedChunks, scaleFactor);
        ViaRomana.LOGGER.debug("Surveyor chunk processing complete: {} chunks had data out of {} allowed chunks", 
                             chunksWithData, allowedChunks.size());
        if (chunksWithData == 0 && !allowedChunks.isEmpty()) {
            ViaRomana.LOGGER.warn("Map Bake: No Surveyor chunk data was available for the requested map area. The map may appear blank.");
        }
    }

    private int attemptRender(BufferedImage img, ServerLevel level, WorldTerrainSummary terrain, ChunkPos min, ChunkPos max, Set<ChunkPos> allowedChunks, int scaleFactor) {
        int surveyorDataChunks = 0;
        int totalChecked = 0;
        
        for (int cx = min.x; cx <= max.x; cx++) {
            for (int cz = min.z; cz <= max.z; cz++) {
                ChunkPos chunkPos = new ChunkPos(cx, cz);
                if (!allowedChunks.contains(chunkPos)) continue;
                
                totalChecked++;
                ChunkSummary chunkSummary = terrain.get(chunkPos);
                if (chunkSummary != null) {
                    surveyorDataChunks++;
                    ViaRomana.LOGGER.debug("Rendering chunk {} with Surveyor data", chunkPos);
                    renderSurveyorChunk(img, level, terrain, chunkSummary, min, cx, cz, scaleFactor);
                } else {
                    ViaRomana.LOGGER.debug("No Surveyor data for chunk {}", chunkPos);
                }
            }
        }
        
        ViaRomana.LOGGER.debug("Render attempt complete: {}/{} chunks had Surveyor data", surveyorDataChunks, totalChecked);
        return surveyorDataChunks;
    }

    private void renderSurveyorChunk(BufferedImage img, ServerLevel level, WorldTerrainSummary terrain, ChunkSummary summary, ChunkPos minPos, int cx, int cz, int scaleFactor) {
        LayerSummary.Raw layer = summary.toSingleLayer(null, null, level.getMaxBuildHeight());
        if (layer == null) return;
        
        var blockPalette = terrain.getBlockPalette(new ChunkPos(cx, cz));
        int worldTop = level.getMaxBuildHeight();
        
        int[] heights = new int[256];
        for (int i = 0; i < 256; i++) {
            heights[i] = layer.exists().get(i) ? worldTop - layer.depths()[i] : Integer.MIN_VALUE;
        }
        
        int baseX = (cx - minPos.x) * 16;
        int baseZ = (cz - minPos.z) * 16;
        
        for (int lx = 0; lx < 16; lx += scaleFactor) {
            for (int lz = 0; lz < 16; lz += scaleFactor) {
                int px = (baseX + lx) / scaleFactor;
                int pz = (baseZ + lz) / scaleFactor;
                
                if (px >= img.getWidth() || pz >= img.getHeight()) continue;
                
                int color = getPixelColor(lx * 16 + lz, heights, layer.blocks(), layer.waterDepths(), layer.exists(), blockPalette);
                if (color != -1) {
                    img.setRGB(px, pz, color | 0xFF000000);
                }
            }
        }
    }

    private int getPixelColor(int idx, int[] heights, int[] blocks, int[] waterDepths, BitSet exists, RegistryPalette<Block>.ValueView blockPalette) {
        boolean hasWater = waterDepths != null && waterDepths[idx] > 0;
        if (!exists.get(idx) && !hasWater) return -1;
        
        MapColor mapColor;
        MapColor.Brightness brightness;
        
        if (hasWater) {
            mapColor = MapColor.WATER;
            brightness = calculateWaterBrightness(idx, waterDepths[idx]);
        } else {
            Block block = blockPalette.byId(blocks[idx]);
            if (block == null || block == Blocks.AIR) return -1;
            
            mapColor = block.defaultMapColor();
            brightness = calculateTerrainBrightness(heights, idx);
        }
        
        int mcColor = mapColor.calculateRGBColor(brightness);

        return ((mcColor & 0xFF) << 16) | (mcColor & 0xFF00) | ((mcColor >> 16) & 0xFF);
    }

    private MapColor.Brightness calculateWaterBrightness(int idx, int waterDepth) {
        double shade = Math.min(waterDepth / 8.0, 1.0) + (((idx >> 4) + (idx & 15)) & 1) * 0.15;
        return shade < 0.3 ? MapColor.Brightness.HIGH : shade > 0.7 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
    }

    private MapColor.Brightness calculateTerrainBrightness(int[] heights, int idx) {
        int lx = idx >> 4;
        int lz = idx & 15;
        
        int currentHeight = heights[idx];
        int westHeight = (lx > 0) ? heights[idx - 16] : currentHeight;
        
        double shade = (currentHeight - westHeight) * 4.0 / (double) 2.0 + ((((lz + lx) & 1) - 0.5) * 0.4);
        
        if (shade > 0.6) return MapColor.Brightness.HIGH;
        if (shade < -0.6) return MapColor.Brightness.LOW;
        return MapColor.Brightness.NORMAL;
    }
}