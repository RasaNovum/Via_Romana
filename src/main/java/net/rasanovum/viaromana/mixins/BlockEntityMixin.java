package net.rasanovum.viaromana.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.rasanovum.viaromana.storage.path.legacy.ICustomDataHolder;
import net.minecraft.nbt.CompoundTag;
//? if >1.21
import net.minecraft.core.HolderLookup;

@Mixin(BlockEntity.class)
public class BlockEntityMixin implements ICustomDataHolder {
	@Unique
	private CompoundTag fabricData = new CompoundTag();

	@Inject(method = "saveAdditional", at = @At("RETURN"), remap = false)
	//? if <1.21 {
	/*private void onSaveAdditional(CompoundTag nbt, CallbackInfo ci) {
	*///?} else {
	private void onSaveAdditional(CompoundTag nbt, HolderLookup.Provider registries, CallbackInfo ci) {
	//?}
		if (!fabricData.isEmpty()) {
			nbt.put("FabricData", fabricData);
		}
	}

	//? if <1.21 {
	/*@Inject(method = "load", at = @At("RETURN"), remap = false)
	private void onLoad(CompoundTag nbt, CallbackInfo ci) {
	*///?} else {
	@Inject(method = "loadAdditional", at = @At("RETURN"), remap = false)
	private void onLoad(CompoundTag nbt, HolderLookup.Provider registries, CallbackInfo ci) {
	//?}
		if (nbt.contains("FabricData")) {
			fabricData = nbt.getCompound("FabricData");
		}
	}

	@Inject(method = "saveWithoutMetadata", at = @At("RETURN"), cancellable = true, remap = false)
	//? if <1.21 {
	/*private void onSaveWithoutMetadata(CallbackInfoReturnable<CompoundTag> cir) {
	*///?} else {
	private void onSaveWithoutMetadata(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> cir) {
	//?}
		CompoundTag nbt = cir.getReturnValue();
		if (!fabricData.isEmpty()) {
			nbt.put("FabricData", fabricData);
		}
		cir.setReturnValue(nbt);
	}

	@Override
	public CompoundTag getCustomData() {
		return fabricData;
	}

	@Override
	public void setCustomData(CompoundTag data) {
		this.fabricData = data;
	}
}
