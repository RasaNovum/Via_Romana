package net.rasanovum.viaromana.loaders.neoforge;

//? if neoforge {
/*import net.neoforged.fml.ModList;
*///?} else if forge {
/*import net.minecraftforge.fml.ModList;
*///?}
//? if neoforge || forge {
/*import net.rasanovum.viaromana.loaders.Platform;

public class NeoForgePlatformImpl implements Platform {
    @Override
    public boolean isModLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }

    @Override
    public String loader() {
        return "neoforge";
    }

}
*///?}