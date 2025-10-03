package net.rasanovum.viaromana.map;

import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import dev.corgitaco.dataanchor.data.TrackedDataContainer;
import dev.corgitaco.dataanchor.data.type.chunk.ChunkTrackedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.FluidState;
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
import java.util.Map;
import java.util.HashMap;

/**
 * Utility for rendering/loading chunk PNGs and hooking to Data Anchor.
 */
public class ChunkPngUtil {

    /**
     * Renders 16x16 PNG bytes from chunk surface (adapt your MapBakeWorker logic).
     */
    public static byte[] renderChunkPngBytes(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = level.getChunk(pos.x, pos.z);
        if (chunk == null) return new byte[0];

        int maxY = level.getMaxBuildHeight();
        int minY = chunk.getMinBuildHeight();
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        int[] heights = new int[256];
        BitSet exists = new BitSet(256);
        int[] blocks = new int[256]; // Local IDs
        int[] waterDepths = new int[256];

        // Simple palette (expand to full if needed)
        Map<Integer, Block> globalToBlock = new HashMap<>();
        globalToBlock.put(0, Blocks.AIR);
        int paletteSize = 1;

        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int idx = lx * 16 + lz;
                BlockPos blockPos = new BlockPos(chunkMinX + lx, 0, chunkMinZ + lz);
                int surfaceY = -1;
                for (int y = maxY - 1; y >= minY; y--) {
                    blockPos = blockPos.atY(y);
                    BlockState state = chunk.getBlockState(blockPos);
                    if (!state.isAir()) {
                        surfaceY = y;
                        break;
                    }
                }
                if (surfaceY == -1) {
                    heights[idx] = Integer.MIN_VALUE;
                    img.setRGB(lx, lz, 0x00000000);
                    continue;
                }

                exists.set(idx);
                heights[idx] = surfaceY;
                blockPos = blockPos.atY(surfaceY);
                BlockState surfaceState = chunk.getBlockState(blockPos);
                int globalId = Block.getId(surfaceState.getBlock().defaultBlockState());
                int localId = getOrAddToPalette(globalId, globalToBlock, paletteSize);
                blocks[idx] = localId;

                // Water depth
                int waterDepth = 0;
                FluidState fluid = surfaceState.getFluidState();
                if (fluid.is(FluidTags.WATER)) waterDepth = 1;
                for (int wy = surfaceY + 1; wy < maxY && wy < surfaceY + 9; wy++) {
                    blockPos = blockPos.atY(wy);
                    BlockState wState = chunk.getBlockState(blockPos);
                    if (wState.getFluidState().is(FluidTags.WATER)) waterDepth++;
                    else break;
                }
                waterDepths[idx] = waterDepth;

                // Color (your getPixelColor logic)
                int color = getPixelColor(idx, heights, blocks, waterDepths, exists, globalToBlock);
                img.setRGB(lx, lz, color | 0xFF000000);
            }
        }

        // Encode to PNG bytes
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            ViaRomana.LOGGER.error("PNG encode failed for {}", pos, e);
            return new byte[0];
        }
    }

    // Your getPixelColor (adapted for map)
    private static int getPixelColor(int idx, int[] heights, int[] blocks, int[] waterDepths, BitSet exists, Map<Integer, Block> palette) {
        boolean hasWater = waterDepths[idx] > 0;
        if (!exists.get(idx) && !hasWater) return -1;

        MapColor mapColor;
        MapColor.Brightness brightness;

        if (hasWater) {
            mapColor = MapColor.WATER;
            brightness = calculateWaterBrightness(idx, waterDepths[idx]);
        } else {
            Block block = palette.get(blocks[idx]); // Local to block
            if (block == null || block == Blocks.AIR) return -1;
            mapColor = block.defaultMapColor();
            brightness = calculateTerrainBrightness(heights, idx);
        }

        int mcColor = mapColor.calculateRGBColor(brightness);
        return ((mcColor & 0xFF) << 16) | (mcColor & 0xFF00) | ((mcColor >> 16) & 0xFF);
    }

    // Your brightness calcs (copy from MapBakeWorker)
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

    // Simple palette add (stub; expand if >256 unique)
    private static int getOrAddToPalette(int globalId, Map<Integer, Block> palette, int currentSize) {
        if (globalId == 0) return 0; // Air
        if (!palette.containsKey(globalId)) {
            palette.put(globalId, BuiltInRegistries.BLOCK.byId(globalId));
        }
        // Stub local ID as global for POC (use dense mapping for full)
        return globalId;
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

        ViaRomana.LOGGER.debug("Set PNG for chunk {}", pos);
    }

    /**
     * Loads bytes to BufferedImage (for drawImage in bake).
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