package net.rasanovum.viaromana.storage;

import net.minecraft.nbt.CompoundTag;

public interface ICustomDataHolder {
	CompoundTag getCustomData();

	void setCustomData(CompoundTag data);
}
