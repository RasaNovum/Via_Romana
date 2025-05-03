package net.rasanovum.viaromana.client.toasts;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.systems.RenderSystem;

public class ToastNormalToast implements Toast {
	private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/toasts.png");
	private long firstDrawTime;
	private boolean playedSound;

	@Override
	public Visibility render(GuiGraphics guiGraphics, ToastComponent component, long lastChanged) {
		if (this.firstDrawTime == 0L) {
			this.firstDrawTime = lastChanged;
			if (!this.playedSound) {
				component.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 1.0F, 1.0F));
				this.playedSound = true;
			}
		}
		guiGraphics.blit(TEXTURE, 0, 0, 0, 32, this.width(), this.height());
		RenderSystem.enableBlend();
		guiGraphics.blit(new ResourceLocation("via_romana:textures/screens/via_20x20.png"), 6, 7, 1, 1, 18, 18, 20, 20);
		guiGraphics.drawString(component.getMinecraft().font, Component.translatable("toasts.via_romana.toast_normal.title"), 30, 7, -11534256, false);
		guiGraphics.drawString(component.getMinecraft().font, Component.translatable("toasts.via_romana.toast_normal.description"), 30, 18, -16777216, false);
		if (lastChanged - this.firstDrawTime <= 5000)
			return Visibility.SHOW;
		else
			return Visibility.HIDE;
	}

	@Override
	public int width() {
		return 160;
	}

	@Override
	public int height() {
		return 32;
	}
}
