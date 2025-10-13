package net.rasanovum.viaromana.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.core.LinkHandler;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.util.VersionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.rasanovum.viaromana.core.LinkHandler.LinkData;
import org.joml.Matrix4f;

/**
 * Renders ribbons between connected path nodes.
 */
//? if fabric
@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
//? if neoforge
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)*/
public final class NodeConnectionRenderer {
    // General Constants
    private static final double RENDER_DISTANCE = 16.0;
    private static final double FADE_BUFFER_DISTANCE = 4.0;
    private static final float RIBBON_FADE_FRACTION = 0.25f;
    private static final int MIN_SEGMENTS = 4;
    private static final int MAX_SEGMENTS = 24;

    // Dynamic Path Constants
    private static final float COHERENCE = 0.6f;
    private static final float WANDER_AMPLITUDE = 0.5f;
    private static final float POINT_DENSITY = 0.5f;
    private static final int SUB_SEGMENTS = 4;
    private static final float VERTICAL_WANDER_SCALE = 0.4f;

    private static final ResourceLocation CONNECTION_TEXTURE = VersionUtils.getLocation("via_romana:textures/effect/connection_ribbon.png");
    private static RenderType getRenderType() {
        boolean shadersInUse = false;
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = irisApiClass.getMethod("getInstance").invoke(null);
            shadersInUse = (Boolean) irisApiClass.getMethod("isShaderPackInUse").invoke(instance);
        } catch (Exception e) {
            shadersInUse = false;
        }
        return shadersInUse ? RenderType.entityTranslucentEmissive(CONNECTION_TEXTURE, true) : RenderType.beaconBeam(CONNECTION_TEXTURE, true);
    }

    // Animation Constants
    private record RibbonConfig(float baseAlpha, float width, float scrollSpeedSec, float crossAngleRadians, float r, float g, float b, VertexConsumer consumer) {}
    private record PathData(List<Vec3> points, List<Vec3> tangents) {}
    
    private NodeConnectionRenderer() {}

    public static void renderConnections(PoseStack poseStack, ClientLevel level, Player player, PathGraph graph, float animationTime, MultiBufferSource bufferSource, float globalAlpha) {
        if (graph == null || graph.nodesView().isEmpty() || globalAlpha <= 0.0f) return;

        Vec3 playerPos = player.position();
        double searchRadius = RENDER_DISTANCE + FADE_BUFFER_DISTANCE;
        
        List<Node> nearby = ClientPathData.getInstance().getNearbyNodes(BlockPos.containing(playerPos), searchRadius, false);
        if (nearby.isEmpty()) return;

        PoseStack.Pose pose = poseStack.last();
        RibbonConfig primaryConfig = new RibbonConfig(0.2f, 0.25f, 0.4f, (float)Math.toRadians(70.0), 1.0f, 1.0f, 1.0f, bufferSource.getBuffer(getRenderType()));
        RibbonConfig secondaryConfig = new RibbonConfig(0.2f, 0.30f, -0.3f, (float)Math.toRadians(70.0), 1.0f, 1.0f, 1.0f, bufferSource.getBuffer(getRenderType()));
        RibbonConfig signConfig = new RibbonConfig(0.3f, 0.20f, 0.16f, (float)Math.toRadians(70.0), 1.0f, 1.0f, 1.0f, bufferSource.getBuffer(getRenderType()));
        RibbonConfig tempSignConfig = new RibbonConfig(0.3f, 0.20f, 0.16f, (float)Math.toRadians(70.0), 0.0f, 1.0f, 0.0f, bufferSource.getBuffer(getRenderType()));

        for (Node a : nearby) {
            Vec3 aCenter = Vec3.atCenterOf(BlockPos.of(a.getPos()));
            if (playerPos.distanceToSqr(aCenter) > searchRadius * searchRadius) continue;

            for (long bPacked : a.getConnectedNodes()) {
                if (bPacked <= a.getPos()) continue;
                graph.getNodeAt(BlockPos.of(bPacked)).ifPresent(b -> {
                    Vec3 bCenter = Vec3.atCenterOf(BlockPos.of(b.getPos()));
                    if (playerPos.distanceToSqr(aCenter.lerp(bCenter, 0.5)) <= searchRadius * searchRadius) {
                        renderNodeConnection(pose, level, playerPos, animationTime, aCenter, bCenter, primaryConfig, secondaryConfig, globalAlpha);
                    }
                });
            }

            a.getSignPos().ifPresent(signPosPacked -> {
                BlockPos signPos = BlockPos.of(signPosPacked);
                if (LinkHandler.isSignBlock(level, signPos)) {
                    Vec3 signCenter = Vec3.atCenterOf(signPos);
                     if (playerPos.distanceToSqr(aCenter.lerp(signCenter, 0.5)) <= searchRadius * searchRadius) {
                        renderSignConnection(pose, level, playerPos, animationTime, aCenter, signCenter, signConfig, globalAlpha);
                    }
                }
            });
        }        

        ClientPathData clientData = ClientPathData.getInstance();

        // Render temporary sign links
        for (LinkData tempLink : clientData.getTemporaryLinks()) {
            BlockPos nodePos = tempLink.nodePos();
            BlockPos signPos = tempLink.signPos();
            if (LinkHandler.isSignBlock(level, signPos)) {
                Vec3 nodeCenter = Vec3.atCenterOf(nodePos);
                Vec3 signCenter = Vec3.atCenterOf(signPos);
                if (playerPos.distanceToSqr(nodeCenter.lerp(signCenter, 0.5)) <= searchRadius * searchRadius) {
                    renderSignConnection(pose, level, playerPos, animationTime, nodeCenter, signCenter, tempSignConfig, globalAlpha);
                }
            }
        }

        // TODO: Render temp nodes with connections
    }

    private static void renderNodeConnection(PoseStack.Pose pose, ClientLevel level, Vec3 playerPos, float animationTime, Vec3 start, Vec3 end, RibbonConfig primary, RibbonConfig secondary, float globalAlpha) {
        Vec3 clampedStart = new Vec3(start.x, RenderUtil.findSuitableYPosition(level, BlockPos.containing(start), 1.2f), start.z);
        Vec3 clampedEnd = new Vec3(end.x, RenderUtil.findSuitableYPosition(level, BlockPos.containing(end), 1.2f), end.z);

        double midDist = playerPos.distanceTo(clampedStart.lerp(clampedEnd, 0.5));
        float alpha1 = NodeRenderer.calculateDistanceAlpha(midDist, primary.baseAlpha()) * globalAlpha;
        float alpha2 = NodeRenderer.calculateDistanceAlpha(midDist, secondary.baseAlpha()) * globalAlpha;

        if (alpha1 <= 0.01f && alpha2 <= 0.01f) return;

        PathData path = generateWanderingPath(clampedStart, clampedEnd, animationTime);

        if (alpha1 > 0.01f) drawPath(pose, path, animationTime, primary, alpha1);
        if (alpha2 > 0.01f) drawPath(pose, path, animationTime, secondary, alpha2);
    }

    private static void renderSignConnection(PoseStack.Pose pose, ClientLevel level, Vec3 playerPos, float animationTime, Vec3 start, Vec3 end, RibbonConfig config, float globalAlpha) {
        Vec3 clampedStart = start.with(Direction.Axis.Y, RenderUtil.findSuitableYPosition(level, BlockPos.containing(start), 1.2f));
        
        double midDist = playerPos.distanceTo(clampedStart.lerp(end, 0.5));
        float alpha = NodeRenderer.calculateDistanceAlpha(midDist, config.baseAlpha()) * globalAlpha;
        
        if (alpha <= 0.01f) return;
        
        PathData path = generateSimpleArcPath(clampedStart, end, animationTime);
        drawPath(pose, path, animationTime, config, alpha);
    }

    private static PathData generateWanderingPath(Vec3 start, Vec3 end, float animationTime) {
        List<Vec3> points = new ArrayList<>();
        List<Vec3> tangents = new ArrayList<>();
        Vec3 diff = end.subtract(start);
        double dist = diff.length();
        if (dist < 0.01) return new PathData(points, tangents);
        float effectiveAmp = WANDER_AMPLITUDE * (1 - COHERENCE);
        if (effectiveAmp <= 0.01f) {
             float baseArc = (float)(0.25 + 0.15 * Math.min(1.0, dist / 8.0));
             float pulse = (float)(Math.sin(animationTime * 2.0 + (start.x + start.z) * 0.3) * 0.1); // Use animationTime
             Vec3 mid = start.add(diff.scale(0.5)).add(0, baseArc + pulse, 0);
             int segments = (int) Mth.clamp(dist * 6, MIN_SEGMENTS, MAX_SEGMENTS);
             for (int i = 0; i <= segments; i++) {
                 float t = (float) i / segments;
                 points.add(quad(start, mid, end, t));
                 tangents.add(quadDerivative(start, mid, end, t).normalize());
             }
        } else {
            List<Vec3> controlPoints = generateControlPoints(start, end, effectiveAmp);
            int numCurveSeg = controlPoints.size() - 1;
            if (numCurveSeg < 1) return new PathData(points, tangents);
            for (int i = 0; i < numCurveSeg; i++) {
                Vec3 p0 = i == 0 ? controlPoints.get(0) : controlPoints.get(i - 1);
                Vec3 p1 = controlPoints.get(i);
                Vec3 p2 = controlPoints.get(i + 1);
                Vec3 p3 = i + 1 == numCurveSeg ? p2 : controlPoints.get(i + 2);
                for (int j = 0; j < SUB_SEGMENTS; j++) {
                    float t = (float) j / SUB_SEGMENTS;
                    points.add(catmullRom(p0, p1, p2, p3, t));
                    tangents.add(catmullRomDer(p0, p1, p2, p3, t).normalize());
                }
            }
            points.add(controlPoints.get(controlPoints.size() - 1));
            tangents.add(tangents.get(tangents.size() - 1));
        }
        return new PathData(points, tangents);
    }
    
    private static PathData generateSimpleArcPath(Vec3 start, Vec3 end, float animationTime) {
        List<Vec3> points = new ArrayList<>();
        List<Vec3> tangents = new ArrayList<>();
        Vec3 diff = end.subtract(start);
        double dist = diff.length();
        if (dist < 0.01) return new PathData(points, tangents);
        float baseArc = (float)(0.15 + 0.1 * Math.min(1.0, dist / 6.0));
        float pulse = (float)(Math.sin(animationTime * 3.0 + (start.x + end.z) * 0.5) * 0.05);
        Vec3 mid = start.add(diff.scale(0.5)).add(0, baseArc + pulse, 0);
        int segments = (int) Mth.clamp(dist * 4, MIN_SEGMENTS, MAX_SEGMENTS);
        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            points.add(quad(start, mid, end, t));
            tangents.add(quadDerivative(start, mid, end, t).normalize());
        }
        return new PathData(points, tangents);
    }

    private static List<Vec3> generateControlPoints(Vec3 start, Vec3 end, float effectiveAmp) {
        List<Vec3> controlPoints = new ArrayList<>();
        controlPoints.add(start);
        Vec3 diff = end.subtract(start);
        double dist = diff.length();
        int numInter = (int) (dist * POINT_DENSITY);
        Vec3 tangent = diff.normalize();
        Vec3 perp = tangent.cross(new Vec3(0, 1, 0)).normalize();
        if (perp.lengthSqr() < 0.1) perp = tangent.cross(new Vec3(1, 0, 0)).normalize();
        Random rand = new Random((long) (start.x * 31 + end.x) ^ (long) (start.z * 31 + end.z));
        for (int i = 1; i <= numInter; i++) {
            float t = (float) i / (numInter + 1);
            Vec3 basePos = start.add(diff.scale(t));
            float horizOffset = (float) (rand.nextGaussian() * effectiveAmp);
            float vertOffset = (float) (rand.nextGaussian() * effectiveAmp * VERTICAL_WANDER_SCALE);
            controlPoints.add(basePos.add(perp.scale(horizOffset)).add(0, vertOffset, 0));
        }
        controlPoints.add(end);
        return controlPoints;
    }

    private static void drawPath(PoseStack.Pose pose, PathData path, float animationTime, RibbonConfig config, float baseAlpha) {
        if (path.points().size() < 2) return;
        float vScroll = -(animationTime * config.scrollSpeedSec());
        double totalDist = path.points().get(0).distanceTo(path.points().get(path.points().size() -1));
        for (int i = 0; i < path.points().size() - 1; i++) {
            float t0 = (float) i / (path.points().size() - 1);
            float t1 = (float) (i + 1) / (path.points().size() - 1);
            float alpha0 = baseAlpha * fadeEnds(t0);
            float alpha1 = baseAlpha * fadeEnds(t1);
            float v0 = t0 * (float) totalDist * 0.25f + vScroll;
            float v1 = t1 * (float) totalDist * 0.25f + vScroll;
            renderCrossedQuads(pose, config.consumer(), path.points().get(i), path.points().get(i+1), path.tangents().get(i), path.tangents().get(i+1), v0, v1, alpha0, alpha1, config.width(), config.crossAngleRadians(), config.r(), config.g(), config.b());
        }
    }

    private static void renderCrossedQuads(PoseStack.Pose pose, VertexConsumer consumer, Vec3 p0, Vec3 p1, Vec3 tangent0, Vec3 tangent1, float v0, float v1, float alpha0, float alpha1, float width, float crossAngleRadians, float r, float g, float b) {
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 offsetDir0 = up.subtract(tangent0.scale(up.dot(tangent0))).normalize().scale(width);
        Vec3 offsetDir1 = up.subtract(tangent1.scale(up.dot(tangent1))).normalize().scale(width);
        Vec3 normal0 = tangent0.cross(offsetDir0).normalize();
        addDoubleSidedQuad(pose, consumer, p0, p1, offsetDir0, offsetDir1, v0, v1, alpha0, alpha1, normal0, r, g, b);
        Vec3 rotatedOffset0 = rotateAroundAxis(offsetDir0, tangent0, crossAngleRadians);
        Vec3 rotatedOffset1 = rotateAroundAxis(offsetDir1, tangent1, crossAngleRadians);
        Vec3 rotatedNormal0 = tangent0.cross(rotatedOffset0).normalize();
        addDoubleSidedQuad(pose, consumer, p0, p1, rotatedOffset0, rotatedOffset1, v0, v1, alpha0, alpha1, rotatedNormal0, r, g, b);
    }

    private static void addDoubleSidedQuad(PoseStack.Pose pose, VertexConsumer consumer, Vec3 p0, Vec3 p1, Vec3 n0, Vec3 n1, float v0, float v1, float alpha0, float alpha1, Vec3 frontNormal, float r, float g, float b) {
        Vec3 e1s = p0.subtract(n0), e1e = p1.subtract(n1);
        Vec3 e2s = p0.add(n0), e2e = p1.add(n1);
        int overlay = 0;
        int light = 0xF000F0;
        int rgb = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
        int color0 = ((int)(alpha0 * 255) << 24) | rgb;
        int color1 = ((int)(alpha1 * 255) << 24) | rgb;
        
        put(pose, consumer, e1s, color0, 0, v0, overlay, light, frontNormal);
        put(pose, consumer, e1e, color1, 0, v1, overlay, light, frontNormal);
        put(pose, consumer, e2e, color1, 1, v1, overlay, light, frontNormal);
        put(pose, consumer, e2s, color0, 1, v0, overlay, light, frontNormal);
        
        Vec3 backNormal = frontNormal.scale(-1);
        
        put(pose, consumer, e2s, color0, 0, v0, overlay, light, backNormal);
        put(pose, consumer, e2e, color1, 0, v1, overlay, light, backNormal);
        put(pose, consumer, e1e, color1, 1, v1, overlay, light, backNormal);
        put(pose, consumer, e1s, color0, 1, v0, overlay, light, backNormal);
    }
    
    private static void put(PoseStack.Pose pose, VertexConsumer consumer, Vec3 pos, int color, float u, float v, int overlay, int light, Vec3 normal) {
        //? if <1.21 {
        /*Matrix4f matrix = pose.pose();
        consumer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z).color(color).uv(u, v).overlayCoords(0).uv2(0xF000F0).normal((float)normal.x, (float)normal.y, (float)normal.z).endVertex();
        *///?} else {
        consumer.addVertex(pose, (float)pos.x, (float)pos.y, (float)pos.z).setColor(color).setUv(u, v).setOverlay(overlay).setLight(light).setNormal((float)normal.x, (float)normal.y, (float)normal.z);
        //?}
    }

    private static Vec3 rotateAroundAxis(Vec3 v, Vec3 axis, float angle) {
        float cos = Mth.cos(angle), sin = Mth.sin(angle);
        float dot = (float) v.dot(axis);
        return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(dot * (1 - cos)));
    }

    private static float fadeEnds(float t) {
        return Mth.clamp(Math.min(t, 1 - t) / RIBBON_FADE_FRACTION, 0.0f, 1.0f);
    }

    private static Vec3 quad(Vec3 p0, Vec3 p1, Vec3 p2, float t) {
        float it = 1.0f - t;
        return new Vec3(it * it * p0.x + 2 * it * t * p1.x + t * t * p2.x, it * it * p0.y + 2 * it * t * p1.y + t * t * p2.y, it * it * p0.z + 2 * it * t * p1.z + t * t * p2.z);
    }

    private static Vec3 quadDerivative(Vec3 p0, Vec3 p1, Vec3 p2, float t) {
        return new Vec3(2 * (1 - t) * (p1.x - p0.x) + 2 * t * (p2.x - p1.x), 2 * (1 - t) * (p1.y - p0.y) + 2 * t * (p2.y - p1.y), 2 * (1 - t) * (p1.z - p0.z) + 2 * t * (p2.z - p1.z));
    }
    
    private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        return p0.scale(-t3 + 2 * t2 - t).add(p1.scale(3 * t3 - 5 * t2 + 2)).add(p2.scale(-3 * t3 + 4 * t2 + t)).add(p3.scale(t3 - t2)).scale(0.5);
    }

    private static Vec3 catmullRomDer(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float t2 = t * t;
        return p0.scale(-3 * t2 + 4 * t - 1).add(p1.scale(9 * t2 - 10 * t)).add(p2.scale(-9 * t2 + 8 * t + 1)).add(p3.scale(3 * t2 - 2 * t)).scale(0.5);
    }
}