package net.rasanovum.viaromana.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rasanovum.viaromana.configuration.ViaRomanaConfig;
import net.rasanovum.viaromana.items.ChartingMap;
import net.rasanovum.viaromana.util.PathUtils;
import net.rasanovum.viaromana.variables.VariableAccess;
import net.minecraft.world.phys.Vec3;

public class InvalidBlockRenderer {
    private static final ResourceLocation BARRIER_TEXTURE = ResourceLocation.parse("minecraft:textures/item/barrier.png");
    private static final float ALPHA = (float) ViaRomanaConfig.invalid_block_overlay_opacity;
    private static final int FADE_BUFFER = 3;
    private static final int REGION_SIZE = ViaRomanaConfig.infrastructure_check_radius + FADE_BUFFER;

    public static void renderInfrastructureBlocks(PoseStack poseStack, Level level, Player player, float tickDelta) {
        boolean shouldRender = ALPHA == 0 || VariableAccess.playerVariables.isChartingPath(player) || player.getMainHandItem().getItem() instanceof ChartingMap || player.getOffhandItem().getItem() instanceof ChartingMap;

        if (!shouldRender) return;

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentCull(BARRIER_TEXTURE));

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        BlockPos playerPos = player.blockPosition();
        Vec3 playerPosition = player.position();

        for (int x = -REGION_SIZE; x <= REGION_SIZE; x++) {
            for (int z = -REGION_SIZE; z <= REGION_SIZE; z++) {
                for (int y = -REGION_SIZE; y <= REGION_SIZE; y++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.isAir()) continue;

                    BlockPos blockAbove = pos.above();
                    BlockState blockStateAbove = level.getBlockState(blockAbove);
                    if (!blockStateAbove.isAir() && blockStateAbove.isCollisionShapeFullBlock(level, blockAbove)) continue;

                    VoxelShape currentShape = state.getCollisionShape(level, pos);
                    if (currentShape.isEmpty()) continue;
                    if (PathUtils.isBlockValidPath(level, pos)) continue;

                    Vec3 blockCenter = Vec3.atCenterOf(pos);
                    double distance = playerPosition.distanceTo(blockCenter);
                    
                    float fadeAlpha;
                    
                    if (distance <= ViaRomanaConfig.infrastructure_check_radius) {
                        fadeAlpha = ALPHA;
                    } else if (distance <= ViaRomanaConfig.infrastructure_check_radius + FADE_BUFFER) {
                        float fadeProgress = (float)(distance - ViaRomanaConfig.infrastructure_check_radius) / FADE_BUFFER;
                        fadeAlpha = ALPHA * (1.0f - fadeProgress);
                    } else {
                        continue;
                    }
                    
                    if (fadeAlpha <= 0) continue;

                    float topY = (float) currentShape.bounds().maxY;

                    poseStack.pushPose();
                    poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                    PoseStack.Pose blockPose = poseStack.last();

                    renderTopFace(blockPose, consumer, 0, 1, topY, 0, 1, fadeAlpha);

                    poseStack.popPose();
                }
            }
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.entityTranslucentCull(BARRIER_TEXTURE));
    }

    private static void renderTopFace(PoseStack.Pose pose, VertexConsumer consumer, float minX, float maxX, float y, float minZ, float maxZ, float alpha) {
        int light = 15728880;
        int overlay = 655360;
        float yOffset = y + 0.0625f;

        int color = ((int)(alpha * 255) << 24) | ((int)(1.0f * 255) << 16) | ((int)(1.0f * 255) << 8) | (int)(1.0f * 255);
        
        consumer.addVertex(pose, minX, yOffset, minZ).setColor(color).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(pose, minX, yOffset, maxZ).setColor(color).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(pose, maxX, yOffset, maxZ).setColor(color).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(pose, maxX, yOffset, minZ).setColor(color).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
    }
}