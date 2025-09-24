package net.rasanovum.viaromana.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;

public class NetworkUtils {
    public static void sendToPlayer(Player player, CustomPacketPayload message) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (ViaRomanaModVariables.networkHandler != null) {
                ViaRomanaModVariables.networkHandler.sendToPlayer(serverPlayer, message);
            }
        }
    }
} 