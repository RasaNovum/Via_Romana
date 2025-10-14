package net.rasanovum.viaromana.loaders.neoforge;

import net.rasanovum.viaromana.loaders.Platform;
//? if neoforge || forge
/*import net.neoforged.fml.ModList;*/

//? if neoforge || forge {
/*public class NeoForgePlatformImpl implements Platform {
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