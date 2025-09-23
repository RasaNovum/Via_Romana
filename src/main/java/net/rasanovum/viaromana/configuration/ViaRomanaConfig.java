package net.rasanovum.viaromana.configuration;

import eu.midnightdust.lib.config.MidnightConfig;
import java.util.List;

import com.google.common.collect.Lists;

public class ViaRomanaConfig extends MidnightConfig {

    public static final String VARIABLES = "variables";
    public static final String MANAGEMENT = "management";

    // @Comment(centered = true) public static Comment server_title;

    // Variables Category
    @Entry(category = VARIABLES) public static int infrastructure_check_radius = 1;
    @Entry(category = VARIABLES) public static int node_distance_minimum = 4;
    @Entry(category = VARIABLES) public static int node_distance_maximum = 8;
    @Entry(category = VARIABLES) public static int node_utility_distance = 3;
    @Entry(category = VARIABLES, min = 0f, max = 1f, precision = 100) public static float path_quality_threshold = 0.6f;
    @Entry(category = VARIABLES) public static int travel_fatigue_cooldown = 30;
    @Entry(category = VARIABLES) public static int fog_of_war_distance = 10;
    @Entry(category = VARIABLES) public static int maximum_map_dimension = 512;
    @Entry(category = VARIABLES) public static int map_refresh_interval = 10;
    @Entry(category = VARIABLES) public static int map_save_interval = 5;
    @Entry(category = VARIABLES, min = 0f, max = 1f) public static float invalid_block_overlay_opacity = 0.4f;
    @Entry(category = VARIABLES) public static boolean enable_surveyor_landmark = false;
    @Entry(category = VARIABLES) public static boolean enable_surveyor_landmark_coloring = false;

    // Management Category
    @Entry(category = MANAGEMENT) public static List<String> invalid_entities = Lists.newArrayList();
    @Entry(category = MANAGEMENT) public static List<String> invalid_dimensions = Lists.newArrayList();
    @Entry(category = MANAGEMENT) public static List<String> path_block_ids = Lists.newArrayList("minecraft:dirt_path", "minecraft:packed_mud", "minecraft:coarse_dirt", "minecraft:rooted_dirt");
    @Entry(category = MANAGEMENT) public static List<String> path_block_tags = Lists.newArrayList("minecraft:slabs", "minecraft:stairs", "minecraft:walls", "minecraft:wool", "minecraft:wool_carpets", "minecraft:planks", "minecraft:logs", "minecraft:rails", "minecraft:fences", "minecraft:fence_gates", "minecraft:buttons", "minecraft:pressure_plates");
    @Entry(category = MANAGEMENT) public static List<String> path_block_strings = Lists.newArrayList("sandstone", "polished", "cobble", "brick", "smooth", "basalt", "path", "road");
    @Entry(category = MANAGEMENT) public static List<String> warp_block_ids = Lists.newArrayList("via_romana:warp_block");
    @Entry(category = MANAGEMENT) public static List<String> warp_block_tags = Lists.newArrayList("minecraft:all_signs");

    // @Comment(centered = true) public static Comment client_title;
}