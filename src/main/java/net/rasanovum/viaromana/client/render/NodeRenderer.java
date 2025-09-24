package net.rasanovum.viaromana.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;
import net.rasanovum.viaromana.client.ColorUtil;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.configuration.ViaRomanaConfig;
import net.rasanovum.viaromana.items.ChartingMap;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.variables.VariableAccess;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side renderer for visualizing path nodes as textured beams.
 */
public class NodeRenderer {
    // Constants
    private static final float BEAM_HEIGHT = 2.0f;
    private static final float BEAM_WIDTH = 0.6f;
    private static final float BEAM_FADE_FRACTION = 0.5f;
    private static final int BEAM_SEGMENTS = 10;
    private static final float RENDER_DISTANCE = 16.0f;
    private static final float FADE_BUFFER_DISTANCE = 4.0f;
    private static final float NEAR_FADE_START_DISTANCE = 1.0f;
    private static final float NEAR_FADE_END_DISTANCE = 0.5f;
    private static final float VIGNETTE_MAX_INTENSITY = 0.3f;
    private static final int MAX_LIGHT_BRIGHTNESS = 8;
    private static final float BEAM_ALPHA = 0.75f;
    private static final float PULSE_FREQUENCY = 1.0f;
    private static final float PULSE_MIN_ALPHA = 0.1f;
    private static final float PULSE_MAX_ALPHA = 1.0f;
    private static final float CROSS_ANGLE_RADIANS = (float) Math.toRadians(80.0);
    private static final float CROSS_COS = (float) Math.cos(CROSS_ANGLE_RADIANS);
    private static final float CROSS_SIN = (float) Math.sin(CROSS_ANGLE_RADIANS);
    private static final int DEFAULT_BEAM_COLOR = ColorUtil.rgbToHex(255, 255, 255);
    private static final int CHARTING_BEAM_COLOR = ColorUtil.rgbToHex(0, 255, 0);
    private static final ResourceLocation BEAM_TEXTURE = ResourceLocation.parse("via_romana:textures/effect/node_beam.png");
    private static final int SOUND_INTERVAL_TICKS = 40;

    private static int getPulseDistance() { return ViaRomanaConfig.node_utility_distance; }

    private static final float ANIMATION_SPEED_SEC = -1.0f;

    private static final Map<BlockPos, Long> nodeSoundTimes = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Integer> dynamicLightSources = new ConcurrentHashMap<>();
    private static final float GLOBAL_FADE_SPEED = 0.05f;
    private static final float SELECTION_FADE_SPEED = 0.1f;
    private static float globalRenderAlpha = 0.0f;
    private static final Map<BlockPos, Float> animatedNodeAlphas = new ConcurrentHashMap<>();
    private static float currentVignetteIntensity = 0.0f;

    private static long lastRenderTime = 0L;
    private static float animationTime = 0.0f;

    private record NodeRenderData(BlockPos pos, double distance, double adjustedY, int color) {}

    // Public API
    public static int getLightLevel(BlockPos pos) { return dynamicLightSources.getOrDefault(pos, 0); }
    public static float getCurrentVignetteIntensity() { return currentVignetteIntensity * globalRenderAlpha; }
    public static float getBeamHeight() { return BEAM_HEIGHT; }
    public static float calculateDistanceAlpha(double distance, float baseAlpha) { return (float) calculateValueWithFade(distance, baseAlpha); }
    public static int calculateDistanceLightBrightness(double distance) { return (int) Math.round(calculateValueWithFade(distance, MAX_LIGHT_BRIGHTNESS)); }
    public static double calculateDistanceToNodeBeam(Vec3 playerPos, BlockPos nodePos, ClientLevel level) {
        double adjustedY = RenderUtil.findSuitableYPosition(level, nodePos, 0.25f);
        return calculateDistanceToNodeBeamInternal(playerPos, nodePos, adjustedY);
    }

