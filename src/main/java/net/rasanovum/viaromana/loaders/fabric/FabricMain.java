package net.rasanovum.viaromana.loaders.fabric;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.EffectInit;
import net.rasanovum.viaromana.init.BlockInit;
import net.rasanovum.viaromana.init.ItemInit;
import net.rasanovum.viaromana.network.PacketRegistration;

public class FabricMain implements ModInitializer {
    @Override
    public void onInitialize() {
        ViaRomana.initialize();
        new PacketRegistration().initCommon();
        EffectInit.load();
        BlockInit.load();
        ItemInit.load();
        registerServerLifecycleEvents();
    }

    private void registerServerLifecycleEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ViaRomana.onJoin(handler.player);
        });
        
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var level : server.getAllLevels()) {
                ViaRomana.onServerTick(level);
            }
        });
        
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ViaRomana.onServerStart(server);
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ViaRomana.onServerStop();
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            ViaRomana.onDataPackReload(server);
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, level) -> {
            ViaRomana.onDimensionChange(level, player);
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockentity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                return ViaRomana.onBlockBreak(world, pos, serverPlayer);
            }
            return false;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ViaRomana.registerCommands(dispatcher);
        });
    }
}
//?}