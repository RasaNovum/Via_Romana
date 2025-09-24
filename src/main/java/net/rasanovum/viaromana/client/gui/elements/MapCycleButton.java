package net.rasanovum.viaromana.client.gui.elements;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.client.gui.GuiConstants;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Custom cycle button for the LinkSignScreen that removes drop shadows
 */
public class MapCycleButton<T> extends AbstractButton {
    private final List<T> values;
    private final Consumer<T> onValueChange;
    private final java.util.function.Function<T, Component> displayFunction;
    private final Font font;
    private final Component tooltip;
    
    private int selectedIndex = 0;
    private boolean isSelected = false;
    private Runnable customOnPress = null;

    public MapCycleButton(Font font, int x, int y, int width, int height, 
                         List<T> values, T initialValue, 
                         java.util.function.Function<T, Component> displayFunction,
                         Consumer<T> onValueChange) {
        this(font, x, y, width, height, values, initialValue, displayFunction, onValueChange, null);
    }

    public MapCycleButton(Font font, int x, int y, int width, int height, 
                         List<T> values, T initialValue, 
                         java.util.function.Function<T, Component> displayFunction,
                         Consumer<T> onValueChange,
                         Component tooltip) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.values = values;
        this.displayFunction = displayFunction;
        this.onValueChange = onValueChange;
        this.tooltip = tooltip;

        if (this.tooltip != null && !this.tooltip.getString().isEmpty()) {
            this.setTooltip(Tooltip.create(tooltip));
        }
        
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(initialValue)) {
                this.selectedIndex = i;
                break;
            }
        }
    }

    @Override
    public void onPress() {
        if (this.customOnPress != null) {
            this.customOnPress.run();
        } else {
            this.selectedIndex = (this.selectedIndex + 1) % this.values.size();
            T newValue = this.values.get(this.selectedIndex);
            if (this.onValueChange != null) {
                this.onValueChange.accept(newValue);
            }
        }
    }

    public void setOnPress(Runnable onPress) {
        this.customOnPress = onPress;
    }

    public T getValue() {
        return this.values.get(this.selectedIndex);
    }

    public void setValue(T value) {
        for (int i = 0; i < this.values.size(); i++) {
            if (this.values.get(i).equals(value)) {
                this.selectedIndex = i;
                break;
            }
        }
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }

        int currentTextColor = GuiConstants.TEXT_COLOR_PRIMARY;
        int checkboxSize = 8;
        int checkboxX = this.getX() + 6;
        int checkboxY = this.getY() - 1 + (this.height - checkboxSize) / 2;
        
        // Draw checkbox background
        // guiGraphics.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, 0xFFF0F0F0);
        
        guiGraphics.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + 1, 0xFF000000 | GuiConstants.BORDER_COLOR_PRIMARY);
        guiGraphics.fill(checkboxX, checkboxY + checkboxSize - 1, checkboxX + checkboxSize, checkboxY + checkboxSize, 0xFF000000 | GuiConstants.BORDER_COLOR_PRIMARY);
        guiGraphics.fill(checkboxX, checkboxY, checkboxX + 1, checkboxY + checkboxSize, 0xFF000000 | GuiConstants.BORDER_COLOR_PRIMARY);
        guiGraphics.fill(checkboxX + checkboxSize - 1, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, 0xFF000000 | GuiConstants.BORDER_COLOR_PRIMARY);
        
        if (this.isSelected || this.isHovered()) {
            int iconSize = 16;
            int iconX = checkboxX - 3;
            int iconY = checkboxY - 6;

            if (this.isSelected) {
                guiGraphics.blit(ResourceLocation.parse("via_romana:textures/screens/element_check.png"), iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
            } else {
                guiGraphics.pose().pushPose();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.3f);
                guiGraphics.blit(ResourceLocation.parse("via_romana:textures/screens/element_check.png"), iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.disableBlend();
                guiGraphics.pose().popPose();
            }
        }

        Component displayText = this.displayFunction.apply(this.getValue());
        String text = displayText.getString();
        int textX = checkboxX + checkboxSize + 6;
        int textY = this.getY() + (this.height - this.font.lineHeight) / 2;
        
        guiGraphics.drawString(this.font, text, textX, textY, currentTextColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
