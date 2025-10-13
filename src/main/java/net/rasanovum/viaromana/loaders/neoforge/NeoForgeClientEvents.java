package net.rasanovum.viaromana.loaders.neoforge;

//? if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.rasanovum.viaromana.ViaRomana;

@EventBusSubscriber(modid = ViaRomana.MODID, value = Dist.CLIENT)
public final class NeoForgeClientEvents {
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        ViaRomana.LOGGER.info("Initializing {} Client", ViaRomana.MODID);
    }
}
*///?}