package net.rasanovum.viaromana.network;

import net.minecraft.server.level.ServerPlayer;

public interface NetworkHandler {
    void sendToPlayer(ServerPlayer player, Object message);
}