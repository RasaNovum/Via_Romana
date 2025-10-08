package net.rasanovum.viaromana.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
// import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.core.PathRecord;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.client.gui.elements.MapActionButton;
import net.rasanovum.viaromana.client.gui.elements.MapSquareButton;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.network.packets.RoutedActionC2S;
import net.rasanovum.viaromana.storage.player.PlayerData;
import net.rasanovum.viaromana.util.PathUtils;
import net.rasanovum.viaromana.util.VersionUtils;

import java.util.List;
import java.util.Objects;

public class ChartingScreen extends Screen {
    // region Constants
    private static final int BACKGROUND_WIDTH = 256;
    private static final int BACKGROUND_HEIGHT = 256;
    private static final int USABLE_WIDTH = 230;
    private static final int USABLE_HEIGHT = 180;
    private static final int PADDING = 10;
    private static final int SMALL_BUTTON_SIZE = 64;
    private static final int LARGE_BUTTON_SIZE = 80;

    private static final class Textures {
        static final ResourceLocation BACKGROUND = VersionUtils.getLocation("via_romana:textures/screens/background_map.png");
        static final ResourceLocation CHARTING_OVERLAY = VersionUtils.getLocation("via_romana:textures/screens/background_map_charting.png");
        static final ResourceLocation MAP_CANCEL = VersionUtils.getLocation("via_romana:textures/screens/element_cancel.png");
        static final ResourceLocation MAP_TUTORIAL = VersionUtils.getLocation("via_romana:textures/screens/element_tutorial.png");
        static final ResourceLocation MAP_TUTORIAL_BACK = VersionUtils.getLocation("via_romana:textures/screens/element_tutorial_back.png");
        static final ResourceLocation MAP_TUTORIAL_NEXT = VersionUtils.getLocation("via_romana:textures/screens/element_tutorial_next.png");
        static final ResourceLocation MAP_TUTORIAL_RETURN = VersionUtils.getLocation("via_romana:textures/screens/element_return.png");
        static final ResourceLocation CHART_START_TILE = VersionUtils.getLocation("via_romana:textures/screens/chart_start_tile.png");
        static final ResourceLocation CHART_START_FRAME = VersionUtils.getLocation("via_romana:textures/screens/chart_start_frame.png");
        static final ResourceLocation CHART_FINISH_TILE = VersionUtils.getLocation("via_romana:textures/screens/chart_finish_tile.png");
        static final ResourceLocation CHART_FINISH_FRAME = VersionUtils.getLocation("via_romana:textures/screens/chart_finish_frame.png");
        static final ResourceLocation SEVER_PATH_TILE = VersionUtils.getLocation("via_romana:textures/screens/chart_sever_tile.png");
        static final ResourceLocation SEVER_PATH_FRAME = VersionUtils.getLocation("via_romana:textures/screens/chart_sever_frame.png");
        static final ResourceLocation DELETE_BRANCH_TILE = VersionUtils.getLocation("via_romana:textures/screens/chart_delete_branch_tile.png");
        static final ResourceLocation DELETE_BRANCH_FRAME = VersionUtils.getLocation("via_romana:textures/screens/chart_delete_branch_frame.png");
        static final ResourceLocation SEAL_APPROVE = VersionUtils.getLocation("via_romana:textures/screens/seal_approve.png");
        static final ResourceLocation SEAL_CANCEL = VersionUtils.getLocation("via_romana:textures/screens/seal_cancel.png");
        static final ResourceLocation TUTORIAL_1 = VersionUtils.getLocation("via_romana:textures/screens/tutorial_1.png");
        static final ResourceLocation TUTORIAL_2 = VersionUtils.getLocation("via_romana:textures/screens/tutorial_2.png");
        static final ResourceLocation TUTORIAL_3 = VersionUtils.getLocation("via_romana:textures/screens/tutorial_3.png");
        static final ResourceLocation TUTORIAL_4 = VersionUtils.getLocation("via_romana:textures/screens/tutorial_4.png");
        static final ResourceLocation TUTORIAL_5 = VersionUtils.getLocation("via_romana:textures/screens/tutorial_5.png");
        static final ResourceLocation TUTORIAL_6 = VersionUtils.getLocation("via_romana:textures/screens/tutorial_6.png");
        static final ResourceLocation TUTORIAL_7 = VersionUtils.getLocation("via_romana:textures/screens/tutorial_7.png");
        static final ResourceLocation TUTORIAL_8 = VersionUtils.getLocation("via_romana:textures/screens/tutorial_8.png");
    }

