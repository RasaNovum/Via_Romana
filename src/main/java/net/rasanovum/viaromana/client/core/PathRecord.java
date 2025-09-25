package net.rasanovum.viaromana.client.core;

import net.rasanovum.viaromana.variables.VariableAccess;
import net.rasanovum.viaromana.client.HudMessageManager;
// import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.core.DimensionHandler;
import net.rasanovum.viaromana.core.ResetVariables;
import net.rasanovum.viaromana.util.PathUtils;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public class PathRecord {
	public static void start(ClientLevel world, LocalPlayer player, BlockPos playerPos) {
		if (player == null) return;

		if (!DimensionHandler.isValid(world, player)) return;

		float infrastructureQuality = PathUtils.calculateInfrastructureQuality(world, player);

		// double qualityThreshold = CommonConfig.path_quality_threshold;

		// if (infrastructureQuality < qualityThreshold) {
		// 	cancel(world, player, false);
		// 	HudMessageManager.queueMessage("message.via_romana.low_quality_message");
		// 	return;
		// }

		// HudMessageManager.queueMessage("message.via_romana.start_charting");

		ChartingHandler.initializeChartingNodes(player);
		ChartingHandler.addChartingNode(world, player, playerPos, infrastructureQuality);
		VariableAccess.playerVariables.setChartingPath(player, true);
		VariableAccess.playerVariables.syncAndSave(player);
	}
	
	public static void end(ClientLevel world, LocalPlayer player, BlockPos playerPos) {
		if (player == null) return;

		ChartingHandler.finishPath(player);

		ChartingHandler.initializeChartingNodes(player);
		ResetVariables.execute(world, player);
	}

	public static void cancel(ClientLevel world, LocalPlayer player, boolean showMessage) {
		if (player == null) return;

		if (showMessage) HudMessageManager.queueMessage("message.via_romana.cancel_charting");
		
		ChartingHandler.initializeChartingNodes(player);
		ResetVariables.execute(world, player);
	}
}
