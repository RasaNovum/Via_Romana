package net.rasanovum.viaromana.storage.path;

import net.minecraft.nbt.CompoundTag;

public interface ICustomDataHolder {
	CompoundTag getCustomData();

	void setCustomData(CompoundTag data);
}
