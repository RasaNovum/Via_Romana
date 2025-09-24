package net.rasanovum.viaromana.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.MapClient;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C;
import net.rasanovum.viaromana.network.packets.SignValidationC2S;
import net.rasanovum.viaromana.network.packets.TeleportRequestC2S;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.rasanovum.viaromana.teleport.TeleportHelper;

import java.awt.Point;
import java.util.*;
import java.util.stream.Collectors;

public class TeleportMapScreen extends Screen {

    //region Fields & Constants
    private MapClient.MapTexture mapTexture;
    private MapRenderer mapRenderer;
    private final List<DestinationResponseS2C.NodeNetworkInfo> networkNodes;

    // Data
    private final BlockPos signPos;
    private final BlockPos sourceNodePos;
    private final java.util.UUID networkId;
    private final List<TeleportHelper.TeleportDestination> destinations;
    private final Map<BlockPos, DestinationResponseS2C.NodeNetworkInfo> networkNodeMap;

    // Map Bounds
    private BlockPos minBounds, maxBounds;

    // Animation & State
    private float animationProgress = 0.0f;
    private final Set<BlockPos> animatedNodes = new HashSet<>();
    private final List<DestinationResponseS2C.NodeNetworkInfo> nodesToAnimate = new ArrayList<>();
    private final Map<BlockPos, Float> destinationFadeProgress = new HashMap<>();
    private final Set<BlockPos> validatedNodes = new HashSet<>();
    private final Set<BlockPos> destinationPositions;

    // Constants
    private static final int MARKER_SIZE = 16;
    private static final int PLAYER_MARKER_SIZE = 8;
    private static final float ANIM_SPEED = 2.0f;
    private static final float MARKER_FADE_SPEED = 0.05f;
    private static final int DIRECTION_INDICATOR_BUFFER = 2;
    private static final float DIRECTION_ANGLE_RANGE = 45.0f;
    private static final float DIRECTION_BASE_OPACITY = 1.0f;
    private static final float DIRECTION_FADE_CURVE = 1.0f;
    private static final int DIRECTION_COLOR_RGB = net.rasanovum.viaromana.client.ColorUtil.rgbToHex(1,1,1);
    //endregion

    //region Initialization
    public TeleportMapScreen(DestinationResponseS2C packet) {
        super(Component.literal("Teleport Network"));
        this.signPos = packet.signPos();
        this.sourceNodePos = packet.sourceNodePos();
        this.networkId = packet.networkId();
        this.networkNodes = new ArrayList<>(packet.networkNodes());

        this.destinations = packet.destinations().stream()
            .map(dest -> new TeleportHelper.TeleportDestination(dest.position, dest.name, dest.distance, dest.icon))
            .collect(Collectors.toList());

        this.networkNodeMap = packet.networkNodes().stream()
            .collect(Collectors.toMap(info -> info.position, info -> info));

        this.destinationPositions = this.destinations.stream()
            .map(dest -> dest.position)
            .collect(Collectors.toSet());

        calculateBounds();

        // Apply padding consistently using MapConstants
        int widthW = this.maxBounds.getX() - this.minBounds.getX();
        int heightW = this.maxBounds.getZ() - this.minBounds.getZ();
        int paddingX = Math.max(16, (int) (widthW * 0.1f));
        int paddingZ = Math.max(16, (int) (heightW * 0.1f));
        BlockPos paddedMin = this.minBounds.offset(-paddingX, 0, -paddingZ);
        BlockPos paddedMax = this.maxBounds.offset(paddingX, 0, paddingZ);

        // Prepare map renderer holder for drawing
        this.mapRenderer = new MapRenderer(paddedMin, paddedMax, networkNodes);

        // Request map from server
        requestMapAsync(paddedMin, paddedMax);
    }
    
    /**
     * Requests map from server and sets up texture when received.
     */
    private void requestMapAsync(BlockPos paddedMin, BlockPos paddedMax) {        
        MapClient.requestMap(networkId, paddedMin, paddedMax, networkNodes)
            .thenAccept(mapInfo -> {
                if (mapInfo != null) {
                    // Create texture on main thread
                    this.minecraft.execute(() -> {
                        if (this.mapTexture != null) {
                            this.mapTexture.close();
                        }
                        this.mapTexture = MapClient.createTexture(mapInfo);
                        if (this.mapRenderer != null) {
                            this.mapRenderer.setMapTexture(this.mapTexture);
                        }
                    });
                }
            })
            .exceptionally(ex -> {
                ViaRomana.LOGGER.error("Failed to load map for network {}", networkId, ex);
                return null;
            });
    }

