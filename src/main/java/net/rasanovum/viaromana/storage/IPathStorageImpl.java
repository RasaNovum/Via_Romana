package net.rasanovum.viaromana.storage;

import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.storage.IPathStorageImpl;

public class IPathStorageImpl {
    public static IPathStorage get(Level level) {
        return PathStorageImpl.get(level);
    }
}
