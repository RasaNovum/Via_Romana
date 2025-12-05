package net.rasanovum.viaromana.mixins.client;

import com.mojang.blaze3d.systems.RenderSystem;
//? if >1.21
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.rasanovum.viaromana.client.render.LinkIndicationHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin {
    //? >1.21 {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void viaRomana_cancelCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (LinkIndicationHandler.getCrosshairAlpha() <= 0.01f) {
            ci.cancel();
        }
    }
    //?} else {
    /*@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void viaRomana_cancelCrosshair(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (LinkIndicationHandler.getCrosshairAlpha() <= 0.01f) {
            ci.cancel();
        }
    }
    *///?}

    //? >1.21 {
    @Inject(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void viaRomana_preDrawCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
    private void viaRomana_preDrawCrosshair(GuiGraphics guiGraphics, CallbackInfo ci) {
    *///?}
        float alpha = LinkIndicationHandler.getCrosshairAlpha();
        if (alpha < 1.0f) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    //? >1.21
    private void viaRomana_resetCrosshairAlpha(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    //? <1.21
    /*private void viaRomana_resetCrosshairAlpha(GuiGraphics guiGraphics, CallbackInfo ci) {*/
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.defaultBlendFunc();
    }
}