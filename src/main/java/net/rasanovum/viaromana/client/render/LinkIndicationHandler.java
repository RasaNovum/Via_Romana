package net.rasanovum.viaromana.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.FadeManager;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.util.VersionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class LinkIndicationHandler {
    private static int tickCounter = 0;
    private static final int SCAN_INTERVAL = 10;
    private static final double RENDER_RADIUS = 12.0;
    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<BlockPos> CACHED_SIGNS = new ArrayList<>();

    private static final Component TOOLTIP_TEXT = Component.translatable("gui.viaromana.left_click_hover_tip");
    private static final ResourceLocation CURSOR = VersionUtils.getLocation("via_romana:textures/screens/cursor.png");

    private static final int ICON_SIZE = 18;
    private static float currentOpacity = 0.0f;
    private static final float fadeSpeed = 0.25f;
    private static boolean isHovering = false;

    final static double driftRange = 0.5;

    public static void onClientTick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.isPaused()) return;

        if (CommonConfig.enable_custom_cursor) {
            handleLookTarget(client);
            updateOpacity();
        }
        else {
            currentOpacity = 0.0f;
            isHovering = false;
        }

        tickCounter++;
        if (tickCounter % SCAN_INTERVAL != 0) return;

        updateCacheAndSpawnParticles(client);
    }

    public static float getCrosshairAlpha() {
        if (!CommonConfig.enable_custom_cursor) return 1.0f;
        float globalFade = 1.0f - FadeManager.getCurrentFadeAlpha();
        return (1.0f - currentOpacity) * globalFade;
    }

    public static void render(GuiGraphics graphics, float tickDelta) {
        if (currentOpacity <= 0.05f) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.options.hideGui) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        float alpha = Mth.clamp(currentOpacity, 0.0f, 1.0f);
        float globalFade = 1.0f - FadeManager.getCurrentFadeAlpha();

        alpha *= globalFade;

        if (alpha <= 0.01f) return;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        RenderSystem.enableBlend();

//        graphics.drawCenteredString(mc.font, TOOLTIP_TEXT, width / 2, (height / 2) + 10, (alpha << 24) | 0xFFFFFF);
        graphics.blit(CURSOR, (width - ICON_SIZE) / 2, (height - ICON_SIZE) / 2, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.defaultBlendFunc();
    }

    private static void updateOpacity() {
        if (isHovering) {
            currentOpacity += fadeSpeed;
            if (currentOpacity > 1.0f) currentOpacity = 1.0f;
        } else {
            currentOpacity -= fadeSpeed;
            if (currentOpacity < 0.0f) currentOpacity = 0.0f;
        }
    }

    private static void handleLookTarget(Minecraft mc) {
        isHovering = false;

        if (Objects.requireNonNull(mc.player).isCrouching()) return;

        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = ((BlockHitResult) mc.hitResult).getBlockPos();

            if (CACHED_SIGNS.contains(hitPos)) {
                isHovering = true;
            }
        }
    }

    private static void updateCacheAndSpawnParticles(Minecraft client) {
        CACHED_SIGNS.clear();

        assert client.player != null;
        BlockPos playerPos = client.player.blockPosition();
        List<Node> nearbyNodes = ClientPathData.getInstance().getNearbyNodes(playerPos, RENDER_RADIUS, false);

        for (Node node : nearbyNodes) {
            if (node.isLinked()) {
                node.getSignPos().ifPresent(signPosLong -> {
                    BlockPos signPos = BlockPos.of(signPosLong);
                    if (signPos.distSqr(playerPos) <= (RENDER_RADIUS * RENDER_RADIUS)) {
                        CACHED_SIGNS.add(signPos);
                        spawnLinkParticles(client.level, signPos);
                    }
                });
            }
        }
    }

    private static void spawnLinkParticles(Level level, BlockPos pos) {
        if (!CommonConfig.enable_sign_particles) return;
        for (int i = 0; i < 1; i++) {
            double startX = pos.getX() + 0.5 + (RANDOM.nextDouble() - 0.5) * 1.5;
            double startY = pos.getY() + 0.5 + RANDOM.nextDouble() * 0.8;
            double startZ = pos.getZ() + 0.5 + (RANDOM.nextDouble() - 0.5) * 1.5;

            double endX = startX + (RANDOM.nextDouble() - 0.5) * driftRange;
            double endZ = startZ + (RANDOM.nextDouble() - 0.5) * driftRange;

            level.addParticle(
                    ParticleTypes.ENCHANT,
                    endX, startY, endZ,
                    startX - endX, 0, startZ - endZ
            );
        }
    }
}