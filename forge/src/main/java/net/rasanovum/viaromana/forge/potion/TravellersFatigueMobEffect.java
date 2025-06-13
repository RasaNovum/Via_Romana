
package net.rasanovum.viaromana.forge.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import java.util.List;

public class TravellersFatigueMobEffect extends MobEffect {
	public TravellersFatigueMobEffect() {
		super(MobEffectCategory.HARMFUL, -1);
	}

	@Override
	public String getDescriptionId() {
		return "effect.via_romana.travellers_fatigue";
	}

	@Override
	public boolean isInstantenous() {
		return false;
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	@Override
    public List<ItemStack> getCurativeItems() {
        return List.of();
    }
}
