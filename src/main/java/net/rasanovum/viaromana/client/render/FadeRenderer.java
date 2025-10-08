package net.rasanovum.viaromana.client.render;

import net.rasanovum.viaromana.storage.player.PlayerData;
import net.rasanovum.viaromana.util.VersionUtils;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

@Environment(EnvType.CLIENT)
public class FadeRenderer {
    public static void render(GuiGraphics guiGraphics, float tickDelta) {
        int _fade = 0;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        Player entity = Minecraft.getInstance().player;
        if (entity != null) {
            _fade = (int) PlayerData.getFadeAmount(entity);
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        int fadeLevel = Math.max(0, Math.min(10, _fade));
        
        if (fadeLevel > 0) {
            float alpha = fadeLevel / 10.0f;
            
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(0, 0, 0, alpha);
            ResourceLocation blackTexture = VersionUtils.getLocation("via_romana:textures/screens/black.png");
            guiGraphics.blit(blackTexture, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
        
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}