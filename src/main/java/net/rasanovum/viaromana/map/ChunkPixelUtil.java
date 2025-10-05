package net.rasanovum.viaromana.map;

import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import dev.corgitaco.dataanchor.data.TrackedDataContainer;
import dev.corgitaco.dataanchor.data.type.level.LevelTrackedData;
import jdk.jfr.Category;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.tags.FluidTags;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.MapInit;
import net.rasanovum.viaromana.util.VersionUtils;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Utility for rendering/loading chunk pixels.
 */
public class ChunkPixelUtil {
    
    /**
     * Renders 16x16 raw pixel bytes from chunk surface.
     */
    public static byte[] renderChunkPixels(ServerLevel level, ChunkPos pos) {
        long startTime = System.nanoTime();
        LevelChunk chunk = level.getChunk(pos.x, pos.z);

        int minY = chunk.getMinBuildHeight();
        byte[] pixels = new byte[256]; // 16x16 flat array

        int chunkMinX = pos.getMinBlockX();
        int chunkMinZ = pos.getMinBlockZ();

        // Pre-calculate heights for brightness calculation
        int[] heights = new int[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int idx = x + z * 16;
                int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                heights[idx] = surfaceY;
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int idx = x + z * 16;
                int surfaceY = heights[idx];
                
                if (surfaceY <= minY) {
                    pixels[idx] = 0;
                    continue;
                }

                BlockPos posBlock = new BlockPos(chunkMinX + x, surfaceY, chunkMinZ + z);
                BlockState state = chunk.getBlockState(posBlock);
                
                // Calculate water depth
                int waterDepth = 0;
                if (state.is(Blocks.WATER)) {
                    waterDepth = 1;
                    for (int wy = surfaceY - 1; wy >= minY && waterDepth < 8; wy--) {
                        BlockPos checkPos = new BlockPos(chunkMinX + x, wy, chunkMinZ + z);
                        if (chunk.getBlockState(checkPos).getFluidState().is(FluidTags.WATER)) {
                            waterDepth++;
                        } else {
                            break;
                        }
                    }
                }

                MapColor mapColor;
                MapColor.Brightness brightness;
                
                if (waterDepth > 0) {
                    mapColor = MapColor.WATER;
                    brightness = calculateWaterBrightness(idx, waterDepth);
                } else {
                    mapColor = state.getMapColor(level, posBlock);
                    if (mapColor == MapColor.NONE) {
                        pixels[idx] = 0;
                        continue;
                    }
                    brightness = calculateTerrainBrightness(heights, idx);
                }
                
                pixels[idx] = mapColor.getPackedId(brightness);
            }
        }

        long totalTime = System.nanoTime() - startTime;
        ViaRomana.LOGGER.debug("[PERF] Chunk {} render (raw pixels): total={}ms, size=256B", pos, totalTime / 1_000_000.0);
        
