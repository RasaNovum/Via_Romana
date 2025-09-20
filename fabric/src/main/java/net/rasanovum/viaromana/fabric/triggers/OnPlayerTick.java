package net.rasanovum.viaromana.fabric.triggers;

import net.rasanovum.viaromana.core.ChartingHandler;
import net.rasanovum.viaromana.core.TeleportHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class OnPlayerTick {
    public static void onPlayerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Level level = player.level();
            Vec3 pos = player.position();
            
            if (player == null || !player.isAlive() || player.connection == null || !player.connection.isAcceptingMessages())
                continue;

            // TimerUtils.checkTimers(player);
            ChartingHandler.chartPath(level, pos.x, pos.y, pos.z, player);
            TeleportHandler.effect(level, pos.x, pos.y, pos.z, player);
            TeleportHandler.cycle(level, pos.x, pos.y, pos.z, player);
        }
    }
}
