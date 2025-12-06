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
import net.rasanovum.viaromana.client.render.ClientCursorHandler;
import net.rasanovum.viaromana.command.ViaRomanaCommands;
import net.rasanovum.viaromana.core.DimensionHandler;
import net.rasanovum.viaromana.core.LinkHandler;
import net.rasanovum.viaromana.core.SignInteract;
import net.rasanovum.viaromana.init.*;
import net.rasanovum.viaromana.integration.IntegrationManager;
import net.rasanovum.viaromana.map.ChunkPixelRenderer;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.speed.SpeedHandler;
import net.rasanovum.viaromana.storage.player.PlayerData;
import net.rasanovum.viaromana.tags.ServerResourcesGenerator;
import net.rasanovum.viaromana.teleport.ServerTeleportHandler;
import net.rasanovum.viaromana.util.PathSyncUtils;
import net.rasanovum.viaromana.util.VersionUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ViaRomana {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "via_romana";
    @SuppressWarnings("removal")
    public static final DynamicDataPack DYNAMIC_PACK = new DynamicDataPack(VersionUtils.getLocation(MODID, "dynamic_tags"));
    
    private static final int PATH_GRAPH_SYNC_DELAY = 40;
    private static final int PATH_GRAPH_SYNC_MAX_ATTEMPTS = 10;

    private static final Map<UUID, PendingPathGraphSync> pendingPathGraphSyncs = new HashMap<>();

    private static MinecraftServer currentServer = null;

    public static void initialize() {
        LOGGER.info("Initializing Via Romana");

        DataInit.load();

        MidnightConfig.init(MODID, CommonConfig.class);

        ServerResourcesGenerator generator = new ServerResourcesGenerator(DYNAMIC_PACK);
        generator.register();

        IntegrationManager.initialize();
    }

    public static void onJoin(ServerPlayer player) {
        PlayerData.resetVariables(player);
        PlayerData.syncPlayerData(player);
        PathSyncUtils.syncConfigToPlayer(player);

        MinecraftServer server = player.getServer();
        if (server != null) {
            server.execute(() -> {
                boolean synced = PathSyncUtils.syncPathGraphToPlayer(player);
                int maxAttempts = synced ? 1 : PATH_GRAPH_SYNC_MAX_ATTEMPTS;
                queueDelayedPathGraphSync(player, maxAttempts);
            });
        }
    }

    public static void onServerTick(ServerLevel level) {
        ServerTeleportHandler.tick(level);
        MinecraftServer server = level.getServer();
        if (level == server.overworld()) {
            tickPendingPathGraphSyncs(server);
        }

        for (ServerPlayer player : level.players()) {
            SpeedHandler.onPlayerTick(player);
        }
    }

    public static void onServerStart(MinecraftServer server) {
        currentServer = server;
        ServerMapCache.init(server);
        ChunkPixelRenderer.init();
    }

    public static void onServerStop() {
        ServerMapCache.shutdown();
        ServerMapCache.saveAllToDisk(true);
        ServerMapCache.clear();
        pendingPathGraphSyncs.clear();
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

    /**
     * Queues a delayed PathGraph sync for a player.
     */
    private static void queueDelayedPathGraphSync(ServerPlayer player, int maxAttempts) {
        pendingPathGraphSyncs.put(player.getUUID(), new PendingPathGraphSync(PATH_GRAPH_SYNC_DELAY, maxAttempts));
        if (CommonConfig.logging_enum.ordinal() > 0) LOGGER.info("Queued delayed PathGraph sync for player {} (max attempts: {})", player.getName().getString(), maxAttempts);
    }

    /**
     * Ticks the pending PathGraph syncs.
     */
    private static void tickPendingPathGraphSyncs(MinecraftServer server) {
        if (pendingPathGraphSyncs.isEmpty()) return;

        Iterator<Map.Entry<UUID, PendingPathGraphSync>> iterator = pendingPathGraphSyncs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingPathGraphSync> entry = iterator.next();
            PendingPathGraphSync pending = entry.getValue();

            if (!pending.tick()) continue;

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            if (PathSyncUtils.syncPathGraphToPlayer(player)) {
                iterator.remove();
                if (CommonConfig.logging_enum.ordinal() > 0) LOGGER.info("Delayed PathGraph sync succeeded for player {}", player.getName().getString());
            } else {
                if (pending.incrementAttempts() >= pending.maxAttempts) {
                    LOGGER.warn("Giving up on PathGraph sync for player {} after {} attempts", player.getName().getString(), pending.maxAttempts);
                    iterator.remove();
                } else {
                    pending.ticksUntilAttempt = PATH_GRAPH_SYNC_DELAY;
                }
            }
        }
    }

    private static final class PendingPathGraphSync {
        private int ticksUntilAttempt;
        private int attempts;
        private final int maxAttempts;

        private PendingPathGraphSync(int initialDelay, int maxAttempts) {
            this.ticksUntilAttempt = initialDelay;
            this.attempts = 0;
            this.maxAttempts = Math.max(1, maxAttempts);
        }

        private boolean tick() {
            if (ticksUntilAttempt > 0) {
                ticksUntilAttempt--;
            }
            return ticksUntilAttempt <= 0;
        }

        private int incrementAttempts() {
            return ++attempts;
        }
    }
}