    // Main Render Loop
    public static void renderNodeBeams(PoseStack poseStack, Level level, Player player, float partialTicks) {
        if (!(level instanceof ClientLevel clientLevel)) return;

        updateAnimationTimer();
        
        boolean shouldRender = VariableAccess.playerVariables.isChartingPath(player) || player.getMainHandItem().getItem() instanceof ChartingMap || player.getOffhandItem().getItem() instanceof ChartingMap;
        updateGlobalAlpha(shouldRender);

        if (globalRenderAlpha <= 0.0f) {
            clearAllLightSources(clientLevel);
            animatedNodeAlphas.clear();
            currentVignetteIntensity = 0.0f;
            return;
        }

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 playerPos = player.position();
        
        List<NodeRenderData> nodeDataList = gatherRenderData(clientLevel, playerPos);
        
        BlockPos selectedNodePos = findAndSetSelectedNode(player, nodeDataList);
        updateAnimatedAlphas(selectedNodePos);
        currentVignetteIntensity = calculateMaxVignette(nodeDataList);
        
        if (nodeDataList.isEmpty()) {
            updateLightSources(clientLevel, Collections.emptyList());
            return;
        }

        nodeDataList.sort(Comparator.comparingDouble(NodeRenderData::distance).reversed());

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumer beamConsumer = bufferSource.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, true));
        float vOffset = animationTime * ANIMATION_SPEED_SEC;

        for (NodeRenderData data : nodeDataList) {
            renderBeam(poseStack, beamConsumer, data, vOffset);
            playNodeSoundAtPosition(clientLevel, data.pos, data.adjustedY, level.getGameTime());
        }

        bufferSource.endBatch(RenderType.beaconBeam(BEAM_TEXTURE, true));

        PathGraph graph = ClientPathData.getInstance().getGraph();
        if (graph != null && !graph.nodesView().isEmpty()) {
            NodeConnectionRenderer.renderConnections(poseStack, clientLevel, player, graph, animationTime, bufferSource, globalRenderAlpha);
        }

        poseStack.popPose();
        bufferSource.endBatch();
        
        updateLightSources(clientLevel, nodeDataList);
    }

    private static void updateAnimationTimer() {
        if (lastRenderTime == 0L) {
            lastRenderTime = System.nanoTime();
            return;
        }
        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastRenderTime) / 1_000_000_000.0f;
        lastRenderTime = currentTime;
        animationTime += deltaTime;
    }

    // Gathers all visible nodes and pre-calculates their expensive data.
    private static List<NodeRenderData> gatherRenderData(ClientLevel level, Vec3 playerPos) {
        List<NodeRenderData> dataList = new ArrayList<>();
        double searchRadius = RENDER_DISTANCE + FADE_BUFFER_DISTANCE;

        if (VariableAccess.playerVariables.isChartingPath(Minecraft.getInstance().player)) {
            ClientPathData.getInstance().getTemporaryNodes().stream()
                .map(nodeData -> {
                    double adjY = RenderUtil.findSuitableYPosition(level, nodeData.pos(), 0.25f);
                    double dist = calculateDistanceToNodeBeamInternal(playerPos, nodeData.pos(), adjY);
                    return new NodeRenderData(nodeData.pos(), dist, adjY, CHARTING_BEAM_COLOR);
                })
                .filter(data -> data.distance <= searchRadius)
                .forEach(dataList::add);
        }

        PathGraph graph = ClientPathData.getInstance().getGraph();
        if (graph != null) {
            ClientPathData.getInstance().getNearbyNodes(BlockPos.containing(playerPos), searchRadius, false).stream()
                .map(node -> {
                    BlockPos pos = BlockPos.of(node.getPos());
                    double adjY = RenderUtil.findSuitableYPosition(level, pos, 0.25f);
                    double dist = calculateDistanceToNodeBeamInternal(playerPos, pos, adjY);
                    return new NodeRenderData(pos, dist, adjY, DEFAULT_BEAM_COLOR);
                })
                .filter(data -> data.distance <= searchRadius)
                .forEach(dataList::add);
        }
        return dataList;
    }
    
    private static BlockPos findAndSetSelectedNode(Player player, List<NodeRenderData> nodeDataList) {
        ClientPathData clientPathData = ClientPathData.getInstance();
        Node nodeOpt = clientPathData.getNearestNode(player.blockPosition(), getPulseDistance(), false).orElse(null);
        return nodeOpt != null ? nodeOpt.getBlockPos() : null;
    }

    private static void renderBeam(PoseStack poseStack, VertexConsumer consumer, NodeRenderData data, float vOffset) {
        float baseAlpha = animatedNodeAlphas.getOrDefault(data.pos(), BEAM_ALPHA);
        float distanceAlpha = calculateDistanceAlpha(data.distance(), baseAlpha);
        float finalAlpha = distanceAlpha * globalRenderAlpha;

        if (finalAlpha <= 0.01f) return;

        float r = ((data.color() >> 16) & 0xFF) / 255.0f;
        float g = ((data.color() >> 8) & 0xFF) / 255.0f;
        float b = (data.color() & 0xFF) / 255.0f;

        poseStack.pushPose();
        poseStack.translate(data.pos().getX() + 0.5, data.adjustedY(), data.pos().getZ() + 0.5);
        renderBeamGeometry(poseStack, consumer, r, g, b, finalAlpha, vOffset);
        poseStack.popPose();
    }

    private static void updateAnimatedAlphas(BlockPos selectedNodePos) {
        Set<BlockPos> nodesToAnimate = new HashSet<>(animatedNodeAlphas.keySet());
        if (selectedNodePos != null) nodesToAnimate.add(selectedNodePos);

        for (BlockPos pos : nodesToAnimate) {
            boolean isSelected = pos.equals(selectedNodePos);
            float currentAlpha = animatedNodeAlphas.getOrDefault(pos, BEAM_ALPHA);
            float targetAlpha;

            if (isSelected) {
                float pulseValue = (float) (Math.sin(animationTime * PULSE_FREQUENCY * 2 * Math.PI) * 0.5 + 0.5);
                targetAlpha = Mth.lerp(pulseValue, PULSE_MIN_ALPHA, PULSE_MAX_ALPHA);
            } else {
                targetAlpha = BEAM_ALPHA;
            }

            float newAlpha = Mth.lerp(SELECTION_FADE_SPEED, currentAlpha, targetAlpha);

            if (!isSelected && Math.abs(newAlpha - BEAM_ALPHA) < 0.01f) {
                animatedNodeAlphas.remove(pos);
            } else {
                animatedNodeAlphas.put(pos, newAlpha);
            }
        }
    }

    private static void updateGlobalAlpha(boolean shouldRender) {
        float targetAlpha = shouldRender ? 1.0f : 0.0f;
        globalRenderAlpha = Mth.lerp(GLOBAL_FADE_SPEED, globalRenderAlpha, targetAlpha);
        if (Math.abs(globalRenderAlpha - targetAlpha) < 0.005f) {
            globalRenderAlpha = targetAlpha;
        }
    }
    
    private static float calculateMaxVignette(List<NodeRenderData> nodeDataList) {
        return nodeDataList.stream()
            .map(data -> calculateVignetteForDistance(data.distance()))
            .max(Float::compare)
            .orElse(0.0f);
    }
    
    private static void updateLightSources(ClientLevel level, List<NodeRenderData> visibleNodes) {
        LevelLightEngine lightEngine = level.getLightEngine();
        Set<BlockPos> currentLightPositions = new HashSet<>();

        for (NodeRenderData data : visibleNodes) {
            int lightLevel = (int) (calculateDistanceLightBrightness(data.distance()) * globalRenderAlpha);
            if (lightLevel <= 0) continue;

            BlockPos lightPos = BlockPos.containing(data.pos().getX(), data.adjustedY() + BEAM_HEIGHT / 2.0, data.pos().getZ());
            currentLightPositions.add(lightPos);

            if (dynamicLightSources.getOrDefault(lightPos, 0) != lightLevel) {
                dynamicLightSources.put(lightPos, lightLevel);
                lightEngine.checkBlock(lightPos);
            }
        }

        dynamicLightSources.keySet().removeIf(pos -> {
            if (!currentLightPositions.contains(pos)) {
                lightEngine.checkBlock(pos);
                return true;
            }
            return false;
        });
    }

    private static void renderBeamGeometry(PoseStack poseStack, VertexConsumer consumer, float r, float g, float b, float a, float vOffset) {
        PoseStack.Pose pose = poseStack.last();
        float halfWidth = BEAM_WIDTH / 2.0f;
        float x1 = halfWidth, z1 = 0;
        float x2 = halfWidth * CROSS_COS, z2 = halfWidth * CROSS_SIN;

        float vMin = vOffset;
        float vMax = vMin + (BEAM_HEIGHT / 2.0f);

        int overlay = 655360;
        int light = 15728880;
        int rgb = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);

        for (int i = 0; i < BEAM_SEGMENTS; i++) {
            float t0 = (float) i / BEAM_SEGMENTS, t1 = (float) (i + 1) / BEAM_SEGMENTS;
            float y0 = t0 * BEAM_HEIGHT, y1 = t1 * BEAM_HEIGHT;
            float a0 = a * fadeEnds(t0), a1 = a * fadeEnds(t1);
            float vt0 = Mth.lerp(t0, vMin, vMax), vt1 = Mth.lerp(t1, vMin, vMax);
            
            int color0 = ((int)(a0 * 255) << 24) | rgb;
            int color1 = ((int)(a1 * 255) << 24) | rgb;
            
            renderSegment(pose, consumer, -x1, y0, -z1, x1, y1, z1, color0, color1, vt0, vt1, overlay, light);
            renderSegment(pose, consumer, -x2, y0, -z2, x2, y1, z2, color0, color1, vt0, vt1, overlay, light);
        }
    }

    private static void renderSegment(PoseStack.Pose pose, VertexConsumer consumer, float x1, float y0, float z1, float x2, float y1, float z2, int color0, int color1, float v0, float v1, int overlay, int light) {
        consumer.addVertex(pose, x1, y0, z1).setColor(color0).setUv(0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        consumer.addVertex(pose, x2, y0, z2).setColor(color0).setUv(1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        consumer.addVertex(pose, x2, y1, z2).setColor(color1).setUv(1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        consumer.addVertex(pose, x1, y1, z1).setColor(color1).setUv(0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        
        consumer.addVertex(pose, x2, y0, z2).setColor(color0).setUv(0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        consumer.addVertex(pose, x1, y0, z1).setColor(color0).setUv(1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        consumer.addVertex(pose, x1, y1, z1).setColor(color1).setUv(1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        consumer.addVertex(pose, x2, y1, z2).setColor(color1).setUv(0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
    }
    
    private static double calculateValueWithFade(double distance, double maxValue) {
        if (distance < NEAR_FADE_END_DISTANCE) return 0.0;
        if (distance < NEAR_FADE_START_DISTANCE) return maxValue * ((distance - NEAR_FADE_END_DISTANCE) / (NEAR_FADE_START_DISTANCE - NEAR_FADE_END_DISTANCE));
        if (distance > RENDER_DISTANCE) return maxValue * (1.0 - Mth.clamp((distance - RENDER_DISTANCE) / FADE_BUFFER_DISTANCE, 0.0, 1.0));
        return maxValue;
    }

    private static float calculateVignetteForDistance(double distance) {
        if (distance >= NEAR_FADE_START_DISTANCE) return 0.0f;
        if (distance <= NEAR_FADE_END_DISTANCE) return VIGNETTE_MAX_INTENSITY;
        return VIGNETTE_MAX_INTENSITY * (1.0f - (float) ((distance - NEAR_FADE_END_DISTANCE) / (NEAR_FADE_START_DISTANCE - NEAR_FADE_END_DISTANCE)));
    }

    private static double calculateDistanceToNodeBeamInternal(Vec3 playerPos, BlockPos nodePos, double adjustedY) {
        double clampedY = Mth.clamp(playerPos.y, adjustedY, adjustedY + BEAM_HEIGHT);
        return playerPos.distanceTo(new Vec3(nodePos.getX() + 0.5, clampedY, nodePos.getZ() + 0.5));
    }

    private static float fadeEnds(float t) { return Mth.clamp(Math.min(t, 1 - t) / BEAM_FADE_FRACTION, 0.0f, 1.0f); }

    private static void playNodeSoundAtPosition(ClientLevel level, BlockPos pos, double adjustedY, long currentTime) {
        if (nodeSoundTimes.getOrDefault(pos, 0L) > currentTime - SOUND_INTERVAL_TICKS) return;
        nodeSoundTimes.put(pos, currentTime);
        level.playLocalSound(pos.getX() + 0.5, adjustedY + BEAM_HEIGHT / 2.0, pos.getZ() + 0.5, SoundEvents.BEACON_AMBIENT, SoundSource.AMBIENT, 0.3f * globalRenderAlpha, 1.0f, false);
    }

    private static void clearAllLightSources(ClientLevel level) {
        if (!dynamicLightSources.isEmpty()) {
            LevelLightEngine lightEngine = level.getLightEngine();
            dynamicLightSources.keySet().forEach(lightEngine::checkBlock);
            dynamicLightSources.clear();
        }
    }
}