package net.rasanovum.viaromana.surveyor;

import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.Landmarks;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.path.Node;

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
            
            landmarks.remove(serverLevel, ViaRomanaLandmark.TYPE, nodePos);
        } catch (Exception e) {
            ViaRomana.LOGGER.warn("Failed to remove destination landmark", e);
        }
    }
}
