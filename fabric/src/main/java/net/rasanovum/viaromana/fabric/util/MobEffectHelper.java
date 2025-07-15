package net.rasanovum.viaromana.fabric.util;

import net.rasanovum.viaromana.fabric.ViaRomanaMod;
import net.rasanovum.viaromana.variables.VariableAccess;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

public class MobEffectHelper {
    public static void applyEffect(Entity entity, String effectName, LevelAccessor world) {
        if (entity instanceof LivingEntity _entity && !_entity.getLevel().isClientSide()) {
            MobEffect effect = getEffectByName(effectName);
            if (effect != null) {
                _entity.addEffect(new MobEffectInstance(effect, (int) VariableAccess.mapVariables.getTravelFatigueCooldown(world), 0, false, false));
            } else {
                System.err.println("Failed to apply effect: " + effectName + " - Effect not found in registry");
            }
        }
    }

    public static boolean hasEffect(Entity entity, String effectName) {
        if (entity instanceof LivingEntity _entity) {
            MobEffect effect = getEffectByName(effectName);
            return effect != null && _entity.hasEffect(effect);
        }
        return false;
    }

    private static MobEffect getEffectByName(String effectName) {
        return BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(ViaRomanaMod.MODID, effectName));
    }
}
