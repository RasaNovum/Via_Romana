package net.rasanovum.viaromana.integration.surveyor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.PathGraph;

import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.Landmarks;
import folk.sisby.surveyor.landmark.WorldLandmarks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages destination landmarks with the Surveyor API.
 * Handles adding/removing landmarks when signs are linked/unlinked/destroyed/failed validation.
 */
public class ViaRomanaLandmarkManager {
    public static void initialize() {
        Landmarks.register(ViaRomanaLandmark.TYPE);
        ViaRomana.LOGGER.info("Initialized Via Romana Destination landmark type.");
    }

    /**
     * Adds/updates a given landmark
     */
    public static void addDestinationLandmark(Level level, Node node) {
        if (!CommonConfig.enable_surveyor_landmark) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        try {
            WorldSummary worldSummary = WorldSummary.of(serverLevel);
            WorldLandmarks landmarks = worldSummary.landmarks();

            BlockPos nodePos = node.getBlockPos();

            assert landmarks != null;
            if (landmarks.contains(ViaRomanaLandmark.TYPE, nodePos)) {
                landmarks.remove(serverLevel, ViaRomanaLandmark.TYPE, nodePos);
            }

            if (node.getLinkType() != Node.LinkType.DESTINATION) return;

            ViaRomanaLandmark landmark = ViaRomanaLandmark.createDestination(serverLevel, node, nodePos);
            landmarks.put(serverLevel, landmark);
        } catch (Exception e) {
            ViaRomana.LOGGER.warn("Failed to add destination landmark", e);
        }
    }

    /**
     * Removes a given landmark
     */
    public static void removeDestinationLandmark(Level level, Node node) {
        if (!CommonConfig.enable_surveyor_landmark) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        try {
            WorldSummary worldSummary = WorldSummary.of(serverLevel);
            WorldLandmarks landmarks = worldSummary.landmarks();

            BlockPos nodePos = node.getBlockPos();

            assert landmarks != null;
            landmarks.remove(serverLevel, ViaRomanaLandmark.TYPE, nodePos);
        } catch (Exception e) {
            ViaRomana.LOGGER.warn("Failed to remove destination landmark", e);
        }
    }

    /**
     * Updates the colors of all network landmarks in the graph to match their network colors.
     * This method iterates through all networks and updates the color of destination landmarks
     * to match their assigned network color.
     * 
     * @param graph The PathGraph containing all nodes and networks
     * @param level The ServerLevel to update landmarks in
     */
    public static void updateAllNetworkColors(PathGraph graph, ServerLevel level) {
        if (!CommonConfig.enable_surveyor_landmark) return;
        if (!CommonConfig.enable_surveyor_landmark_coloring) return;

        WorldLandmarks worldLandmarks;
        try {
            worldLandmarks = WorldSummary.of(level).landmarks();
        } catch (Exception e) {
            ViaRomana.LOGGER.warn("Failed to access WorldLandmarks, skipping network color update.", e);
            return;
        }

        Set<UUID> processedNetworks = new HashSet<>();
        List<Node> nodes = graph.nodesView();
        
        for (Node node : nodes) {
            PathGraph.NetworkCache cache = graph.getNetworkCache(node);
            if (processedNetworks.contains(cache.id())) {
                continue;
            }

            DyeColor requiredColor = graph.getNetworkColor(node);
            for (Node destinationNode : cache.destinationNodes()) {
                try {
                    Landmark<?> landmark = worldLandmarks.get(ViaRomanaLandmark.TYPE, destinationNode.getBlockPos());
                    if (landmark != null && landmark.color() != requiredColor) {
                        ViaRomanaLandmark updatedLandmark = ViaRomanaLandmark.createDestination(level, destinationNode, destinationNode.getBlockPos());
                        worldLandmarks.put(level, updatedLandmark);
                    }
                } catch (Exception e) {
                    ViaRomana.LOGGER.warn("Failed to update landmark color for node at {}: {}", destinationNode.getBlockPos(), e.getMessage());
                }
            }
            processedNetworks.add(cache.id());
        }
    }
}