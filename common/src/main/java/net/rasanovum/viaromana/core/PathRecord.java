package net.rasanovum.viaromana.core;

import net.rasanovum.viaromana.variables.VariableAccess;
import net.rasanovum.viaromana.PlatformUtils;
import net.rasanovum.viaromana.util.PathUtils;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import java.util.List;

public class PathRecord {
	public static void start(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;

		VariableAccess.playerVariables.setPathData(entity, PathUtils.encodePathData(world, x, y, z, entity));
		ChartingHandler.updateLastNode(entity);
		VariableAccess.playerVariables.setChartingPath(entity, true);
		VariableAccess.playerVariables.syncAndSave(entity);
	}

	public static void end(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;

		List<Object> pathData = PathUtils.decodePathData(VariableAccess.playerVariables.getPathData(entity));
		
		if (!isValidPathData(pathData, entity)) {
			handleError(entity, "Error: Invalid path data format during record end.");
			return;
		}

		double startX = (Double) pathData.get(3);
		double startY = (Double) pathData.get(4);
		double startZ = (Double) pathData.get(5);

		String startSignNbtKey = getStartSignNbtKey(pathData, entity);
		if (startSignNbtKey == null) {
			return;
		}

		updateBlockEntity(world, x, y, z, SignCheck.getTarget(world, x, y, z, entity), VariableAccess.playerVariables.getPathData(entity));
		updateBlockEntity(world, startX, startY, startZ, startSignNbtKey, PathUtils.encodePathData(world, x, y, z, entity));

		ResetVariables.execute(world, entity);
	}

	private static boolean isValidPathData(List<Object> pathData, Entity entity) {
		try {
			pathData.get(3);
			pathData.get(4);
			pathData.get(5);
			return true;
		} catch (IndexOutOfBoundsException | ClassCastException e) {
			handleError(entity, "Error: Invalid path data format during record end.");
			return false;
		}
	}

	private static String getStartSignNbtKey(List<Object> pathData, Entity entity) {
		if (pathData.size() > 6 && pathData.get(6) instanceof String) {
			return (String) pathData.get(6);
		}
		handleError(entity, "Error: Missing start sign data during record end.");
		return null;
	}

	private static void updateBlockEntity(LevelAccessor world, double x, double y, double z, String key, String value) {
		if (!world.isClientSide()) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity != null) {
				PlatformUtils.setString(world, pos, key, value);
			}
		}
	}

	private static void handleError(Entity entity, String message) {
		if (entity instanceof Player player && !player.getLevel().isClientSide()) {
			player.displayClientMessage(Component.literal(message), true);
		}
		ResetVariables.execute(entity.getLevel(), entity);
	}
}
