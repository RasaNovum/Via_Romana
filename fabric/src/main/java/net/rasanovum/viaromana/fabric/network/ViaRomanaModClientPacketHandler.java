package net.rasanovum.viaromana.fabric.network;

import net.rasanovum.viaromana.network.ViaRomanaModVariables;

import net.minecraft.network.FriendlyByteBuf;
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
		ClientPlayNetworking.registerGlobalReceiver(ViaRomanaModPacketHandler.GLOBAL_VARIABLES_SYNC_S2C, ViaRomanaModClientPacketHandler::handleGlobalVariablesS2C);
	}

	private static void handlePlayerVariablesS2C(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		ViaRomanaModVariables.PlayerVariablesSyncMessage message = new ViaRomanaModVariables.PlayerVariablesSyncMessage(buf);
		client.execute(() -> ViaRomanaModVariables.PlayerVariablesSyncMessage.handleClient(message));
	}

	private static void handleGlobalVariablesS2C(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		ViaRomanaModVariables.SavedDataSyncMessage message = new ViaRomanaModVariables.SavedDataSyncMessage(buf);
		client.execute(() -> ViaRomanaModVariables.SavedDataSyncMessage.handleClient(message));
	}

	public static void sendPlayerVariablesToServer(ViaRomanaModVariables.PlayerVariables data) {
		ViaRomanaModVariables.PlayerVariablesSyncMessage message = new ViaRomanaModVariables.PlayerVariablesSyncMessage(data);
		FriendlyByteBuf buf = PacketByteBufs.create();
		message.write(buf);
		ClientPlayNetworking.send(ViaRomanaModPacketHandler.PLAYER_VARIABLES_SYNC_C2S, buf);
	}
}
