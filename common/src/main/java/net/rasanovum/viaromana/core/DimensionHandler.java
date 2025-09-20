package net.rasanovum.viaromana.core;

import net.rasanovum.viaromana.variables.VariableAccess;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

public class DimensionHandler {
	public static void preventHopping(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		
		if (VariableAccess.playerVariables.isChartingPath(entity)) {
			if (entity instanceof Player player && !player.level().isClientSide()) {
				player.displayClientMessage(
					Component.literal(Component.translatable("cancel_path_dimension").getString()), 
					true
				);
			}

			ResetVariables.execute(world, entity);
		}
	}

	public static boolean isValid(LevelAccessor world, Entity entity) {
		if (entity == null)
			return false;

		for (int index0 = 0; index0 < (int) VariableAccess.mapVariables.getValidDimensionList(world).size(); index0++) {
			if (("" + entity.level().dimension()).contains(VariableAccess.mapVariables.getValidDimensionList(world).get((int) index0) instanceof String _s ? _s : "")) {
				return true;
			}
		}

		if (entity instanceof Player _player && !_player.level().isClientSide())
			_player.displayClientMessage(Component.literal((Component.translatable("invalid_dimension").getString())), true);
			
		return false;
	}
}
