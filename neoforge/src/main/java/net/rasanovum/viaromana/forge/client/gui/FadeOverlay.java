package net.rasanovum.viaromana.forge.client.gui;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.rasanovum.viaromana.variables.VariableAccess;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

@EventBusSubscriber(modid = ViaRomanaMod.MODID, value = Dist.CLIENT)
public class FadeOverlay {
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void eventHandler(RenderGuiEvent.Pre event) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        Player entity = Minecraft.getInstance().player;
        int fade = 0;
        
        if (entity != null) {
            fade = (int) VariableAccess.playerVariables.getFadeAmount(entity);
        }

        int fadeLevel = Math.max(0, Math.min(10, fade));
        
        if (fadeLevel > 0) {
            float alpha = fadeLevel / 10.0f;

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(0, 0, 0, alpha);
            
            @SuppressWarnings("removal")
            ResourceLocation blackTexture = ResourceLocation.fromNamespaceAndPath("via_romana","textures/screens/black.png");
            event.getGuiGraphics().blit(blackTexture, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);

            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }
}