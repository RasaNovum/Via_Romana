package net.rasanovum.viaromana.loaders.forge;

//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.rasanovum.viaromana.ViaRomana;

import net.rasanovum.viaromana.client.render.*;

@OnlyIn(Dist.CLIENT)
public class ForgeRenderInit {
	public static void load() {
		MinecraftForge.EVENT_BUS.addListener(ForgeRenderInit::onRenderGui);
		MinecraftForge.EVENT_BUS.addListener(ForgeRenderInit::onRenderLevelStage);
	}

	public static void onRenderGui(RenderGuiEvent.Post event) {
		GuiGraphics matrices = event.getGuiGraphics();
		float tickDelta = event.getPartialTick();
		
		VignetteRenderer.renderVignette(matrices);
		FadeRenderer.render(matrices, tickDelta);
        ClientLinkParticleHandler.render(matrices, tickDelta);
	}

	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && mc.level != null) {
				float tickDelta = mc.getFrameTime();
				
				NodeRenderer.renderNodeBeams(event.getPoseStack(), mc.level, mc.player, tickDelta);
				InvalidBlockRenderer.renderInfrastructureBlocks(event.getPoseStack(), mc.level, mc.player, tickDelta);
			}
		}
	}
}
*///?}

