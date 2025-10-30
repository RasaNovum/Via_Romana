package net.rasanovum.viaromana.map;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.storage.level.LevelDataManager;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Assembles pixel data from individual chunks into full map arrays.
 */
public class MapPixelAssembler {
    public record ChunkPixelResult(byte[] pixels, int cacheIncrement, int renderIncrement) {}
    public record BiomePixelResult(byte[] pixels, int cacheIncrement, int renderIncrement) {}

    /**
     * Calculates the scale factor for pixel rendering based on dimensions.
     */
    public static int calculateScaleFactor(int width, int height) {
        int maxDim = Math.max(width, height);
        int MAX_DIM = CommonConfig.maximum_map_dimension;
        if (maxDim <= MAX_DIM) return 1;
        int requiredScale = (int) Math.ceil((double) maxDim / MAX_DIM);
        return Integer.highestOneBit(requiredScale - 1) << 1;
    }

    /**
     * Processes raw chunk pixels directly into separate biome and chunk pixel arrays.
     * Biome pixels are generated for all chunks, chunk pixels only for renderable chunks.
     */
    public static int processChunkPixels(byte[] biomePixels, byte[] chunkPixels, ServerLevel level, Set<ChunkPos> allChunks, Set<ChunkPos> renderedChunks, int scaleFactor, int pixelWidth, int pixelHeight, ChunkPos minChunk, boolean isPseudo) {
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

        ViaRomana.LOGGER.debug("Starting parallel bake for {} total chunks, {} renderable (isPseudo: {})", totalChunks, renderedChunks.size(), isPseudo);

        targetChunkSet.parallelStream().forEach(chunkToProcess -> {
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
                ChunkPixelResult result = getOrRenderChunkPixels(level, chunkToProcess);
                if (result.pixels() != null && result.pixels().length == 256) {
                    chunkPixelData = result.pixels();
                    chunksFromCache.addAndGet(result.cacheIncrement());
                    chunksRendered.addAndGet(result.renderIncrement());
                }
            }

            if (isPseudo) return;

            int baseX = (chunkToProcess.x - minChunk.x) * scaledChunkSize;
            int baseZ = (chunkToProcess.z - minChunk.z) * scaledChunkSize;
            if (baseX < 0 || baseZ < 0 || baseX + scaledChunkSize > pixelWidth || baseZ + scaledChunkSize > pixelHeight) {
                ViaRomana.LOGGER.warn("Chunk {} out of pixel bounds, skipping", chunkToProcess);
                return;
            }

            if (true) {
                byte[] scaledBiomePixels = (scaleFactor > 1) ? ChunkPixelRenderer.scalePixels(biomeChunkPixels, scaleFactor) : biomeChunkPixels;
                copyChunkToFull(scaledBiomePixels, chunkToProcess, minChunk, scaledChunkSize, pixelWidth, biomePixels);
            }

            if (chunkPixelData != null) {
                byte[] scaledChunkPixels = (scaleFactor > 1) ? ChunkPixelRenderer.scalePixels(chunkPixelData, scaleFactor) : chunkPixelData;
                copyChunkToFull(scaledChunkPixels, chunkToProcess, minChunk, scaledChunkSize, pixelWidth, chunkPixels);
            }

            chunksWithData.incrementAndGet();
        });

        ViaRomana.LOGGER.debug("Render attempt complete: {}/{} chunks processed. High-res: {} cached, {} rendered. Biome: {} cached, {} rendered.",
                chunksWithData.get(), totalChunks, chunksFromCache.get(), chunksRendered.get(), chunksFromBiomeCache.get(), chunksRenderedBiome.get());

        int missingCount = totalChunks - chunksWithData.get();
        if (chunksWithData.get() == 0 && totalChunks > 0) {
            ViaRomana.LOGGER.warn("Map Bake: No chunk pixel data was available for the requested map area.");
        } else if (missingCount > 0) {
            ViaRomana.LOGGER.warn("Map Bake: {} chunks were allowed but had no pixel data.", missingCount);
        }

        return chunksWithData.get();
    }

    /**
     * Returns pixel result along with cache/render increment
     */
    private static ChunkPixelResult getOrRenderChunkPixels(ServerLevel level, ChunkPos chunkPos) {
        Optional<byte[]> cached = LevelDataManager.getPixelBytes(level, chunkPos);
        if (cached.isPresent()) {
            byte[] pixels = cached.get();
            if (pixels.length == 256) {
                return new ChunkPixelResult(pixels, 1, 0);
            }
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
     * Copy scaled chunk row-by-row to full array.
     */
    private static void copyChunkToFull(byte[] chunkPixels, ChunkPos chunkPos, ChunkPos min, int scaledChunkSize, int pixelWidth, byte[] fullPixels) {
        int baseX = (chunkPos.x - min.x) * scaledChunkSize;
        int baseZ = (chunkPos.z - min.z) * scaledChunkSize;
        for (int dz = 0; dz < scaledChunkSize; dz++) {
            int srcIdx = dz * scaledChunkSize;
            int dstIdx = (baseZ + dz) * pixelWidth + baseX;
            System.arraycopy(chunkPixels, srcIdx, fullPixels, dstIdx, scaledChunkSize);
        }
    }
}