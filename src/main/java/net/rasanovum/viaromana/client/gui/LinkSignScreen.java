package net.rasanovum.viaromana.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.client.gui.elements.*;
import net.rasanovum.viaromana.core.LinkHandler;
import net.rasanovum.viaromana.network.packets.SignLinkRequestC2S;
import net.rasanovum.viaromana.network.packets.SignUnlinkRequestC2S;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.util.VersionUtils;
import commonnetwork.api.Dispatcher;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class LinkSignScreen extends Screen {
    // region Constants
    private static final int BACKGROUND_WIDTH = 256;
    private static final int BACKGROUND_HEIGHT = 256;
    private static final int USABLE_WIDTH = 230;
    private static final int USABLE_HEIGHT = 160;
    private static final ResourceLocation BACKGROUND_TEXTURE = VersionUtils.getLocation("via_romana:textures/screens/background_map.png");
    // endregion

    // region UI State
    private final BlockPos nodePos;
    private final BlockPos signPos;
    private final UUID playerUuid;
    private final String playerName;
    private final boolean isTempNode;
    private final boolean isSignLinked;

    private MapEditBox destinationNameField;
    private MapButtonGroup<Node.LinkType> linkTypeButtonGroup;
    private MapCycleButton<Node.LinkType> destinationButton;
    private MapCycleButton<Node.LinkType> privateButton;
    private MapCycleButton<Node.LinkType> accessButton;
    private MapIconButtonGroup iconButtonGroup;
    private MapSignatureButton signatureButton;
    private MapActionButton unlinkButton;
    private String destinationName = "Travel Destination";
    private Node.LinkType linkType = Node.LinkType.DESTINATION;
    private Node.Icon icon = Node.Icon.SIGNPOST;
    // endregion

    public LinkSignScreen(Player player, LinkHandler.LinkData linkData, boolean isTempNode, boolean isSignLinked) {
        super(Component.translatable("gui.viaromana.configure_sign_link.title"));
        this.playerUuid = player.getUUID();
        this.playerName = player.getName().getString();
        this.signPos = linkData.signPos();
        this.nodePos = linkData.nodePos();
        this.linkType = linkData.linkType() != null ? linkData.linkType() : Node.LinkType.DESTINATION;
        this.icon = linkData.icon() != null ? linkData.icon() : Node.Icon.SIGNPOST;
        this.destinationName = linkData.destinationName() != null ? linkData.destinationName() : "Travel Destination";
        this.isTempNode = isTempNode;
        this.isSignLinked = isSignLinked;
    }

    // region Initialization
    @Override
    protected void init() {
        super.init();
        
        int panelX = (this.width - BACKGROUND_WIDTH) / 2;
        int panelY = (this.height - BACKGROUND_HEIGHT) / 2;
        
        int usableX = panelX + (BACKGROUND_WIDTH - USABLE_WIDTH) / 2;
        int usableY = panelY + (BACKGROUND_HEIGHT - USABLE_HEIGHT) / 2;
        usableY -= 8;
        
        this.destinationNameField = new MapEditBox(this.font, usableX + 10, usableY + 30, USABLE_WIDTH - 20, 20, Component.translatable("gui.viaromana.destination_name"));
        this.destinationNameField.setValue(this.destinationName);
        this.destinationNameField.setMaxLength(50);
        this.destinationNameField.setTextColor(GuiConstants.TEXT_COLOR_PRIMARY);
        this.addRenderableWidget(this.destinationNameField);
        
        this.linkTypeButtonGroup = new MapButtonGroup<>(value -> this.linkType = value);
        
        int buttonWidth = (USABLE_WIDTH / 2) - 40;
        int buttonHeight = 16;
        int buttonStartX = usableX + 10;
        
        this.destinationButton = new MapCycleButton<>(
            this.font, buttonStartX, usableY + 74, buttonWidth, buttonHeight,
            Arrays.asList(Node.LinkType.DESTINATION), Node.LinkType.DESTINATION,
            linkType -> Component.translatable("gui.viaromana.destination_button"),
            null, Component.translatable("gui.viaromana.destination_tooltip")
        );
        
        this.privateButton = new MapCycleButton<>(
            this.font, buttonStartX, usableY + 94, buttonWidth, buttonHeight,
            Arrays.asList(Node.LinkType.PRIVATE), Node.LinkType.PRIVATE,
            linkType -> Component.translatable("gui.viaromana.private_button"),
            null, Component.translatable("gui.viaromana.private_tooltip")
        );
        
        this.accessButton = new MapCycleButton<>(
            this.font, buttonStartX, usableY + 114, buttonWidth, buttonHeight,
            Arrays.asList(Node.LinkType.ACCESS), Node.LinkType.ACCESS,
            linkType -> Component.translatable("gui.viaromana.access_button"),
            null, Component.translatable("gui.viaromana.access_tooltip")
        );
        
        this.linkTypeButtonGroup.addButton(this.destinationButton);
        this.linkTypeButtonGroup.addButton(this.privateButton);
        this.linkTypeButtonGroup.addButton(this.accessButton);
        
        this.linkTypeButtonGroup.selectByValue(this.linkType);
        
        this.addRenderableWidget(this.destinationButton);
        this.addRenderableWidget(this.privateButton);
        this.addRenderableWidget(this.accessButton);
        
        int iconGridX = usableX + (USABLE_WIDTH / 2) - 20;
        int iconGridY = usableY + 70;
        this.iconButtonGroup = new MapIconButtonGroup(value -> this.icon = value);
        
        List<MapIconButton> iconButtons = this.iconButtonGroup.createIconButtons(this.font, iconGridX, iconGridY, 4, 0);
        
        for (MapIconButton iconBtn : iconButtons) this.addRenderableWidget(iconBtn);
        
        this.iconButtonGroup.selectByIcon(this.icon);
        this.signatureButton = new MapSignatureButton(this.font, usableX + 10, usableY + 146, USABLE_WIDTH - 100, this.playerName, 
            (value) -> this.confirmLinking()
        );
        this.addRenderableWidget(this.signatureButton);
        
        if (this.isSignLinked) {
            int unlinkButtonWidth = 60;
            int unlinkButtonHeight = 16;
            this.unlinkButton = new MapActionButton(usableX + USABLE_WIDTH - unlinkButtonWidth - 10, usableY + 148, unlinkButtonWidth, unlinkButtonHeight,
            Component.translatable("gui.viaromana.unlink_button"),
            Component.translatable("gui.viaromana.unlink_tooltip"),
                (value) -> {
                    commonnetwork.api.Dispatcher.sendToServer(new SignUnlinkRequestC2S(this.signPos));
                    this.onClose();
                },
                VersionUtils.getLocation("via_romana:textures/screens/element_unlink.png")
            );
            this.addRenderableWidget(this.unlinkButton);
        }
        
        this.setInitialFocus(this.destinationNameField);
    }

    // region Rendering & Ticking
    @Override
    public void tick() {
        super.tick();
        this.destinationNameField.tick();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Skip 1.21 background rendering
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        int panelX = (this.width - BACKGROUND_WIDTH) / 2;
        int panelY = (this.height - BACKGROUND_HEIGHT) / 2;
        
        int usableX = panelX + (BACKGROUND_WIDTH - USABLE_WIDTH) / 2;
        int usableY = panelY + (BACKGROUND_HEIGHT - USABLE_HEIGHT) / 2;
        usableY -= 8;

        this.renderTransparentBackground(guiGraphics);
        
        guiGraphics.blit(BACKGROUND_TEXTURE, panelX, panelY, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        
        Component title = Component.translatable("gui.viaromana.configure_sign_link.title");
        int titleWidth = this.font.width(title);

        guiGraphics.drawString(this.font, title, usableX + (USABLE_WIDTH - titleWidth) / 2, usableY + 10, GuiConstants.TEXT_COLOR_PRIMARY, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.viaromana.name_label"), usableX + 10, usableY + 20, GuiConstants.TEXT_COLOR_PRIMARY, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.viaromana.type_label"), usableX + 10, usableY + 60, GuiConstants.TEXT_COLOR_PRIMARY, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.viaromana.icon_label"), usableX + (USABLE_WIDTH / 2) - 20, usableY + 60, GuiConstants.TEXT_COLOR_PRIMARY, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.viaromana.signature_label"), usableX + 10, usableY + 138, GuiConstants.TEXT_COLOR_PRIMARY, false);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    // endregion
    
    // region Actions & Logic
    
    private void confirmLinking() {
        String finalName = this.destinationNameField.getValue().trim();
        if (finalName.isEmpty()) {
            finalName = Component.translatable("gui.viaromana.default_destination_name").getString();
        }

        Node.LinkType selectedLinkType = this.linkType;
        if (selectedLinkType == null) {
            selectedLinkType = Node.LinkType.DESTINATION;
        }

        Node.Icon selectedIcon = this.icon;
        if (selectedIcon == null) {
            selectedIcon = Node.Icon.SIGNPOST;
        }

        UUID owner = (selectedLinkType == Node.LinkType.PRIVATE) ? this.playerUuid : null;

        LinkHandler.LinkData linkData = new LinkHandler.LinkData(
            this.signPos,
            this.nodePos,
            selectedLinkType,
            selectedIcon,
            finalName,
            owner
        );

        SignLinkRequestC2S packet = new SignLinkRequestC2S(linkData, this.isTempNode);
        Dispatcher.sendToServer(packet);
        
        ClientPathData clientPathData = ClientPathData.getInstance();
        boolean isTargetNodeTemp = clientPathData.isTemporaryNode(this.nodePos);
        boolean hasExistingTempLink = clientPathData.getTemporarySignLink(this.signPos).isPresent();
        
        if (isTargetNodeTemp || hasExistingTempLink) {
            clientPathData.addTemporaryLink(linkData);
        } else if (hasExistingTempLink && !isTargetNodeTemp) {
            clientPathData.getTemporarySignLink(this.signPos).ifPresent(clientPathData::removeTemporaryLink);
        }

        this.onClose();
    }
    // endregion
    
    // region Event Handlers
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 && this.destinationNameField.isFocused()) { // Enter key
            this.confirmLinking();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    // endregion
}