package net.rasanovum.viaromana.client;

import net.rasanovum.viaromana.CommonConfig;

/**
 * Client-side cache for server-authoritative config values.
 */
public class ClientConfigCache {
    public static float pathQualityThreshold = CommonConfig.path_quality_threshold;
    public static int nodeDistanceMinimum = CommonConfig.node_distance_minimum;
    public static int nodeDistanceMaximum = CommonConfig.node_distance_maximum;
    public static int nodeUtilityDistance = CommonConfig.node_utility_distance;
    public static int infrastructureCheckRadius = CommonConfig.infrastructure_check_radius;

    public static void updateFromServer(float pathQuality, int nodeMin, int nodeMax, int utilityDist, int infraRadius) {
        pathQualityThreshold = pathQuality;
        nodeDistanceMinimum = nodeMin;
        nodeDistanceMaximum = nodeMax;
        nodeUtilityDistance = utilityDist;
        infrastructureCheckRadius = infraRadius;
    }
}