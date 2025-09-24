package net.rasanovum.viaromana.client.core;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.rasanovum.viaromana.util.PathUtils;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.HudMessageManager;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.configuration.ViaRomanaConfig;
import net.rasanovum.viaromana.core.LinkHandler.LinkData;
import net.rasanovum.viaromana.network.packets.ChartedPathC2S;
import net.rasanovum.viaromana.network.packets.LinkSignRequestPacket;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.Node.NodeData;
import net.rasanovum.viaromana.variables.VariableAccess;

import java.util.List;
import java.util.Optional;
/**
 * Handles start / progress / completion of player path-charting.
 */
public final class ChartingHandler {
    public static void chartPath(LevelAccessor level, Entity entity) {
        if (!VariableAccess.playerVariables.isChartingPath(entity)) return;

        float nodeDistance = PathUtils.calculateNodeDistance(entity);
        float infrastructureQuality = PathUtils.calculateInfrastructureQuality(level, entity);

        if (nodeDistance > ViaRomanaConfig.node_distance_maximum) {
            HudMessageManager.queueMessage("message.via_romana.too_far_from_node_message");
            return;
        }
        if (infrastructureQuality < ViaRomanaConfig.path_quality_threshold) {
            float threshold = ViaRomanaConfig.path_quality_threshold;
            int requiredBlocks = (int) Math.ceil(threshold * 9.0);
            int currentBlocks = Math.round(infrastructureQuality * 9.0f);
            HudMessageManager.queueMessage(Component.translatable("gui.viaromana.infrastructure_insufficient", currentBlocks, requiredBlocks));
            return;
        }

        if (nodeDistance < ViaRomanaConfig.node_distance_minimum) {
            HudMessageManager.queueMessage("message.via_romana.path_charting");
            return;
        }

        addChartingNode(level, entity, entity.blockPosition(), infrastructureQuality);
    }

    private static void playCartographySound(LevelAccessor level, Entity entity) {
        if (!(level instanceof Level lvl)) return;

        var pos  = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
        var snd  = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("minecraft:ui.cartography_table.take_result"));

        if (lvl.isClientSide()) {
            lvl.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), snd, SoundSource.PLAYERS, 1, 1, false);
        } else {
            lvl.playSound(null, pos, snd, SoundSource.PLAYERS, 1, 1);
        }
    }

    /**
     * Merges with an existing nearby node (preferred) or creates a new one.
     */
    public static void addChartingNode(LevelAccessor level, Entity entity, BlockPos pos, Float quality) {
        if (entity == null) return;  
        if (Math.random() > 0.9) playCartographySound(level, entity);

        Optional<Node> nearbyNode = ClientPathData.getInstance().getNearestNode(pos, ViaRomanaConfig.node_utility_distance, true);

        if (nearbyNode.isPresent()) {
            BlockPos nearbyPos = nearbyNode.get().getBlockPos();
            
            pos = nearbyPos;
        }

        VariableAccess.playerVariables.setLastNodePos(entity, pos);
        ClientPathData.getInstance().addTemporaryNode(pos, quality);
    }

    /**
     * Creates a path link between nodes using the new PathGraph approach
     */
    public static void finishPath(LocalPlayer player) {
        ClientPathData clientPathData = ClientPathData.getInstance();
        List<NodeData> chartingNodes = clientPathData.getTemporaryNodes();
        List<LinkData> chartingLinks = clientPathData.getTemporaryLinks();

        if (player == null || chartingNodes == null || chartingNodes.isEmpty()) return;

        ChartedPathC2S packet = new ChartedPathC2S(chartingNodes);
        if (ViaRomanaModVariables.networkHandler != null) {
            ViaRomanaModVariables.networkHandler.sendToServer(packet);
        } else {
            ViaRomana.LOGGER.error("Failed to send charted path - NetworkHandler not available");
        }

        if (chartingLinks != null && !chartingLinks.isEmpty()) {
            for (LinkData link : chartingLinks) {
                LinkSignRequestPacket linkPacket = new LinkSignRequestPacket(link, false);
                if (ViaRomanaModVariables.networkHandler != null) {
                    ViaRomanaModVariables.networkHandler.sendToServer(linkPacket);
                } else {
                    ViaRomana.LOGGER.error("Failed to send link packet - NetworkHandler not available");
                }
            }
        }

        HudMessageManager.queueMessage("message.via_romana.finish_charting");
    }

    /*
    * Clears temporary lists when the player starts a brand-new path.
    */
    public static void initializeChartingNodes(LocalPlayer player) {
        if (player != null) ClientPathData.getInstance().clearTemporaryNodes();
    }
}
