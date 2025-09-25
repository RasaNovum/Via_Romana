package net.rasanovum.viaromana.init;

import net.rasanovum.viaromana.network.ViaRomanaModClientPacketHandler;
import net.rasanovum.viaromana.client.HudMessageManager;
import net.rasanovum.viaromana.client.triggers.OnClientPlayerTick;
import net.rasanovum.viaromana.core.ResetVariables;

import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ClientModInitializer;

@Environment(EnvType.CLIENT)
public class ClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RenderInit.load();
        ViaRomanaModClientPacketHandler.registerS2CPackets();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) {
                ResetVariables.execute(client.player.level(), client.player);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HudMessageManager.onClientTick();
            OnClientPlayerTick.onClientTick();
        });
    }
}
