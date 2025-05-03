package net.rasanovum.viaromana.fabric;

import net.rasanovum.viaromana.fabric.network.ViaRomanaModClientPacketHandler;
import net.rasanovum.viaromana.fabric.init.ViaRomanaModOverlays;
import net.rasanovum.viaromana.core.ResetVariables;

import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ClientModInitializer;

@Environment(EnvType.CLIENT)
public class ClientInit implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ViaRomanaModOverlays.load();
		ViaRomanaModClientPacketHandler.registerS2CPackets();

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) {
                ResetVariables.execute(client.player.level(), client.player);
            }
        });
	}
}
