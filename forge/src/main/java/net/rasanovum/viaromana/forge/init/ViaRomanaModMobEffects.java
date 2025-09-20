package net.rasanovum.viaromana.forge.init;

import net.rasanovum.viaromana.forge.potion.TravellersFatigueMobEffect;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

public class ViaRomanaModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ViaRomanaMod.MODID);
    
    public static final RegistryObject<MobEffect> TRAVELLERS_FATIGUE = MOB_EFFECTS.register("travellers_fatigue", TravellersFatigueMobEffect::new);
    
    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
