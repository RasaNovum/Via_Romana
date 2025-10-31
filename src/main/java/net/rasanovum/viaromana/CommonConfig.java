package net.rasanovum.viaromana;

import eu.midnightdust.lib.config.MidnightConfig;
import java.util.List;

import com.google.common.collect.Lists;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackResources;
import net.rasanovum.viaromana.tags.TagGenerator;

public class CommonConfig extends MidnightConfig {
    //? if >1.21 {
    @Override
    public void writeChanges(String modid) {
        System.out.println("Config save triggered for mod: " + modid);
        super.writeChanges(modid);

        MinecraftServer server = ViaRomana.getServer();
        if (server == null) return;

        server.execute(() -> {
            ViaRomana.LOGGER.info("Reloading server resources due to config change...");
            server.reloadResources(server.getResourceManager().listPacks().map(PackResources::packId).toList());
        });
    }
    //?}

    public static final String CHARTING = "charting";
    public static final String MAP = "map";
    public static final String WARP = "warp";
    public static final String VISUALS = "visuals";

    @Entry(category = CHARTING, min = 1) public static int infrastructure_check_radius = 2;
    @Entry(category = CHARTING, min = 0f, max = 1f) public static float path_quality_threshold = 0.3f;
    @Entry(category = CHARTING, min = 1) public static int node_distance_minimum = 5;
    @Entry(category = CHARTING, min = 2) public static int node_distance_maximum = 10;
    @Entry(category = CHARTING, min = 1) public static int node_utility_distance = 3;
    @Entry(category = CHARTING) public static List<String> path_block_ids = Lists.newArrayList(
            "minecraft:dirt_path", "minecraft:packed_mud", "minecraft:coarse_dirt", "minecraft:rooted_dirt"
    );
    @Entry(category = CHARTING) public static List<String> path_block_tags = Lists.newArrayList(
            "minecraft:slabs", "minecraft:stairs", "minecraft:walls", "minecraft:wool", "minecraft:wool_carpets", "minecraft:planks", "minecraft:logs",
            "minecraft:rails", "minecraft:buttons", "minecraft:pressure_plates",
            "c:slabs", "c:stairs", "c:walls", "c:wool", "c:wool_carpets", "c:planks", "c:logs", "c:rails", "c:buttons", "c:pressure_plates",
            "forge:slabs", "forge:stairs", "forge:walls", "forge:wool", "forge:wool_carpets", "forge:planks", "forge:logs", "forge:rails", "forge:buttons", "forge:pressure_plates"
    );
    @Entry(category = CHARTING) public static List<String> path_block_strings = Lists.newArrayList(
            "sandstone", "polished", "cobble", "brick", "smooth", "basalt", "path", "road", "concrete", "pavement", "glazed", "tile", "wall", "fence"
    );
    @Entry(category = CHARTING) public static List<String> block_string_blacklist = Lists.newArrayList("diagonalwalls:", "diagonalfences:", "diagonalwindows:");
    @Entry(category = CHARTING) public static List<String> invalid_dimensions = Lists.newArrayList();
    @Comment(category = CHARTING) public static Comment charting_footer;

    @Entry(category = WARP) public static List<String> invalid_entities = Lists.newArrayList();
    @Entry(category = WARP) public static List<String> warp_block_ids = Lists.newArrayList("via_romana:warp_block");
    @Entry(category = WARP) public static List<String> warp_block_tags = Lists.newArrayList("minecraft:all_signs");
    @Entry(category = WARP, min = 0) public static int travel_fatigue_cooldown = 10;
    @Comment(category = WARP) public static Comment warp_footer;

    @Entry(category = MAP, min = 1) public static int fog_of_war_distance = 10;
    @Entry(category = MAP, min = 0f, max = 200f) public static float spline_animation_speed = 2.0f;
    @Entry(category = MAP, min = 128) public static int maximum_map_dimension = 512;
    @Entry(category = MAP, min = 0) public static int map_refresh_interval = 10;
    @Entry(category = MAP, min = 0) public static int map_save_interval = 5;
    @Entry(category = MAP) public static List<String> biomeColor = Lists.newArrayList();
    @Entry(category = MAP) public static boolean use_biome_fallback_for_lowres = true;
    @Entry(category = MAP) public static boolean enable_surveyor_landmark = false;
    @Entry(category = MAP) public static boolean enable_surveyor_landmark_coloring = false;
    @Comment(category = MAP) public static Comment map_footer;

    @Entry(category = VISUALS, min = 0f, max = 1f) public static float invalid_block_overlay_opacity = 0.4f;
    @Entry(category = VISUALS, min = 0f, max = 1f) public static float biome_map_opacity = 0.4f;
    @Entry(category = VISUALS, min = 0f, max = 1f) public static float node_vignette_opacity = 1.0f;
    @Entry(category = VISUALS) public static boolean enable_teleport_particles = true;
}