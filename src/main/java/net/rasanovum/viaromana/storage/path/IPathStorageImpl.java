package net.rasanovum.viaromana.storage.path;

import net.minecraft.world.level.Level;

public class IPathStorageImpl {
    public static IPathStorage get(Level level) {
        return PathStorageImpl.get(level);
    }
}
