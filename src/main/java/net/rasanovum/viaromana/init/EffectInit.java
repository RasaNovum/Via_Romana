package net.rasanovum.viaromana.init;

import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.effect.TravellersFatigueEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;

public class EffectInit {
	public static MobEffect TRAVELLERS_FATIGUE;

	public static void load() {
		TRAVELLERS_FATIGUE = Registry.register(BuiltInRegistries.MOB_EFFECT, new ResourceLocation(ViaRomana.MODID, "travellers_fatigue"), new TravellersFatigueEffect());
	}
}
