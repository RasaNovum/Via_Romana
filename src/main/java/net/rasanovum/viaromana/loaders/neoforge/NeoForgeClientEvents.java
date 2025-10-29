package net.rasanovum.viaromana.loaders.neoforge;

//? if neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.api.distmarker.Dist;
*///?} else if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
*///?}
//? if neoforge || forge {
/*import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.HudMessageManager;
import net.rasanovum.viaromana.client.FadeManager;
import net.rasanovum.viaromana.client.triggers.OnClientPlayerTick;
import net.rasanovum.viaromana.network.PacketRegistration;
import net.rasanovum.viaromana.storage.player.PlayerData;

//? if neoforge
/^@EventBusSubscriber(modid = ViaRomana.MODID, value = Dist.CLIENT)^/
//? if forge
/^@Mod.EventBusSubscriber(modid = ViaRomana.MODID, value = Dist.CLIENT)^/
public class NeoForgeClientEvents {
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        ViaRomana.LOGGER.info("Initializing Via Romana Client");
        new PacketRegistration().initClient();
    }

    @SubscribeEvent
    public static void onPlayerLogin(final ClientPlayerNetworkEvent.LoggingIn event) {
        PlayerData.resetVariables(event.getPlayer());
    }

    @SubscribeEvent
    //? if neoforge {
    /^public static void onClientTick(final ClientTickEvent.Post event) {
        HudMessageManager.onClientTick();
        OnClientPlayerTick.onClientTick();
        FadeManager.onClientTick();
    }
    ^///?} else if forge {
    /^public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            HudMessageManager.onClientTick();
            OnClientPlayerTick.onClientTick();
            FadeManager.onClientTick();
        }
    }
    ^///?}
}
*///?}