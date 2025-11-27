package net.rasanovum.viaromana.loaders.forge;

//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.HudMessageManager;
import net.rasanovum.viaromana.client.FadeManager;
import net.rasanovum.viaromana.client.triggers.OnClientPlayerTick;
import net.rasanovum.viaromana.network.PacketRegistration;
import net.rasanovum.viaromana.storage.player.PlayerData;

@Mod.EventBusSubscriber(modid = ViaRomana.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeClientEvents {
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        ViaRomana.LOGGER.info("Initializing Via Romana Client");
        ForgeRenderInit.load();
        
        MinecraftForge.EVENT_BUS.addListener(ForgeClientEvents::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(ForgeClientEvents::onClientTick);
    }

    public static void onPlayerLogin(final ClientPlayerNetworkEvent.LoggingIn event) {
        PlayerData.resetVariables(event.getPlayer());
    }

    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            HudMessageManager.onClientTick();
            OnClientPlayerTick.onClientTick();
            FadeManager.onClientTick();
        }
    }
}
*///?}

