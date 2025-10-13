package net.rasanovum.viaromana.loaders.fabric;

import net.rasanovum.viaromana.loaders.Platform;
//? if fabric {
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatformImpl implements Platform {
    @Override
    public boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }

    @Override
    public String loader() {
        return "fabric";
    }

}
//?}