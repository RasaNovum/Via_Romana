package net.rasanovum.viaromana.loaders.neoforge;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.rasanovum.viaromana.client.render.FadeRenderer;
import net.rasanovum.viaromana.client.render.InvalidBlockRenderer;
import net.rasanovum.viaromana.client.render.NodeRenderer;
import net.rasanovum.viaromana.client.render.VignetteRenderer;

@Environment(EnvType.CLIENT)
public class NeoForgeRenderInit {
	public static void load() {
		HudRenderCallback.EVENT.register((matrices, tickDeltaTrack) -> {
			//? if <1.21 {
			/*float tickDelta = tickDeltaTrack;
			*///?} else {
			float tickDelta = tickDeltaTrack.getGameTimeDeltaTicks();
			//?}
			VignetteRenderer.renderVignette(matrices);
			FadeRenderer.render(matrices, tickDelta);
		});
		
		WorldRenderEvents.LAST.register((context) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && mc.level != null) {
				//? if <1.21 {
				/*float tickDelta = context.tickDelta();
				*///?} else {
				float tickDelta = mc.getTimer().getGameTimeDeltaTicks();
				//?}
				NodeRenderer.renderNodeBeams(context.matrixStack(), mc.level, mc.player, tickDelta);
				InvalidBlockRenderer.renderInfrastructureBlocks(context.matrixStack(), mc.level, mc.player, tickDelta);
			}
		});
	}
}