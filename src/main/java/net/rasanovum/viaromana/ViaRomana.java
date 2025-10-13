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

    public static void initialize() {
        LOGGER.info("Initializing Via Romana");

        MidnightConfig.init(MODID, CommonConfig.class);

        new PacketRegistration().init();

        DataInit.load();

        ServerResourcesGenerator generator = new ServerResourcesGenerator(DYNAMIC_PACK);
        generator.register();
    }

    public static void onJoin(ServerPlayer player) {
        PlayerData.resetVariables(player);
        PathSyncUtils.syncPathGraphToPlayer(player);
    }

    public static void onServerTick(ServerLevel level) {
        ServerTeleportHandler.tick(level);
    }

    public static void onServerStart(MinecraftServer server) {
        ServerMapCache.init(server);
    }

    public static void onServerStop() {
        ServerMapCache.processAllDirtyNetworks();
        ServerMapCache.saveAllToDisk(true);
        ServerMapCache.clear();
    }

    public static void onDataPackReload(MinecraftServer server) {
        ServerMapCache.shutdown();
        ServerMapCache.clear();
        ServerMapCache.init(server);
    }

    public static void onDimensionChange(ServerLevel level, ServerPlayer player) {
        DimensionHandler.preventHopping(level, player);
        DimensionHandler.syncPathDataOnDimensionChange(level, player);
    }

    public static boolean onBlockBreak(LevelAccessor world, BlockPos pos, ServerPlayer player) {
        if (LinkHandler.isSignBlock(world, pos) && LinkHandler.isSignLinked(world, pos)) {
            if (!player.isShiftKeyDown()) {
                return true;
            }
        }

        SignInteract.broken(world, pos, player);
        return false;
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        ViaRomanaCommands.register(dispatcher);
    }
}