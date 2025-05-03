package net.rasanovum.viaromana.forge.configuration;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rasanovum.viaromana.variables.VariableAccess;
import net.rasanovum.viaromana.forge.init.ViaRomanaModConfigs;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

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
		
		ModConfigSpec.DoubleValue infrastructureRadius = (ModConfigSpec.DoubleValue) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "infrastructure_check_radius");
		VariableAccess.mapVariables.setInfrastructureCheckRadius(world, infrastructureRadius.get());

		ModConfigSpec.DoubleValue pathQualityThreshold = (ModConfigSpec.DoubleValue) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "path_quality_threshold");
		VariableAccess.mapVariables.setInfrastructureCheckQuality(world, pathQualityThreshold.get());

		ModConfigSpec.DoubleValue nodeDistMin = (ModConfigSpec.DoubleValue) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "node_distance_minimum");
		VariableAccess.mapVariables.setNodeDistanceMinimum(world, nodeDistMin.get());

		ModConfigSpec.DoubleValue nodeDistMax = (ModConfigSpec.DoubleValue) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "node_distance_maximum");
		VariableAccess.mapVariables.setNodeDistanceMaximum(world, nodeDistMax.get());

		ModConfigSpec.DoubleValue pathDistMin = (ModConfigSpec.DoubleValue) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "path_distance_minimum");
		VariableAccess.mapVariables.setPathDistanceMinimum(world, pathDistMin.get());

		ModConfigSpec.DoubleValue pathDistMax = (ModConfigSpec.DoubleValue) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "path_distance_maximum");
		VariableAccess.mapVariables.setPathDistanceMaximum(world, pathDistMax.get());

		ModConfigSpec.DoubleValue travelFatigue = (ModConfigSpec.DoubleValue) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "travel_fatigue_cooldown");
		VariableAccess.mapVariables.setTravelFatigueCooldown(world, travelFatigue.get());
		
		@SuppressWarnings("unchecked")
		ModConfigSpec.ConfigValue<String> validTags = (ModConfigSpec.ConfigValue<String>) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "valid_block_tags");
		valid_tag_string = validTags.get();
		
		@SuppressWarnings("unchecked")
		ModConfigSpec.ConfigValue<String> validBlockIds = (ModConfigSpec.ConfigValue<String>) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "valid_block_ids");
		valid_id_string = validBlockIds.get();
		
		@SuppressWarnings("unchecked")
		ModConfigSpec.ConfigValue<String> validBlockStrings = (ModConfigSpec.ConfigValue<String>) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "valid_block_strings");
		valid_name_string = validBlockStrings.get();
		
		@SuppressWarnings("unchecked")
		ModConfigSpec.ConfigValue<String> validDimensions = (ModConfigSpec.ConfigValue<String>) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "valid_dimensions");
		valid_dimension_string = validDimensions.get();
		
		@SuppressWarnings("unchecked")
		ModConfigSpec.ConfigValue<String> validEntities = (ModConfigSpec.ConfigValue<String>) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "valid_entities");
		valid_entity_string = validEntities.get();
		
		@SuppressWarnings("unchecked")
		ModConfigSpec.ConfigValue<String> validSignStrings = (ModConfigSpec.ConfigValue<String>) ViaRomanaModConfigs.getConfigValue(ConfigConfiguration.class, "valid_sign_strings");
		valid_sign_id_string = validSignStrings.get();
		
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
