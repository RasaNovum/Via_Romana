package net.rasanovum.viaromana.map;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.storage.level.LevelDataManager;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Assembles pixel data from individual chunks into full map arrays.
 */
public class MapPixelAssembler {
    public record ChunkPixelResult(byte[] pixels, int cacheIncrement, int renderIncrement) {}
    public record BiomePixelResult(byte[] pixels, int cacheIncrement, int renderIncrement) {}
    public record RenderScale(int chunkScale, int chunkStride, int effectiveScale) {}

    /**
     * Calculates chunk-level scale and stride for rendering.
     * When total scale > 16, render fewer chunks (stride).
     * 
     * @param width Full width in blocks
     * @param height Full height in blocks
     * @return RenderScale with chunkScale (max 16), chunkStride, and effectiveScale
     */
    public static RenderScale calculateRenderScale(int width, int height) {
        int maxDim = Math.max(width, height);
        int MAX_DIM = CommonConfig.maximum_map_dimension;
        
        int totalScale = 1;
        if (maxDim > MAX_DIM) {
            int requiredScale = (int) Math.ceil((double) maxDim / MAX_DIM);
            totalScale = Integer.highestOneBit(requiredScale - 1) << 1;
        }
        
        int chunkScale = Math.min(totalScale, 16);
        int chunkStride = Math.max(1, totalScale / 16);
        return new RenderScale(chunkScale, chunkStride, chunkScale * chunkStride);
    }

    /**
     * Processes raw chunk pixels directly into separate biome and chunk pixel arrays.
     * Biome pixels are generated for all chunks, chunk pixels only for renderable chunks.
     * When stride > 1, only chunks at stride intervals are processed.
     */
    public static int processChunkPixels(byte[] biomePixels, byte[] chunkPixels, ServerLevel level, Set<ChunkPos> allChunks, Set<ChunkPos> renderedChunks, int scaleFactor, int pixelWidth, int pixelHeight, ChunkPos minChunk, boolean isPseudo, int chunkStride) {
        AtomicInteger chunksWithData = new AtomicInteger(0);
        AtomicInteger chunksFromCache = new AtomicInteger(0);
        AtomicInteger chunksRendered = new AtomicInteger(0);
        AtomicInteger chunksFromBiomeCache = new AtomicInteger(0);
        AtomicInteger chunksRenderedBiome = new AtomicInteger(0);
        Set<ChunkPos> targetChunkSet = !isPseudo ? allChunks : renderedChunks;
        int scaledChunkSize = 16 / scaleFactor;
        int totalChunks = allChunks.size();

        var chunkSource = level.getChunkSource();
        var randomState = chunkSource.randomState();
        var biomeSource = chunkSource.getGenerator().getBiomeSource();
        var climateSampler = randomState.sampler();
        int maxBuildHeight = level.getMaxBuildHeight();

        if (CommonConfig.logging_enum.ordinal() > 1) ViaRomana.LOGGER.info("Starting parallel bake for {} total chunks, {} renderable (isPseudo: {}, stride: {})", totalChunks, renderedChunks.size(), isPseudo, chunkStride);

        targetChunkSet.parallelStream().forEach(chunkToProcess -> {
            if (chunkStride > 1) { // Skip chunks that don't match stride pattern
                int relX = chunkToProcess.x - minChunk.x;
                int relZ = chunkToProcess.z - minChunk.z;
                if (relX % chunkStride != 0 || relZ % chunkStride != 0) {
                    return;
                }
            }
            
            byte[] biomeChunkPixels = null;
            if (!isPseudo) {
                var biomeResult = ChunkPixelRenderer.getOrRenderBiomePixels(level, chunkToProcess, biomeSource, climateSampler, maxBuildHeight);
                biomeChunkPixels = biomeResult.pixels();

                if (biomeChunkPixels == null || biomeChunkPixels.length != 256) {
                    ViaRomana.LOGGER.warn("Failed to generate biome pixels for chunk {}", chunkToProcess);
                    return;
                }
                chunksFromBiomeCache.addAndGet(biomeResult.cacheIncrement());
                chunksRenderedBiome.addAndGet(biomeResult.renderIncrement());
            }

            byte[] chunkPixelData = null;
            if (renderedChunks.contains(chunkToProcess)) {
                ChunkPixelResult result = getOrRenderChunkPixels(level, chunkToProcess, scaleFactor, biomeChunkPixels);
                if (result.pixels() != null && result.pixels().length == 256) {
                    chunkPixelData = result.pixels();
                    chunksFromCache.addAndGet(result.cacheIncrement());
                    chunksRendered.addAndGet(result.renderIncrement());
                }
            }

            if (isPseudo) return;

            int baseX = (chunkToProcess.x - minChunk.x) / chunkStride * scaledChunkSize;
            int baseZ = (chunkToProcess.z - minChunk.z) / chunkStride * scaledChunkSize;
            if (baseX < 0 || baseZ < 0 || baseX + scaledChunkSize > pixelWidth || baseZ + scaledChunkSize > pixelHeight) {
                ViaRomana.LOGGER.warn("Chunk {} out of pixel bounds, skipping", chunkToProcess);
                return;
            }

            byte[] scaledBiomePixels = (scaleFactor > 1) ? ChunkPixelRenderer.scalePixels(biomeChunkPixels, scaleFactor) : biomeChunkPixels;
            copyChunkToFullDirect(scaledBiomePixels, baseX, baseZ, scaledChunkSize, pixelWidth, biomePixels);

            if (chunkPixelData != null) {
                byte[] scaledChunkPixels = (scaleFactor > 1) ? ChunkPixelRenderer.scalePixels(chunkPixelData, scaleFactor) : chunkPixelData;
                copyChunkToFullDirect(scaledChunkPixels, baseX, baseZ, scaledChunkSize, pixelWidth, chunkPixels);
            }

            chunksWithData.incrementAndGet();
        });

        if (CommonConfig.logging_enum.ordinal() > 1) ViaRomana.LOGGER.info("Render attempt complete: {}/{} chunks processed. High-res: {} cached, {} rendered. Biome: {} cached, {} rendered.",
                chunksWithData.get(), totalChunks, chunksFromCache.get(), chunksRendered.get(), chunksFromBiomeCache.get(), chunksRenderedBiome.get());

        if (!isPseudo) {
            int missingCount = totalChunks - chunksWithData.get();
            if (chunksWithData.get() == 0 && totalChunks > 0) {
                ViaRomana.LOGGER.warn("Map Bake: No chunk pixel data was available for the requested map area.");
            } else if (missingCount > 0) {
                ViaRomana.LOGGER.warn("Map Bake: {} chunks were allowed but had no pixel data.", missingCount);
            }
        }

        return chunksWithData.get();
    }

