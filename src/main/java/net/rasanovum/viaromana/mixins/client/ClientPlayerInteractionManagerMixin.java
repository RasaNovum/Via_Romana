package net.rasanovum.viaromana.mixins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.rasanovum.viaromana.core.SignInteract;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Shadow @Final private Minecraft minecraft;

    private boolean signAttackCancelled = false;

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void viaRomana_onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (this.minecraft.player != null && this.minecraft.level != null) {
            boolean shouldCancel = SignInteract.clicked(this.minecraft.level, pos, this.minecraft.player);
            this.signAttackCancelled = shouldCancel;
            if (shouldCancel) cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void viaRomana_onContinueDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (this.signAttackCancelled) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"))
    private void viaRomana_onStopDestroyBlock(CallbackInfo ci) {
        this.signAttackCancelled = false;
    }
}