package net.rasanovum.viaromana.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public interface NetworkHandler {
    void sendToPlayer(ServerPlayer player, CustomPacketPayload message);
    void sendToServer(CustomPacketPayload message);
}