        return pixels;
    }

    /**
     * Converts raw pixel bytes to ARGB int array for BufferedImage creation.
     */
    public static int[] pixelsToArgb(byte[] pixels) {
        if (pixels == null || pixels.length != 256) {
            return new int[256];
        }
        
        int[] argb = new int[256];
        for (int i = 0; i < 256; i++) {
            int packedId = pixels[i] & 0xFF;
            if (packedId == 0) {
                argb[i] = 0x00000000;
            } else {
                argb[i] = MapColor.getColorFromPackedId(packedId);
            }
        }
        return argb;
    }

    /**
     * Scales a 16x16 chunk pixel array down by a scale factor using nearest-neighbor sampling.
     */
    public static byte[] scalePixels(byte[] pixels, int scaleFactor) {
        if (scaleFactor == 1) return pixels;
        
        int newSize = 16 / scaleFactor;
        byte[] scaled = new byte[newSize * newSize];
        
        for (int dx = 0; dx < newSize; dx++) {
            for (int dz = 0; dz < newSize; dz++) {
                int srcX = dx * scaleFactor;
                int srcZ = dz * scaleFactor;
                int srcIdx = srcX + srcZ * 16;
                scaled[dx + dz * newSize] = pixels[srcIdx];
            }
        }
        
        return scaled;
    }

    /**
     * Calculates the brightness of the water.
     */
    private static MapColor.Brightness calculateWaterBrightness(int idx, int waterDepth) {
        double shade = Math.min(waterDepth / 8.0, 1.0) + (((idx >> 4) + (idx & 15)) & 1) * 0.15;
        shade = Math.max(0.1, Math.min(0.9, shade));
        
        if (shade < 0.3) return MapColor.Brightness.HIGH;
        if (shade > 0.7) return MapColor.Brightness.LOW;
        return MapColor.Brightness.NORMAL;
    }

    /**
     * Calculates the brightness of the terrain.
     */
    private static MapColor.Brightness calculateTerrainBrightness(int[] heights, int idx) {
        int x = idx >> 4;
        int z = idx & 15;
        int currentHeight = heights[idx];
        int westHeight = (x > 0) ? heights[idx - 16] : currentHeight;

        double shade = (currentHeight - westHeight) * 4.0 / 2.0 + ((((z + x) & 1) - 0.5) * 0.4);
        
        if (shade > 0.6) return MapColor.Brightness.HIGH;
        if (shade < -0.6) return MapColor.Brightness.LOW;

        return MapColor.Brightness.NORMAL;
    }

    /**
     * Gets pixel bytes from Data Anchor.
     */
    public static Optional<byte[]> getPixelBytes(ServerLevel level, ChunkPos pos) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return Optional.empty();

        return container.dataAnchor$getTrackedData(MapInit.CHUNK_PIXEL_KEY)
                .filter(data -> data instanceof LevelPixelTrackedData)
                .flatMap(data -> data.getPixelBytes(pos));
    }

    /**
     * Sets pixel bytes to Data Anchor.
     */
    public static void setPixelBytes(ServerLevel level, ChunkPos pos, byte[] bytes) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        container.dataAnchor$createTrackedData();

        container.dataAnchor$getTrackedData(MapInit.CHUNK_PIXEL_KEY)
                .filter(data -> data instanceof LevelPixelTrackedData)
                .ifPresent(data -> data.setPixelBytes(pos, bytes));
    }

    /**
     * Clears pixel bytes from Data Anchor.
     */
    public static void clearPixelBytes(ServerLevel level, ChunkPos pos) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        container.dataAnchor$getTrackedData(MapInit.CHUNK_PIXEL_KEY)
                .filter(data -> data instanceof LevelPixelTrackedData)
                .ifPresent(data -> data.setPixelBytes(pos, null));
    }

    /**
     * Clears pixel bytes for all chunks in the given set.
     */
    public static void clearPixelBytesForChunks(ServerLevel level, Set<ChunkPos> chunks) {
        long startTime = System.nanoTime();
        for (ChunkPos pos : chunks) {
            clearPixelBytes(level, pos);
        }
        long totalTime = System.nanoTime() - startTime;
        ViaRomana.LOGGER.info("[PERF] Cleared pixel data for {} chunks in {}ms", chunks.size(), totalTime / 1_000_000.0);
    }

    /**
     * Regenerates pixel bytes for all chunks in the given set.
     */
    public static void regeneratePixelBytesForChunks(ServerLevel level, Set<ChunkPos> chunks) {
        long startTime = System.nanoTime();
        int regenerated = 0;
        
        for (ChunkPos pos : chunks) {
            byte[] newBytes = renderChunkPixels(level, pos);
            if (newBytes.length == 256) {
                setPixelBytes(level, pos, newBytes);
                regenerated++;
            }
        }
        
        long totalTime = System.nanoTime() - startTime;
        long totalBytes = regenerated * 256L;
        ViaRomana.LOGGER.info("[PERF] Regenerated pixel data for {} chunks in {}ms, total size={}KB (256B/chunk)", 
            regenerated, totalTime / 1_000_000.0, totalBytes / 1024.0);
    }

    public static byte[] generateBiomeFallbackPixels(ServerLevel level, Holder<Biome> biomeHolder) {
        Biome biome = biomeHolder.value();
        ResourceLocation biomeId = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome);
        assert biomeId != null;
        String biomePath = biomeId.getPath().toLowerCase();
        int shade = 2;

        int colorIndex = 0;

        TagKey<Biome> badlandsTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_badlands"));
        TagKey<Biome> beachTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_beach"));
        TagKey<Biome> deepOceanTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_deep_ocean"));
        TagKey<Biome> endTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_end"));
        TagKey<Biome> forestTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_forest"));
        TagKey<Biome> hillTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_hill"));
        TagKey<Biome> jungleTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_jungle"));
        TagKey<Biome> mountainTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_mountain"));
        TagKey<Biome> netherTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_nether"));
        TagKey<Biome> oceanTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_ocean"));
        TagKey<Biome> riverTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_river"));
        TagKey<Biome> savannaTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_savanna"));
        TagKey<Biome> taigaTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_taiga"));
        TagKey<Biome> plainsTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:has_structure/village_plains"));
        
        if (biomePath.contains("snow") || biomePath.contains("ice") || biomePath.contains("tundra")) {
            colorIndex = 8;
        } else if (biomeHolder.is(forestTag) || biomeHolder.is(taigaTag) || biomeHolder.is(jungleTag) || biomePath.contains("forest")) {
            colorIndex = 7;
        } else if (biomeHolder.is(deepOceanTag) || biomeHolder.is(oceanTag) || biomeHolder.is(riverTag) || biomePath.contains("ocean")) {
            colorIndex = 12;
            shade = 0;
        } else if (biomeHolder.is(mountainTag) || biomeHolder.is(hillTag) || biomePath.contains("mountain") || biomePath.contains("hill")) {
            colorIndex = 11;
        } else if (biomeHolder.is(plainsTag) || biomePath.contains("plains") || biomePath.contains("meadow")) {
            colorIndex = 1;
        } else if (biomeHolder.is(beachTag) || biomeHolder.is(endTag)) {
            colorIndex = 2;
        } else if (biomeHolder.is(savannaTag)) {
            colorIndex = 49;
        } else if (biomeHolder.is(badlandsTag)) {
            colorIndex = 15;
        } else if (biomeHolder.is(netherTag)) {
            colorIndex = 35;
        }

        // ViaRomana.LOGGER.info("Biome key: {}, rgb: 0x{}, base: {}", biomeId, Integer.toHexString(targetRgb), colorIndex);

        byte[] pixels = new byte[256];
        int baseByte = colorIndex * 4 + shade;
        Arrays.fill(pixels, (byte) baseByte);
        return pixels;
    }
}

