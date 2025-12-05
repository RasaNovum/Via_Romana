package net.rasanovum.viaromana.loaders.neoforge;

//? if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.rasanovum.viaromana.ViaRomana;

import net.rasanovum.viaromana.client.render.*;

@OnlyIn(Dist.CLIENT)
public class NeoForgeRenderInit {
	public static void load() {
		NeoForge.EVENT_BUS.addListener(NeoForgeRenderInit::onRenderGui);
		NeoForge.EVENT_BUS.addListener(NeoForgeRenderInit::onRenderLevelStage);
	}

	public static void onRenderGui(RenderGuiEvent.Post event) {
		GuiGraphics matrices = event.getGuiGraphics();
		float tickDelta = event.getPartialTick().getGameTimeDeltaTicks();
		
		VignetteRenderer.renderVignette(matrices);
		FadeRenderer.render(matrices, tickDelta);
        ClientLinkParticleHandler.render(matrices, tickDelta);
	}

	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && mc.level != null) {
				float tickDelta = mc.getTimer().getGameTimeDeltaTicks();
				
				NodeRenderer.renderNodeBeams(event.getPoseStack(), mc.level, mc.player, tickDelta);
				InvalidBlockRenderer.renderInfrastructureBlocks(event.getPoseStack(), mc.level, mc.player, tickDelta);
			}
		}
	}
}
*///?}