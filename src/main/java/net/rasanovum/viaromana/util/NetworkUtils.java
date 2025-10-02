package net.rasanovum.viaromana.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import commonnetwork.api.Dispatcher;

public class NetworkUtils {
    //? if >=1.21 {
    public static void sendToPlayer(Player player, CustomPacketPayload message) {
        if (player instanceof ServerPlayer serverPlayer) {
            Dispatcher.sendToClient(message, serverPlayer);
        }
    }
    //?} else {
    /*public static void sendToPlayer(Player player, Object message) {
        if (player instanceof ServerPlayer serverPlayer) {
            Dispatcher.sendToClient(message, serverPlayer);
        }
    }
    *///?}
} 