    private static final class Sounds {
        static final ResourceLocation SEVER_PATH = VersionUtils.getLocation("minecraft:entity.sheep.shear");
        static final ResourceLocation DELETE_BRANCH_PROMPT = VersionUtils.getLocation("minecraft:item.flintandsteel.use");
        static final ResourceLocation DELETE_BRANCH_CONFIRM = VersionUtils.getLocation("minecraft:block.fire.ambient");
        static final ResourceLocation DELETE_BRANCH_CANCEL = VersionUtils.getLocation("minecraft:block.fire.extinguish");
        static final ResourceLocation START_CHARTING = VersionUtils.getLocation("minecraft:item.book.page_turn");
    }
    // endregion

    private enum ScreenState {IDLE, CHARTING, DELETE_APPROVAL, TUTORIAL}
    private ScreenState currentScreenState = ScreenState.IDLE;
    private int tutorialPage = 0;

    private MapSquareButton severPathButton, deleteBranchButton, chartStartButton, chartFinishButton, sealApproveButton, sealCancelButton;
    private MapActionButton cancelChartingButton, exitTutorialButton, tutorialButton, tutorialBackButton, tutorialNextButton;

    private int panelX, panelY, usableX, usableY;
    private int textBoundsTopY, textBoundsHeight;
    private boolean isNearNode = false;
    private boolean hasGoodInfrastructure = false;
    private float currentInfrastructureQuality = 0.0f;

    public ChartingScreen(Component title) {
        super(title);
    }

    // region Lifecycle & Rendering
    @Override
    protected void init() {
        super.init();
        this.calculateLayout();
        this.createWidgets();
        this.updateNearNodeStatus();
        this.updateInfrastructureStatus();
        this.syncScreenState();
    }

