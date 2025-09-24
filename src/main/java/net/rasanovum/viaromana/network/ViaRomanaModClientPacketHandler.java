package net.rasanovum.viaromana.network;

import net.rasanovum.viaromana.client.MapClient;
import net.rasanovum.viaromana.client.gui.ChartingScreen;
import net.rasanovum.viaromana.client.gui.TeleportMapScreen;

import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.rasanovum.viaromana.network.packets.*;

@Environment(EnvType.CLIENT)
public class ViaRomanaModClientPacketHandler {
	public static void registerS2CPackets() {
		ClientPlayNetworking.registerGlobalReceiver(ViaRomanaModVariables.PlayerVariablesSyncMessage.TYPE, ViaRomanaModClientPacketHandler::handlePlayerVariablesS2C);
		ClientPlayNetworking.registerGlobalReceiver(DestinationResponseS2C.TYPE, ViaRomanaModClientPacketHandler::handleDestinationResponseS2C);
		ClientPlayNetworking.registerGlobalReceiver(MapResponseS2C.TYPE, ViaRomanaModClientPacketHandler::handleMapResponseS2C);
		ClientPlayNetworking.registerGlobalReceiver(SignValidationS2C.TYPE, ViaRomanaModClientPacketHandler::handleSignValidationS2C);
		ClientPlayNetworking.registerGlobalReceiver(OpenChartingScreenS2C.TYPE, ViaRomanaModClientPacketHandler::handleOpenChartingScreenS2C);
		ClientPlayNetworking.registerGlobalReceiver(PathGraphSyncPacket.TYPE, ViaRomanaModClientPacketHandler::handlePathGraphSyncS2C);
		ClientPlayNetworking.registerGlobalReceiver(OpenWarpBlockScreenS2C.TYPE, ViaRomanaModClientPacketHandler::handleOpenWarpBlockScreenS2C);
	}

	private static void handlePlayerVariablesS2C(ViaRomanaModVariables.PlayerVariablesSyncMessage message, ClientPlayNetworking.Context context) {
		context.client().execute(() -> {
			ViaRomanaModVariables.PlayerVariablesSyncMessage.handleClient(message);
			if (context.client().screen instanceof net.rasanovum.viaromana.client.gui.ChartingScreen) {
				boolean isCharting = net.rasanovum.viaromana.network.ViaRomanaModVariables.getPlayerVariables(context.client().player).ChartingPath;
				if (!isCharting) {
					context.client().setScreen(null);
				} else {
					context.client().setScreen(new ChartingScreen(Component.translatable("gui.viaromana.charting_screen.title")));
				}
			}
		});
	}

	private static void handlePathGraphSyncS2C(PathGraphSyncPacket message, ClientPlayNetworking.Context context) {
		context.client().execute(() -> PathGraphSyncPacket.handleClient(message));
	}
	
	private static void handleMapResponseS2C(MapResponseS2C message, ClientPlayNetworking.Context context) {
		context.client().execute(() -> {
			MapClient.handleMapResponse(message);
		});
	}

	private static void handleDestinationResponseS2C(DestinationResponseS2C message, ClientPlayNetworking.Context context) {
		context.client().execute(() -> {
			TeleportMapScreen screen = new TeleportMapScreen(message);
        	Minecraft.getInstance().setScreen(screen);
		});
	}

	private static void handleOpenChartingScreenS2C(OpenChartingScreenS2C message, ClientPlayNetworking.Context context) {
		context.client().execute(() -> {
			Component title = Component.translatable("gui.viaromana.charting_screen.title");
			ChartingScreen screen = new ChartingScreen(title);
			context.client().setScreen(screen);
		});
	}

	private static void handleSignValidationS2C(SignValidationS2C message, ClientPlayNetworking.Context context) {
		context.client().execute(() -> {
			if (context.client().screen instanceof TeleportMapScreen teleportScreen) {
				teleportScreen.handleSignValidation(message.nodePos(), message.isValid());
			}
		});
	}

	public static void sendPlayerVariablesToServer(ViaRomanaModVariables.PlayerVariables data) {
		ViaRomanaModVariables.PlayerVariablesSyncMessage message = new ViaRomanaModVariables.PlayerVariablesSyncMessage(data);
		ClientPlayNetworking.send(message);
	}

	private static void handleOpenWarpBlockScreenS2C(OpenWarpBlockScreenS2C message, ClientPlayNetworking.Context context) {
		context.client().execute(() -> {
			net.rasanovum.viaromana.client.gui.WarpBlockScreen screen = new net.rasanovum.viaromana.client.gui.WarpBlockScreen(message.blockPos());
			context.client().setScreen(screen);
		});
	}
}