package net.rasanovum.viaromana;

import eu.midnightdust.lib.config.MidnightConfig;
import java.util.List;

import com.google.common.collect.Lists;

public class CommonConfig extends MidnightConfig {
    public static final String CHARTING = "charting";
    public static final String MAP = "map";
    public static final String WARP = "warp";
    public static final String VISUALS = "visuals";

    @Entry(category = CHARTING, min = 1) public static int infrastructure_check_radius = 2;
    @Entry(category = CHARTING, min = 0f, max = 1f) public static float path_quality_threshold = 0.3f;
    @Entry(category = CHARTING, min = 1) public static int node_distance_minimum = 4;
    @Entry(category = CHARTING, min = 2) public static int node_distance_maximum = 8;
    @Entry(category = CHARTING, min = 1) public static int node_utility_distance = 3;
    @Entry(category = CHARTING) public static List<String> path_block_ids = Lists.newArrayList(
            "minecraft:dirt_path", "minecraft:packed_mud", "minecraft:coarse_dirt", "minecraft:rooted_dirt"
    );
    @Entry(category = CHARTING) public static List<String> path_block_tags = Lists.newArrayList();
    @Entry(category = CHARTING) public static List<String> path_block_strings = Lists.newArrayList(
            "sandstone", "polished", "cobble", "brick", "smooth", "basalt", "path", "road", "concrete", "pavement", "glazed", "tile", "_wall", "_fence", "_slab", "_stairs", "_wool", "_carpet", "_plank", "_log", "_wood", "rail", "_button", "_pressure_plate"
    );
    @Entry(category = CHARTING) public static List<String> block_string_blacklist = Lists.newArrayList("diagonalwalls:", "diagonalfences:", "diagonalwindows:");
    @Entry(category = CHARTING) public static List<String> invalid_dimensions = Lists.newArrayList();
    @Entry(category = CHARTING) public static boolean no_gui_charting = false;
    @Comment(category = CHARTING) public static Comment charting_footer;

    @Entry(category = WARP) public static List<String> invalid_entities = Lists.newArrayList();
    @Entry(category = WARP) public static List<String> warp_block_ids = Lists.newArrayList("via_romana:warp_block");
    @Entry(category = WARP) public static List<String> warp_block_tags = Lists.newArrayList("minecraft:all_signs");
    @Entry(category = WARP, min = 0) public static int travel_fatigue_cooldown = 6;
    @Entry(category = WARP, min = 0f, max = 5f) public static double fast_movement_speed = 0.4D;
    @Entry(category = WARP) public static boolean direct_warp = false;
    @Comment(category = WARP) public static Comment warp_footer;

    @Entry(category = MAP, min = 1) public static int fog_of_war_distance = 10;
    @Entry(category = MAP, min = 0.5f, max = 10f) public static float spline_animation_time = 2.0f;
    @Entry(category = MAP, min = 128) public static int maximum_map_dimension = 512;
    @Entry(category = MAP, min = 0) public static int map_refresh_interval = 10;
    @Entry(category = MAP, min = 0) public static int map_refresh_threshold = 10;
    @Entry(category = MAP, min = 0) public static int map_save_interval = 5;
    @Entry(category = MAP) public static List<String> biome_color_pairs = Lists.newArrayList();
    @Entry(category = MAP) public static boolean use_biome_fallback_for_lowres = false;
    @Entry(category = MAP) public static boolean enable_remote_map_access = true;
    @Entry(category = MAP) public static boolean enable_surveyor_landmark = false;
    @Entry(category = MAP) public static boolean enable_surveyor_landmark_coloring = false;
    @Comment(category = MAP) public static Comment map_footer;

    @Entry(category = VISUALS, min = 0f, max = 1f) public static float invalid_block_overlay_opacity = 0.4f;
    @Entry(category = VISUALS, min = 0f, max = 1f) public static float biome_map_opacity = 0.3f;
    @Entry(category = VISUALS, min = 0f, max = 1f) public static float node_vignette_opacity = 1.0f;
    @Entry(category = VISUALS, isColor = true) public static List<String> line_colors = Lists.newArrayList("#ffffff", "#cccccc");
    @Entry(category = VISUALS, min = 0f, max = 1f) public static float line_opacity = 1.0f;
    @Entry(category = VISUALS) public static boolean enable_teleport_particles = true;
    @Entry(category = VISUALS) public static boolean enable_sign_particles = true;
    @Entry(category = VISUALS) public static boolean enable_custom_cursor = true;
    @Entry(category = VISUALS) public static LoggingEnum logging_enum = LoggingEnum.NONE;
    public enum LoggingEnum { NONE, ADMIN, DEBUG }

    @Override
    public void writeChanges() {
        super.writeChanges();
        ViaRomana.LOGGER.info("Via Romana config saved. Use /reload to update state");
        //TODO: Update relevant systems using a diff check (or similar)
    }
}