package net.rasanovum.viaromana.init;

import net.rasanovum.viaromana.client.render.FadeRenderer;
import net.rasanovum.viaromana.client.render.InvalidBlockRenderer;
import net.rasanovum.viaromana.client.render.NodeRenderer;
import net.rasanovum.viaromana.client.render.VignetteRenderer;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class RenderInit {
	public static void load() {
		HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
			VignetteRenderer.renderVignette(matrices);
			FadeRenderer.render(matrices, tickDelta);
		});
		
		WorldRenderEvents.LAST.register((context) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && mc.level != null) {
				NodeRenderer.renderNodeBeams(context.matrixStack(), mc.level, mc.player, context.tickDelta());
				InvalidBlockRenderer.renderInfrastructureBlocks(context.matrixStack(), mc.level, mc.player, context.tickDelta());
			}
		});
	}
}
