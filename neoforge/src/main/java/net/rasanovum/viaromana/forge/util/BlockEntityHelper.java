package net.rasanovum.viaromana.forge.util;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.rasanovum.viaromana.forge.capabilities.CustomDataCapability;

import java.util.Optional;

public class BlockEntityHelper {

    // Check if the block entity has custom data capability
    public static boolean hasCustomData(BlockEntity blockEntity) {
        if (blockEntity == null) return false;
        
        Optional<ICustomDataHolder> capability = blockEntity.getData(CustomDataCapability.CUSTOM_DATA_CAPABILITY);
        return capability.isPresent();
    }
    
    // Get the custom data holder from the block entity
    public static LazyOptional<ICustomDataHolder> getCustomDataHolder(BlockEntity blockEntity) {
        if (blockEntity == null) return LazyOptional.empty();
        
        return blockEntity.getCapability(CustomDataCapability.CUSTOM_DATA_CAPABILITY);
    }
}