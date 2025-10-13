package net.rasanovum.viaromana.mixins;

import net.minecraft.nbt.NbtIo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.rasanovum.viaromana.util.EffectUtils;

@Mixin(LivingEntity.class)
public abstract class PreventFatigueRemovalMixin {
    @Unique
    private MobEffectInstance viaRomana$storedFatigueEffect = null;

    @Inject(method = "removeEffect", at = @At("HEAD"), cancellable = true, remap = false)
            //? if <1.21 {
    /*private void viaRomana$preventNonCommandRemoval(MobEffect effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect == EffectUtils.getEffect("travellers_fatigue")) {
    *///?} else {
    private void viaRomana$preventNonCommandRemoval(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        Holder<MobEffect> fatigueEffect = EffectUtils.getEffect("travellers_fatigue");
        if (fatigueEffect != null && effect.value() == fatigueEffect.value()) {
            //?}
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                String methodName = element.getMethodName();
                if (className.contains("command") || className.contains("Command") ||
                        className.contains("commands") || className.contains("Commands") ||
                        methodName.contains("command") || methodName.contains("execute") ||
                        className.contains("net.minecraft.server.commands") ||
                        className.contains("brigadier")) {
                    return;
                }
            }
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "removeAllEffects", at = @At("HEAD"), remap = false)
    private void viaRomana$storeFatigueEffect(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        //? if <1.21 {
        /*MobEffectInstance fatigueEffect = self.getEffect(EffectUtils.getEffect("travellers_fatigue"));
         *///?} else {
        MobEffectInstance fatigueEffect = self.getEffect(EffectUtils.getEffect("travellers_fatigue"));
        //?}

        if (fatigueEffect != null) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                String methodName = element.getMethodName();
                if (className.contains("command") || className.contains("Command") ||
                        className.contains("commands") || className.contains("Commands") ||
                        methodName.contains("command") || methodName.contains("execute") ||
                        className.contains("net.minecraft.server.commands") ||
                        className.contains("brigadier")) {
                    return;
                }
            }

            viaRomana$storedFatigueEffect = new MobEffectInstance(
                    fatigueEffect.getEffect(),
                    fatigueEffect.getDuration(),
                    fatigueEffect.getAmplifier(),
                    fatigueEffect.isAmbient(),
                    fatigueEffect.isVisible()
            );
        }
    }

    @Inject(method = "removeAllEffects", at = @At("RETURN"), remap = false)
    private void viaRomana$restoreTravellerFatigue(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        //? if <1.21 {
        /*if (cir.getReturnValue() && viaRomana$storedFatigueEffect != null && !self.hasEffect(EffectUtils.getEffect("travellers_fatigue"))) {
         *///?} else {
        if (cir.getReturnValue() && viaRomana$storedFatigueEffect != null && !self.hasEffect(EffectUtils.getEffect("travellers_fatigue"))) {
            //?}
            self.addEffect(viaRomana$storedFatigueEffect);
            viaRomana$storedFatigueEffect = null;
        }
    }
}