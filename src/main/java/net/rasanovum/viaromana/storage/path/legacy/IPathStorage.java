package net.rasanovum.viaromana.storage.path.legacy;

import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.path.PathGraph;

/**
 * Read-only interface for accessing legacy path storage.
 */
public interface IPathStorage {
    PathGraph graph();

    static IPathStorage get(Level level) {
        return PathStorageImpl.get(level);
    }
}