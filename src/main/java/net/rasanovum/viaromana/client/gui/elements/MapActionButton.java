package net.rasanovum.viaromana.client.gui.elements;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.client.gui.GuiConstants;

import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

/**
 * Custom button for various actions with full borders, always visible text, optional image, and hover background.
 */
public class MapActionButton extends AbstractButton {
    private final Consumer<Void> onPress;
    @Nullable
    private final ResourceLocation imageLocation;
    private final boolean hasMessage;

    public MapActionButton(int x, int y, int width, int height, Component message, Component tooltip, Consumer<Void> onPress) {
        this(x, y, width, height, message, tooltip, onPress, null);
    }

    public MapActionButton(int x, int y, int width, int height, Consumer<Void> onPress, ResourceLocation imageLocation) {
        this(x, y, width, height, null, null, onPress, imageLocation);
    }

    public MapActionButton(int x, int y, int width, int height, @Nullable Component message, @Nullable Component tooltip, Consumer<Void> onPress, @Nullable ResourceLocation imageLocation) {
        super(x, y, width, height, message != null ? message : Component.empty());
        this.onPress = onPress;
        this.imageLocation = imageLocation;
        this.hasMessage = message != null;
        if (tooltip != null) this.setTooltip(Tooltip.create(tooltip));
    }

    @Override
    public void onPress() {
        if (this.onPress != null) {
            this.onPress.accept(null);
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }

        if (this.isHovered()) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, GuiConstants.HOVER_BACKGROUND_COLOR);
        }

        guiGraphics.hLine(this.getX(), this.getX() + this.width - 1, this.getY(), GuiConstants.BORDER_COLOR_PRIMARY);
        guiGraphics.hLine(this.getX(), this.getX() + this.width - 1, this.getY() + this.height - 1, GuiConstants.BORDER_COLOR_PRIMARY);
        guiGraphics.vLine(this.getX(), this.getY(), this.getY() + this.height - 1, GuiConstants.BORDER_COLOR_PRIMARY);
        guiGraphics.vLine(this.getX() + this.width - 1, this.getY(), this.getY() + this.height - 1, GuiConstants.BORDER_COLOR_PRIMARY);

        Font font = net.minecraft.client.Minecraft.getInstance().font;
        int imageX;
        int textY = this.getY() + ((this.height - font.lineHeight) / 2) + 1;

        int imageY = this.getY() + (this.height - 10) / 2;

        if (imageLocation != null) {
            if (!hasMessage) {
                imageX = this.getX() + (this.width - 17) / 2;
            } else {
                imageX = this.getX() + 5;
            }
            guiGraphics.blit(imageLocation, imageX, imageY, 0, 0, 17, 10, 17, 10);
        }

        if (hasMessage) {
            int textX = this.getX() + this.width - font.width(this.getMessage()) - 5;
            guiGraphics.drawString(font, this.getMessage(), textX, textY, GuiConstants.TEXT_COLOR_PRIMARY, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
