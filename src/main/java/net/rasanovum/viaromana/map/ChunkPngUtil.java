package net.rasanovum.viaromana.map;

import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import dev.corgitaco.dataanchor.data.TrackedDataContainer;
import dev.corgitaco.dataanchor.data.type.chunk.ChunkTrackedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.tags.FluidTags;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.MapInit;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.Optional;
import java.util.Set;

/**
 * Utility for rendering/loading chunk PNGs and hooking to Data Anchor.
 */
public class ChunkPngUtil {
    /**
     * Renders 16x16 PNG bytes from chunk surface.
     */
    public static byte[] renderChunkPngBytes(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = level.getChunk(pos.x, pos.z);

        int minY = chunk.getMinBuildHeight();
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        int[] heights = new int[256];
        BitSet exists = new BitSet(256);
        BlockState[] blockStates = new BlockState[256];
        int[] waterDepths = new int[256];

        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int idx = lx * 16 + lz;
                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);

                if (surfaceY <= minY) {
                    heights[idx] = Integer.MIN_VALUE;
                    img.setRGB(lx, lz, 0x00000000);
                    continue;
                }

                exists.set(idx);
                heights[idx] = surfaceY;
                BlockPos blockPos = new BlockPos(chunkMinX + lx, surfaceY, chunkMinZ + lz);
                BlockState surfaceState = chunk.getBlockState(blockPos);
                blockStates[idx] = surfaceState;

                int waterDepth = 0;
                if (surfaceState.is(Blocks.WATER)) {
                    waterDepth = 1;
                    for (int wy = surfaceY - 1; wy >= minY && waterDepth < 8; wy--) {
                        BlockPos checkPos = new BlockPos(chunkMinX + lx, wy, chunkMinZ + lz);
                        BlockState wState = chunk.getBlockState(checkPos);
                        if (wState.getFluidState().is(FluidTags.WATER)) {
                            waterDepth++;
                        } else {
                            break;
                        }
                    }
                }
                waterDepths[idx] = waterDepth;

                int color = getPixelColor(level, blockPos, idx, heights, blockStates, waterDepths, exists);
                img.setRGB(lx, lz, color | 0xFF000000);
            }
        }

        // Encode to PNG bytes
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(img, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            ViaRomana.LOGGER.error("PNG encode failed for {}", pos, e);
            return new byte[0];
        }
    }

    private static int getPixelColor(ServerLevel level, BlockPos blockPos, int idx, int[] heights, BlockState[] blockStates, int[] waterDepths, BitSet exists) {
        boolean hasWater = waterDepths[idx] > 0;
        if (!exists.get(idx) && !hasWater) return -1;

        MapColor mapColor;
        MapColor.Brightness brightness;

        if (hasWater) {
            mapColor = MapColor.WATER;
            brightness = calculateWaterBrightness(idx, waterDepths[idx]);
        } else {
            BlockState state = blockStates[idx];
            if (state == null || state.isAir()) return -1;
            
            mapColor = state.getMapColor(level, blockPos);
            brightness = calculateTerrainBrightness(heights, idx);
        }

        int mcColor = mapColor.calculateRGBColor(brightness);

        return ((mcColor & 0xFF) << 16) | (mcColor & 0xFF00) | ((mcColor >> 16) & 0xFF);
    }

    private static MapColor.Brightness calculateWaterBrightness(int idx, int waterDepth) {
        double shade = Math.min(waterDepth / 8.0, 1.0) + (((idx >> 4) + (idx & 15)) & 1) * 0.15;
        return shade < 0.3 ? MapColor.Brightness.HIGH : shade > 0.7 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
    }

    private static MapColor.Brightness calculateTerrainBrightness(int[] heights, int idx) {
        int lx = idx >> 4;
        int lz = idx & 15;
        int currentHeight = heights[idx];
        int westHeight = (lx > 0) ? heights[idx - 16] : currentHeight;
        double shade = (currentHeight - westHeight) * 4.0 / 2.0 + ((((lz + lx) & 1) - 0.5) * 0.4);
        if (shade > 0.6) return MapColor.Brightness.HIGH;
        if (shade < -0.6) return MapColor.Brightness.LOW;
        return MapColor.Brightness.NORMAL;
    }


    /**
     * Gets PNG bytes from Data Anchor.
     */
    public static Optional<byte[]> getPngBytes(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = level.getChunk(pos.x, pos.z);

        TrackedDataContainer<ChunkAccess, ChunkTrackedData> container = TrackedDataRegistries.CHUNK.getContainer(chunk);
        if (container == null) return Optional.empty();

        return container.dataAnchor$getTrackedData(MapInit.CHUNK_PNG_KEY)
                .filter(data -> data instanceof ChunkPngTrackedData)
                .map(data -> data.getPngBytes().orElse(new byte[0]));
    }

    /**
     * Sets PNG bytes to Data Anchor.
     */
    public static void setPngBytes(ServerLevel level, ChunkPos pos, byte[] bytes) {
        LevelChunk chunk = level.getChunk(pos.x, pos.z);

        TrackedDataContainer<ChunkAccess, ChunkTrackedData> container = TrackedDataRegistries.CHUNK.getContainer(chunk);
        if (container == null) return;

        container.dataAnchor$createTrackedData();

        container.dataAnchor$getTrackedData(MapInit.CHUNK_PNG_KEY)
                .filter(data -> data instanceof ChunkPngTrackedData)
                .ifPresent(data -> data.setPngBytes(bytes));
    }

    /**
     * Clears PNG bytes from Data Anchor.
     */
    public static void clearPngBytes(ServerLevel level, ChunkPos pos) {
        setPngBytes(level, pos, new byte[0]);
    }

    /**
     * Clears PNG bytes for all chunks in the given set.
     */
    public static void clearPngBytesForChunks(ServerLevel level, Set<ChunkPos> chunks) {
        for (ChunkPos pos : chunks) {
            clearPngBytes(level, pos);
        }
        ViaRomana.LOGGER.info("Cleared PNG data for {} chunks", chunks.size());
    }

    /**
     * Regenerates PNG bytes for all chunks in the given set.
     */
    public static void regeneratePngBytesForChunks(ServerLevel level, Set<ChunkPos> chunks) {
        int regenerated = 0;
        for (ChunkPos pos : chunks) {
            byte[] newBytes = renderChunkPngBytes(level, pos);
            if (newBytes.length > 0) {
                setPngBytes(level, pos, newBytes);
                regenerated++;
            }
        }
        ViaRomana.LOGGER.info("Regenerated PNG data for {} chunks", regenerated);
    }

    /**
     * Loads bytes to BufferedImage.
     */
    public static BufferedImage loadPngFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            ViaRomana.LOGGER.error("PNG load failed", e);
            return null;
        }
    }
}