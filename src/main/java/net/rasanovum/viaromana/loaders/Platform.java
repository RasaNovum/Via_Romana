package net.rasanovum.viaromana.loaders;

public interface Platform {
    //? if fabric
    Platform INSTANCE = new net.rasanovum.viaromana.loaders.fabric.FabricPlatformImpl();
    //? if neoforge
    /*Platform INSTANCE = new net.rasanovum.viaromana.loaders.neoforge.NeoForgePlatformImpl();*/
    //? if forge
    /*Platform INSTANCE = new net.rasanovum.viaromana.loaders.forge.ForgePlatformImpl();*/


    boolean isModLoaded(String modid);
    boolean isClientSide();
    boolean isServerSide();
    String loader();
}