package net.rasanovum.viaromana.network.packets;

import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
//? if >=1.21 {
import net.minecraft.advancements.AdvancementHolder;
//?} else {
/*import net.minecraft.advancements.Advancement;
*///?}

import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.StatInit;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.network.AbstractPacket;
import net.rasanovum.viaromana.path.Node.NodeData;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.storage.path.PathDataManager;
import net.rasanovum.viaromana.util.PathSyncUtils;
import net.rasanovum.viaromana.util.VersionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packet sent from client to server when a player finishes charting a path.
 * Contains all the temporary nodes/links that should be made permanent
 * and be connected as a path on the server-side PathGraph.
 */
public record ChartedPathC2S(List<NodeData> chartedNodes) implements AbstractPacket {

    public ChartedPathC2S(List<NodeData> chartedNodes) {
        this.chartedNodes = chartedNodes != null ? List.copyOf(chartedNodes) : List.of();
    }

    public ChartedPathC2S(FriendlyByteBuf buf) {
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
        buf.writeInt(this.chartedNodes.size());
        for (NodeData nodeData : this.chartedNodes) {
            buf.writeBlockPos(nodeData.pos());
            buf.writeFloat(nodeData.quality());
            buf.writeFloat(nodeData.clearance());
        }
    }

    public void handle(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            PathGraph graph = PathGraph.getInstance(serverLevel);
            UUID playerUUID = serverPlayer.getUUID();

            if (this.chartedNodes.isEmpty()) {
                ViaRomana.LOGGER.warn("Received empty charted path from player {}", serverPlayer.getName().getString());
                return;
            }

            try {
                double totalDistance = 0.0;
                for (int i = 1; i < this.chartedNodes.size(); i++) {
                    BlockPos prev = this.chartedNodes.get(i - 1).pos();
                    BlockPos curr = this.chartedNodes.get(i).pos();
                    totalDistance += Math.sqrt(prev.distSqr(curr));
                }

                serverPlayer.awardStat(Stats.CUSTOM.get(StatInit.DISTANCE_CHARTED), (int) (totalDistance * 100));
                
                graph.createConnectedPath(this.chartedNodes);
                PathDataManager.markDirty(serverLevel);
                PathSyncUtils.syncPathGraphToAllPlayers(serverLevel);
                awardChartingAdvancements(serverPlayer);

                UUID pseudoNetworkId = ServerMapCache.getPseudoNetworkId(playerUUID);
                ServerMapCache.invalidatePseudoNetwork(pseudoNetworkId);

                int totalCharted = serverPlayer.getStats().getValue(Stats.CUSTOM.get(StatInit.DISTANCE_CHARTED));
                ViaRomana.LOGGER.debug("Created charted path with {} nodes for player {} ({}m charted, {}m total), cleaned up pseudonetwork {}",
                    this.chartedNodes.size(), serverPlayer.getName().getString(), (int)totalDistance, totalCharted, pseudoNetworkId);
            } catch (Exception e) {
                ViaRomana.LOGGER.error("Failed to create charted path for player {}: {}", serverPlayer.getName().getString(), e.getMessage());
            }
        }
    }

    private static void awardChartingAdvancements(ServerPlayer player) {
        awardAdvancement(player, "via_romana:story/a_strand_type_game");

        int totalCharted = player.getStats().getValue(Stats.CUSTOM.get(StatInit.DISTANCE_CHARTED));
        if (totalCharted >= 5000 * 100) { // Units in cm
            awardAdvancement(player, "via_romana:story/straight_up_pathing_it");
        }
    }

    private static void awardAdvancement(ServerPlayer player, String advancementId) {
        try {
            //? if <1.21 {
            /*Advancement advancement = player.server.getAdvancements().getAdvancement(new ResourceLocation(advancementId));
            if (advancement != null) {
                AdvancementProgress advancementProgress = player.getAdvancements().getOrStartProgress(advancement);
                if (!advancementProgress.isDone()) {
                    for (String criterion : advancementProgress.getRemainingCriteria()) {
                        player.getAdvancements().award(advancement, criterion);
                    }
                }
            }
            *///?} else {
            ResourceLocation id = VersionUtils.getLocation(advancementId);
            AdvancementHolder advancement = player.server.getAdvancements().get(id);
            if (advancement != null) {
                AdvancementProgress advancementProgress = player.getAdvancements().getOrStartProgress(advancement);
                if (!advancementProgress.isDone()) {
                    boolean grantedAny = false;
                    for (String criterion : advancementProgress.getRemainingCriteria()) {
                        boolean granted = player.getAdvancements().award(advancement, criterion);
                        if (granted) grantedAny = true;
                    }

                    if (grantedAny) player.getAdvancements().flushDirty(player);
                }
            }
            //?}
        } catch (Exception e) {
            ViaRomana.LOGGER.warn("Failed to award advancement {} to player {}: {}", advancementId, player.getName().getString(), e.getMessage());
        }
    }
}
