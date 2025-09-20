package net.rasanovum.viaromana.fabric.init;

import net.rasanovum.viaromana.fabric.configuration.ConfigConfiguration;

import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.AutoConfig;

import java.lang.reflect.Field;

public class ViaRomanaModConfigs {
	public static void register() {
		AutoConfig.register(ConfigConfiguration.class, Toml4jConfigSerializer::new);
	}

	public static <T extends ConfigData> T getConfig(Class<T> configClass) {
		return AutoConfig.getConfigHolder(configClass).getConfig();
	}

	public static <T extends ConfigData> Object getConfigValue(Class<T> configClass, String fieldName) {
		T config = getConfig(configClass);
		for (Field field : configClass.getDeclaredFields()) {
			field.setAccessible(true);
			try {
				Object category = field.get(config);
				if (category != null) {
					for (Field categoryField : category.getClass().getDeclaredFields()) {
						categoryField.setAccessible(true);
						if (categoryField.getName().equals(fieldName)) {
							return categoryField.get(category);
						}
					}
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
