package net.rasanovum.viaromana.client.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ForceTooltipButton extends Button {

    protected final Component tooltip;

    public ForceTooltipButton(int x, int y, int width, int height, Component message, OnPress onPress, Component tooltip) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.tooltip = tooltip;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(graphics, mouseX, mouseY, partialTicks);

        if (this.isHoveredOrFocused()) {
            graphics.renderTooltip(Minecraft.getInstance().font, this.tooltip, mouseX, mouseY);
        }
    }
}