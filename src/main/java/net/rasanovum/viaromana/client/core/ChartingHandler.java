package net.rasanovum.viaromana.client.core;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.rasanovum.viaromana.network.packets.SignLinkRequestC2S;
import net.rasanovum.viaromana.util.PathUtils;
import net.rasanovum.viaromana.util.VersionUtils;
import net.rasanovum.viaromana.client.HudMessageManager;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.client.ClientConfigCache;
import net.rasanovum.viaromana.core.LinkHandler.LinkData;
import net.rasanovum.viaromana.network.packets.ChartedPathC2S;
import net.rasanovum.viaromana.network.packets.PreProcessChunksC2S;
import commonnetwork.api.Dispatcher;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.Node.NodeData;
import net.rasanovum.viaromana.storage.player.PlayerData;

import java.util.List;
import java.util.Optional;
/**
 * Handles start / progress / completion of player path-charting.
 */
public final class ChartingHandler {
    private static final int PREPROCESS_INTERVAL = 5;

    public static void chartPath(LevelAccessor level, Entity entity) {
        if (!PlayerData.isChartingPath((Player) entity)) return;

        float nodeDistance = PathUtils.calculateNodeDistance(entity);
        float infrastructureQuality = PathUtils.calculateInfrastructureQuality(level, entity);
        float clearance = PathUtils.calculateClearance(level, entity);

        if (nodeDistance > ClientConfigCache.nodeDistanceMaximum) {
            HudMessageManager.queueMessage("message.via_romana.too_far_from_node_message");
            return;
        }

        if (infrastructureQuality < ClientConfigCache.pathQualityThreshold) {
            float threshold = ClientConfigCache.pathQualityThreshold;
            int requiredBlocks = (int) Math.ceil(threshold * 9.0);
            int currentBlocks = Math.round(infrastructureQuality * 9.0f);
            HudMessageManager.queueMessage(Component.translatable("gui.viaromana.infrastructure_insufficient", currentBlocks, requiredBlocks));
            return;
        }

        if (nodeDistance < ClientConfigCache.nodeDistanceMinimum) {
            HudMessageManager.queueMessage("message.via_romana.path_charting");
            return;
        }

        addChartingNode(level, entity, entity.blockPosition(), infrastructureQuality, clearance);
    }

    private static void playCartographySound(LevelAccessor level, Player player) {
        if (!(level instanceof Level lvl)) return;

        var pos  = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        var sound  = BuiltInRegistries.SOUND_EVENT.get(VersionUtils.getLocation("minecraft:ui.cartography_table.take_result"));
        assert sound != null;

        if (lvl.isClientSide()) {
            lvl.playLocalSound(player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1, 1, false);
        } else {
            lvl.playSound(null, pos, sound, SoundSource.PLAYERS, 1, 1);
        }
    }

    /**
     * Merges with an existing nearby node (preferred) or creates a new one.
     */
    public static void addChartingNode(LevelAccessor level, Entity entity, BlockPos pos, Float quality, Float clearance) {
        if (!(entity instanceof Player player)) return;
        if (Math.random() > 0.9) playCartographySound(level, player);

        Optional<Node> nearbyNode = ClientPathData.getInstance().getNearestNode(pos, ClientConfigCache.nodeUtilityDistance, 1.0f, true);

        if (nearbyNode.isPresent()) {
            pos = nearbyNode.get().getBlockPos();
            clearance = nearbyNode.get().getClearance();
        }

        PlayerData.setLastNodePos(player, pos, false);
        ClientPathData.getInstance().addTemporaryNode(pos, quality, clearance);
        
        List<NodeData> tempNodes = ClientPathData.getInstance().getTemporaryNodes();
        if (tempNodes.size() % PREPROCESS_INTERVAL == 0 && tempNodes.size() >= 2) {
            PreProcessChunksC2S packet = new PreProcessChunksC2S(tempNodes);
            Dispatcher.sendToServer(packet);
        }
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
        Dispatcher.sendToServer(packet);

        if (chartingLinks != null && !chartingLinks.isEmpty()) {
            for (LinkData link : chartingLinks) {
                SignLinkRequestC2S linkPacket = new SignLinkRequestC2S(link, false);
                Dispatcher.sendToServer(linkPacket);
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
