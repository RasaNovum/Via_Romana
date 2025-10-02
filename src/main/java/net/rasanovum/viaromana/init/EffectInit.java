package net.rasanovum.viaromana.init;

import net.rasanovum.viaromana.effect.TravellersFatigueEffect;
import net.rasanovum.viaromana.util.VersionUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;

public class EffectInit {
	public static MobEffect TRAVELLERS_FATIGUE;

	public static void load() {
		TRAVELLERS_FATIGUE = Registry.register(BuiltInRegistries.MOB_EFFECT, VersionUtils.getLocation("via_romana:travellers_fatigue"), new TravellersFatigueEffect());
	}
}
