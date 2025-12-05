package net.rasanovum.viaromana.map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.biome.Climate;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.storage.level.LevelDataManager;
import net.rasanovum.viaromana.util.VersionUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Utility for rendering/loading chunk pixels.
 */
public class ChunkPixelRenderer {
    private static final Map<ResourceLocation, Integer> biomeColorCache = new HashMap<>();
    private static Map<String, Integer> parsedBiomeColors = null;
    
    private static final MapColor WATER_MAP_COLOR = MapColor.WATER;
    
    private static TagKey<Biome> badlandsTag;
    private static TagKey<Biome> beachTag;
    private static TagKey<Biome> oceanTag;
    private static TagKey<Biome> deepOceanTag;
    private static TagKey<Biome> endTag;
    private static TagKey<Biome> forestTag;
    private static TagKey<Biome> hillTag;
    private static TagKey<Biome> jungleTag;
    private static TagKey<Biome> mountainTag;
    private static TagKey<Biome> netherTag;
    private static TagKey<Biome> riverTag;
    private static TagKey<Biome> savannaTag;
    private static TagKey<Biome> taigaTag;
    private static TagKey<Biome> desertTag;
    private static TagKey<Biome> swampTag;
    private static TagKey<Biome> plainsTag;
    
    /**
     * Initialize static data structures.
     */
    public static void init() {
        biomeColorCache.clear();

        parsedBiomeColors = new HashMap<>();
        for (String biomePair : CommonConfig.biome_color_pairs) {
            if (biomePair.trim().isEmpty()) continue;
            try {
                String[] parts = biomePair.split("=", 2);
                if (parts.length != 2) continue;
                String biome = parts[0].trim();
                int color = Integer.parseInt(parts[1].trim());
                parsedBiomeColors.put(biome, color);
            } catch (Exception e) {
                ViaRomana.LOGGER.warn("Skipping invalid biome color pair '{}': {}", biomePair, e.getMessage());
            }
        }
        ViaRomana.LOGGER.info("Loaded {} biome-color pairs from config.", parsedBiomeColors.size());
        
        badlandsTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_badlands"));
        beachTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_beach"));
        oceanTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_ocean"));
        deepOceanTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_deep_ocean"));
        endTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_end"));
        forestTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_forest"));
        hillTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_hill"));
        jungleTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_jungle"));
        mountainTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_mountain"));
        netherTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_nether"));
        riverTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_river"));
        savannaTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_savanna"));
        taigaTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_taiga"));
        desertTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:is_desert"));
        swampTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:allows_surface_slime_spawns"));
        plainsTag = TagKey.create(Registries.BIOME, VersionUtils.getLocation("minecraft:has_structure/village_plains"));
    }

    /**
     * Renders 16x16 raw pixel bytes from chunk surface.
     */
    public static byte[] renderChunkPixels(ServerLevel level, ChunkPos pos) {
        long startTime = System.nanoTime();
        LevelChunk chunk = level.getChunk(pos.x, pos.z);

        int minY = chunk.getMinBuildHeight();
        byte[] pixels = new byte[256];

        int chunkMinX = pos.getMinBlockX();
        int chunkMinZ = pos.getMinBlockZ();

        int[] surfaceHeights = new int[256];
        int[] floorHeights = new int[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int idx = x + z * 16;
                surfaceHeights[idx] = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                floorHeights[idx] = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
            }
        }

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int idx = x + z * 16;
                int surfaceY = surfaceHeights[idx];

                if (surfaceY <= minY) {
                    pixels[idx] = 0;
                    continue;
                }

                int waterDepth = surfaceY - floorHeights[idx];

                BlockState state;
                MapColor mapColor;
                MapColor.Brightness brightness;

                if (waterDepth > 0) {
                    mapColor = WATER_MAP_COLOR;
                    brightness = calculateWaterBrightness(idx, waterDepth);
                } else {
                    mutablePos.set(chunkMinX + x, surfaceY, chunkMinZ + z);
                    state = chunk.getBlockState(mutablePos);
                    mapColor = state.getMapColor(level, mutablePos);

                    while (mapColor == MapColor.NONE && mutablePos.getY() > minY) {
                        mutablePos.move(0, -1, 0);
                        state = chunk.getBlockState(mutablePos);
                        mapColor = state.getMapColor(level, mutablePos);
                    }

//                    ViaRomana.LOGGER.info("Colour ID {} for block {} at {}", mapColor.id, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), mutablePos);

                    brightness = calculateTerrainBrightness(surfaceHeights, idx);
                }

                pixels[idx] = mapColor.getPackedId(brightness);
            }
        }

        long totalTime = System.nanoTime() - startTime;
