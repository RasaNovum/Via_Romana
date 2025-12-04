package net.rasanovum.viaromana.mixins.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.rasanovum.viaromana.speed.FieldOfViewHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin extends Player {

    public AbstractClientPlayerMixin(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, float rot, com.mojang.authlib.GameProfile profile) {
        super(level, pos, rot, profile);
    }

    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    public void modifyFov(CallbackInfoReturnable<Float> cir) {
        if (FieldOfViewHelper.shouldIgnoreProximitySpeed(this)) {
            float original = cir.getReturnValue();
            float correction = FieldOfViewHelper.getProximityFovCorrection(this);
            cir.setReturnValue(original * correction);
        }
    }
}