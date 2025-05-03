package net.rasanovum.viaromana.forge.configuration;

import net.neoforged.neoforge.common.ModConfigSpec;

// TODO: Port to cloth config for pairity with fabric, or at least abstract the variables
public class ConfigConfiguration {
	public final ModConfigSpec.DoubleValue infrastructure_check_radius;
	public final ModConfigSpec.DoubleValue node_distance_minimum;
	public final ModConfigSpec.DoubleValue node_distance_maximum;
	public final ModConfigSpec.DoubleValue path_quality_threshold;
	public final ModConfigSpec.DoubleValue path_distance_minimum;
	public final ModConfigSpec.DoubleValue path_distance_maximum;
	public final ModConfigSpec.DoubleValue travel_fatigue_cooldown;

	public final ModConfigSpec.ConfigValue<String> valid_entities;
	public final ModConfigSpec.ConfigValue<String> valid_dimensions;
	public final ModConfigSpec.ConfigValue<String> valid_block_tags;
	public final ModConfigSpec.ConfigValue<String> valid_block_ids;
	public final ModConfigSpec.ConfigValue<String> valid_block_strings;
	public final ModConfigSpec.ConfigValue<String> valid_sign_strings;

	public ConfigConfiguration(ModConfigSpec.Builder builder) {
		builder.comment("Via Romana Configuration").push("Variables");

		infrastructure_check_radius = builder
			.comment("Radius to check for nearby infrastructure.")
			.defineInRange("infrastructure_check_radius", 1.0, 0.1, 10.0);
		node_distance_minimum = builder
			.comment("Minimum distance between path nodes.")
			.defineInRange("node_distance_minimum", 10.0, 1.0, 100.0);
		node_distance_maximum = builder
			.comment("Maximum distance between path nodes.")
			.defineInRange("node_distance_maximum", 20.0, 1.0, 200.0);
		path_quality_threshold = builder
			.comment("Minimum path quality threshold for pathfinding.")
			.defineInRange("path_quality_threshold", 0.6, 0.0, 1.0);
		path_distance_minimum = builder
			.comment("Minimum distance for a valid path segment.")
			.defineInRange("path_distance_minimum", 10.0, 1.0, 1000.0);
		path_distance_maximum = builder
			.comment("Maximum distance for a valid path segment.")
			.defineInRange("path_distance_maximum", 100000.0, 100.0, Double.MAX_VALUE);
		travel_fatigue_cooldown = builder
			.comment("Cooldown in seconds for travel fatigue effect.")
			.defineInRange("travel_fatigue_cooldown", 30.0, 0.0, 3600.0);

		builder.pop();
		builder.push("Management");

		valid_entities = builder
			.comment("Comma-separated list of entity IDs allowed to use paths (e.g., minecraft:player, minecraft:horse).")
			.define("valid_entities", "minecraft:player, minecraft:boat, minecraft:horse, minecraft:pig, minecraft:strider, minecraft:camel");
		valid_dimensions = builder
			.comment("Comma-separated list of dimension IDs where the mod operates (e.g., minecraft:overworld).")
			.define("valid_dimensions", "minecraft:overworld, minecraft:the_end");
		valid_block_tags = builder
			.comment("Comma-separated list of block tags considered valid path blocks (e.g., via_romana:path_block).")
			.define("valid_block_tags", "via_romana:path_block");
		valid_block_ids = builder
			.comment("Comma-separated list of specific block IDs considered valid path blocks (e.g., minecraft:stone_bricks).")
			.define("valid_block_ids", "");
		valid_block_strings = builder
			.comment("Comma-separated list of block state strings considered valid path blocks (e.g., minecraft:cobblestone_slab[type=bottom]).")
			.define("valid_block_strings", "");
		valid_sign_strings = builder
			.comment("Comma-separated list of sign block IDs used for nodes (e.g., supplementaries:sign_post).")
			.define("valid_sign_strings", "supplementaries:sign_post");

		builder.pop();
	}
}
