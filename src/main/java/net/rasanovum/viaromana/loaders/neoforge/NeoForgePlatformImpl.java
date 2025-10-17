package net.rasanovum.viaromana.loaders.neoforge;

//? if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
*///?} else if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
*///?}
//? if neoforge || forge {
/*import net.rasanovum.viaromana.loaders.Platform;

public class NeoForgePlatformImpl implements Platform {
    @Override
    public boolean isModLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }

    @Override
    public boolean isClientSide() {
        return FMLLoader.getDist() == Dist.CLIENT;
    }

    @Override
    public boolean isServerSide() {
        return FMLLoader.getDist() == Dist.DEDICATED_SERVER;
    }

    @Override
    public String loader() {
        //? if neoforge
        /^return "neoforge";^/
        //? if forge
        /^return "forge";^/
    }
}
*///?}