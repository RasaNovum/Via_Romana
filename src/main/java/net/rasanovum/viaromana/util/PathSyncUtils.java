package net.rasanovum.viaromana.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.rasanovum.viaromana.integration.IntegrationManager;
import net.rasanovum.viaromana.network.packets.PathGraphSyncPacket;
import net.rasanovum.viaromana.network.packets.ConfigSyncS2C;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.CommonConfig;
import commonnetwork.api.Dispatcher;

/**
 * Utility class for synchronizing PathGraph data from server to clients.
 */
public class PathSyncUtils {
    
    /**
     * Syncs the current PathGraph to all players in the specified level.
     */
    public static void syncPathGraphToAllPlayers(ServerLevel level) {
        try {
            PathGraph graph = PathGraph.getInstance(level);

            if (CommonConfig.enable_surveyor_landmark && CommonConfig.enable_surveyor_landmark_coloring) {
                try {
                    IntegrationManager.updateAllNetworkColors(graph, level);
                } catch (NullPointerException e) {
                    ViaRomana.LOGGER.warn("Surveyor landmark system not ready yet, skipping network color update for sync");
                }
            }

            PathGraphSyncPacket packet = new PathGraphSyncPacket(graph, level.dimension());

            for (ServerPlayer player : level.getPlayers(player -> true)) {
                Dispatcher.sendToClient(packet, player);
            }

            ViaRomana.LOGGER.debug("Synced PathGraph with {} nodes to {} players", graph.size(), level.getPlayers(player -> true).size());

        } catch (Exception e) {
            ViaRomana.LOGGER.error("Failed to sync PathGraph to players in level " + level.dimension().location(), e);
        }
    }
    
    /**
     * Syncs the current PathGraph to a specific player.
     */
    public static boolean syncPathGraphToPlayer(ServerPlayer player) {
        try {
            ServerLevel level = player.serverLevel();
            PathGraph graph = PathGraph.getInstance(level);

            PathGraphSyncPacket packet = new PathGraphSyncPacket(graph, level.dimension());
            Dispatcher.sendToClient(packet, player);
            
            if (CommonConfig.logging_enum.ordinal() > 1) ViaRomana.LOGGER.info("Synced PathGraph with {} nodes to player {}", graph.size(), player.getName().getString());
            return true;
        } catch (Exception e) {
            ViaRomana.LOGGER.error("Failed to sync PathGraph to player " + player.getName().getString(), e);
            return false;
        }
    }

    public static void syncNetworkInvalidationToPlayers(ServerLevel level, int networkId) {
     
    }

    /**
     * Syncs server config values to a specific player.
     */
    public static void syncConfigToPlayer(ServerPlayer player) {
        try {
            ConfigSyncS2C packet = new ConfigSyncS2C(
                CommonConfig.path_quality_threshold,
                CommonConfig.node_distance_minimum,
                CommonConfig.node_distance_maximum,
                CommonConfig.node_utility_distance,
                CommonConfig.infrastructure_check_radius
            );
            Dispatcher.sendToClient(packet, player);
            
            ViaRomana.LOGGER.debug("Synced config to player {}", player.getName().getString());
        } catch (Exception e) {
            ViaRomana.LOGGER.error("Failed to sync config to player " + player.getName().getString(), e);
        }
    }

    /**
     * Syncs server config values to all players on the server.
     */
    public static void syncConfigToAllPlayers(net.minecraft.server.MinecraftServer server) {
        try {
            ConfigSyncS2C packet = new ConfigSyncS2C(
                CommonConfig.path_quality_threshold,
                CommonConfig.node_distance_minimum,
                CommonConfig.node_distance_maximum,
                CommonConfig.node_utility_distance,
                CommonConfig.infrastructure_check_radius
            );

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Dispatcher.sendToClient(packet, player);
            }

            ViaRomana.LOGGER.debug("Synced config to {} players", server.getPlayerList().getPlayerCount());
        } catch (Exception e) {
            ViaRomana.LOGGER.error("Failed to sync config to all players", e);
        }
    }
}
