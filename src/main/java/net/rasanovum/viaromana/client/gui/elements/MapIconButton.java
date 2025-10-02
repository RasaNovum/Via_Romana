package net.rasanovum.viaromana.client.gui.elements;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.util.VersionUtils;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.function.Consumer;

/**
 * Custom icon button for the LinkSignScreen that displays a scaled icon
 */
public class MapIconButton extends AbstractButton {
    private final Node.Icon icon;
    private final Consumer<Node.Icon> onPress;
    
    private boolean isSelected = false;
    private static final int ICON_SIZE = 16;
    public static final int CIRCLE_SIZE = 32;

    public MapIconButton(Font font, int x, int y, Node.Icon icon, Consumer<Node.Icon> onPress) {
        super(x, y, CIRCLE_SIZE, CIRCLE_SIZE, Component.empty());
        this.icon = icon;
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        if (this.onPress != null) {
            this.onPress.accept(this.icon);
        }
    }

    public Node.Icon getIcon() {
        return this.icon;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    private ResourceLocation getIconTexture(Node.Icon icon) {
        String textureName = switch (icon) {
            case SIGNPOST -> "marker_signpost";
            case HOUSE -> "marker_house";
            case SHOP -> "marker_shop";
            case TOWER -> "marker_tower";
            case CAVE -> "marker_cave";
            case CROP -> "marker_crop";
            case PORTAL -> "marker_portal";
            case BOOK -> "marker_book";
        };
        return VersionUtils.getLocation("via_romana:textures/screens/" + textureName + ".png");
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }

        int centerX = this.getX() + this.width / 2;
        int centerY = this.getY() + this.height / 2;

        ResourceLocation iconTexture = getIconTexture(this.icon);
        int iconX = centerX - ICON_SIZE / 2;
        int iconY = centerY - ICON_SIZE / 2;
        guiGraphics.blit(iconTexture, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        if (this.isSelected || this.isHovered()) {
            ResourceLocation circleTexture = VersionUtils.getLocation("via_romana:textures/screens/element_circle.png");
            int circleX = centerX - CIRCLE_SIZE / 2;
            int circleY = centerY - CIRCLE_SIZE / 2;
            
            if (this.isSelected) {
                guiGraphics.blit(circleTexture, circleX, circleY, 0, 0, CIRCLE_SIZE, CIRCLE_SIZE, CIRCLE_SIZE, CIRCLE_SIZE);
            } else {
                guiGraphics.pose().pushPose();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.3f);
                guiGraphics.blit(circleTexture, circleX, circleY, 0, 0, CIRCLE_SIZE, CIRCLE_SIZE, CIRCLE_SIZE, CIRCLE_SIZE);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.disableBlend();
                guiGraphics.pose().popPose();
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
