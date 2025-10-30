package net.rasanovum.viaromana.integration;

import net.minecraft.server.level.ServerLevel;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.loaders.Platform;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.PathGraph;

/**
 * Centralized manager for all mod integrations.
 * Handles mod-loaded checks and error handling for optional dependencies.
 */
public class IntegrationManager {
    
    /**
     * Initializes all available integrations during mod startup.
     */
    public static void initialize() {
        //? if fabric {
        if (Platform.INSTANCE.isModLoaded("surveyor")) {
            try {
                net.rasanovum.viaromana.integration.surveyor.ViaRomanaLandmarkManager.initialize();
                ViaRomana.LOGGER.info("Surveyor integration initialized successfully");
            } catch (Exception e) {
                ViaRomana.LOGGER.error("Failed to initialize Surveyor integration", e);
            }
        }
        //?}
    }
    
    /**
     * Adds a destination landmark for a node across all available map integrations.
     * 
     * @param level The ServerLevel where the destination is located
     * @param node The node to add as a destination
     */
    public static void addDestination(ServerLevel level, Node node) {
        //? if fabric {
        if (Platform.INSTANCE.isModLoaded("surveyor")) {
            try {
                net.rasanovum.viaromana.integration.surveyor.ViaRomanaLandmarkManager.addDestinationLandmark(level, node);
            } catch (Exception e) {
                ViaRomana.LOGGER.warn("Failed to add Surveyor landmark: {}", e.getMessage());
            }
        }
        //?}
    }
    
    /**
     * Removes a destination landmark for a node across all available map integrations.
     * 
     * @param level The ServerLevel where the destination is located
     * @param node The node to remove as a destination
     */
    public static void removeDestination(ServerLevel level, Node node) {
        //? if fabric {
        if (Platform.INSTANCE.isModLoaded("surveyor")) {
            try {
                net.rasanovum.viaromana.integration.surveyor.ViaRomanaLandmarkManager.removeDestinationLandmark(level, node);
            } catch (Exception e) {
                ViaRomana.LOGGER.warn("Failed to remove Surveyor landmark: {}", e.getMessage());
            }
        }
        //?}
    }
    
    /**
     * Updates all network colors in map integrations to match their assigned network colors.
     * 
     * @param graph The PathGraph containing all nodes and networks
     * @param level The ServerLevel to update landmarks in
     */
    public static void updateAllNetworkColors(PathGraph graph, ServerLevel level) {
        //? if fabric {
        if (Platform.INSTANCE.isModLoaded("surveyor")) {
            try {
                net.rasanovum.viaromana.integration.surveyor.ViaRomanaLandmarkManager.updateAllNetworkColors(graph, level);
            } catch (Exception e) {
                ViaRomana.LOGGER.warn("Failed to update Surveyor network colors: {}", e.getMessage());
            }
        }
        //?}
    }
}