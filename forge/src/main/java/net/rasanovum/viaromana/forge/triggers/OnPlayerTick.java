package net.rasanovum.viaromana.forge.triggers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rasanovum.viaromana.core.ChartingHandler;
import net.rasanovum.viaromana.core.TeleportHandler;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

@Mod.EventBusSubscriber(modid = ViaRomanaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OnPlayerTick {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer server = event.getServer();
            if (server == null) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player == null || !player.isAlive() || player.connection == null || !player.connection.isAcceptingMessages())
                    continue;

                Level level = player.getLevel();
                Vec3 pos = player.position();

                // TimerUtils.checkTimers(player);
                ChartingHandler.chartPath(level, pos.x, pos.y, pos.z, player);
                TeleportHandler.effect(level, pos.x, pos.y, pos.z, player);
                TeleportHandler.cycle(level, pos.x, pos.y, pos.z, player);
            }
        }
    }
}

