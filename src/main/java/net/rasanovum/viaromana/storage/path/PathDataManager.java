package net.rasanovum.viaromana.storage.path;

import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import dev.corgitaco.dataanchor.data.TrackedDataContainer;
import dev.corgitaco.dataanchor.data.type.level.LevelTrackedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.init.DataInit;

import java.util.Optional;

/**
 * Utility class for accessing and managing path data via Data Anchor.
 * Provides level-specific PathGraph instances.
 */
public class PathDataManager {
    /**
     * Gets or creates the PathGraph for a specific dimension.
     */
    public static PathGraph getOrCreatePathGraph(ServerLevel level) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) {
            throw new IllegalStateException("Cannot get TrackedDataContainer for level: " + level.dimension().location());
        }

        container.dataAnchor$createTrackedData();

        Optional<LevelPathTrackedData> data = container.dataAnchor$getTrackedData(DataInit.PATH_GRAPH_KEY)
                .filter(d -> d instanceof LevelPathTrackedData);
        
        if (data.isEmpty()) {
            throw new IllegalStateException("Failed to create LevelPathTrackedData for level: " + level.dimension().location());
        }
        
        return data.get().getGraph();
    }

    /**
     * Marks the path data as dirty for a specific dimension, triggering a save.
     */
    public static void markDirty(ServerLevel level) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        container.dataAnchor$getTrackedData(DataInit.PATH_GRAPH_KEY)
                .filter(d -> d instanceof LevelPathTrackedData)
                .ifPresent(LevelPathTrackedData::markDirty);
    }

    /**
     * Clears all paths in a specific dimension.
     */
    public static void clearAllPaths(ServerLevel level) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        container.dataAnchor$getTrackedData(DataInit.PATH_GRAPH_KEY)
                .filter(d -> d instanceof LevelPathTrackedData)
                .ifPresent(LevelPathTrackedData::clearAll);
    }
}
