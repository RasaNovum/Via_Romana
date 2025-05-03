package net.rasanovum.viaromana.forge.util;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.util.LazyOptional;
import net.rasanovum.viaromana.forge.capabilities.CustomDataCapability;

public class BlockTagHelper {
	public static void setDouble(LevelAccessor world, BlockPos pos, String tagName, double tagValue) {
		updateBlockEntity(world, pos, (customData) -> customData.putDouble(tagName, tagValue));
	}

	public static void setString(LevelAccessor world, BlockPos pos, String tagName, String tagValue) {
		updateBlockEntity(world, pos, (customData) -> customData.putString(tagName, tagValue));
	}

	public static void setBoolean(LevelAccessor world, BlockPos pos, String tagName, boolean tagValue) {
		updateBlockEntity(world, pos, (customData) -> customData.putBoolean(tagName, tagValue));
	}

	public static double getDouble(LevelAccessor world, BlockPos pos, String tagName) {
		return getValueFromBlockEntity(world, pos, (customData) -> customData.getDouble(tagName), 0.0);
	}

	public static String getString(LevelAccessor world, BlockPos pos, String tagName) {
		return getValueFromBlockEntity(world, pos, (customData) -> customData.getString(tagName), "");
	}

	public static boolean getBoolean(LevelAccessor world, BlockPos pos, String tagName) {
		return getValueFromBlockEntity(world, pos, (customData) -> customData.getBoolean(tagName), false);
	}

	// Generic method to update the block entity's custom data
	private static void updateBlockEntity(LevelAccessor world, BlockPos pos, java.util.function.Consumer<CompoundTag> updateFunction) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null) {
			LazyOptional<ICustomDataHolder> capability = blockEntity.getCapability(CustomDataCapability.CUSTOM_DATA_CAPABILITY);
			
			// Check if the capability is present and update the custom data
			capability.ifPresent(dataHolder -> {
				CompoundTag customData = dataHolder.getCustomData();
				updateFunction.accept(customData);
				dataHolder.setCustomData(customData);
				blockEntity.setChanged();

				BlockState state = world.getBlockState(pos);
				world.setBlock(pos, state, 3);
				if (world instanceof ServerLevel) {
					ServerLevel serverWorld = (ServerLevel) world;
					serverWorld.getChunkSource().blockChanged(pos);
					var chunk = serverWorld.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, true);
					if (chunk != null) {
						chunk.setUnsaved(true);
					}
				}
			});
		}
	}

	// Generic method to get a value from the block entity's custom data
	private static <T> T getValueFromBlockEntity(LevelAccessor world, BlockPos pos, java.util.function.Function<CompoundTag, T> getFunction, T defaultValue) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null) {
			LazyOptional<ICustomDataHolder> capability = blockEntity.getCapability(CustomDataCapability.CUSTOM_DATA_CAPABILITY);
			
			return capability.map(dataHolder -> {
				CompoundTag customData = dataHolder.getCustomData();
				return getFunction.apply(customData);
			}).orElse(defaultValue);
		}
		return defaultValue;
	}
}
