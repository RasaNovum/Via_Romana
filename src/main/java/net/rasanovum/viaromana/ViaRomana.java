package net.rasanovum.viaromana;

import eu.midnightdust.lib.config.MidnightConfig;
import folk.sisby.surveyor.WorldSummary;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicDataPack;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.command.ViaRomanaCommands;
import net.rasanovum.viaromana.core.DimensionHandler;
import net.rasanovum.viaromana.core.ResetVariables;
import net.rasanovum.viaromana.init.*;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.network.PacketRegistration;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.rasanovum.viaromana.surveyor.ViaRomanaLandmarkManager;
import net.rasanovum.viaromana.tags.ServerResourcesGenerator;
import net.rasanovum.viaromana.util.VersionUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("deprecation")
public class ViaRomana implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "via_romana";
    @SuppressWarnings("removal")
    public static final DynamicDataPack DYNAMIC_PACK = new DynamicDataPack(VersionUtils.getLocation(MODID, "dynamic_tags"));

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

        ServerResourcesGenerator generator = new ServerResourcesGenerator(DYNAMIC_PACK);
        generator.register();

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

        // ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resourceManager) -> { });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            ServerMapCache.shutdown();
            ServerMapCache.clear();
            ServerMapCache.init(server);
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            DimensionHandler.preventHopping(destination, player);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ViaRomanaCommands.register(dispatcher));
    }
}