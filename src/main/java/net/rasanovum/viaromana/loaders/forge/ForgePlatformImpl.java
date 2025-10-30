package net.rasanovum.viaromana.loaders.forge;

//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.rasanovum.viaromana.loaders.Platform;

public class ForgePlatformImpl implements Platform {
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
        return "forge";
    }
}
*///?}

