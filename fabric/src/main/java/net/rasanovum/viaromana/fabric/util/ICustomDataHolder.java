package net.rasanovum.viaromana.fabric.util;

import net.minecraft.nbt.CompoundTag;

public interface ICustomDataHolder {
	CompoundTag getCustomData();

	void setCustomData(CompoundTag data);
}
