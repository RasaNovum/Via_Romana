package net.rasanovum.viaromana.network;

import net.rasanovum.viaromana.client.MapClient;
import net.rasanovum.viaromana.client.gui.ChartingScreen;
import net.rasanovum.viaromana.client.gui.TeleportMapScreen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class ViaRomanaModClientPacketHandler {
	public static void registerS2CPackets() {
		ClientPlayNetworking.registerGlobalReceiver(ViaRomanaModPacketHandler.PLAYER_VARIABLES_SYNC_S2C, ViaRomanaModClientPacketHandler::handlePlayerVariablesS2C);
		ClientPlayNetworking.registerGlobalReceiver(ViaRomanaModPacketHandler.DESTINATION_RESPONSE_S2C, ViaRomanaModClientPacketHandler::handleDestinationResponseS2C);
		ClientPlayNetworking.registerGlobalReceiver(ViaRomanaModPacketHandler.OPEN_LINK_SIGN_SCREEN_S2C, ViaRomanaModClientPacketHandler::handleOpenLinkSignScreenS2C);
		ClientPlayNetworking.registerGlobalReceiver(ViaRomanaModPacketHandler.OPEN_CHARTING_SCREEN_S2C, ViaRomanaModClientPacketHandler::handleOpenChartingScreenS2C);
		ClientPlayNetworking.registerGlobalReceiver(ViaRomanaModPacketHandler.PATH_GRAPH_SYNC_S2C, ViaRomanaModClientPacketHandler::handlePathGraphSyncS2C);
		ClientPlayNetworking.registerGlobalReceiver(ViaRomanaModPacketHandler.MAP_RESPONSE_S2C, ViaRomanaModClientPacketHandler::handleMapResponseS2C);
		ClientPlayNetworking.registerGlobalReceiver(ViaRomanaModPacketHandler.SIGN_VALIDATION_S2C, ViaRomanaModClientPacketHandler::handleSignValidationS2C);
		ClientPlayNetworking.registerGlobalReceiver(ViaRomanaModPacketHandler.OPEN_WARP_BLOCK_SCREEN_S2C, ViaRomanaModClientPacketHandler::handleOpenWarpBlockScreenS2C);
	}

	private static void handlePlayerVariablesS2C(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		ViaRomanaModVariables.PlayerVariablesSyncMessage message = new ViaRomanaModVariables.PlayerVariablesSyncMessage(buf);
		client.execute(() -> {
			ViaRomanaModVariables.PlayerVariablesSyncMessage.handleClient(message);
			if (client.screen instanceof net.rasanovum.viaromana.client.gui.ChartingScreen) {
				boolean isCharting = net.rasanovum.viaromana.network.ViaRomanaModVariables.getPlayerVariables(client.player).ChartingPath;
				if (!isCharting) {
					client.setScreen(null);
				} else {
					client.setScreen(new ChartingScreen(Component.translatable("gui.viaromana.charting_screen.title")));
				}
			}
		});
	}

	private static void handlePathGraphSyncS2C(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		PathGraphSyncPacket message = new PathGraphSyncPacket(buf);
		client.execute(() -> PathGraphSyncPacket.handleClient(message));
	}
	
	private static void handleMapResponseS2C(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		MapResponseS2C message = new MapResponseS2C(buf);
		client.execute(() -> {
			MapClient.handleMapResponse(message);
		});
	}

	private static void handleDestinationResponseS2C(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		DestinationResponsePacket message = new DestinationResponsePacket(buf);
		client.execute(() -> {
			TeleportMapScreen screen = new TeleportMapScreen(message);
        	Minecraft.getInstance().setScreen(screen);
		});
	}

	private static void handleOpenLinkSignScreenS2C(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		OpenLinkScreenPacket message = new OpenLinkScreenPacket(buf);
		client.execute(() -> OpenLinkScreenPacket.handleClient(message));
	}

	private static void handleOpenChartingScreenS2C(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		client.execute(() -> {
			Component title = Component.translatable("gui.viaromana.charting_screen.title");
			ChartingScreen screen = new ChartingScreen(title);
			client.setScreen(screen);
		});
	}

	private static void handleSignValidationS2C(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		SignValidationS2C message = new SignValidationS2C(buf);
		client.execute(() -> {
			if (client.screen instanceof TeleportMapScreen teleportScreen) {
				teleportScreen.handleSignValidation(message.getNodePos(), message.isValid());
			}
		});
	}

	public static void sendPlayerVariablesToServer(ViaRomanaModVariables.PlayerVariables data) {
		ViaRomanaModVariables.PlayerVariablesSyncMessage message = new ViaRomanaModVariables.PlayerVariablesSyncMessage(data);
		FriendlyByteBuf buf = PacketByteBufs.create();
		message.write(buf);
		ClientPlayNetworking.send(ViaRomanaModPacketHandler.PLAYER_VARIABLES_SYNC_C2S, buf);
	}

	private static void handleOpenWarpBlockScreenS2C(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		OpenWarpBlockScreenS2CPacket message = new OpenWarpBlockScreenS2CPacket(buf);
		client.execute(() -> OpenWarpBlockScreenS2CPacket.handleClient(message));
	}
}