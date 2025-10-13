package net.rasanovum.viaromana.util;

import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.CommonConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.rasanovum.viaromana.ViaRomana;

public class EffectUtils {
    public static void applyEffect(Entity entity, String effectName) {
        if (CommonConfig.travel_fatigue_cooldown == 0) return;
        if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
            if (getEffect(effectName) != null) {
                _entity.addEffect(new MobEffectInstance(getEffect(effectName), (CommonConfig.travel_fatigue_cooldown * 20), 0, false, false));
            } else {
                ViaRomana.LOGGER.warn("Failed to apply effect: {} not found in registry", effectName);
            }
        }
    }

    public static boolean hasEffect(Entity entity, String effectName) {
        if (entity instanceof LivingEntity _entity) {
            if (getEffect(effectName) != null) {
                return _entity.hasEffect(getEffect(effectName));
            } else {
                ViaRomana.LOGGER.warn("Failed to detect effect: {} not found in registry", effectName);
            }
        }

        return false;
    }

    //? if <1.21 {
    /*private static MobEffect getEffect(String effectName) {
        return BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(ViaRomana.MODID, effectName));
    }
     *///?} else {
    public static Holder<MobEffect> getEffect(String effectName) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(VersionUtils.getLocation(ViaRomana.MODID, effectName)).orElse(null);
    }
    //?}
}