package net.rasanovum.viaromana.util;

import net.rasanovum.viaromana.configuration.ViaRomanaConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.registries.BuiltInRegistries;

public class EffectUtils {
    public static void applyEffect(Entity entity, String effectName, LevelAccessor world) {
        if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
            Holder<MobEffect> effect = getEffectByName(effectName);
            if (effect != null) {
                _entity.addEffect(new MobEffectInstance(effect, ViaRomanaConfig.travel_fatigue_cooldown, 0, false, false));
            } else {
                System.err.println("Failed to apply effect: " + effectName + " - Effect not found in registry");
            }
        }
    }

    public static boolean hasEffect(Entity entity, String effectName) {
        if (entity instanceof LivingEntity _entity) {
            Holder<MobEffect> effect = getEffectByName(effectName);
            return effect != null && _entity.hasEffect(effect);
        }
        return false;
    }

    private static Holder<MobEffect> getEffectByName(String effectName) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("via_romana:" + effectName)).orElse(null);
    }
}
