package net.rasanovum.viaromana.fabric.init;

import net.rasanovum.viaromana.fabric.client.gui.ToastOverlay;
import net.rasanovum.viaromana.fabric.client.gui.FadeOverlay;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class ViaRomanaModOverlays {
	public static void load() {
		HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
			FadeOverlay.render(matrices, tickDelta.getRealtimeDeltaTicks());
			ToastOverlay.render(matrices, tickDelta.getRealtimeDeltaTicks());
		});
	}
}