    @Override
    protected void init() {
        super.init();
        if (this.sourceNodePos != null) {
            Optional.ofNullable(networkNodeMap.get(sourceNodePos)).ifPresent(nodesToAnimate::add);
        }
    }
    //endregion

    //region Main Render Loop
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        if (this.mapTexture == null || this.minecraft == null || this.minecraft.player == null || this.mapRenderer == null) return;
        
        this.mapRenderer.render(guiGraphics, this.width, this.height, this.minecraft.player);

        renderNetwork(guiGraphics, partialTicks);

        Set<BlockPos> revealedNodes = new HashSet<>(animatedNodes);
        nodesToAnimate.forEach(n -> revealedNodes.add(n.position));

        renderDestinationMarkers(guiGraphics, revealedNodes, mouseX, mouseY);
        renderPlayerMarker(guiGraphics, this.minecraft.player);
        renderTooltip(guiGraphics, revealedNodes, mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
    //endregion

    //region Network Animation
    private void renderNetwork(GuiGraphics guiGraphics, float partialTicks) {
        if (networkNodeMap.isEmpty()) return;

        // Update animation progress
        if (!nodesToAnimate.isEmpty()) {
            animationProgress = Math.min(1.0f, animationProgress + partialTicks * ANIM_SPEED);
        }

        Set<BlockPos> currentlyAnimatingSources = nodesToAnimate.stream()
                .map(info -> info.position)
                .collect(Collectors.toSet());

        // Draw connections
        for (DestinationResponseS2C.NodeNetworkInfo nodeInfo : networkNodeMap.values()) {
            final BlockPos startPos = nodeInfo.position;
            final boolean isStartCompleted = animatedNodes.contains(startPos);
            final boolean isStartAnimating = currentlyAnimatingSources.contains(startPos);

            if (!isStartCompleted && !isStartAnimating) continue;

            worldToScreen(startPos).ifPresent(startScreenPos -> {
                for (BlockPos endPos : nodeInfo.connections) {
                    final boolean isEndCompleted = animatedNodes.contains(endPos);
                    final boolean isEndAnimating = currentlyAnimatingSources.contains(endPos);

                    // Case 1: Connection between two fully completed nodes. Drawn once.
                    if (isStartCompleted && isEndCompleted) {
                        if (startPos.hashCode() < endPos.hashCode()) {
                            worldToScreen(endPos).ifPresent(endScreenPos -> drawLine(guiGraphics, startScreenPos, endScreenPos, 0xFFFFFFFF, 1));
                        }
                    }
                    // Case 2: "Bridge" connection from a completed node to the current animating wave.
                    else if (isStartCompleted && isEndAnimating) {
                        worldToScreen(endPos).ifPresent(endScreenPos -> drawLine(guiGraphics, startScreenPos, endScreenPos, 0xFFFFFFFF, 1));
                    }
                    // Case 3: The actively growing spline from the current wave to un-animated nodes.
                    else if (isStartAnimating && !isEndCompleted) {
                        worldToScreen(endPos).ifPresent(endScreenPos -> {
                            Point animatedEndPoint = getAnimatedPoint(startScreenPos, endScreenPos, animationProgress);
                            drawLine(guiGraphics, startScreenPos, animatedEndPoint, 0xFFFFFFFF, 1);
                        });
                    }
                }
            });
        }

        // Progress to the next animation wave if the current one is finished
        if (animationProgress >= 1.0f && !nodesToAnimate.isEmpty()) {
            Set<BlockPos> nextWavePositions = new HashSet<>();
            for (DestinationResponseS2C.NodeNetworkInfo completedNode : nodesToAnimate) {
                animatedNodes.add(completedNode.position);

                if (destinationPositions.contains(completedNode.position)) {
                    validateNodeSign(completedNode.position);
                }

                for (BlockPos connPos : completedNode.connections) {
                    if (!animatedNodes.contains(connPos)) {
                        nextWavePositions.add(connPos);
                    }
                }
            }

            nodesToAnimate.clear();
            nextWavePositions.stream()
                .map(networkNodeMap::get)
                .filter(Objects::nonNull)
                .forEach(nodesToAnimate::add);
            
            animationProgress = 0.0f;
        }
    }

    private Point getAnimatedPoint(Point start, Point end, float progress) {
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        return new Point((int) (start.x + dx * progress), (int) (start.y + dy * progress));
    }
    //endregion

    //region Marker & Tooltip Rendering
    private void renderDestinationMarkers(GuiGraphics guiGraphics, Set<BlockPos> revealedNodes, int mouseX, int mouseY) {
        // Update fade-in progress for newly validated destinations that have been revealed
        revealedNodes.stream()
            .filter(destinationPositions::contains)
            .filter(validatedNodes::contains)
            .forEach(pos -> {
                float progress = destinationFadeProgress.getOrDefault(pos, 0.0f);
                if (progress < 1.0f) {
                    destinationFadeProgress.put(pos, Math.min(1.0f, progress + MARKER_FADE_SPEED));
                }
            });

        // Render the markers that have begun fading in and are validated
        for (TeleportHelper.TeleportDestination dest : destinations) {
            if (destinationFadeProgress.containsKey(dest.position) && validatedNodes.contains(dest.position)) {
                worldToScreen(dest.position).ifPresent(screenPos -> {
                    float alpha = destinationFadeProgress.get(dest.position);

                    boolean isHovered = getDestinationAtPosition(revealedNodes, mouseX, mouseY)
                            .map(hovered -> hovered.position.equals(dest.position))
                            .orElse(false);

                    ResourceLocation markerTexture = ResourceLocation.parse("via_romana:textures/screens/marker_" + dest.icon.toString().toLowerCase() + ".png");
                    int x = screenPos.x - MARKER_SIZE / 2;
                    int y = screenPos.y - MARKER_SIZE / 2;

                    guiGraphics.pose().pushPose();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();

                    // Shadow
                    RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, alpha / 2);
                    guiGraphics.blit(markerTexture, x + 1, y + 1, 0, 0, MARKER_SIZE, MARKER_SIZE, MARKER_SIZE, MARKER_SIZE);

                    // Icon
                    float brightness = isHovered ? 1.25f : 1.0f;
                    RenderSystem.setShaderColor(brightness, brightness, brightness, alpha);
                    guiGraphics.blit(markerTexture, x, y, 0, 0, MARKER_SIZE, MARKER_SIZE, MARKER_SIZE, MARKER_SIZE);

                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                    RenderSystem.disableBlend();
                    guiGraphics.pose().popPose();
                });
            }
        }
    }

