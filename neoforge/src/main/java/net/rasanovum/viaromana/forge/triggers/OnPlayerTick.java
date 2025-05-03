package net.rasanovum.viaromana.forge.triggers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rasanovum.viaromana.core.ChartingHandler;
import net.rasanovum.viaromana.core.TeleportHandler;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

@EventBusSubscriber(modid = ViaRomanaMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class OnPlayerTick {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
            MinecraftServer server = event.getServer();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player == null || !player.isAlive() || player.connection == null || !player.connection.isAcceptingMessages())
                    continue;

                Level level = player.level();
                Vec3 pos = player.position();

                // TimerUtils.checkTimers(player);
                ChartingHandler.chartPath(level, pos.x, pos.y, pos.z, player);
                TeleportHandler.effect(level, pos.x, pos.y, pos.z, player);
                TeleportHandler.cycle(level, pos.x, pos.y, pos.z, player);
            }
        }
    }

