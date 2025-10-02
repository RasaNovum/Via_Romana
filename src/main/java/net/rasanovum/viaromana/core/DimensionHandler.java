package net.rasanovum.viaromana.core;

import net.rasanovum.viaromana.client.HudMessageManager;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.variables.VariableAccess;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
// import net.minecraft.world.level.dimension.DimensionType;
// import net.minecraft.core.registries.Registries;
// import net.minecraft.resources.ResourceLocation;
// import net.minecraft.tags.TagKey;
// import net.minecraft.server.level.ServerLevel;

import java.util.List;

public class DimensionHandler {
	public static void preventHopping(LevelAccessor world, Entity entity) {
		if (entity == null) return;

		if (VariableAccess.playerVariables.isChartingPath(entity)) {
			HudMessageManager.queueMessage("message.via_romana.cancel_path_dimension");

			ResetVariables.execute(world, entity);
		}
	}

	public static boolean isValid(LevelAccessor world, Entity entity) {
		if (entity == null) return false;

		// if (world instanceof ServerLevel serverLevel) {
		// 	TagKey<DimensionType> CHARTABLE_DIMENSIONS = TagKey.create(Registries.DIMENSION_TYPE, VersionUtils.getLocation("via_romana:chartable"));
		// 	return serverLevel.dimensionTypeRegistration().is(CHARTABLE_DIMENSIONS);
		// }
		
		List<String> invalidDimensions = CommonConfig.invalid_dimensions;
		
		for (String invalidDimension : invalidDimensions) {
			if (entity.level().dimension().toString().contains(invalidDimension)) {
				HudMessageManager.queueMessage("message.via_romana.invalid_dimension");
				return false;
			}
		}

		return true;
	}
}
