package net.rasanovum.viaromana.loaders.neoforge;

//? if neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.HudMessageManager;
import net.rasanovum.viaromana.client.FadeManager;
import net.rasanovum.viaromana.client.render.ClientCursorHandler;
import net.rasanovum.viaromana.client.render.LinkIndicationHandler;
import net.rasanovum.viaromana.client.triggers.OnClientPlayerTick;
import net.rasanovum.viaromana.storage.player.PlayerData;

@EventBusSubscriber(modid = ViaRomana.MODID, value = Dist.CLIENT)
public class NeoForgeClientEvents {
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        ViaRomana.LOGGER.info("Initializing Via Romana Client");
        NeoForgeRenderInit.load();

        NeoForge.EVENT_BUS.addListener(NeoForgeClientEvents::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientEvents::onGameShuttingDown);
    }

    public static void onPlayerLogin(final ClientPlayerNetworkEvent.LoggingIn event) {
        PlayerData.resetVariables(event.getPlayer());
    }

    public static void onClientTick(final ClientTickEvent.Post event) {
        HudMessageManager.onClientTick();
        OnClientPlayerTick.onClientTick();
        FadeManager.onClientTick();
        LinkIndicationHandler.onClientTick();
        ClientCursorHandler.onClientTick();
    }

    public static void onGameShuttingDown(final GameShuttingDownEvent event) {
        ClientCursorHandler.destroy();
    }
}
*///?}