package net.rasanovum.viaromana.loaders;

//? if fabric
import net.rasanovum.viaromana.loaders.fabric.FabricPlatformImpl;
//? if neoforge
/*import net.rasanovum.viaromana.loaders.neoforge.NeoforgePlatformImpl;*/




public interface Platform {
    //? if fabric
    Platform INSTANCE = new FabricPlatformImpl();
    //? if neoforge
    /*Platform INSTANCE = new NeoforgePlatformImpl();*/


    boolean isModLoaded(String modid);
    String loader();
}