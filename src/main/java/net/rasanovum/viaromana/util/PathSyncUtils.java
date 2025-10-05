package net.rasanovum.viaromana.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.rasanovum.viaromana.storage.path.IPathStorage;
import net.rasanovum.viaromana.network.packets.PathGraphSyncPacket;
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
            IPathStorage storage = IPathStorage.get(level);
            PathGraph graph = storage.graph();

            if (CommonConfig.enable_surveyor_landmark && CommonConfig.enable_surveyor_landmark_coloring) {
                try {
                    graph.updateAllNetworkColors(level);
                } catch (NullPointerException e) {
                    ViaRomana.LOGGER.warn("Surveyor landmark system not ready yet, skipping network color update for sync");
                }
            }

            PathGraphSyncPacket packet = new PathGraphSyncPacket(graph);

            for (ServerPlayer player : level.getPlayers(player -> true)) {
                Dispatcher.sendToClient(packet, player);
            }

            ViaRomana.LOGGER.info("Synced PathGraph with {} nodes to {} players", graph.size(), level.getPlayers(player -> true).size());

        } catch (Exception e) {
            ViaRomana.LOGGER.error("Failed to sync PathGraph to players in level " + level.dimension().location(), e);
        }
    }
    
    /**
     * Syncs the current PathGraph to a specific player.
     */
    public static void syncPathGraphToPlayer(ServerPlayer player) {
        try {
            ServerLevel level = player.serverLevel();
            IPathStorage storage = IPathStorage.get(level);

            PathGraphSyncPacket packet = new PathGraphSyncPacket(storage.graph());
            Dispatcher.sendToClient(packet, player);
            
            ViaRomana.LOGGER.debug("Synced PathGraph with {} nodes to player {}", storage.graph().size(), player.getName().getString());
                
        } catch (Exception e) {
            ViaRomana.LOGGER.error("Failed to sync PathGraph to player " + player.getName().getString(), e);
        }
    }

    public static void syncNetworkInvalidationToPlayers(ServerLevel level, int networkId) {
     
    }
}
