package net.rasanovum.viaromana.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.client.gui.LinkSignScreen;
import net.rasanovum.viaromana.client.gui.WarpBlockScreen;
import net.rasanovum.viaromana.client.gui.elements.ForceTooltipButton;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.core.LinkHandler;
import net.rasanovum.viaromana.core.LinkHandler.LinkData;
import net.rasanovum.viaromana.mixins.client.AbstractSignEditScreenAccessor;

import java.util.Optional;

public class SignEditHelper {
    public static Button createLinkButton(Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity player = minecraft.player;
        if (player == null) return null;

        ClientPathData clientPathData = ClientPathData.getInstance();
        BlockPos signPos;

        if (screen instanceof AbstractSignEditScreen signEditScreen) {
            signPos = ((AbstractSignEditScreenAccessor) signEditScreen).getSign().getBlockPos();
        } else if (screen instanceof WarpBlockScreen warpBlockScreen) {
            signPos = warpBlockScreen.getBlockPos();
        } else if (screen instanceof net.mehvahdjukaar.supplementaries.client.screens.SignPostScreen signPostScreen) {
            try {
                java.lang.reflect.Field tileField = signPostScreen.getClass().getSuperclass().getDeclaredField("tile");
                tileField.setAccessible(true);
                Object tile = tileField.get(signPostScreen);
                if (tile instanceof BlockEntity blockEntity) {
                    signPos = blockEntity.getBlockPos();
                } else {
                    return null;
                }
            } catch (Exception e) {
                ViaRomana.LOGGER.error("Via Romana: Reflection failed: " + e.getMessage());
                return null;
            }
        } else {
            return null;
        }

        if (!LinkHandler.isSignBlock(minecraft.level, signPos)) return null;

        boolean isSignPermLinked = LinkHandler.isSignLinked(player.level(), signPos);
        boolean isSignTempLinked = clientPathData.isNodeSignLinked(null, signPos);
        Node linkedNode = LinkHandler.getLinkedNode(player.level(), signPos).orElse(null);

        boolean disableButton = false;
        boolean isNodeTemp = false;
        String buttonText = "";
        String tooltip = "";

        LinkData linkData = null;

        if (isSignPermLinked || isSignTempLinked) {
            if (isSignPermLinked) {
                linkData = LinkHandler.getLinkData(player.level(), signPos).orElse(null);
            } else {
                linkData = clientPathData.getTemporarySignLink(signPos).orElse(null);
                if (linkData != null) {
                    isNodeTemp = clientPathData.isTemporaryNode(linkData.nodePos());
                }
            }

            boolean hasAccess = LinkHandler.hasAccess(player, linkedNode);

            if (hasAccess || isSignTempLinked) {
                buttonText = "gui.viaromana.edit_destination";
                tooltip = "gui.viaromana.edit_destination_tooltip";
            } else {
                disableButton = true;
                buttonText = "gui.viaromana.no_access";
                tooltip = "gui.viaromana.no_access_tooltip";
            }
        } else {
            Optional<Node> nearestNode = clientPathData.getNearestNode(signPos, CommonConfig.node_utility_distance * 2, true, node -> !node.isLinked(), node -> !clientPathData.isNodeSignLinked(node.getBlockPos(), null));
            boolean isNearNode = nearestNode.isPresent();
            buttonText = "gui.viaromana.add_to_path";

            if (isNearNode) {
                Node node = nearestNode.get();
                isNodeTemp = clientPathData.isTemporaryNode(node.getBlockPos());

                linkData = new LinkData(
                        signPos,
                        node.getBlockPos(),
                        Node.LinkType.DESTINATION,
                        Node.Icon.SIGNPOST,
                        Component.translatable("gui.viaromana.default_destination_name").getString(),
                        null
                );

                tooltip = "gui.viaromana.add_to_path_tooltip";
            } else {
                disableButton = true;
                tooltip = "gui.viaromana.place_near_node_tooltip";
            }
        }

        final LinkData finalLinkData = linkData;
        final boolean finalIsNodeTemp = isNodeTemp;
        final boolean finalIsSignLinked = isSignPermLinked || isSignTempLinked;

        ForceTooltipButton signLinkButton = new ForceTooltipButton(screen.width / 2 - 100, screen.height / 4 + (144 - 24), 200, 20,
        Component.translatable(buttonText),
            (button) -> {
                if (finalLinkData != null) {
                    LinkSignScreen linkScreen = new LinkSignScreen(Minecraft.getInstance().player, finalLinkData, finalIsNodeTemp, finalIsSignLinked);
                    Minecraft.getInstance().setScreen(linkScreen);
                }
            },
            Component.translatable(tooltip)
        );

        signLinkButton.active = !disableButton;
        return signLinkButton;
    }
}