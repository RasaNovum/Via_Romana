package net.rasanovum.viaromana.loaders.neoforge;

//? if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
*///?} else if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
*///?}
//? if neoforge || forge {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.render.FadeRenderer;
import net.rasanovum.viaromana.client.render.InvalidBlockRenderer;
import net.rasanovum.viaromana.client.render.NodeRenderer;
import net.rasanovum.viaromana.client.render.VignetteRenderer;

@OnlyIn(Dist.CLIENT)
public class NeoForgeRenderInit {
	//? if neoforge {
	/^public static void load() {
		NeoForge.EVENT_BUS.addListener(NeoForgeRenderInit::onRenderGui);
		NeoForge.EVENT_BUS.addListener(NeoForgeRenderInit::onRenderLevelStage);
	}
	^///?} else if forge {
	/^public static void load() {
		MinecraftForge.EVENT_BUS.addListener(NeoForgeRenderInit::onRenderGui);
		MinecraftForge.EVENT_BUS.addListener(NeoForgeRenderInit::onRenderLevelStage);
	}
	^///?}

	public static void onRenderGui(RenderGuiEvent.Post event) {
		GuiGraphics matrices = event.getGuiGraphics();
		//? if neoforge
		/^float tickDelta = event.getPartialTick().getGameTimeDeltaTicks();^/
		//? if forge
		/^float tickDelta = event.getPartialTick();^/
		
		VignetteRenderer.renderVignette(matrices);
		FadeRenderer.render(matrices, tickDelta);
	}

	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && mc.level != null) {
				//? if neoforge
				/^float tickDelta = mc.getTimer().getGameTimeDeltaTicks();^/
				//? if forge
				/^float tickDelta = mc.getFrameTime();^/
				
				NodeRenderer.renderNodeBeams(event.getPoseStack(), mc.level, mc.player, tickDelta);
				InvalidBlockRenderer.renderInfrastructureBlocks(event.getPoseStack(), mc.level, mc.player, tickDelta);
			}
		}
	}
}
*///?}