package net.rasanovum.viaromana.core;

import net.rasanovum.viaromana.variables.VariableAccess;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public class ResetVariables {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;

		VariableAccess.playerVariables.setChartingPath(entity, false);
		VariableAccess.playerVariables.setLastNodePos(entity, BlockPos.ZERO);
		VariableAccess.playerVariables.syncAndSave(entity);
	}
}