    //? if >1.21
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackgroundTexture(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // this.renderBlurredBackground(partialTick); // Looked a little weird, but will visit later

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        renderHeader(guiGraphics);

        if (this.currentScreenState == ScreenState.TUTORIAL) {
            renderTutorialContent(guiGraphics);
        } else if (this.currentScreenState == ScreenState.DELETE_APPROVAL) {
            //? if >1.21
            this.minecraft.gameRenderer.processBlurEffect(partialTick);
            this.minecraft.getMainRenderTarget().bindWrite(false);
            
            guiGraphics.fillGradient(0, 0, this.width, this.height, 0x80000000, 0x80000000);
            
            renderApprovalText(guiGraphics);
            
            withSealButtonsVisible(() -> {
                sealApproveButton.render(guiGraphics, mouseX, mouseY, partialTick);
                sealCancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
            });
        }
        
        if (this.currentScreenState == ScreenState.TUTORIAL) {
            renderComponentTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderBackgroundTexture(GuiGraphics guiGraphics) {
        RenderSystem.enableBlend();
        guiGraphics.blit(Textures.BACKGROUND, panelX, panelY, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        if (this.currentScreenState == ScreenState.CHARTING) {
            guiGraphics.blit(Textures.CHARTING_OVERLAY, panelX, panelY, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        }
        RenderSystem.disableBlend();
    }

    private void renderHeader(GuiGraphics guiGraphics) {
        Component statusMessage;
        if (this.currentScreenState == ScreenState.CHARTING) {
            statusMessage = Component.translatable("gui.viaromana.charting_in_progress");
        } else if (this.currentScreenState == ScreenState.TUTORIAL) {
            statusMessage = Component.translatable("gui.viaromana.tutorial_title");
        } else {
            statusMessage = Component.translatable("gui.viaromana.charting_map");
        }

        int barY = panelY + PADDING + 40;
        int barColor = 0xFF3C1E00;
        int textWidth = this.font.width(statusMessage);
        int textX = usableX + (USABLE_WIDTH - textWidth) / 2;
        int textY = barY - this.font.lineHeight / 2 + 1;
        int barPadding = 6;

        guiGraphics.fill(usableX, barY, textX - barPadding, barY + 1, barColor);
        guiGraphics.fill(textX + textWidth + barPadding, barY, usableX + USABLE_WIDTH, barY + 1, barColor);
        guiGraphics.drawString(this.font, statusMessage, textX, textY, barColor, false);
    }

    private void renderApprovalText(GuiGraphics guiGraphics) {
        Component messageComponent = Component.translatable("gui.viaromana.delete_branch_confirmation");
        int textY = usableY + (USABLE_HEIGHT - LARGE_BUTTON_SIZE - PADDING - 48) / 2;
        renderWrappedCenteredText(guiGraphics, messageComponent, textY, USABLE_WIDTH + 80, 1.5f, 0xFFFFFFFF);
    }

    private void renderTutorialContent(GuiGraphics guiGraphics) {
        ResourceLocation tutorialImage = switch (tutorialPage) {
            case 0 -> Textures.TUTORIAL_1;
            case 1 -> Textures.TUTORIAL_2;
            case 2 -> Textures.TUTORIAL_3;
            case 3 -> Textures.TUTORIAL_4;
            case 4 -> Textures.TUTORIAL_5;
            case 5 -> Textures.TUTORIAL_6;
            case 6 -> Textures.TUTORIAL_7;
            case 7 -> Textures.TUTORIAL_8;
            default -> null;
        };

        if (tutorialImage == null) return;

        int imageWidth = 200;
        int imageHeight = 120;
        int imageX = usableX + (USABLE_WIDTH - imageWidth) / 2;
        int imageY = usableY + PADDING;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.75f);
        guiGraphics.blit(tutorialImage, imageX, imageY, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        Component tutorialMessage = getTutorialMessage();

        imageHeight = 96; // Fixes text tooltip detection, don't worry about it :}

        if (tutorialMessage != Component.empty()) {
            int textTopY = usableY + imageHeight + PADDING * 2 + 8;
            int textHeight = USABLE_HEIGHT - imageHeight - PADDING * 4 - 16;
            
            this.textBoundsTopY = textTopY;
            this.textBoundsHeight = textHeight;

            int textCenterY = textTopY + textHeight / 2;
            int textColor = 0xFF3C1E00;
            renderWrappedCenteredText(guiGraphics, tutorialMessage, textCenterY, USABLE_WIDTH - PADDING, 1.0f, textColor);
        }
    }
    // endregion

    // region Helpers
    private void renderWrappedCenteredText(GuiGraphics guiGraphics, Component text, int centerY, int maxWidth, float scale, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0f);

        int scaledMaxWidth = (int) (maxWidth / scale);

        List<FormattedCharSequence> lines = this.font.split(text, scaledMaxWidth);

        if (lines.isEmpty()) {
            guiGraphics.pose().popPose();
            return;
        }

        int lineSpacing = 1;
        int totalTextHeight = (lines.size() * this.font.lineHeight) + ((lines.size() - 1) * lineSpacing);
        int startY = (int) ((centerY / scale) - (totalTextHeight / 2f));

        int centerX = (int) ((usableX + USABLE_WIDTH / 2f) / scale);

        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            int lineY = startY + i * (this.font.lineHeight + lineSpacing);
            
            int lineWidth = this.font.width(line);
            guiGraphics.drawString(this.font, line, centerX - lineWidth / 2, lineY, color, false);
        }

        guiGraphics.pose().popPose();
    }
    
    private void renderComponentTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (mouseX >= usableX && mouseX < usableX + USABLE_WIDTH &&
            mouseY >= this.textBoundsTopY && mouseY < this.textBoundsTopY + this.textBoundsHeight) {

            Style style = getStyleAt(mouseX, mouseY);
            
            if (style != null && style.getHoverEvent() != null) {
                guiGraphics.renderComponentHoverEffect(this.font, style, mouseX, mouseY);
            }
        }
    }

    /*
     * Returns the Style at the given mouse coordinates within the tutorial text area, or null if none.
     */
    private Style getStyleAt(double mouseX, double mouseY) {
        Component tutorialMessage = getTutorialMessage();
        if (tutorialMessage == Component.empty()) {
            return null;
        }

        int maxWidth = USABLE_WIDTH - PADDING;
        List<FormattedCharSequence> lines = this.font.split(tutorialMessage, maxWidth);
        
        int totalTextHeight = lines.size() * this.font.lineHeight;
        int startY = (this.textBoundsTopY + this.textBoundsHeight / 2) - (totalTextHeight / 2);

        int hoveredLineIndex = (int) ((mouseY - startY) / this.font.lineHeight);

        if (hoveredLineIndex >= 0 && hoveredLineIndex < lines.size()) {
            FormattedCharSequence line = lines.get(hoveredLineIndex);
            int centerX = usableX + USABLE_WIDTH / 2;
            int lineWidth = this.font.width(line);
            int lineStartX = centerX - lineWidth / 2;

            if (mouseX >= lineStartX && mouseX <= lineStartX + lineWidth) {
                return this.font.getSplitter().componentStyleAtWidth(line, (int) (mouseX - lineStartX));
            }
        }

        return null;
    }

    private Component getTutorialMessage() {
        return switch (tutorialPage) {
            case 0 -> Component.translatable("gui.viaromana.tutorial_page_1");
            case 1 -> {
                Component tooltipText = Component.translatable("gui.viaromana.tooltip.infrastructure");
                HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltipText);
                Component infrastructureTerm = Component.translatable("gui.viaromana.tutorial_infrastructure_term")
                        .setStyle(Style.EMPTY.withHoverEvent(hoverEvent).withUnderlined(true));
                int requiredAmount = (int) Math.ceil((CommonConfig.infrastructure_check_radius * 2 + 1) * CommonConfig.path_quality_threshold);

                yield tooltipFromPlaceholder(I18n.get("gui.viaromana.tutorial_page_2", requiredAmount), infrastructureTerm);
            }
            case 2 -> Component.translatable("gui.viaromana.tutorial_page_3");
            case 3 -> Component.translatable("gui.viaromana.tutorial_page_4");
            case 4 -> Component.translatable("gui.viaromana.tutorial_page_5");
            case 5 -> Component.translatable("gui.viaromana.tutorial_page_6");
            case 6 -> Component.translatable("gui.viaromana.tutorial_page_7");
            case 7 -> Component.translatable("gui.viaromana.tutorial_page_8");
            default -> Component.empty();
        };
    }

