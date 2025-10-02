package net.rasanovum.viaromana.util;

import net.rasanovum.viaromana.CommonConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.rasanovum.viaromana.ViaRomana;

public class EffectUtils {
    public static void applyEffect(Entity entity, String effectName, LevelAccessor world) {
        if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
            //? if <1.21 {
            /*MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(ViaRomana.MODID, effectName));
            *///?} else {
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(VersionUtils.getLocation(ViaRomana.MODID, effectName)).orElse(null);
            //?}

            if (effect != null) {
                _entity.addEffect(new MobEffectInstance(effect, CommonConfig.travel_fatigue_cooldown, 0, false, false));
            } else {
                System.err.println("Failed to apply effect: " + effectName + " - Effect not found in registry");
            }
        }
    }
}