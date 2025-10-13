package net.rasanovum.viaromana.mixins.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.rasanovum.viaromana.client.gui.WarpBlockScreen;
import net.rasanovum.viaromana.util.SignEditHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WarpBlockScreen.class)
public abstract class WarpBlockScreenMixin extends Screen {

    protected WarpBlockScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void onInit(CallbackInfo ci) {
        Button linkButton = SignEditHelper.createLinkButton(this);
        if (linkButton != null) {
            this.addRenderableWidget(linkButton);
        }
    }
}
