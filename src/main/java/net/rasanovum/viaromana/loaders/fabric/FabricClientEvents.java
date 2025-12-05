package net.rasanovum.viaromana.loaders.fabric;
//? if fabric {
import net.rasanovum.viaromana.client.HudMessageManager;
import net.rasanovum.viaromana.client.FadeManager;
import net.rasanovum.viaromana.client.render.ClientLinkParticleHandler;
import net.rasanovum.viaromana.client.triggers.OnClientPlayerTick;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ClientModInitializer;
import net.rasanovum.viaromana.storage.player.PlayerData;

@Environment(EnvType.CLIENT)
public class FabricClientEvents implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricRenderInit.load();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PlayerData.resetVariables(client.player);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HudMessageManager.onClientTick();
            OnClientPlayerTick.onClientTick();
            FadeManager.onClientTick();
            ClientLinkParticleHandler.onClientTick();
        });
    }
}
//?}