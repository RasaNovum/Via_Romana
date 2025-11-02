package net.rasanovum.viaromana;

import com.mojang.brigadier.CommandDispatcher;
import eu.midnightdust.lib.config.MidnightConfig;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicDataPack;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.rasanovum.viaromana.command.ViaRomanaCommands;
import net.rasanovum.viaromana.core.DimensionHandler;
import net.rasanovum.viaromana.core.LinkHandler;
import net.rasanovum.viaromana.core.SignInteract;
import net.rasanovum.viaromana.init.*;
import net.rasanovum.viaromana.integration.IntegrationManager;
import net.rasanovum.viaromana.map.ChunkPixelRenderer;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.network.PacketRegistration;
import net.rasanovum.viaromana.storage.player.PlayerData;
import net.rasanovum.viaromana.tags.ServerResourcesGenerator;
import net.rasanovum.viaromana.teleport.ServerTeleportHandler;
import net.rasanovum.viaromana.util.PathSyncUtils;
import net.rasanovum.viaromana.util.VersionUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ViaRomana {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "via_romana";
    @SuppressWarnings("removal")
    public static final DynamicDataPack DYNAMIC_PACK = new DynamicDataPack(VersionUtils.getLocation(MODID, "dynamic_tags"));
    
    private static MinecraftServer currentServer = null;

    public static void initialize() {
        LOGGER.info("Initializing Via Romana");

        MidnightConfig.init(MODID, CommonConfig.class);

        DataInit.load();

        ServerResourcesGenerator generator = new ServerResourcesGenerator(DYNAMIC_PACK);
        generator.register();

        IntegrationManager.initialize();
    }

    public static void onJoin(ServerPlayer player) {
        PlayerData.resetVariables(player);
        PathSyncUtils.syncConfigToPlayer(player);
        PathSyncUtils.syncPathGraphToPlayer(player);
    }

    public static void onServerTick(ServerLevel level) {
        ServerTeleportHandler.tick(level);
    }

    public static void onServerStart(MinecraftServer server) {
        currentServer = server;
        ServerMapCache.init(server);
        ChunkPixelRenderer.init();
    }

    public static void onServerStop() {
        ServerMapCache.processAllDirtyNetworks(true);
        ServerMapCache.shutdown();
        ServerMapCache.saveAllToDisk(true);
        ServerMapCache.clear();
        currentServer = null;
    }

    public static void onDataPackReload(MinecraftServer server) {
        ServerMapCache.shutdown();
        ServerMapCache.clear();
        ServerMapCache.init(server);
        ChunkPixelRenderer.init();
        PathSyncUtils.syncConfigToAllPlayers(server);
    }

    public static void onDimensionChange(ServerLevel level, ServerPlayer player) {
        DimensionHandler.preventHopping(level, player);
        DimensionHandler.syncPathDataOnDimensionChange(level, player);
    }

    public static boolean onBlockBreak(LevelAccessor world, BlockPos pos, ServerPlayer player) {
        if (LinkHandler.isSignBlock(world, pos) && LinkHandler.isSignLinked(world, pos)) {
            if (!player.isShiftKeyDown()) return false;
            else SignInteract.broken(world, pos, player);
        }
        return true;
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        ViaRomanaCommands.register(dispatcher);
    }

    /**
     * Gets the current server instance. May be null if server is not running.
     */
    public static MinecraftServer getServer() {
        return currentServer;
    }
}