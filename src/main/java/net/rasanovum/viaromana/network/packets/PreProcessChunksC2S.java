package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.map.MapBaker;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.network.AbstractPacket;
import net.rasanovum.viaromana.path.Node.NodeData;
import net.rasanovum.viaromana.path.PathGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Packet sent from client to server while charting a path.
 * Contains all the temporary nodes of the path so far
 * for the server to render chunk data.
 */
public record PreProcessChunksC2S(List<NodeData> tempNodes) implements AbstractPacket {

    public PreProcessChunksC2S(List<NodeData> tempNodes) {
        this.tempNodes = tempNodes != null ? List.copyOf(tempNodes) : List.of();
    }

    public PreProcessChunksC2S(FriendlyByteBuf buf) {
        this(readNodes(buf));
    }

    private static List<NodeData> readNodes(FriendlyByteBuf buf) {
        int nodeCount = buf.readInt();
        List<NodeData> nodes = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            BlockPos pos = buf.readBlockPos();
            float quality = buf.readFloat();
            float clearance = buf.readFloat();
            nodes.add(new NodeData(pos, quality, clearance));
        }
        return nodes;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.tempNodes.size());
        for (NodeData nodeData : this.tempNodes) {
            buf.writeBlockPos(nodeData.pos());
            buf.writeFloat(nodeData.quality());
            buf.writeFloat(nodeData.clearance());
        }
    }

    public void handle(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            if (this.tempNodes.isEmpty() || this.tempNodes.size() < 2) return;

            UUID playerUUID = serverPlayer.getUUID();
            UUID pseudoNetworkId = ServerMapCache.getPseudoNetworkId(playerUUID);

            if (CommonConfig.logging_enum.ordinal() > 1) ViaRomana.LOGGER.info("Creating/updating pseudonetwork {} for player {} with {} temp nodes",
                pseudoNetworkId, serverPlayer.getName().getString(), this.tempNodes.size());

            PathGraph graph = PathGraph.getInstance(serverLevel);
            if (graph == null) {
                ViaRomana.LOGGER.warn("PathGraph is null, cannot create pseudonetwork");
                return;
            }

            graph.createOrUpdatePseudoNetwork(pseudoNetworkId, this.tempNodes);

            ServerMapCache.markAsPseudoNetwork(pseudoNetworkId);

            ExecutorService executor = ServerMapCache.getMapBakingExecutor();
            if (executor != null && !executor.isShutdown()) {
                MapBaker.bakeAsync(pseudoNetworkId, serverLevel, executor)
                    .thenAccept(mapInfo -> {
                        if (mapInfo != null) {
                            if (CommonConfig.logging_enum.ordinal() > 1) ViaRomana.LOGGER.info("Completed async chunk pre-processing for pseudonetwork {}: {}x{} pixels",
                                pseudoNetworkId, mapInfo.pixelWidth(), mapInfo.pixelHeight());
                        }
                    })
                    .exceptionally(ex -> {
                        ViaRomana.LOGGER.error("Error during async chunk pre-processing for pseudonetwork {}", pseudoNetworkId, ex);
                        return null;
                    });
            } else {
                ViaRomana.LOGGER.warn("Map baking executor not available, skipping chunk pre-processing");
            }
        }
    }
}
