package net.rasanovum.viaromana.util;

// import net.minecraft.world.level.block.state.BlockState;
// import net.rasanovum.viaromana.storage.path.ICustomDataHolder;
// import net.minecraft.world.level.block.entity.BlockEntity;
// import net.minecraft.world.level.LevelAccessor;
// import net.minecraft.server.level.ServerLevel;
// import net.minecraft.nbt.CompoundTag;
// import net.minecraft.core.BlockPos;

public class TagUtils {
	// Below methods work but are unused, so commented out for now.

	// public static void setString(LevelAccessor world, BlockPos pos, String tagName, String tagValue) {
	// 	updateBlockEntity(world, pos, (customData) -> customData.putString(tagName, tagValue));
	// }

	// public static String getString(LevelAccessor world, BlockPos pos, String tagName) {
	// 	String result = getValueFromBlockEntity(world, pos, (customData) -> customData.getString(tagName), "");
	// 	return result;
	// }

	// public static void removeString(LevelAccessor world, BlockPos pos, String tagName) {
	// 	updateBlockEntity(world, pos, (customData) -> customData.remove(tagName));
	// }

	// public static boolean hasString(LevelAccessor world, BlockPos pos, String tagName) {
	// 	return getValueFromBlockEntity(world, pos, (customData) -> customData.contains(tagName), false);
	// }

	// private static void updateBlockEntity(LevelAccessor world, BlockPos pos, java.util.function.Consumer<CompoundTag> updateFunction) {
	// 	BlockEntity blockEntity = world.getBlockEntity(pos);
	// 	if (blockEntity instanceof ICustomDataHolder) {
	// 		ICustomDataHolder customDataHolder = (ICustomDataHolder) blockEntity;
	// 		CompoundTag customData = customDataHolder.getCustomData();
	// 		updateFunction.accept(customData);
	// 		customDataHolder.setCustomData(customData);
	// 		blockEntity.setChanged();

	// 		BlockState state = world.getBlockState(pos);
	// 		world.setBlock(pos, state, 3);
	// 		if (world instanceof ServerLevel) {
	// 			ServerLevel serverWorld = (ServerLevel) world;
	// 			serverWorld.getChunkSource().blockChanged(pos);
	// 			serverWorld.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, true).setUnsaved(true);
	// 		}
	// 	}
	// }

	// private static <T> T getValueFromBlockEntity(LevelAccessor world, BlockPos pos, java.util.function.Function<CompoundTag, T> getFunction, T defaultValue) {
	// 	BlockEntity blockEntity = world.getBlockEntity(pos);
	// 	if (blockEntity instanceof ICustomDataHolder) {
	// 		ICustomDataHolder customDataHolder = (ICustomDataHolder) blockEntity;
	// 		CompoundTag customData = customDataHolder.getCustomData();
	// 		return getFunction.apply(customData);
	// 	}
	// 	return defaultValue;
	// }

	// public static void setDouble(LevelAccessor world, BlockPos pos, String tagName, double tagValue) {
	// 	updateBlockEntity(world, pos, (customData) -> customData.putDouble(tagName, tagValue));
	// }

	// public static void setBoolean(LevelAccessor world, BlockPos pos, String tagName, boolean tagValue) {
	// 	updateBlockEntity(world, pos, (customData) -> customData.putBoolean(tagName, tagValue));
	// }

	// public static double getDouble(LevelAccessor world, BlockPos pos, String tagName) {
	// 	return getValueFromBlockEntity(world, pos, (customData) -> customData.getDouble(tagName), 0.0);
	// }

	// public static boolean getBoolean(LevelAccessor world, BlockPos pos, String tagName) {
	// 	return getValueFromBlockEntity(world, pos, (customData) -> customData.getBoolean(tagName), false);
	// }

	// public static void setInt(LevelAccessor world, BlockPos pos, String tagName, int tagValue) {
	// 	updateBlockEntity(world, pos, (customData) -> customData.putInt(tagName, tagValue));
	// }

	// public static int getInt(LevelAccessor world, BlockPos pos, String tagName) {
	// 	return getValueFromBlockEntity(world, pos, (customData) -> customData.getInt(tagName), 0);
	// }
}