    private void renderPlayerMarker(GuiGraphics guiGraphics, Player player) {
        if (player == null || this.minecraft == null) return;

        worldToScreen(player.blockPosition()).ifPresent(screenPos -> {
            int x = screenPos.x - PLAYER_MARKER_SIZE / 2;
            int y = screenPos.y - PLAYER_MARKER_SIZE / 2;
            ResourceLocation skin = this.minecraft.getSkinManager().getInsecureSkin(player.getGameProfile()).texture();

            guiGraphics.pose().pushPose();
            RenderSystem.enableBlend();

            // Shadow
            RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 0.5f);
            guiGraphics.blit(skin, x + 1, y + 1, 8, 8, PLAYER_MARKER_SIZE, PLAYER_MARKER_SIZE, 64, 64);

            // Head
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            guiGraphics.blit(skin, x, y, 8, 8, PLAYER_MARKER_SIZE, PLAYER_MARKER_SIZE, 64, 64);

            RenderSystem.disableBlend();
            guiGraphics.pose().popPose();

            renderPlayerDirectionIndicator(guiGraphics, screenPos, PLAYER_MARKER_SIZE, player.getYRot());
        });
    }

    private void renderPlayerDirectionIndicator(GuiGraphics guiGraphics, Point centerPos, int markerSize, float yaw) {
        int indicatorSize = markerSize + DIRECTION_INDICATOR_BUFFER;
        float facingAngle = ((yaw % 360) + 360 + 180) % 360;
        int baseAlpha = (int) (255 * DIRECTION_BASE_OPACITY);
        int directionColor = (baseAlpha << 24) | DIRECTION_COLOR_RGB;

        renderDirectionalPixels(guiGraphics, centerPos, indicatorSize, facingAngle, directionColor);
    }

    private void renderDirectionalPixels(GuiGraphics guiGraphics, Point center, int size, float facingAngle, int baseColor) {
        int half = size / 2;
        int left = center.x - half;
        int top = center.y - half;

        // Iterate over the perimeter of the square indicator
        for (int i = 0; i < size * 4 - 4; i++) {
            int x, y;
            if (i < size) { x = left + i; y = top; } // Top edge
            else if (i < size * 2 - 1) { x = left + size - 1; y = top + (i - size + 1); } // Right edge
            else if (i < size * 3 - 2) { x = left + size - 1 - (i - (size * 2 - 2)); y = top + size - 1; } // Bottom edge
            else { x = left; y = top + size - 1 - (i - (size * 3 - 3)); } // Left edge

            float pixelAngle = getPixelAngle(center, new Point(x, y));
            if (isWithinAngleRange(pixelAngle, facingAngle, DIRECTION_ANGLE_RANGE)) {
                int fadedColor = getFadedColor(baseColor, pixelAngle, facingAngle, DIRECTION_ANGLE_RANGE);
                guiGraphics.fill(x, y, x + 1, y + 1, fadedColor);
            }
        }
    }

    private void renderTooltip(GuiGraphics guiGraphics, Set<BlockPos> revealedNodes, int mouseX, int mouseY) {
        getDestinationAtPosition(revealedNodes, mouseX, mouseY).ifPresentOrElse(
            dest -> {
                long dist = Math.round(dest.distance);
                String unit = dist > 1000 ? "km" : "m";
                long displayDist = dist > 1000 ? dist / 1000 : dist;
                String text = String.format("%s - %d %s", dest.name, displayDist, unit);
                guiGraphics.renderTooltip(this.font, Component.literal(text), mouseX, mouseY);
            },
            () -> {
                if (isMouseOverPlayer(mouseX, mouseY)) {
                    guiGraphics.renderTooltip(this.font, Component.translatable("gui.viaromana.player_marker"), mouseX, mouseY);
                }
            }
        );
    }
    //endregion

    //region Input & Interaction
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Set<BlockPos> revealedNodes = new HashSet<>(animatedNodes);
            nodesToAnimate.forEach(node -> revealedNodes.add(node.position));
            getDestinationAtPosition(revealedNodes, (int) mouseX, (int) mouseY).ifPresent(this::selectDestination);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void selectDestination(TeleportHelper.TeleportDestination destination) {
        if (minecraft == null || minecraft.player == null) return;
        TeleportRequestC2S packet = new TeleportRequestC2S(this.signPos, destination.position);

        if (ViaRomanaModVariables.networkHandler != null) {
            ViaRomanaModVariables.networkHandler.sendToServer(packet);
        } else {
            ViaRomana.LOGGER.error("Network handler is null - cannot send teleport request");
        }
        this.onClose();
    }
    //endregion

    //region Networking
    private void validateNodeSign(BlockPos nodePos) {
        if (!validatedNodes.contains(nodePos)) {
            SignValidationC2S packet = new SignValidationC2S(nodePos);
            if (ViaRomanaModVariables.networkHandler != null) {
                ViaRomanaModVariables.networkHandler.sendToServer(packet);
            }
        }
    }

    public void handleSignValidation(BlockPos nodePos, boolean isValid) {
        if (isValid) {
            validatedNodes.add(nodePos);
        }
    }
    //endregion

    //region Utility & Helpers
    private void calculateBounds() {
        if (networkNodeMap.isEmpty()) {
            BlockPos playerPos = minecraft.player != null ? minecraft.player.blockPosition() : BlockPos.ZERO;
            this.minBounds = playerPos.offset(-128, 0, -128);
            this.maxBounds = playerPos.offset(128, 0, 128);
            return;
        }

        IntSummaryStatistics xStats = networkNodeMap.keySet().stream().mapToInt(BlockPos::getX).summaryStatistics();
        IntSummaryStatistics zStats = networkNodeMap.keySet().stream().mapToInt(BlockPos::getZ).summaryStatistics();

        int width = xStats.getMax() - xStats.getMin();
        int height = zStats.getMax() - zStats.getMin();
        int padding = Math.max(16, (int) (Math.max(width, height) * 0.1f));

        this.minBounds = new BlockPos(xStats.getMin() - padding, 0, zStats.getMin() - padding);
        this.maxBounds = new BlockPos(xStats.getMax() + padding, 0, zStats.getMax() + padding);
    }

    private Optional<Point> worldToScreen(BlockPos worldPos) {
        if (mapRenderer == null) return Optional.empty();
        return Optional.ofNullable(mapRenderer.worldToScreen(worldPos, this.width, this.height));
    }

    private boolean isMouseOver(Point screenPos, int size, int mouseX, int mouseY) {
        int tolerance = size / 2;
        return Math.abs(mouseX - screenPos.x) <= tolerance && Math.abs(mouseY - screenPos.y) <= tolerance;
    }

    private boolean isMouseOverPlayer(int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null) return false;
        return worldToScreen(minecraft.player.blockPosition())
                .map(screenPos -> isMouseOver(screenPos, PLAYER_MARKER_SIZE, mouseX, mouseY))
                .orElse(false);
    }

    private Optional<TeleportHelper.TeleportDestination> getDestinationAtPosition(Set<BlockPos> revealedNodes, int mouseX, int mouseY) {
        for (TeleportHelper.TeleportDestination dest : destinations) {
            if (revealedNodes.contains(dest.position) && validatedNodes.contains(dest.position)) {
                Optional<Point> screenPosOpt = worldToScreen(dest.position);
                if (screenPosOpt.isPresent()) {
                    if (isMouseOver(screenPosOpt.get(), MARKER_SIZE, mouseX, mouseY)) {
                        return Optional.of(dest);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private void drawLine(GuiGraphics guiGraphics, Point start, Point end, int color, int thickness) {
        // Bresenham's line algorithm
        int dx = Math.abs(end.x - start.x), sx = start.x < end.x ? 1 : -1;
        int dy = -Math.abs(end.y - start.y), sy = start.y < end.y ? 1 : -1;
        int err = dx + dy, e2;
        int x = start.x, y = start.y;
        int half = thickness / 2;

        while (true) {
            guiGraphics.fill(x - half, y - half, x + thickness - half, y + thickness - half, color);
            if (x == end.x && y == end.y) break;
            e2 = 2 * err;
            if (e2 >= dy) { err += dy; x += sx; }
            if (e2 <= dx) { err += dx; y += sy; }
        }
    }

    private float getPixelAngle(Point center, Point pixel) {
        return (float) ((Math.toDegrees(Math.atan2(pixel.x - center.x, center.y - pixel.y)) + 360) % 360);
    }

    private boolean isWithinAngleRange(float angle, float target, float range) {
        float diff = Math.abs(angle - target);
        if (diff > 180) diff = 360 - diff;
        return diff <= range;
    }

    private int getFadedColor(int baseColor, float pixelAngle, float facingAngle, float angleRange) {
        float diff = Math.abs(pixelAngle - facingAngle);
        if (diff > 180) diff = 360 - diff;

        float fadeFactor = (float) Math.pow(1.0f - (diff / angleRange), DIRECTION_FADE_CURVE);
        fadeFactor = Math.max(0.0f, Math.min(1.0f, fadeFactor));

        int alpha = (baseColor >> 24) & 0xFF;
        int fadedAlpha = (int) (alpha * fadeFactor);
        return (fadedAlpha << 24) | (baseColor & 0x00FFFFFF);
    }
    //endregion

    //region Overrides
    @Override
    public void onClose() {
        // Clean up map texture
        if (this.mapTexture != null) {
            this.mapTexture.close();
            this.mapTexture = null;
        }
        
        // Clean up map renderer
        if (this.mapRenderer != null) {
            this.mapRenderer.close();
            this.mapRenderer = null;
        }
        
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    //endregion
}