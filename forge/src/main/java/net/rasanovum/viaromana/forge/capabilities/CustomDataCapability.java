package net.rasanovum.viaromana.forge.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.rasanovum.viaromana.forge.ViaRomanaMod;
import net.rasanovum.viaromana.forge.util.ICustomDataHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CustomDataCapability {
    
    // Capability for custom data
    public static final Capability<ICustomDataHolder> CUSTOM_DATA_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
    @SuppressWarnings("removal")
    public static final ResourceLocation CUSTOM_DATA_ID = new ResourceLocation(ViaRomanaMod.MODID, "custom_data");
    
    public static class CustomDataHolder implements ICustomDataHolder, INBTSerializable<CompoundTag> {
        private CompoundTag customData = new CompoundTag();
        
        @Override
        public CompoundTag getCustomData() {
            return customData;
        }
        
        @Override
        public void setCustomData(CompoundTag data) {
            this.customData = data;
        }
        
        @Override
        public CompoundTag serializeNBT() {
            return customData.copy();
        }
        
        @Override
        public void deserializeNBT(CompoundTag nbt) {
            this.customData = nbt;
        }
    }

    // This class is used to provide the capability to a BlockEntity
    public static class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final CustomDataHolder customData = new CustomDataHolder();
        private final LazyOptional<ICustomDataHolder> customDataOptional = LazyOptional.of(() -> customData);
        
        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return CUSTOM_DATA_CAPABILITY.orEmpty(cap, customDataOptional);
        }
        
        @Override
        public CompoundTag serializeNBT() {
            return customData.serializeNBT();
        }
        
        @Override
        public void deserializeNBT(CompoundTag nbt) {
            customData.deserializeNBT(nbt);
        }
    }
    
    // This method is called when the capability is attached to a BlockEntity
    public static void onAttachCapabilitiesToBlockEntity(AttachCapabilitiesEvent<BlockEntity> event) {
        // BlockEntity blockEntity = event.getObject();
        Provider provider = new Provider();
        event.addCapability(CUSTOM_DATA_ID, provider);
    }
}