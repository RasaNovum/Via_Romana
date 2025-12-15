package net.rasanovum.viaromana.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.corgitaco.dataanchor.network.broadcast.PacketBroadcaster;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.FadeManager;
import net.rasanovum.viaromana.client.HudMessageManager;
import net.rasanovum.viaromana.client.MapClient;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C;
import net.rasanovum.viaromana.network.packets.SignValidationRequestC2S;
import net.rasanovum.viaromana.network.packets.TeleportRequestC2S;
import net.rasanovum.viaromana.storage.player.PlayerData;
import net.rasanovum.viaromana.teleport.TeleportHelper;
import net.rasanovum.viaromana.util.EffectUtils;
import net.rasanovum.viaromana.util.VersionUtils;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
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
    private long animationStartMillis = -1L;
    private long totalAnimationDurationMillis = 500;
    private int totalAnimationWaves = 1;
    private int completedAnimationWaves = 0;
    private final Set<BlockPos> animatedNodes = new HashSet<>();
    private final List<DestinationResponseS2C.NodeNetworkInfo> nodesToAnimate = new ArrayList<>();
    private final Map<BlockPos, Float> destinationFadeProgress = new HashMap<>();
    private final Set<BlockPos> validatedNodes = new HashSet<>();
    private final Set<BlockPos> destinationPositions;

    private static final float SCREEN_FADE_IN_SPEED = 0.05f;
    private float screenAlpha = 0.0f;

    // Constants
    private static final int MARKER_SIZE = 16;
    private static final int PLAYER_MARKER_SIZE = 8;
    private static final float MARKER_FADE_SPEED = 0.05f;
    private static final int DIRECTION_INDICATOR_BUFFER = 2;
    private static final float DIRECTION_ANGLE_RANGE = 45.0f;
    private static final float DIRECTION_BASE_OPACITY = 1.0f;
    private static final float DIRECTION_FADE_CURVE = 1.0f;
    private static final int DIRECTION_COLOR_RGB = 0xFFFFFF;
    private static int LINE_COLOR_BASE = 0xFFFFFF;
    private static int LINE_COLOR_UNDERGROUND = 0xFFFFFF;
    private static float LINE_OPACITY = 1.0f;
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

        configureAnimationTiming();
        calculateBounds();

        this.mapRenderer = new MapRenderer(this.minBounds, this.maxBounds);

        requestMapAsync(this.minBounds, this.maxBounds);
    }

    private void requestMapAsync(BlockPos paddedMin, BlockPos paddedMax) {
        MapClient.requestMap(networkId, paddedMin, paddedMax, networkNodes)
                .thenAccept(mapInfo -> {
                    if (mapInfo != null) {
                        assert this.minecraft != null;
                        this.minecraft.execute(() -> {
                            if (this.mapTexture != null) {
                                this.mapTexture.close();
                            }
                            this.mapTexture = MapClient.createTexture(mapInfo);
                            if (this.mapRenderer != null) {
                                this.mapRenderer.setMapTexture(this.mapTexture);
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
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
    //? if >1.21
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Skip 1.21 background rendering
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        if (this.mapTexture == null || this.minecraft == null || this.minecraft.player == null || this.mapRenderer == null) return;

        //? if neoforge
        /*screenAlpha = 1.0f;*/

        if (this.screenAlpha < 1.0f) {
            this.screenAlpha = Math.min(1.0f, this.screenAlpha + SCREEN_FADE_IN_SPEED);
        }

        //? if >1.21
        this.renderBlurredBackground(partialTicks);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.screenAlpha);

        this.mapRenderer.render(guiGraphics, this.width, this.height);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        renderNetwork(guiGraphics);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0f, 0.0f, 10.0f);

        Set<BlockPos> revealedNodes = getRevealedNodes();
        TeleportHelper.TeleportDestination hoveredDestination = findDestinationAtPosition(revealedNodes, mouseX, mouseY);

        renderDestinationMarkers(guiGraphics, revealedNodes, hoveredDestination);
        renderPlayerMarker(guiGraphics, this.minecraft.player);
        renderTooltip(guiGraphics, hoveredDestination, mouseX, mouseY);

        guiGraphics.pose().popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private int applyGlobalFade(int color) {
        if (this.screenAlpha >= 1.0f) return color;
        int alpha = (color >> 24) & 0xFF;
        int newAlpha = (int)(alpha * this.screenAlpha);
        return (newAlpha << 24) | (color & 0x00FFFFFF);
    }

    private int applyLineAlpha(int color) {
        if (LINE_OPACITY >= 1.0f) return color;
        int alpha = (color >> 24) & 0xFF;
        int newAlpha = (int)(alpha * LINE_OPACITY);
        return (newAlpha << 24) | (color & 0x00FFFFFF);
    }
    //endregion

    //region Network Animation
    private void renderNetwork(GuiGraphics guiGraphics) {
        if (networkNodeMap.isEmpty() || mapRenderer == null) return;

        float scale = 1.0f;
        Point p1 = mapRenderer.worldToScreen(minBounds, this.width, this.height);
        Point p2 = mapRenderer.worldToScreen(minBounds.offset(1, 0, 0), this.width, this.height);

        if (p1 != null && p2 != null) {
            double dist = Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
            if (dist > 0.001) {
                scale = (float) dist;
            }
        }

        float drawSize = Math.max(1.0f, scale);

        LINE_COLOR_BASE = Color.decode(CommonConfig.line_colors.get(0)).getRGB();
        LINE_COLOR_UNDERGROUND = Color.decode(CommonConfig.line_colors.get(1)).getRGB();
        LINE_OPACITY = CommonConfig.line_opacity;

        updateAnimationProgress();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = guiGraphics.pose().last().pose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f renderMatrix = new Matrix4f(matrix);
        renderMatrix.translate(0.0f, 0.0f, 5.0f);

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LESS);
        RenderSystem.depthMask(true);

        Set<BlockPos> dynamicNodeSet = new HashSet<>();
        for (DestinationResponseS2C.NodeNetworkInfo n : nodesToAnimate) {
            dynamicNodeSet.add(n.position);
        }

        for (DestinationResponseS2C.NodeNetworkInfo nodeInfo : this.networkNodes) {
            BlockPos startPos = nodeInfo.position;
            boolean isStatic = animatedNodes.contains(startPos);
            boolean isDynamic = dynamicNodeSet.contains(startPos);

            if (!isStatic && !isDynamic) continue;

            Optional<Point> startScreenPosOpt = worldToScreen(startPos);
            if (startScreenPosOpt.isEmpty()) continue;
            Point startScreenPos = startScreenPosOpt.get();

            for (BlockPos endPos : nodeInfo.connections) {
                boolean isEndStatic = animatedNodes.contains(endPos);
                boolean isEndDynamic = dynamicNodeSet.contains(endPos);

                float progress = -1.0f;

                if (isStatic && (isEndStatic || isEndDynamic)) progress = 1.0f;
                else if (isDynamic && !isEndStatic) progress = animationProgress;

                if (progress >= 0.0f) drawConnection(buffer, renderMatrix, startScreenPos, endPos, nodeInfo, progress, scale, drawSize);
            }
        }

        MeshData mesh = buffer.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);

        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void drawConnection(BufferBuilder buffer, Matrix4f matrix, Point startScreenPos, BlockPos endPos, DestinationResponseS2C.NodeNetworkInfo startInfo, float progress, float scale, float drawSize) {
        DestinationResponseS2C.NodeNetworkInfo endInfo = networkNodeMap.get(endPos);
        boolean bothUnderground = startInfo.clearance > 0 && startInfo.clearance < 24 &&
                endInfo != null && endInfo.clearance > 0 && endInfo.clearance < 24;

        int color = bothUnderground ? LINE_COLOR_UNDERGROUND : LINE_COLOR_BASE;
        color = applyLineAlpha(color);

        Optional<Point> endScreenPosOpt = worldToScreen(endPos);
        if (endScreenPosOpt.isEmpty()) return;
        Point targetScreenPos = endScreenPosOpt.get();

        if (progress < 1.0f) {
            Point animatedEndPoint = getAnimatedPoint(startScreenPos, targetScreenPos, progress);
            drawPixelLine(buffer, matrix, startScreenPos, animatedEndPoint, scale, drawSize, color);
        } else {
            drawPixelLine(buffer, matrix, startScreenPos, targetScreenPos, scale, drawSize, color);
        }
    }

    /**
     * Draws a line conforming to the map's pixel grid
     */
    private void drawPixelLine(BufferBuilder buffer, Matrix4f matrix, Point p1, Point p2, float scale, float drawSize, int color) {
        int x0 = (int)(p1.x / scale);
        int y0 = (int)(p1.y / scale);
        int x1 = (int)(p2.x / scale);
        int y1 = (int)(p2.y / scale);

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            float drawX = x0 * scale;
            float drawY = y0 * scale;

            addPixelQuad(buffer, matrix, drawX, drawY, drawSize, color);

            if (x0 == x1 && y0 == y1) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private void addPixelQuad(BufferBuilder buffer, Matrix4f matrix, float x, float y, float size, int color) {
        addVertex(buffer, matrix, x, y, color);
        addVertex(buffer, matrix, x, y + size, color);
        addVertex(buffer, matrix, x + size, y + size, color);
        addVertex(buffer, matrix, x + size, y, color);
    }

    private void addVertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        Vector4f vec = new Vector4f(x, y, 0.0F, 1.0F);
        matrix.transform(vec);

        buffer.addVertex(vec.x, vec.y, vec.z).setColor(r, g, b, a);
    }

    private void updateAnimationProgress() {
        if (nodesToAnimate.isEmpty()) {
            return;
        }

        long now = Util.getMillis();
        if (animationStartMillis < 0L) {
            animationStartMillis = now;
        }

        float normalizedProgress = totalAnimationDurationMillis <= 0
                ? 1.0f
                : (now - animationStartMillis) / (float) totalAnimationDurationMillis;
        normalizedProgress = Math.max(0.0f, Math.min(1.0f, normalizedProgress));

        float globalWaveProgress = normalizedProgress * totalAnimationWaves;
        int targetCompletedWaves = Math.min(totalAnimationWaves, (int) Math.floor(globalWaveProgress));

        while (completedAnimationWaves < targetCompletedWaves && !nodesToAnimate.isEmpty()) {
            advanceAnimationWave();
        }

        if (nodesToAnimate.isEmpty()) {
            animationProgress = 1.0f;
            return;
        }

        animationProgress = Math.max(0.0f, Math.min(1.0f, globalWaveProgress - completedAnimationWaves));
    }

    private void advanceAnimationWave() {
        if (nodesToAnimate.isEmpty()) return;

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

        completedAnimationWaves++;

        if (nodesToAnimate.isEmpty()) {
            animationProgress = 1.0f;
        }
    }

    private Point getAnimatedPoint(Point start, Point end, float progress) {
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        return new Point((int) (start.x + dx * progress), (int) (start.y + dy * progress));
    }
    //endregion

    //region Marker & Tooltip Rendering
    private void renderDestinationMarkers(GuiGraphics guiGraphics, Set<BlockPos> revealedNodes, TeleportHelper.TeleportDestination hoveredDestination) {
        for (TeleportHelper.TeleportDestination dest : destinations) {
            boolean isValidated = validatedNodes.contains(dest.position) || sourceNodePos == null;
            boolean isRevealed = revealedNodes.contains(dest.position);

            if (isRevealed && isValidated) {
                destinationFadeProgress.merge(dest.position, MARKER_FADE_SPEED, (a, b) -> Math.min(1.0f, a + b));
            }

            if (destinationFadeProgress.containsKey(dest.position) && isValidated) {
                worldToScreen(dest.position).ifPresent(screenPos -> {
                    float alpha = destinationFadeProgress.get(dest.position) * this.screenAlpha;

                    boolean isHovered = hoveredDestination != null && hoveredDestination.position.equals(dest.position);

                    ResourceLocation markerTexture = VersionUtils.getLocation("via_romana:textures/screens/marker_" + dest.icon.toString().toLowerCase() + ".png");
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
            //? if <1.21 {
            /*ResourceLocation skin = this.minecraft.getSkinManager().getInsecureSkinLocation(player.getGameProfile());
             *///?} else {
            ResourceLocation skin = this.minecraft.getSkinManager().getInsecureSkin(player.getGameProfile()).texture();
            //?}

            guiGraphics.pose().pushPose();
            RenderSystem.enableBlend();

            // Shadow
            RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 0.5f * this.screenAlpha);
            guiGraphics.blit(skin, x + 1, y + 1, 8, 8, PLAYER_MARKER_SIZE, PLAYER_MARKER_SIZE, 64, 64);

            // Head
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.screenAlpha);
            guiGraphics.blit(skin, x, y, 8, 8, PLAYER_MARKER_SIZE, PLAYER_MARKER_SIZE, 64, 64);
            guiGraphics.blit(skin, x, y, 40, 8, PLAYER_MARKER_SIZE, PLAYER_MARKER_SIZE, 64, 64);

            RenderSystem.disableBlend();
            guiGraphics.pose().popPose();

            renderPlayerDirectionIndicator(guiGraphics, screenPos, player.getYRot());
        });
    }

    private void renderPlayerDirectionIndicator(GuiGraphics guiGraphics, Point centerPos, float yaw) {
        int indicatorSize = PLAYER_MARKER_SIZE + DIRECTION_INDICATOR_BUFFER;
        float facingAngle = ((yaw % 360) + 360 + 180) % 360;
        int baseAlpha = (int) (255 * DIRECTION_BASE_OPACITY);
        int directionColor = (baseAlpha << 24) | DIRECTION_COLOR_RGB;

        renderDirectionalPixels(guiGraphics, centerPos, indicatorSize, facingAngle, directionColor); // TODO: Make this less bad
    }

    private void renderDirectionalPixels(GuiGraphics guiGraphics, Point center, int size, float facingAngle, int baseColor) {
        int half = size / 2;
        int left = center.x - half;
        int top = center.y - half;

        for (int i = 0; i < size * 4 - 4; i++) {
            int x, y;
            if (i < size) { x = left + i; y = top; } // Top edge
            else if (i < size * 2 - 1) { x = left + size - 1; y = top + (i - size + 1); } // Right edge
            else if (i < size * 3 - 2) { x = left + size - 1 - (i - (size * 2 - 2)); y = top + size - 1; } // Bottom edge
            else { x = left; y = top + size - 1 - (i - (size * 3 - 3)); } // Left edge

            float pixelAngle = getPixelAngle(center, new Point(x, y));
            if (isWithinAngleRange(pixelAngle, facingAngle)) {
                int fadedColor = getFadedColor(baseColor, pixelAngle, facingAngle);
                fadedColor = applyGlobalFade(fadedColor);
                guiGraphics.fill(x, y, x + 1, y + 1, fadedColor);
            }
        }
    }

    private void renderTooltip(GuiGraphics guiGraphics, TeleportHelper.TeleportDestination hoveredDestination, int mouseX, int mouseY) {
        if (this.screenAlpha < 0.8f) return;

        if (hoveredDestination != null) {
            long dist = Math.round(hoveredDestination.distance);
            long displayDist = dist > 1000 ? dist / 1000 : dist;
            Component text = Component.translatable(
                    "gui.viaromana.distance_" + (dist > 1000 ? "kilometers" : "meters"),
                    hoveredDestination.name, displayDist
            );
            guiGraphics.renderTooltip(this.font, text, mouseX, mouseY);
        } else if (isMouseOverPlayer(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.viaromana.player_marker"), mouseX, mouseY);
        }
    }
    //endregion

    //region Input & Interaction
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.screenAlpha < 0.5f) return false; // Prevent clicking while fading in

        if (button == 0) { // Left Click
            TeleportHelper.TeleportDestination destination = findDestinationAtPosition(getRevealedNodes(), (int) mouseX, (int) mouseY);
            if (destination != null) {
                selectDestination(destination);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void selectDestination(TeleportHelper.TeleportDestination destination) {
        if (minecraft == null || minecraft.player == null) return;

        if (PlayerData.isChartingPath(minecraft.player)) {
            HudMessageManager.queueMessage("message.via_romana.cannot_warp_when_recording");
            this.onClose();
            return;
        }

        if (EffectUtils.hasEffect(minecraft.player, "travellers_fatigue")) {
            HudMessageManager.queueMessage("message.via_romana.has_fatigue");
            this.onClose();
            return;
        }

        if (FadeManager.isActive()) {
            HudMessageManager.queueMessage("message.via_romana.cannot_warp_when_warping");
            this.onClose();
            return;
        }

        TeleportRequestC2S packet = new TeleportRequestC2S(this.signPos, destination.position);
        PacketBroadcaster.C2S.sendToServer(packet);
        this.onClose();
    }
    //endregion

    //region Networking
    private void validateNodeSign(BlockPos nodePos) {
        if (!validatedNodes.contains(nodePos)) {
            SignValidationRequestC2S packet = new SignValidationRequestC2S(nodePos);
            PacketBroadcaster.C2S.sendToServer(packet);
        }
    }

    public void handleSignValidation(BlockPos nodePos, boolean isValid) {
        if (isValid) {
            validatedNodes.add(nodePos);
        }
    }
    //endregion

    //region Utility & Helpers
    private Set<BlockPos> getRevealedNodes() {
        Set<BlockPos> revealedNodes = new HashSet<>(animatedNodes);
        nodesToAnimate.forEach(node -> revealedNodes.add(node.position));
        return revealedNodes;
    }

    private void calculateBounds() {
        if (networkNodeMap.isEmpty()) {
            assert minecraft != null;
            BlockPos playerPos = minecraft.player != null ? minecraft.player.blockPosition() : BlockPos.ZERO;
            this.minBounds = playerPos.offset(-128, 0, -128);
            this.maxBounds = playerPos.offset(128, 0, 128);
            return;
        }

        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : networkNodeMap.keySet()) {
            minX = Math.min(minX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        this.minBounds = new BlockPos(minX, 0, minZ);
        this.maxBounds = new BlockPos(maxX, 0, maxZ);
    }

    private void configureAnimationTiming() {
        this.totalAnimationWaves = computeAnimationWaveCount();
        float totalDurationSeconds = CommonConfig.spline_animation_time;
        this.totalAnimationDurationMillis = Math.max(1L, (long) (totalDurationSeconds * 1000.0f));
        this.completedAnimationWaves = 0;
        this.animationProgress = 0.0f;
        this.animationStartMillis = -1L;
    }

    private int computeAnimationWaveCount() {
        if (sourceNodePos == null || !networkNodeMap.containsKey(sourceNodePos)) return 1;

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(sourceNodePos);
        visited.add(sourceNodePos);

        int waves = 0;
        while (!frontier.isEmpty()) {
            int size = frontier.size();
            waves++;
            for (int i = 0; i < size; i++) {
                BlockPos current = frontier.poll();
                DestinationResponseS2C.NodeNetworkInfo node = networkNodeMap.get(current);
                if (node == null) continue;
                for (BlockPos neighbor : node.connections) {
                    if (visited.add(neighbor)) {
                        frontier.add(neighbor);
                    }
                }
            }
        }

        return Math.max(1, waves);
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

    private TeleportHelper.TeleportDestination findDestinationAtPosition(Set<BlockPos> revealedNodes, int mouseX, int mouseY) {
        for (TeleportHelper.TeleportDestination dest : destinations) {
            boolean isValidated = validatedNodes.contains(dest.position) || sourceNodePos == null;
            if (revealedNodes.contains(dest.position) && isValidated) {
                Optional<Point> screenPosOpt = worldToScreen(dest.position);
                if (screenPosOpt.isPresent()) {
                    if (isMouseOver(screenPosOpt.get(), MARKER_SIZE, mouseX, mouseY)) {
                        return dest;
                    }
                }
            }
        }
        return null;
    }

    private float getPixelAngle(Point center, Point pixel) {
        return (float) ((Math.toDegrees(Math.atan2(pixel.x - center.x, center.y - pixel.y)) + 360) % 360);
    }

    private boolean isWithinAngleRange(float angle, float target) {
        float diff = Math.abs(angle - target);
        if (diff > 180) diff = 360 - diff;
        return diff <= DIRECTION_ANGLE_RANGE;
    }

    private int getFadedColor(int baseColor, float pixelAngle, float facingAngle) {
        float diff = Math.abs(pixelAngle - facingAngle);
        if (diff > 180) diff = 360 - diff;

        float fadeFactor = (float) Math.pow(1.0f - (diff / DIRECTION_ANGLE_RANGE), DIRECTION_FADE_CURVE);
        fadeFactor = Math.max(0.0f, Math.min(1.0f, fadeFactor));

        int alpha = (baseColor >> 24) & 0xFF;
        int fadedAlpha = (int) (alpha * fadeFactor);
        return (fadedAlpha << 24) | (baseColor & 0x00FFFFFF);
    }
    //endregion

    //region Overrides
    @Override
    public void onClose() {
        if (this.mapTexture != null) {
            this.mapTexture.close();
            this.mapTexture = null;
        }

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