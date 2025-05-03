package net.rasanovum.viaromana.forge.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rasanovum.viaromana.potion.TravellersFatigueMobEffect;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

import net.minecraft.world.effect.MobEffect;

public class ViaRomanaModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ViaRomanaMod.MODID);
    
    public static final DeferredHolder<MobEffect, TravellersFatigueMobEffect> TRAVELLERS_FATIGUE = MOB_EFFECTS.register("travellers_fatigue", TravellersFatigueMobEffect::new);
    
    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
