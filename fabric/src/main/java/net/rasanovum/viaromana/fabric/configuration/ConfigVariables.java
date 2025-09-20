package net.rasanovum.viaromana.fabric.configuration;

import net.rasanovum.viaromana.variables.VariableAccess;
import net.rasanovum.viaromana.fabric.ViaRomanaMod;
import net.rasanovum.viaromana.fabric.init.ViaRomanaModConfigs;
import net.minecraft.world.level.LevelAccessor;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class ConfigVariables {
	public static void load(LevelAccessor world) {
		String valid_tag_string = "";
		String valid_id_string = "";
		String valid_name_string = "";
		String valid_dimension_string = "";
		String valid_entity_string = "";
		String valid_sign_id_string = "";
		
		VariableAccess.mapVariables.setInfrastructureCheckRadius(world, (double) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "infrastructure_check_radius"));
		VariableAccess.mapVariables.setInfrastructureCheckQuality(world, (double) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "path_quality_threshold"));
		VariableAccess.mapVariables.setNodeDistanceMinimum(world, (double) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "node_distance_minimum"));
		VariableAccess.mapVariables.setNodeDistanceMaximum(world, (double) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "node_distance_maximum"));
		VariableAccess.mapVariables.setPathDistanceMinimum(world, (double) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "path_distance_minimum"));
		VariableAccess.mapVariables.setPathDistanceMaximum(world, (double) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "path_distance_maximum"));
		VariableAccess.mapVariables.setTravelFatigueCooldown(world, (double) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "travel_fatigue_cooldown"));
		
		valid_tag_string = (String) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "valid_block_tags");
		valid_id_string = (String) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "valid_block_ids");
		valid_name_string = (String) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "valid_block_strings");
		valid_dimension_string = (String) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "valid_dimensions");
		valid_entity_string = (String) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "valid_entities");
		valid_sign_id_string = (String) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "valid_sign_strings");
		
		VariableAccess.mapVariables.setValidTagList(createListFromCommaSeparated(valid_tag_string));
		VariableAccess.mapVariables.setValidBlockList(world, createListFromCommaSeparated(valid_id_string));
		VariableAccess.mapVariables.setValidStringList(world, createListFromCommaSeparated(valid_name_string));
		VariableAccess.mapVariables.setValidDimensionList(world, createListFromCommaSeparated(valid_dimension_string));
		VariableAccess.mapVariables.setValidEntityList(world, createListFromCommaSeparated(valid_entity_string));
		VariableAccess.mapVariables.setValidSignList(world, createListFromCommaSeparated(valid_sign_id_string));

		VariableAccess.mapVariables.markAndSync(world);
		
		ViaRomanaMod.LOGGER.info("Config Loaded");
	}
	
	private static List<Object> createListFromCommaSeparated(String commaSeparated) {
		return new ArrayList<>(Arrays.asList(commaSeparated.replace(" ", "").split(",")));
	}
}
