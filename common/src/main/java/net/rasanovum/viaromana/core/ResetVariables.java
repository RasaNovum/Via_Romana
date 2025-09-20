package net.rasanovum.viaromana.core;

import net.rasanovum.viaromana.variables.VariableAccess;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

public class ResetVariables {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;

		VariableAccess.playerVariables.setChartingPath(entity, false);
		VariableAccess.playerVariables.setPathData(entity, "");
		VariableAccess.playerVariables.setLastSignPosition(entity, "");
		VariableAccess.playerVariables.setLastNodeX(entity, 0);
		VariableAccess.playerVariables.setLastNodeY(entity, 0);
		VariableAccess.playerVariables.setLastNodeZ(entity, 0);
		VariableAccess.playerVariables.setAwaitingToast(entity, false);
		VariableAccess.playerVariables.syncAndSave(entity);
	}
}