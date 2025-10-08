package net.rasanovum.viaromana.core;

import net.rasanovum.viaromana.client.HudMessageManager;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.variables.VariableAccess;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class DimensionHandler {
	/**
	 * Prevents dimension hopping while charting a path.
	 */
	public static void preventHopping(LevelAccessor world, Entity entity) {
		if (entity == null) return;

		if (VariableAccess.playerVariables.isChartingPath(entity)) {
			HudMessageManager.queueMessage("message.via_romana.cancel_path_dimension");

			ResetVariables.execute(world, entity);
		}
	}
	
	/**
	 * Syncs path data for the new dimension when a player changes dimensions.
	 */
	public static void syncPathDataOnDimensionChange(LevelAccessor world, Entity entity) {
		if (entity == null || !(entity instanceof ServerPlayer player)) return;
		
		net.rasanovum.viaromana.util.PathSyncUtils.syncPathGraphToPlayer(player);
	}

	/**
	 * Checks if the current dimension is valid for path charting.
	 */
	public static boolean isValid(LevelAccessor world, Entity entity) {
		if (entity == null) return false;
		
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
