package net.rasanovum.viaromana.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.util.VersionUtils;
//? if fabric {
import net.minecraft.core.Registry;
//?} else if neoforge {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?} else if forge {
/*import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
*///?}

public class StatInit {
    public static final ResourceLocation DISTANCE_CHARTED = VersionUtils.getLocation(ViaRomana.MODID, "distance_charted");
    public static final ResourceLocation DISTANCE_WALKED = VersionUtils.getLocation(ViaRomana.MODID, "distance_walked");

    //? if fabric {
    public static void register() {
        Registry.register(BuiltInRegistries.CUSTOM_STAT, DISTANCE_CHARTED, DISTANCE_CHARTED);
        Registry.register(BuiltInRegistries.CUSTOM_STAT, DISTANCE_WALKED, DISTANCE_WALKED);

        Stats.CUSTOM.get(DISTANCE_CHARTED, StatFormatter.DISTANCE);
        Stats.CUSTOM.get(DISTANCE_WALKED, StatFormatter.DISTANCE);
    }
    //?} else if neoforge || forge {
    /*//? if neoforge
    /^private static final DeferredRegister<ResourceLocation> CUSTOM_STATS = DeferredRegister.create(BuiltInRegistries.CUSTOM_STAT, ViaRomana.MODID);^/
    //? if forge
    /^private static final DeferredRegister<ResourceLocation> CUSTOM_STATS = DeferredRegister.create(Registries.CUSTOM_STAT, ViaRomana.MODID);^/

    static {
        CUSTOM_STATS.register(DISTANCE_CHARTED.getPath(), () -> DISTANCE_CHARTED);
        CUSTOM_STATS.register(DISTANCE_WALKED.getPath(), () -> DISTANCE_WALKED);
    }

    public static void register(IEventBus eventBus) {
        CUSTOM_STATS.register(eventBus);
    }

    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Stats.CUSTOM.get(DISTANCE_CHARTED, StatFormatter.DISTANCE);
            Stats.CUSTOM.get(DISTANCE_WALKED, StatFormatter.DISTANCE);
        });
    }
    *///?}
}