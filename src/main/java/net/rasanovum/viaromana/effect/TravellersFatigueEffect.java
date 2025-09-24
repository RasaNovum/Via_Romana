package net.rasanovum.viaromana.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class TravellersFatigueEffect extends MobEffect {
	public TravellersFatigueEffect() {
		super(MobEffectCategory.HARMFUL, -1);
	}

	@Override
	public String getDescriptionId() {
		return "effect.via_romana.travellers_fatigue";
	}
}
