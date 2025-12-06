package net.rasanovum.viaromana.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class LinkedSignToast implements Toast {
    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/recipe");
    
    private final Component title;
    private final Component description;
    private final ItemStack icon;

    public LinkedSignToast(Component title, Component description) {
        this.title = title;
        this.description = description;
        this.icon = new ItemStack(Items.OAK_SIGN);
    }

    @Override
    public @NotNull Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        guiGraphics.blitSprite(BACKGROUND_SPRITE, 0, 0, 160, 32);
        guiGraphics.drawString(toastComponent.getMinecraft().font, this.description, 30, 18, -16777216, false);
        guiGraphics.drawString(toastComponent.getMinecraft().font, this.title, 30, 7, -11534256, false);
        guiGraphics.renderFakeItem(this.icon, 8, 8);
        return timeSinceLastVisible >= 7500L ? Visibility.HIDE : Visibility.SHOW;
    }
}