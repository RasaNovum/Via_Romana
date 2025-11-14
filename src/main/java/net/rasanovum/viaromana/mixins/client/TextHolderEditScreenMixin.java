package net.rasanovum.viaromana.mixins.client;

import net.mehvahdjukaar.supplementaries.client.screens.SignPostScreen;
import net.mehvahdjukaar.supplementaries.client.screens.TextHolderEditScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.rasanovum.viaromana.util.SignEditHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextHolderEditScreen.class)
public abstract class TextHolderEditScreenMixin extends Screen {

    protected TextHolderEditScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void signPostScreenInit(CallbackInfo ci) {
        if ((Object) this instanceof SignPostScreen) {
            Button linkButton = SignEditHelper.createLinkButton((SignPostScreen) (Object) this);
            if (linkButton != null) {
                this.addRenderableWidget(linkButton);
            }
        }

        GuiEventListener buttonWidget = this.children().get(0);

        if (buttonWidget instanceof Button doneButton) {
            int vanillaY = this.height / 4 + 144;
            if (doneButton.getY() != vanillaY) {
                doneButton.setY(vanillaY);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}