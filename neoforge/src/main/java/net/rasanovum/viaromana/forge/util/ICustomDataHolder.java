package net.rasanovum.viaromana.forge.util;

import net.minecraft.nbt.CompoundTag;

public interface ICustomDataHolder {
    CompoundTag getCustomData();
    void setCustomData(CompoundTag data);
}
