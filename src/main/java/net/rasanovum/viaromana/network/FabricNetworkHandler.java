package net.rasanovum.viaromana.network;

import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

class FabricNetworkHandler implements NetworkHandler {
    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload message) {
        if (player == null) return;

        ServerPlayNetworking.send(player, message);
    }
    
    @Override
    public void sendToServer(CustomPacketPayload message) {
        ClientPlayNetworking.send(message);
    }
}