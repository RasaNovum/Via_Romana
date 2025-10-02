package net.rasanovum.viaromana.surveyor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.rasanovum.viaromana.ViaRomana;

//? if fabric {
/*import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
*///?} elif neoforge {
import net.rasanovum.viaromana.terrain.WorldTerrainSummary;
import net.rasanovum.viaromana.terrain.ChunkSummary;
import net.rasanovum.viaromana.terrain.LayerSummary;
import net.rasanovum.viaromana.terrain.SimpleBlockPalette;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
//?}

/**
 * Utility class for interacting with the terrain summary system (Surveyor on Fabric, custom on NeoForge).
 */
public class SurveyorUtil {

    //? if neoforge
    private static final ConcurrentHashMap<ServerLevel, WorldTerrainSummary> terrainCaches = new ConcurrentHashMap<>();

    /**
     * Gets terrain summary data for the given level.
     *
     * @param level The server level to get terrain data for
     * @return WorldTerrainSummary or null if unavailable
     */
    public static WorldTerrainSummary getTerrain(ServerLevel level) {
        try {
            //? if fabric {
            /*WorldTerrainSummary terrain = WorldSummary.of(level).terrain();
            *///?} elif neoforge {
            WorldTerrainSummary terrain = terrainCaches.computeIfAbsent(level, l -> new WorldTerrainSummary(l));
            //?}
            if (terrain == null) {
                ViaRomana.LOGGER.error("SurveyorUtil: Terrain data is null for level: " + level.dimension().location());
            }
            return terrain;
        } catch (Exception e) {
            ViaRomana.LOGGER.error("SurveyorUtil: Failed to get terrain: " + e.getMessage());
            return null;
        }
    }

    /**
     * Requests refresh of terrain summary for a specific chunk if it is already loaded.
     */
    public static void refreshChunkTerrain(ServerLevel level, ChunkPos pos) {
        try {
            WorldTerrainSummary terrain = getTerrain(level);
            if (terrain == null) return;

            LevelChunk chunk = level.getChunk(pos.x, pos.z);
            if (chunk != null) {
                //? if fabric {
                /*terrain.put(level, chunk);
                *///?} elif neoforge {
                ChunkSummary newSummary = computeChunkSummary(level, chunk, pos);
                terrain.put(pos, newSummary);
                //?}
            }
        } catch (Throwable t) {
            ViaRomana.LOGGER.debug("SurveyorUtil: Failed to refresh terrain for {} in {}: {}", pos, level.dimension().location(), t.toString());
        }
    }

    //? if neoforge {
    private static ChunkSummary computeChunkSummary(ServerLevel level, LevelChunk chunk, ChunkPos pos) {
        int maxY = level.getMaxBuildHeight();
        int minY = chunk.getMinBuildHeight();
        BitSet exists = new BitSet(256);
        int[] depths = new int[256];
        int[] blocks = new int[256];
        int[] waterDepths = new int[256];
        SimpleBlockPalette palette = new SimpleBlockPalette();
        palette.addOrGet(0); // Air

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int idx = lx * 16 + lz;
                // Find surface: first non-air from top
                int surfaceY = -1;
                for (int y = maxY - 1; y >= minY; y--) {
                    BlockState state = chunk.getBlockState(lx, y, lz);
                    if (!state.isAir()) {
                        surfaceY = y;
                        break;
                    }
                }
                if (surfaceY == -1) {
                    continue; // All air
                }

                exists.set(idx);
                depths[idx] = maxY - surfaceY;
                BlockState surfaceState = chunk.getBlockState(lx, surfaceY, lz);
                int globalBlockId = Block.getId(surfaceState.getBlock());
                blocks[idx] = palette.addOrGet(globalBlockId);

                // Water depth: count consecutive water blocks starting from surface +1 upward
                int waterDepth = 0;
                if (surfaceState.getFluidState().getFluid().is(FluidTags.WATER)) {
                    waterDepth = 1; // Surface is water
                }
                for (int wy = surfaceY + 1; wy < maxY; wy++) {
                    BlockState wState = chunk.getBlockState(lx, wy, lz);
                    if (wState.getFluidState().getFluid().is(FluidTags.WATER)) {
                        waterDepth++;
                    } else {
                        break;
                    }
                }
                waterDepths[idx] = waterDepth;
            }
        }

        LayerSummary.Raw raw = new LayerSummary.Raw(exists, depths, blocks, waterDepths);
        return new ChunkSummary(pos, raw, palette);
    }
    //?}
}