package net.rasanovum.viaromana.fabric.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.rasanovum.viaromana.fabric.init.ViaRomanaModMobEffects;

@Mixin(LivingEntity.class)
public abstract class PreventFatigueRemovalMixin {
    @Inject(method = "removeEffect", at = @At("HEAD"), cancellable = true)
    private void viaRomana$keepTravellerFatigue(MobEffect effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect == ViaRomanaModMobEffects.TRAVELLERS_FATIGUE) {
            cir.setReturnValue(false);
        }
    }
}