    /**
     * Returns pixel result along with cache/render increment.
     * If scaleFactor >= 4 and no chunkPixel exists in cache, uses biomePixels to avoid expensive chunk rendering.
     */
    private static ChunkPixelResult getOrRenderChunkPixels(ServerLevel level, ChunkPos chunkPos, int scaleFactor, byte[] biomePixels) {
        byte @Nullable [] cached = LevelDataManager.getPixelBytes(level, chunkPos);
        if (cached != null) {
            if (cached.length == 256) {
                return new ChunkPixelResult(cached, 1, 0);
            }
        }

        if (CommonConfig.use_biome_fallback_for_lowres && scaleFactor >= 4 && biomePixels != null && biomePixels.length == 256) {
            return new ChunkPixelResult(biomePixels, 0, 0);
        }

        level.getChunk(chunkPos.x, chunkPos.z);

        byte[] pixels = ChunkPixelRenderer.renderChunkPixels(level, chunkPos);
        if (pixels.length == 256) {
            LevelDataManager.setPixelBytes(level, chunkPos, pixels);
            return new ChunkPixelResult(pixels, 0, 1);
        }

        ViaRomana.LOGGER.warn("Chunk {} failed to render (invalid size: {}), skipping", chunkPos, pixels.length);
        return new ChunkPixelResult(null, 0, 0);
    }

    /**
     * Copy scaled chunk row-by-row to full array at specified position.
     */
    private static void copyChunkToFullDirect(byte[] chunkPixels, int baseX, int baseZ, int scaledChunkSize, int pixelWidth, byte[] fullPixels) {
        for (int dz = 0; dz < scaledChunkSize; dz++) {
            int srcIdx = dz * scaledChunkSize;
            int dstIdx = (baseZ + dz) * pixelWidth + baseX;
            System.arraycopy(chunkPixels, srcIdx, fullPixels, dstIdx, scaledChunkSize);
        }
    }
}