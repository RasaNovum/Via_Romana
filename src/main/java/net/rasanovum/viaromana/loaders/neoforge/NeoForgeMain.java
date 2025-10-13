package net.rasanovum.viaromana.loaders.neoforge;

//? if neoforge {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.BlockInit;
import net.rasanovum.viaromana.init.EffectInit;
import net.rasanovum.viaromana.init.ItemInit;

@Mod("via_romana")
public class NeoForgeMain {
    public NeoForgeMain(IEventBus modEventBus) {
        ViaRomana.initialize();
        EffectInit.MOB_EFFECTS.register(modEventBus);
        BlockInit.BLOCKS.register(modEventBus);
        BlockInit.ITEMS.register(modEventBus);
        ItemInit.ITEMS.register(modEventBus);
        NeoForgeRenderInit.load();
        modEventBus.addListener(ItemInit::onBuildContents);
    }
}
*///?}