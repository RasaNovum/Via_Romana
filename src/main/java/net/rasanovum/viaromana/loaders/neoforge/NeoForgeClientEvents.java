package net.rasanovum.viaromana.loaders.neoforge;

//? if neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.api.distmarker.Dist;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.HudMessageManager;
import net.rasanovum.viaromana.client.FadeManager;
import net.rasanovum.viaromana.client.triggers.OnClientPlayerTick;
import net.rasanovum.viaromana.network.PacketRegistration;
import net.rasanovum.viaromana.storage.player.PlayerData;

@EventBusSubscriber(modid = ViaRomana.MODID, value = Dist.CLIENT)
public class NeoForgeClientEvents {
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        ViaRomana.LOGGER.info("Initializing Via Romana Client");
        new PacketRegistration().initClient();
        NeoForgeRenderInit.load();
    }

    @SubscribeEvent
    public static void onPlayerLogin(final ClientPlayerNetworkEvent.LoggingIn event) {
        PlayerData.resetVariables(event.getPlayer());
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        HudMessageManager.onClientTick();
        OnClientPlayerTick.onClientTick();
        FadeManager.onClientTick();
    }
}
*///?}