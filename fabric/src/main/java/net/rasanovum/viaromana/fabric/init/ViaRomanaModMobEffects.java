package net.rasanovum.viaromana.fabric.init;

import net.rasanovum.viaromana.fabric.ViaRomanaMod;
import net.rasanovum.viaromana.fabric.potion.TravellersFatigueMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.core.Registry;

public class ViaRomanaModMobEffects {
	public static MobEffect TRAVELLERS_FATIGUE;

	public static void load() {
		TRAVELLERS_FATIGUE = Registry.register(Registry.MOB_EFFECT, new ResourceLocation(ViaRomanaMod.MODID, "travellers_fatigue"), new TravellersFatigueMobEffect());
	}
}
