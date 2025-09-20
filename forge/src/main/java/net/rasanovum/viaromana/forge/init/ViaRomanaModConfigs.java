package net.rasanovum.viaromana.forge.init;

import net.rasanovum.viaromana.forge.configuration.ConfigConfiguration;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import java.lang.reflect.Field;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

public class ViaRomanaModConfigs {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ConfigConfiguration COMMON;

    static {
        final Pair<ConfigConfiguration, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ConfigConfiguration::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    @SuppressWarnings("removal")
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

    public static Object getConfigValue(Class<?> configClass, String fieldName) {
        try {
            if (configClass == ConfigConfiguration.class) {
                Field field = ConfigConfiguration.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(COMMON);
            }
            return null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            ViaRomanaMod.LOGGER.error("Failed to get config value '{}': {}", fieldName, e.getMessage());
            return null;
        }
    }
}
