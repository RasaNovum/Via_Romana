package net.rasanovum.viaromana.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.gui.TeleportMapScreen;
import net.rasanovum.viaromana.util.VersionUtils;

import com.mojang.blaze3d.systems.RenderSystem;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class VignetteRenderer {
    private static final ResourceLocation VIGNETTE_TEXTURE = VersionUtils.getLocation("via_romana:textures/screens/overlay_vignette.png");
    private static final ResourceLocation CHARTING_VIGNETTE_TEXTURE = VersionUtils.getLocation("via_romana:textures/screens/overlay_vignette_charting.png");

    public static void renderVignette(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof Screen || mc.isPaused()) return;

        try {
            float intensity = NodeRenderer.getCurrentVignetteIntensity();
            int color = NodeRenderer.getCurrentNodeColor();
            ResourceLocation texture = color == NodeRenderer.CHARTING_BEAM_COLOR ? CHARTING_VIGNETTE_TEXTURE : VIGNETTE_TEXTURE;
            
            if (intensity > 0.0f) {
                renderVignetteOverlay(guiGraphics, intensity, texture);
            }
        } catch (Exception e) {
            ViaRomana.LOGGER.error("Error rendering vignette effect", e);
        }
    }
    
    private static void renderVignetteOverlay(GuiGraphics guiGraphics, float intensity, ResourceLocation texture) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, intensity * CommonConfig.node_vignette_opacity);
        guiGraphics.blit(texture, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