//        ViaRomana.LOGGER.info("[PERF] Chunk {} render (raw pixels): total={}ms, size=256B", pos, totalTime / 1_000_000.0);

        return pixels;
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
        int z_row = idx >> 4;
        int x_col = idx & 15;
        int currentHeight = heights[idx];
        int westHeight = (x_col > 0) ? heights[idx - 1] : currentHeight;

        double shade = (currentHeight - westHeight) * 4.0 / 2.0 + ((((x_col + z_row) & 1) - 0.5) * 0.4);

        if (shade > 0.6) return MapColor.Brightness.HIGH;
        if (shade < -0.6) return MapColor.Brightness.LOW;

        return MapColor.Brightness.NORMAL;
    }

    /**
     * Generates or retrieves from cache a 16x16 byte array representing the
     * low-resolution biome map of a chunk.
     *
     * @param level          The server level.
     * @param biomeChunk     The position of the chunk to process.
     * @param biomeSource    The world's BiomeSource, from {@code level.getChunkSource().getGenerator().getBiomeSource()}.
     * @param climateSampler The world's Climate.Sampler, from {@code level.getChunkSource().randomState().sampler()}.
     * @param maxBuildHeight The maximum build height for this world.
     * @return A BiomePixelResult containing the pixel data and cache/render status.
     */
    public static MapPixelAssembler.BiomePixelResult getOrRenderBiomePixels(ServerLevel level, ChunkPos biomeChunk, BiomeSource biomeSource, Climate.Sampler climateSampler, int maxBuildHeight) {
        byte @Nullable [] cachedCorners = LevelDataManager.getCornerBytes(level, biomeChunk);
        int[] cornerPackedIds = new int[4];
        int cacheIncrement = 0;
        int renderIncrement = 0;

        if (cachedCorners != null) {
            for (int i = 0; i < 4; i++) {
                cornerPackedIds[i] = cachedCorners[i] & 0xFF;
            }
            cacheIncrement = 1;
        } else {
            int[][] corners = {{0, 0}, {3, 0}, {0, 3}, {3, 3}};
            Holder<Biome>[] cornerBiomes = new Holder[4];

            for (int c = 0; c < 4; c++) {
                int blockX = biomeChunk.getMinBlockX() + corners[c][0] * 4;
                int blockZ = biomeChunk.getMinBlockZ() + corners[c][1] * 4;

                int quartX = blockX >> 2;
                int quartY = maxBuildHeight >> 2;
                int quartZ = blockZ >> 2;

                cornerBiomes[c] = biomeSource.getNoiseBiome(quartX, quartY, quartZ, climateSampler);
            }

            byte[] cornerBytes = new byte[4];

            for (int c = 0; c < 4; c++) {
                int colorIndex = getColorIndex(level, cornerBiomes[c]);
                int brightness = (colorIndex == 12) ? 0 : 1;
                cornerPackedIds[c] = colorIndex * 4 + brightness;
                cornerBytes[c] = (byte) cornerPackedIds[c];
            }

            LevelDataManager.setCornerBytes(level, biomeChunk, cornerBytes);
            renderIncrement = 1;
        }

        byte[] pixels = new byte[256];

        for (int i = 0; i < 256; i++) {
            int px = i % 16;
            int pz = i / 16;
            int cornerIndex = ((pz >> 3) << 1) | (px >> 3);
            pixels[i] = (byte) cornerPackedIds[cornerIndex];
        }

        return new MapPixelAssembler.BiomePixelResult(pixels, cacheIncrement, renderIncrement);
    }

    /**
     * Helper to get color index using your existing logic.
     */
    private static int getColorIndex(ServerLevel level, Holder<Biome> holder) {
        Biome biome = holder.value();
        ResourceLocation biomeId = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome);
        assert biomeId != null;

        if (biomeColorCache.containsKey(biomeId)) {
            return biomeColorCache.get(biomeId);
        }

        int colorIndex = 0;
        
        if (parsedBiomeColors != null && parsedBiomeColors.containsKey(biomeId.toString())) {
            colorIndex = parsedBiomeColors.get(biomeId.toString());
            biomeColorCache.put(biomeId, colorIndex);
            return colorIndex;
        }

        String biomePath = biomeId.getPath().toLowerCase();

        if (biomePath.contains("snow") || biomePath.contains("ice") || biomePath.contains("tundra")) {
            colorIndex = 8;
        } else if (holder.is(forestTag) || holder.is(taigaTag) || holder.is(jungleTag) || holder.is(swampTag) || biomePath.contains("forest")) {
            colorIndex = 7;
        } else if (holder.is(deepOceanTag) || holder.is(oceanTag) || holder.is(riverTag) || biomePath.contains("ocean")) {
            colorIndex = 12;
        } else if (holder.is(mountainTag) || holder.is(hillTag) || biomePath.contains("mountain") || biomePath.contains("hill") || biomePath.contains("stony_shore")) {
            colorIndex = 11;
        } else if (holder.is(plainsTag) || biomePath.contains("plains") || biomePath.contains("meadow")) {
            colorIndex = 1;
        } else if (holder.is(beachTag) || holder.is(endTag) || holder.is(desertTag) || biomePath.contains("desert")) {
            colorIndex = 2;
        } else if (holder.is(savannaTag) || biomePath.contains("savanna")) {
            colorIndex = 27;
        } else if (holder.is(badlandsTag) || biomePath.contains("badlands")) {
            colorIndex = 15;
        } else if (holder.is(netherTag)) {
            colorIndex = 35;
        } else if (biomePath.contains("mushroom_fields")) {
            colorIndex = 24;
        } else {
            ViaRomana.LOGGER.warn("Biome {} did not match any color index rules, defaulting to 0", biomeId);
        }

        biomeColorCache.put(biomeId, colorIndex);

        return colorIndex;
    }
}