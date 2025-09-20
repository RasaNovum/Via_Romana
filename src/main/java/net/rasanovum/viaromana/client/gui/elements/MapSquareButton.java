package net.rasanovum.viaromana.client.gui.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

/**
 * A square button with animated image and text stroke support.
 */
public class MapSquareButton extends AbstractButton {

    private final Consumer<MapSquareButton> onPress;
    private final AnimationController animationController;
    private final boolean fixedTooltipPosition;
    private final float hoverBrightness;
    private final float clickBrightness;
    private boolean isPressed = false;

    @Nullable
    private ResourceLocation soundEffectLocation;

    private Component tooltipComponent;
    @Nullable
    private Component disabledTooltipComponent;
    private boolean frozen = false;

    private MapSquareButton(Builder builder) {
        super(builder.x, builder.y, builder.width, builder.height, builder.message);
        this.onPress = builder.onPress;
        this.tooltipComponent = builder.tooltip;
        this.disabledTooltipComponent = builder.disabledTooltip;
        this.fixedTooltipPosition = builder.fixedTooltipPosition;
        this.animationController = new AnimationController(builder);
        this.hoverBrightness = builder.hoverBrightness;
        this.clickBrightness = builder.clickBrightness;

        this.soundEffectLocation = builder.soundEffectLocation;

        updateTooltip();
    }

    // region Public API
    public void setDisabled(boolean disabled) {
        this.active = !disabled;
        updateTooltip();
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
        updateTooltip();
    }

    public void setTooltip(Component tooltip) {
        this.tooltipComponent = tooltip;
        updateTooltip();
    }

    public void setTooltips(Component tooltip, @Nullable Component disabledTooltip) {
        this.tooltipComponent = tooltip;
        this.disabledTooltipComponent = disabledTooltip;
        updateTooltip();
    }

