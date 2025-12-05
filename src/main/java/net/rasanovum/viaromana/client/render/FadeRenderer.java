package net.rasanovum.viaromana.client.render;

import net.rasanovum.viaromana.client.FadeManager;
import net.rasanovum.viaromana.client.gui.TeleportMapScreen;
import net.rasanovum.viaromana.util.VersionUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class FadeRenderer {
    public static void render(GuiGraphics guiGraphics, float tickDelta) {
        float alpha = FadeManager.getCurrentFadeAlpha();
        
        if (alpha <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0, 0, 0, alpha);
        ResourceLocation blackTexture = VersionUtils.getLocation("via_romana:textures/screens/black.png");
        guiGraphics.blit(blackTexture, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}