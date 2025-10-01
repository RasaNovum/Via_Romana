package net.rasanovum.viaromana;

import eu.midnightdust.lib.config.MidnightConfig;
import folk.sisby.surveyor.WorldSummary;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.command.ViaRomanaCommands;
import net.rasanovum.viaromana.core.DimensionHandler;
import net.rasanovum.viaromana.core.ResetVariables;
import net.rasanovum.viaromana.init.*;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.network.PacketRegistration;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.rasanovum.viaromana.surveyor.ViaRomanaLandmarkManager;
import net.rasanovum.viaromana.tags.TagGenerator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pers.solid.brrp.v1.api.RuntimeResourcePack;
import pers.solid.brrp.v1.fabric.api.RRPCallback;

public class ViaRomana implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "via_romana";
    public static final RuntimeResourcePack RUNTIME_PACK = RuntimeResourcePack.create(ResourceLocation.parse("via_romana:runtime_pack"));

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing ViaRomanaMod");

        MidnightConfig.init(MODID, CommonConfig.class);
        WorldSummary.enableTerrain();

        new PacketRegistration().init();

        BlockInit.load();
        EffectInit.load();
        ItemInit.load();
        // SoundInit.load();
        TriggerInit.load();

        ViaRomanaLandmarkManager.initialize();

        RRPCallback.BEFORE_VANILLA.register(resources -> {
            TagGenerator.generateAllTags(RUNTIME_PACK);
            resources.add(RUNTIME_PACK);
        });

        registerServerLifecycleEvents();
    }

    private void registerServerLifecycleEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ViaRomanaModVariables.playerLoggedIn(handler.player);
            ResetVariables.execute(handler.player.level(), handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ViaRomanaModVariables.playerLoggedOut(handler.player);
        });
        
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ServerMapCache.init(server);
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ServerMapCache.processAllDirtyNetworks();
            ServerMapCache.saveAllToDisk(true);
            ServerMapCache.clear();
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            boolean keepInventory = oldPlayer.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
            ViaRomanaModVariables.playerRespawned(oldPlayer, newPlayer, keepInventory || !alive);
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                try {
                    MidnightConfig.init(MODID, CommonConfig.class);
                    LOGGER.info("Via Romana config reloaded and synced to players.");
                } catch (Exception e) {
                    LOGGER.error("Failed to reload MidnightConfig", e);
                }

                ServerMapCache.shutdown();
                ServerMapCache.clear();
                ServerMapCache.init(server);
            }
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            DimensionHandler.preventHopping(destination, player);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ViaRomanaCommands.register(dispatcher));
    }
}