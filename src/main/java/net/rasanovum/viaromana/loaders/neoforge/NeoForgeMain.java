package net.rasanovum.viaromana.loaders.neoforge;

//? if neoforge {
//?} else if forge {
//?}
//? if neoforge || forge {
//?}

//? if neoforge {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
*///?} else if forge {
/*import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
*///?}
//? if neoforge || forge {
/*import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.BlockInit;
import net.rasanovum.viaromana.init.EffectInit;
import net.rasanovum.viaromana.init.ItemInit;

@Mod(ViaRomana.MODID)
public class NeoForgeMain {
    //? if neoforge
    /^public NeoForgeMain(IEventBus modEventBus) {^/
    //? if forge {
    /^public NeoForgeMain() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ^///?}

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