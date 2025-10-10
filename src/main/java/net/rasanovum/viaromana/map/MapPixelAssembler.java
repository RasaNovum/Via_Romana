package net.rasanovum.viaromana.map;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.rasanovum.viaromana.ViaRomana;

import java.util.Optional;
import java.util.Set;

import static net.rasanovum.viaromana.storage.level.LevelDataManager.getPixelBytes;
import static net.rasanovum.viaromana.storage.level.LevelDataManager.setPixelBytes;

/**
 * Assembles pixel data from individual chunks into full map arrays.
 */
public class MapPixelAssembler {
    public record PixelResult(byte[] pixels, int cacheIncrement, int renderIncrement) {}

    /**
     * Calculates the scale factor for pixel rendering based on dimensions.
     */
    public static int calculateScaleFactor(int width, int height) {
        int maxDim = Math.max(width, height);
        int MAX_DIM = net.rasanovum.viaromana.CommonConfig.maximum_map_dimension;
        if (maxDim <= MAX_DIM) return 1;
        int requiredScale = (int) Math.ceil((double) maxDim / MAX_DIM);
        return Integer.highestOneBit(requiredScale - 1) << 1;
    }

    /**
     * Processes raw chunk pixels directly into separate biome and chunk pixel arrays.
     * Biome pixels are generated for all chunks, chunk pixels only for renderable chunks.
     */
    public static int processChunkPixels(byte[] biomePixels, byte[] chunkPixels, ServerLevel level, Set<ChunkPos> allChunks, Set<ChunkPos> renderedChunks, int scaleFactor, int pixelWidth, int pixelHeight, ChunkPos minChunk) {
        int chunksWithData = 0;
        int chunksFromCache = 0;
        int chunksRendered = 0;
        int chunksFromBiome = 0;
        int scaledChunkSize = 16 / scaleFactor;

        int totalChunks = allChunks.size();

        ViaRomana.LOGGER.info("Total chunks={}, renderable={}", totalChunks, renderedChunks.size());

        for (ChunkPos chunkToProcess : allChunks) {
            byte[] biomeChunkPixels = ChunkPixelRenderer.getOrRenderBiomePixels(level, chunkToProcess);
            if (biomeChunkPixels.length != 256) {
                ViaRomana.LOGGER.warn("Failed to generate biome pixels for chunk {}", chunkToProcess);
                continue;
            }

            byte[] chunkPixelData = null;

            if (renderedChunks.contains(chunkToProcess)) {
                PixelResult result = getOrRenderChunkPixels(level, chunkToProcess);
                if (result.pixels() != null && result.pixels().length == 256) {
                    chunkPixelData = result.pixels();
                    chunksFromCache += result.cacheIncrement();
                    chunksRendered += result.renderIncrement();
                }
            }

            int baseX = (chunkToProcess.x - minChunk.x) * scaledChunkSize;
            int baseZ = (chunkToProcess.z - minChunk.z) * scaledChunkSize;
            if (baseX < 0 || baseZ < 0 || baseX + scaledChunkSize > pixelWidth || baseZ + scaledChunkSize > pixelHeight) {
                ViaRomana.LOGGER.warn("Chunk {} out of pixel bounds (baseX={}, baseZ={}, width={}, height={}), skipping",
                        chunkToProcess, baseX, baseZ, pixelWidth, pixelHeight);
                continue;
            }

            // Scale biome pixels
            byte[] scaledBiomePixels = biomeChunkPixels;
            if (scaleFactor > 1) {
                scaledBiomePixels = ChunkPixelRenderer.scalePixels(biomeChunkPixels, scaleFactor);
            }
            copyChunkToFull(scaledBiomePixels, chunkToProcess, minChunk, scaledChunkSize, pixelWidth, biomePixels);

            // Scale and copy chunk pixels if available
            if (chunkPixelData != null) {
                byte[] scaledChunkPixels = chunkPixelData;
                if (scaleFactor > 1) {
                    scaledChunkPixels = ChunkPixelRenderer.scalePixels(chunkPixelData, scaleFactor);
                }
                copyChunkToFull(scaledChunkPixels, chunkToProcess, minChunk, scaledChunkSize, pixelWidth, chunkPixels);
            }

            chunksWithData++;
        }

        ViaRomana.LOGGER.info("Render attempt complete: {}/{} chunks had pixel data ({} cached chunkPixels, {} cached biomePixels, {} freshly rendered)",
                chunksWithData, totalChunks, chunksFromCache, chunksFromBiome, chunksRendered);

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
    private static PixelResult getOrRenderChunkPixels(ServerLevel level, ChunkPos chunkPos) {
        Optional<byte[]> cached = getPixelBytes(level, chunkPos);
        if (cached.isPresent()) {
            byte[] pixels = cached.get();
            if (pixels.length == 256) {
                return new PixelResult(pixels, 1, 0);
            }
        }

        level.getChunk(chunkPos.x, chunkPos.z); // Force-load chunk if no cache present

        byte[] pixels = ChunkPixelRenderer.renderChunkPixels(level, chunkPos);
        if (pixels.length == 256) {
            setPixelBytes(level, chunkPos, pixels);
            return new PixelResult(pixels, 0, 1);
        }

        ViaRomana.LOGGER.warn("Chunk {} failed to render (invalid size: {}), skipping", chunkPos, pixels.length);
        return new PixelResult(null, 0, 0);
    }

    /**
     * Copy scaled chunk row-by-row to full array
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