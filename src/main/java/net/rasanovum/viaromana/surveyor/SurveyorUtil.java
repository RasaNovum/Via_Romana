package net.rasanovum.viaromana.surveyor;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.rasanovum.viaromana.ViaRomana;

//? if fabric {
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
//?}

/**
 * Utility class for interacting with the Surveyor mod.
 */
public class SurveyorUtil {
    
    /**
     * Gets terrain summary data from Surveyor for the given level.
     * 
     * @param level The server level to get terrain data for
     * @return WorldTerrainSummary or null if unavailable
     */
    public static WorldTerrainSummary getTerrain(ServerLevel level) {
        try {
            WorldTerrainSummary terrain = WorldSummary.of(level).terrain();
            if (terrain == null) {
                ViaRomana.LOGGER.error("SurveyorUtil: Surveyor terrain data is null for level: " + level.dimension().location());
            }
            return terrain;
        } catch (Exception e) {
            ViaRomana.LOGGER.error("SurveyorUtil: Failed to get Surveyor terrain: " + e.getMessage());
            return null;
        }
    }

    /**
     * Requests Surveyor to refresh terrain summary for a specific chunk if it is already loaded.
     */
    public static void refreshChunkTerrain(ServerLevel level, ChunkPos pos) {
        try {
            WorldTerrainSummary terrain = getTerrain(level);
            if (terrain == null) return;

            ServerChunkCache chunkSource = level.getChunkSource();
            LevelChunk chunk = chunkSource.getChunkNow(pos.x, pos.z);
            if (chunk != null) {
                terrain.put(level, chunk);
            }
        } catch (Throwable t) {
            ViaRomana.LOGGER.debug("SurveyorUtil: Failed to refresh terrain for {} in {}: {}", pos, level.dimension().location(), t.toString());
        }
    }
}
