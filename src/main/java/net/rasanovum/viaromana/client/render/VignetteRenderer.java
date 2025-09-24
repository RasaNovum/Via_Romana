package net.rasanovum.viaromana.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.ViaRomana;
import com.mojang.blaze3d.systems.RenderSystem;

public class VignetteRenderer {
    private static final ResourceLocation VIGNETTE_TEXTURE = ResourceLocation.parse("via_romana:textures/screens/overlay_vignette.png");

    public static void renderVignette(GuiGraphics guiGraphics) {
        try {
            float intensity = NodeRenderer.getCurrentVignetteIntensity();
            
            if (intensity > 0.0f) {
                renderVignetteOverlay(guiGraphics, intensity);
            }
        } catch (Exception e) {
            ViaRomana.LOGGER.error("Error rendering vignette effect", e);
        }
    }
    
    private static void renderVignetteOverlay(GuiGraphics guiGraphics, float intensity) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, intensity);
        guiGraphics.blit(VIGNETTE_TEXTURE, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
