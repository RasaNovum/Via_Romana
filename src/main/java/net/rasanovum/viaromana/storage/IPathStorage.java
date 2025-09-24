package net.rasanovum.viaromana.storage;

import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.path.PathGraph;

public interface IPathStorage {
    PathGraph graph();
    void setDirty();

    static IPathStorage get(Level level) {
        return PathStorageImpl.get(level);
    }
}