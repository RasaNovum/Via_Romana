package net.rasanovum.viaromana.loaders.forge;

//? if forge {
/*import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.BlockInit;
import net.rasanovum.viaromana.init.EffectInit;
import net.rasanovum.viaromana.init.ItemInit;
import net.rasanovum.viaromana.network.PacketRegistration;

@Mod(ViaRomana.MODID)
public class ForgeMain {
    public ForgeMain() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ViaRomana.initialize();
        new PacketRegistration().initCommon();
        EffectInit.MOB_EFFECTS.register(modEventBus);
        BlockInit.BLOCKS.register(modEventBus);
        BlockInit.ITEMS.register(modEventBus);
        ItemInit.ITEMS.register(modEventBus);
        modEventBus.addListener(ItemInit::onBuildContents);
    }
}
*///?}

