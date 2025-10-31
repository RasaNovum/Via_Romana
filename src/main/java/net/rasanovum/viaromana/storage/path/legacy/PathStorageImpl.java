package net.rasanovum.viaromana.storage.path.legacy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.path.*;

/**
 * Read-only implementation for accessing legacy path storage.
 */
public final class PathStorageImpl extends SavedData implements IPathStorage {
    private final PathGraph graph = new PathGraph();

    public PathStorageImpl() {
        super();
    }

    @Override 
    public PathGraph graph() { 
        return graph; 
    }

    //? if <1.21 {
    /*public static PathStorageImpl load(CompoundTag nbt) {
        PathStorageImpl storage = new PathStorageImpl();
        storage.graph.deserialize(nbt);
        return storage;
    }

    @Override 
    public CompoundTag save(CompoundTag nbt) { 
        return nbt;
    }

    public static IPathStorage get(Level level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(PathStorageImpl::load, PathStorageImpl::new, "viaromana_paths");
    }
    *///?} else {
    public PathStorageImpl(CompoundTag nbt, HolderLookup.Provider provider) {
        this();
        graph.deserialize(nbt);
    }

    @Override 
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) { 
        return new CompoundTag();
    }

    public static IPathStorage get(Level level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                PathStorageImpl::new,
                (nbt, provider) -> new PathStorageImpl(nbt, provider),
                null
            ),
            "viaromana_paths"
        );
    }
     //?}
}
