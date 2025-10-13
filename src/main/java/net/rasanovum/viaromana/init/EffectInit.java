package net.rasanovum.viaromana.init;

import net.minecraft.world.effect.MobEffectCategory;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.effect.TravellersFatigueEffect;
import net.rasanovum.viaromana.util.VersionUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
//? if neoforge {
/*import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

public class EffectInit {
	//? if fabric {
	public static MobEffect TRAVELLERS_FATIGUE;

	public static void load() {
		TRAVELLERS_FATIGUE = Registry.register(BuiltInRegistries.MOB_EFFECT, VersionUtils.getLocation("via_romana:travellers_fatigue"), new TravellersFatigueEffect());
	}
	//?} else if neoforge {
	/*public static final DeferredRegister<MobEffect> MOB_EFFECTS =
			DeferredRegister.create(Registries.MOB_EFFECT, ViaRomana.MODID);

	public static final Holder<MobEffect> TRAVELLERS_FATIGUE = MOB_EFFECTS.register(
			"travellers_fatigue",
            TravellersFatigueEffect::new
	);
	*///?}
}
