package net.rasanovum.viaromana.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.path.*;

public final class PathStorageImpl extends SavedData implements IPathStorage {
    private final PathGraph graph = new PathGraph();

    public PathStorageImpl() {
        super();
    }

    @Override public PathGraph graph() { return graph; }
    @Override public void setDirty() { setDirty(true); }

    //? if <1.21 {
    /*public static PathStorageImpl load(CompoundTag nbt) {
        PathStorageImpl storage = new PathStorageImpl();
        storage.graph.deserialize(nbt);
        storage.setDirty();
        return storage;
    }

    @Override public CompoundTag save(CompoundTag nbt) { return graph.serialize(nbt); }

    public static IPathStorage get(Level level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(PathStorageImpl::load, PathStorageImpl::new, "viaromana_paths");
    }
    *///?} else {
    public PathStorageImpl(CompoundTag nbt, HolderLookup.Provider provider) {
        this();
        graph.deserialize(nbt);
        setDirty();
    }

    @Override public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) { return graph.serialize(nbt); }

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
