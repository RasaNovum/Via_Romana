package net.rasanovum.viaromana.fabric.init;

import net.rasanovum.viaromana.fabric.ViaRomanaMod;
import net.rasanovum.viaromana.potion.TravellersFatigueMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;

public class ViaRomanaModMobEffects {
	public static MobEffect TRAVELLERS_FATIGUE;

	public static void load() {
		TRAVELLERS_FATIGUE = Registry.register(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(ViaRomanaMod.MODID, "travellers_fatigue"), new TravellersFatigueMobEffect());
	}
}
