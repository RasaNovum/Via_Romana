package net.rasanovum.viaromana.mixins.client;

import net.mehvahdjukaar.supplementaries.client.screens.SignPostScreen;
import net.mehvahdjukaar.supplementaries.client.screens.TextHolderEditScreen;
import net.minecraft.client.gui.screens.Screen;
import net.rasanovum.viaromana.util.SignEditHelper;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
//?}

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(TextHolderEditScreen.class)
public abstract class TextHolderEditScreenMixin extends Screen {

    protected TextHolderEditScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init()V", at = @At("TAIL"), remap = false)
    private void addLinkButton(CallbackInfo ci) {
        if ((Object) this instanceof SignPostScreen) {
            Button linkButton = SignEditHelper.createLinkButton((SignPostScreen) (Object) this);
            if (linkButton != null) this.addRenderableWidget(linkButton);
        }
    }

    // Fixed in Supplementaries 3.1.38
    //? if fabric {
    // Match vanilla abstract sign positioning
    @ModifyArgs(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button$Builder;bounds(IIII)Lnet/minecraft/client/gui/components/Button$Builder;", remap = false))
    private void adjustDoneButtonPosition(Args args) {
        try {
            Version fixVersion = Version.parse("1.20-3.1.38");

            FabricLoader.getInstance().getModContainer("supplementaries").ifPresent(modContainer -> {
                Version installedVersion = modContainer.getMetadata().getVersion();

                if (installedVersion.compareTo(fixVersion) < 0) {
                    int originalY = args.get(1);
                    args.set(1, originalY + 24);
                }
            });
        } catch (VersionParsingException e) {
            e.printStackTrace();
        }
    }
    //?}

    // Disables pausing when editing signpost
    // public boolean isPauseScreen() {
    //     return false;
    // }
}