    public static Component tooltipFromPlaceholder(String rawString, Component styledTerm) {
        String[] parts = rawString.split("!", 2);

        if (parts.length == 2) {
            return Component.literal(parts[0])
                .append(styledTerm)
                .append(Component.literal(parts[1]));
        } else {
            return Component.literal(rawString);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.currentScreenState == ScreenState.DELETE_APPROVAL) {
            withSealButtonsVisible(() -> {
                sealApproveButton.mouseMoved(mouseX, mouseY);
                sealCancelButton.mouseMoved(mouseX, mouseY);
            });
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.currentScreenState == ScreenState.DELETE_APPROVAL && button == 0) {
            final boolean[] handled = {false};
            withSealButtonsVisible(() -> {
                if (sealApproveButton.mouseClicked(mouseX, mouseY, button) || 
                    sealCancelButton.mouseClicked(mouseX, mouseY, button)) {
                    handled[0] = true;
                }
            });
            if (handled[0]) return true;
        }
        
        if (button == 1) { // Right click
            Objects.requireNonNull(minecraft).setScreen(null);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendRoutedAction(RoutedActionC2S.Operation operation) {
        RoutedActionC2S packet = new RoutedActionC2S(operation);
        commonnetwork.api.Dispatcher.sendToServer(packet);
        Objects.requireNonNull(minecraft).setScreen(null);
    }
    
    /**
     * Temporarily makes seal buttons visible, executes the action, then restores visibility.
     */
    private void withSealButtonsVisible(Runnable action) {
        boolean wasApproveVisible = sealApproveButton.visible;
        boolean wasCancelVisible = sealCancelButton.visible;
        
        sealApproveButton.visible = true;
        sealCancelButton.visible = true;
        
        action.run();
        
        sealApproveButton.visible = wasApproveVisible;
        sealCancelButton.visible = wasCancelVisible;
    }
    // endregion

    // region Pre-condition Checks
    private void updateNearNodeStatus() {
        if (minecraft == null || minecraft.player == null) return;
        double utilityDistance = CommonConfig.node_utility_distance;
        isNearNode = ClientPathData.getInstance().getNearestNode(minecraft.player.blockPosition(), utilityDistance, false).isPresent();
    }

    private void updateInfrastructureStatus() {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        this.currentInfrastructureQuality = PathUtils.calculateInfrastructureQuality(minecraft.level, minecraft.player);
        float threshold = CommonConfig.path_quality_threshold;
        this.hasGoodInfrastructure = this.currentInfrastructureQuality >= threshold;
    }
    // endregion

    // region State Management
    private void setState(ScreenState newState) {
        this.currentScreenState = newState;
        this.updateWidgetStates();
    }

    private void syncScreenState() {
        boolean isPlayerCharting = PlayerData.isChartingPath(this.minecraft.player);
        if (isPlayerCharting && this.currentScreenState == ScreenState.IDLE) {
            setState(ScreenState.CHARTING);
        } else if (!isPlayerCharting && this.currentScreenState == ScreenState.CHARTING) {
            setState(ScreenState.IDLE);
        } else {
            updateWidgetStates();
        }
    }

    private void updateWidgetStates() {
        boolean isIdle = currentScreenState == ScreenState.IDLE;
        boolean isCharting = currentScreenState == ScreenState.CHARTING;
        boolean isApproving = currentScreenState == ScreenState.DELETE_APPROVAL;
        boolean isTutorial = currentScreenState == ScreenState.TUTORIAL;

        severPathButton.visible = !isTutorial;
        deleteBranchButton.visible = !isTutorial;
        chartStartButton.visible = isIdle || isApproving;
        chartFinishButton.visible = isCharting;
        sealApproveButton.visible = false;
        sealCancelButton.visible = false;
        cancelChartingButton.visible = isCharting;
        exitTutorialButton.visible = isTutorial;
        tutorialButton.visible = isIdle;
        tutorialBackButton.visible = isTutorial && tutorialPage > 0;
        tutorialNextButton.visible = isTutorial && tutorialPage < 7;

        severPathButton.setFrozen(isApproving);
        deleteBranchButton.setFrozen(isApproving);
        chartStartButton.setFrozen(isApproving);

        severPathButton.setDisabled(!isIdle || !isNearNode);
        deleteBranchButton.setDisabled(!isIdle || !isNearNode);
        chartStartButton.setDisabled(!isIdle || !hasGoodInfrastructure);

        updateTooltips();
    }

    private void updateTooltips() {
        if (currentScreenState == ScreenState.CHARTING) {
            severPathButton.setTooltips(Component.translatable("gui.viaromana.sever_path_tooltip"), Component.translatable("gui.viaromana.sever_path_disabled_charting"));
            deleteBranchButton.setTooltips(Component.translatable("gui.viaromana.delete_branch_tooltip"), Component.translatable("gui.viaromana.delete_branch_disabled_charting"));
        } else if (!isNearNode) {
            double utilityDistance = CommonConfig.node_utility_distance;
            Component tooltip = Component.translatable("gui.viaromana.node_distance_tooltip", utilityDistance);
            severPathButton.setTooltips(Component.translatable("gui.viaromana.sever_path_tooltip"), tooltip);
            deleteBranchButton.setTooltips(Component.translatable("gui.viaromana.delete_branch_tooltip"), tooltip);
        } else {
            severPathButton.setTooltip(Component.translatable("gui.viaromana.sever_path_tooltip"));
            deleteBranchButton.setTooltip(Component.translatable("gui.viaromana.delete_branch_tooltip"));
        }

        if (!hasGoodInfrastructure) {
            float threshold = CommonConfig.path_quality_threshold;
            int requiredBlocks = (int) Math.ceil(threshold * 9.0);
            int currentBlocks = Math.round(this.currentInfrastructureQuality * 9.0f);
            chartStartButton.setTooltips(
                getChartStartTooltip(),
                Component.translatable("gui.viaromana.infrastructure_insufficient", currentBlocks, requiredBlocks)
            );
        } else {
            chartStartButton.setTooltip(getChartStartTooltip());
        }

        if (currentScreenState == ScreenState.DELETE_APPROVAL) {
            sealApproveButton.setTooltip(Component.translatable("gui.viaromana.approve_deleting"));
            sealCancelButton.setTooltip(Component.translatable("gui.viaromana.cancel_deleting"));
        }
    }

    private Component getChartStartTooltip() {
        return Component.translatable(!isNearNode ? "gui.viaromana.chart_start_tooltip" : "gui.viaromana.chart_continue_tooltip");
    }
    // endregion

    // region Widget Creation
    private void calculateLayout() {
        this.panelX = (this.width - BACKGROUND_WIDTH) / 2;
        this.panelY = (this.height - BACKGROUND_HEIGHT) / 2;
        this.usableX = panelX + (BACKGROUND_WIDTH - USABLE_WIDTH) / 2;
        this.usableY = panelY + (BACKGROUND_HEIGHT - USABLE_HEIGHT) / 2 + 7;
    }

    private void createWidgets() {
        addRenderableWidget(severPathButton = createSeverPathButton());
        addRenderableWidget(deleteBranchButton = createDeleteBranchButton());
        addRenderableWidget(chartStartButton = createChartStartButton());
        addRenderableWidget(chartFinishButton = createChartFinishButton());
        addRenderableWidget(sealApproveButton = createSealApproveButton());
        addRenderableWidget(sealCancelButton = createSealCancelButton());
        addRenderableWidget(cancelChartingButton = createCancelChartingButton());
        addRenderableWidget(exitTutorialButton = createExitTutorialButton());
        addRenderableWidget(tutorialButton = createTutorialButton());
        addRenderableWidget(tutorialBackButton = createTutorialBackButton());
        addRenderableWidget(tutorialNextButton = createTutorialNextButton());
    }

    private MapSquareButton createSeverPathButton() {
        return new MapSquareButton.Builder(
            usableX + PADDING, usableY + PADDING, SMALL_BUTTON_SIZE, SMALL_BUTTON_SIZE,
            Component.translatable("gui.viaromana.sever_path_button"),
            button -> sendRoutedAction(RoutedActionC2S.Operation.SEVER_NEAREST_NODE))
            .tooltip(Component.translatable("gui.viaromana.sever_path_tooltip"))
            .image(Textures.SEVER_PATH_FRAME, SMALL_BUTTON_SIZE, SMALL_BUTTON_SIZE)
            .animation(Textures.SEVER_PATH_TILE, 7)
            .soundEffect(Sounds.SEVER_PATH)
            .initialHover(0, 6, false).hoverLoop(6, 6, false).mouseOff(6, 0, true)
            .fixedTooltipPosition(true)
            .build();
    }

    private MapSquareButton createDeleteBranchButton() {
        return new MapSquareButton.Builder(
            usableX + USABLE_WIDTH - SMALL_BUTTON_SIZE - PADDING, usableY + PADDING, SMALL_BUTTON_SIZE, SMALL_BUTTON_SIZE,
            Component.translatable("gui.viaromana.remove_branch_button"),
            button -> setState(ScreenState.DELETE_APPROVAL))
            .tooltip(Component.translatable("gui.viaromana.delete_branch_detailed_tooltip"))
            .image(Textures.DELETE_BRANCH_FRAME, SMALL_BUTTON_SIZE, SMALL_BUTTON_SIZE)
            .animation(Textures.DELETE_BRANCH_TILE, 12)
            .soundEffect(Sounds.DELETE_BRANCH_PROMPT)
            .initialHover(0, 11, false).hoverLoop(11, 11, false).mouseOff(9, 0, true)
            .fixedTooltipPosition(true)
            .build();
    }

    private MapSquareButton createChartStartButton() {
        return new MapSquareButton.Builder(
            usableX + (USABLE_WIDTH - LARGE_BUTTON_SIZE) / 2, usableY + USABLE_HEIGHT - LARGE_BUTTON_SIZE - PADDING, LARGE_BUTTON_SIZE, LARGE_BUTTON_SIZE,
            Component.translatable("gui.viaromana.message.via_romana.start_charting_button"),
            button -> {
                PathRecord.start(minecraft.level, minecraft.player, minecraft.player.blockPosition());
                minecraft.setScreen(null);
            })
            .tooltip(getChartStartTooltip())
            .image(Textures.CHART_START_FRAME, LARGE_BUTTON_SIZE, LARGE_BUTTON_SIZE)
            .animation(Textures.CHART_START_TILE, 18)
            .soundEffect(Sounds.START_CHARTING)
            .initialHover(0, 5, false).hoverLoop(6, 17, false).mouseOff(5, 0, true)
            .fixedTooltipPosition(true)
            .build();
    }

    private MapSquareButton createChartFinishButton() {
        return new MapSquareButton.Builder(
            usableX + (USABLE_WIDTH - LARGE_BUTTON_SIZE) / 2, usableY + USABLE_HEIGHT - LARGE_BUTTON_SIZE - PADDING, LARGE_BUTTON_SIZE, LARGE_BUTTON_SIZE,
            Component.translatable("gui.viaromana.message.via_romana.finish_charting_button"),
            button -> {
                PathRecord.end(minecraft.level, minecraft.player, minecraft.player.blockPosition());
                minecraft.setScreen(null);
            })
            .tooltip(Component.translatable(!isNearNode ? "gui.viaromana.message.via_romana.finish_charting_tooltip" : "gui.viaromana.message.via_romana.finish_charting_nearby_tooltip"))
            .image(Textures.CHART_FINISH_FRAME, LARGE_BUTTON_SIZE, LARGE_BUTTON_SIZE)
            .animation(Textures.CHART_FINISH_TILE, 7)
            .soundEffect(Sounds.START_CHARTING)
            .initialHover(0, 6, false).hoverLoop(6, 6, false).mouseOff(6, 0, true)
            .fixedTooltipPosition(true)
            .build();
    }

    private MapActionButton createCancelChartingButton() {
        int btnWidth = 60, btnHeight = 16;
        return new MapActionButton(usableX + USABLE_WIDTH - btnWidth - PADDING + 10, usableY + USABLE_HEIGHT - btnHeight - PADDING, btnWidth, btnHeight,
            Component.translatable("gui.viaromana.cancel_button"), Component.translatable("gui.viaromana.message.via_romana.cancel_charting_tooltip"),
            button -> {
                PathRecord.cancel(minecraft.level, minecraft.player, true);
                minecraft.setScreen(null);
            },
            Textures.MAP_CANCEL);
    }

    private MapActionButton createTutorialButton() {
        int btnWidth = 62, btnHeight = 16;
        return new MapActionButton(usableX + USABLE_WIDTH - btnWidth - PADDING + 10, usableY + USABLE_HEIGHT - btnHeight - PADDING, btnWidth, btnHeight,
            Component.translatable("gui.viaromana.tutorial_title"), Component.translatable("gui.viaromana.tutorial_tooltip"),
            button -> {
                // if (!PlayerData.hasReceivedTutorial(this.minecraft.player)) {
                //     PlayerData.setReceivedTutorial(this.minecraft.player, true);
                //     ViaRomana.LOGGER.info("Player {} has completed the tutorial.", this.minecraft.player.getName().getString());
                // }

                setState(ScreenState.TUTORIAL);
            },
            Textures.MAP_TUTORIAL);
    }
    
    private MapActionButton createExitTutorialButton() {
        int btnWidth = 60, btnHeight = 16;
        return new MapActionButton(usableX + USABLE_WIDTH - btnWidth - PADDING + 10, usableY + USABLE_HEIGHT - btnHeight - PADDING, btnWidth, btnHeight,
            Component.translatable("gui.viaromana.exit_tutorial_button"), Component.translatable("gui.viaromana.exit_tutorial_tooltip"),
            button -> {
                setState(ScreenState.IDLE);
                tutorialPage = 0;
            },
            Textures.MAP_TUTORIAL_RETURN);
    }

    private MapActionButton createTutorialBackButton() {
        int btnWidth = 20, btnHeight = 16;
        return new MapActionButton(usableX + PADDING + 76, usableY + USABLE_HEIGHT - btnHeight - PADDING, btnWidth, btnHeight,
            button -> {
                if (tutorialPage > 0) {
                    tutorialPage--;
                    this.updateWidgetStates();
                }
            },
            Textures.MAP_TUTORIAL_BACK);
    }

    private MapActionButton createTutorialNextButton() {
        int btnWidth = 20, btnHeight = 16;
        return new MapActionButton(usableX + PADDING + 117, usableY + USABLE_HEIGHT - btnHeight - PADDING, btnWidth, btnHeight,
            button -> {
                tutorialPage++;
                this.updateWidgetStates();
            },
            Textures.MAP_TUTORIAL_NEXT);
    }

    private MapSquareButton createSealApproveButton() {
        int sealY = usableY + USABLE_HEIGHT - LARGE_BUTTON_SIZE - PADDING - 48;
        int centerX = usableX + USABLE_WIDTH / 2;
        return new MapSquareButton.Builder(
            centerX - LARGE_BUTTON_SIZE - 10, sealY, LARGE_BUTTON_SIZE, LARGE_BUTTON_SIZE,
            Component.translatable("gui.viaromana.confirm_button"),
            button -> sendRoutedAction(RoutedActionC2S.Operation.REMOVE_BRANCH))
            .image(Textures.SEAL_APPROVE, LARGE_BUTTON_SIZE, LARGE_BUTTON_SIZE)
            .soundEffect(Sounds.DELETE_BRANCH_CONFIRM)
            .fixedTooltipPosition(true)
            .hoverBrightness(1.2f)
            .build();
    }

    private MapSquareButton createSealCancelButton() {
        int sealY = usableY + USABLE_HEIGHT - LARGE_BUTTON_SIZE - PADDING - 48;
        int centerX = usableX + USABLE_WIDTH / 2;
        return new MapSquareButton.Builder(
            centerX + 10, sealY, LARGE_BUTTON_SIZE, LARGE_BUTTON_SIZE,
            Component.translatable("gui.viaromana.cancel_button"),
            button -> setState(ScreenState.IDLE))
            .image(Textures.SEAL_CANCEL, LARGE_BUTTON_SIZE, LARGE_BUTTON_SIZE)
            .soundEffect(Sounds.DELETE_BRANCH_CANCEL)
            .fixedTooltipPosition(true)
            .hoverBrightness(1.2f)
            .build();
    }
    // endregion
}