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

    public PathStorageImpl(CompoundTag nbt, HolderLookup.Provider provider) {
        this();
        graph.deserialize(nbt);
        setDirty();
    }
    
    @Override public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) { return graph.serialize(nbt); }
    @Override public PathGraph graph() { return graph; }
    @Override public void setDirty() { setDirty(true); }

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
}
