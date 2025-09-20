package net.rasanovum.viaromana.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.path.*;

public final class PathStorageImpl extends SavedData implements IPathStorage {
    private final PathGraph graph = new PathGraph();

    public static PathStorageImpl load(CompoundTag nbt) {
        var d = new PathStorageImpl();
        d.graph.deserialize(nbt);
        return d;
    }
    
    @Override public CompoundTag save(CompoundTag nbt) { return graph.serialize(nbt); }
    @Override public PathGraph graph() { return graph; }
    @Override public void setDirty() { setDirty(true); }

    public static IPathStorage get(Level level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(PathStorageImpl::load, PathStorageImpl::new, "viaromana_paths");
    }
}
