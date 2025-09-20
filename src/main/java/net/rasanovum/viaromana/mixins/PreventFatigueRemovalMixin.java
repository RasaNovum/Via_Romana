package net.rasanovum.viaromana.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.rasanovum.viaromana.init.EffectInit;

@Mixin(LivingEntity.class)
public abstract class PreventFatigueRemovalMixin {
    @Unique
    private MobEffectInstance viaRomana$storedFatigueEffect = null;
    
    @Inject(method = "removeEffect", at = @At("HEAD"), cancellable = true)
    private void viaRomana$preventNonCommandRemoval(MobEffect effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect == EffectInit.TRAVELLERS_FATIGUE) {
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
    
    @Inject(method = "removeAllEffects", at = @At("HEAD"))
    private void viaRomana$storeFatigueEffect(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        MobEffectInstance fatigueEffect = self.getEffect(EffectInit.TRAVELLERS_FATIGUE);
        
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
    
    @Inject(method = "removeAllEffects", at = @At("RETURN"))
    private void viaRomana$restoreTravellerFatigue(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (cir.getReturnValue() && viaRomana$storedFatigueEffect != null && !self.hasEffect(EffectInit.TRAVELLERS_FATIGUE)) {
            self.addEffect(viaRomana$storedFatigueEffect);
            viaRomana$storedFatigueEffect = null;
        }
    }
}
