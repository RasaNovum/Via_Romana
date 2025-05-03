package net.rasanovum.viaromana.fabric.configuration;

import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.ConfigData;

@Config(name = "via_romana")
public class ConfigConfiguration implements ConfigData {
	@ConfigEntry.Gui.CollapsibleObject
	public VariablesCategory Variables = new VariablesCategory();

	public static class VariablesCategory {
		@ConfigEntry.Gui.Tooltip
		public double infrastructure_check_radius = 1;
		@ConfigEntry.Gui.Tooltip
		public double node_distance_minimum = 10;
		@ConfigEntry.Gui.Tooltip
		public double node_distance_maximum = 20;
		@ConfigEntry.Gui.Tooltip
		public double path_quality_threshold = 0.6;
		@ConfigEntry.Gui.Tooltip
		public double path_distance_minimum = 10;
		@ConfigEntry.Gui.Tooltip
		public double path_distance_maximum = 100000;
		@ConfigEntry.Gui.Tooltip
		public double travel_fatigue_cooldown = 30;
	}

	@ConfigEntry.Gui.CollapsibleObject
	public ManagementCategory Management = new ManagementCategory();

	public static class ManagementCategory {
		@ConfigEntry.Gui.Tooltip
		public String valid_entities = "minecraft:player, minecraft:boat, minecraft:horse, minecraft:pig, minecraft:strider, minecraft:camel";
		@ConfigEntry.Gui.Tooltip
		public String valid_dimensions = "minecraft:overworld, minecraft:the_end";
		@ConfigEntry.Gui.Tooltip
		public String valid_block_tags = "via_romana:path_block";
		@ConfigEntry.Gui.Tooltip
		public String valid_block_ids = "";
		@ConfigEntry.Gui.Tooltip
		public String valid_block_strings = "";
		@ConfigEntry.Gui.Tooltip
		public String valid_sign_strings = "supplementaries:sign_post";
	}
}
