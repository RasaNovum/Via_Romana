package net.rasanovum.viaromana.client.gui.elements;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.rasanovum.viaromana.client.gui.GuiConstants;

import java.util.function.Consumer;

/**
 * Custom signature button for the LinkSignScreen that appears as a bottom line
 * and displays the player's username when hovered.
 */
public class MapSignatureButton extends AbstractButton {
    private final String playerName;
    private final Consumer<Void> onPress;
    private static final int LINE_HEIGHT = 1;

    public MapSignatureButton(Font font, int x, int y, int width, String playerName, Consumer<Void> onPress) {
        super(x, y, width, 20, Component.literal(""));
        this.playerName = playerName;
        this.onPress = onPress;
        
        this.setTooltip(Tooltip.create(Component.translatable("gui.viaromana.confirm_link_tooltip")));
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

        Font font = net.minecraft.client.Minecraft.getInstance().font;

        int lineY = this.getY() + this.height - LINE_HEIGHT - 2;
        guiGraphics.fill(this.getX(), lineY, this.getX() + this.width, lineY + LINE_HEIGHT, 0xFF000000);

        if (this.isHovered()) {
            String displayText = playerName;
            int textX = this.getX() + 5;
            int textY = lineY - font.lineHeight - 2;
            
            guiGraphics.drawString(font, displayText, textX, textY, GuiConstants.HOVER_TEXT_COLOR, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