    // region Overrides
    @Override
    public void onPress() {
        if (this.active && !this.frozen && this.onPress != null) {
            this.onPress.accept(this);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.frozen) return false;
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        if (result) this.isPressed = true;
        return result;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.frozen) return false;
        boolean result = super.mouseReleased(mouseX, mouseY, button);
        this.isPressed = false;
        return result;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.frozen) return false;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    protected boolean isValidClickButton(int button) {
        if (this.frozen) return false;
        return super.isValidClickButton(button);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;

        if (!this.frozen) animationController.update(this.isHovered());

        renderButtonImage(guiGraphics);
        renderButtonText(guiGraphics);

        if (!this.frozen) renderFixedTooltip(guiGraphics);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.playDownSound(Minecraft.getInstance().getSoundManager());
        super.onClick(mouseX, mouseY);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if (this.soundEffectLocation != null) {
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(this.soundEffectLocation);
            soundManager.play(SimpleSoundInstance.forUI(soundEvent, 1.0F));
        } else {
            super.playDownSound(soundManager);
        }
    }

    // region Rendering
    private void renderButtonImage(GuiGraphics guiGraphics) {
        AnimationController.RenderData data = animationController.getRenderData();
        if (data.texture == null) return;

        int imageSize = Math.min(this.width, this.height);
        int imageX = this.getX() + (this.width - imageSize) / 2;
        int imageY = this.getY() + (this.height - imageSize) / 2;

        if (!this.active) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 0.5f);
        } else {
            float multiplier = 1.0f;
            if (isPressed) {
                multiplier = clickBrightness;
            } else if (this.isHovered()) {
                multiplier = hoverBrightness;
            }
            RenderSystem.setShaderColor(multiplier, multiplier, multiplier, 1.0f);
        }

        guiGraphics.blit(data.texture, imageX, imageY, data.u, data.v, imageSize, imageSize, data.textureWidth, data.textureHeight);

        if (!this.active) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        } else {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void renderButtonText(GuiGraphics guiGraphics) {
        Font font = Minecraft.getInstance().font;
        int textX = this.getX() + (this.width - font.width(this.getMessage())) / 2;
        int textY = this.getY() + this.height - font.lineHeight - 2;

        int textColor = this.active ? 0xFFFFFF : 0xA0A0A0;
        int strokeColor = this.active ? 0x000000 : 0x404040;

        // Render a thick stroke/outline for the text
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    guiGraphics.drawString(font, this.getMessage(), textX + dx, textY + dy, strokeColor, false);
                }
            }
        }
        guiGraphics.drawString(font, this.getMessage(), textX, textY, textColor, false);
    }

    private void renderFixedTooltip(GuiGraphics guiGraphics) {
        if (this.isHovered() && this.fixedTooltipPosition) {
            Component currentTooltip = getActiveTooltip();
            if (currentTooltip != null) {
                // This positioning seems very specific. Kept it as-is.
                var window = Minecraft.getInstance().getWindow();
                int screenWidth = window.getGuiScaledWidth();
                int screenHeight = window.getGuiScaledHeight();
                int textWidth = Minecraft.getInstance().font.width(currentTooltip);
                int x = (screenWidth - textWidth) / 2 - 12;
                int y = screenHeight - (((screenHeight - 256) / 2 + 30) / 2) + 7;
                guiGraphics.renderTooltip(Minecraft.getInstance().font, currentTooltip, x, y);
            }
        }
    }

    // region Helpers
    private void updateTooltip() {
        if (!fixedTooltipPosition) {
            if (this.frozen) {
                super.setTooltip(null);
            } else {
                Component currentTooltip = getActiveTooltip();
                super.setTooltip(currentTooltip != null ? Tooltip.create(currentTooltip) : null);
            }
        }
    }

    @Nullable
    private Component getActiveTooltip() {
        return !this.active && this.disabledTooltipComponent != null ? this.disabledTooltipComponent : this.tooltipComponent;
    }

    // region Animation
    private enum AnimationDirection {FORWARD, REVERSE}

    private record AnimationFrameRange(int startFrame, int endFrame, AnimationDirection direction) {
        int getFrameCount() {
            return Math.abs(endFrame - startFrame) + 1;
        }
    }

    private static class AnimationController {
        private enum AnimationState {IDLE, HOVER_INITIAL, HOVER_LOOP, MOUSE_OFF}

        private final Builder settings;
        private AnimationState currentState = AnimationState.IDLE;
        private boolean wasHovered = false;
        private long animationStartTime = 0;

        AnimationController(Builder builder) {
            this.settings = builder;
        }

        public void update(boolean isHovered) {
            if (isHovered && !wasHovered) {
                startAnimation(AnimationState.HOVER_INITIAL);
            } else if (!isHovered && wasHovered) {
                startAnimation(AnimationState.MOUSE_OFF);
            }
            wasHovered = isHovered;
        }

        private void startAnimation(AnimationState state) {
            this.currentState = state;
            this.animationStartTime = Objects.requireNonNull(Minecraft.getInstance().level).getGameTime();
        }

        public RenderData getRenderData() {
            if (settings.animationLocation == null || currentState == AnimationState.IDLE) {
                return new RenderData(settings.imageLocation, 0, 0, settings.tileWidth, settings.tileHeight);
            }

            long elapsedTicks = Objects.requireNonNull(Minecraft.getInstance().level).getGameTime() - animationStartTime;
            var activeRange = getActiveRange();

            if (activeRange == null) {
                currentState = AnimationState.IDLE;
                return new RenderData(settings.imageLocation, 0, 0, settings.tileWidth, settings.tileHeight);
            }

            int frameCount = activeRange.getFrameCount();
            int frameIndex = (int) elapsedTicks;
            boolean loop = isLooping();

            if (!loop && frameIndex >= frameCount) {
                // Transition to next state after one-shot animation finishes
                if (currentState == AnimationState.HOVER_INITIAL) startAnimation(AnimationState.HOVER_LOOP);
                else if (currentState == AnimationState.MOUSE_OFF) currentState = AnimationState.IDLE;
                frameIndex = frameCount - 1; // Clamp to last frame
            }

            int frameOffset = loop ? frameIndex % frameCount : frameIndex;
            int currentFrame = activeRange.direction == AnimationDirection.REVERSE
                    ? activeRange.startFrame - frameOffset
                    : activeRange.startFrame + frameOffset;

            int vOffset = currentFrame * settings.tileHeight;
            return new RenderData(settings.animationLocation, 0, vOffset, settings.tileWidth, settings.totalFrames * settings.tileHeight);
        }

        private boolean isLooping() {
            return currentState == AnimationState.HOVER_LOOP;
        }

        @Nullable
        private AnimationFrameRange getActiveRange() {
            return switch (currentState) {
                case HOVER_INITIAL -> settings.initialHoverAnimation;
                case HOVER_LOOP -> settings.hoverLoopAnimation;
                case MOUSE_OFF -> settings.mouseOffAnimation;
                default -> null;
            };
        }

        record RenderData(@Nullable ResourceLocation texture, int u, int v, int textureWidth, int textureHeight) {}
    }

    // region Builder
    public static class Builder {
        // Required
        private final int x, y, width, height;
        private final Component message;
        private final Consumer<MapSquareButton> onPress;

        // Optional
        private Component tooltip = Component.empty();
        @Nullable private Component disabledTooltip = null;
        @Nullable private ResourceLocation imageLocation = null;
        @Nullable private ResourceLocation animationLocation = null;
        @Nullable private ResourceLocation soundEffectLocation = null;
        private int totalFrames = 0, tileWidth = 0, tileHeight = 0;
        @Nullable private AnimationFrameRange initialHoverAnimation, hoverLoopAnimation, mouseOffAnimation;
        private boolean fixedTooltipPosition = false;
        private float hoverBrightness = 1.0f;
        private float clickBrightness = 1.0f;

        public Builder(int x, int y, int width, int height, Component message, Consumer<MapSquareButton> onPress) {
            this.x = x; this.y = y; this.width = width; this.height = height;
            this.message = message; this.onPress = onPress;
        }

        public Builder tooltip(Component tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder tooltips(Component tooltip, @Nullable Component disabledTooltip) {
            this.tooltip = tooltip;
            this.disabledTooltip = disabledTooltip;
            return this;
        }

        public Builder image(ResourceLocation imageLocation, int tileWidth, int tileHeight) {
            this.imageLocation = imageLocation;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            return this;
        }

        public Builder animation(ResourceLocation animationLocation, int totalFrames) {
            this.animationLocation = animationLocation;
            this.totalFrames = totalFrames;
            return this;
        }

        public Builder hoverBrightness(float brightness) {
            this.hoverBrightness = brightness;
            return this;
        }

        public Builder clickBrightness(float brightness) {
            this.clickBrightness = brightness;
            return this;
        }

        public Builder soundEffect(ResourceLocation soundLocation) {
            this.soundEffectLocation = soundLocation;
            return this;
        }

        public Builder initialHover(int start, int end, boolean reverse) {
            if (start != -1 && end != -1)
                this.initialHoverAnimation = new AnimationFrameRange(start, end, reverse ? AnimationDirection.REVERSE : AnimationDirection.FORWARD);
            return this;
        }

        public Builder hoverLoop(int start, int end, boolean reverse) {
            if (start != -1 && end != -1)
                this.hoverLoopAnimation = new AnimationFrameRange(start, end, reverse ? AnimationDirection.REVERSE : AnimationDirection.FORWARD);
            return this;
        }

        public Builder mouseOff(int start, int end, boolean reverse) {
            if (start != -1 && end != -1)
                this.mouseOffAnimation = new AnimationFrameRange(start, end, reverse ? AnimationDirection.REVERSE : AnimationDirection.FORWARD);
            return this;
        }

        public Builder fixedTooltipPosition(boolean fixed) {
            this.fixedTooltipPosition = fixed;
            return this;
        }

        public MapSquareButton build() {
            return new MapSquareButton(this);
        }
    }
}