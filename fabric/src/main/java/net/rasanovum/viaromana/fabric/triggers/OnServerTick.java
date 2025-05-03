package net.rasanovum.viaromana.fabric.triggers;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class OnServerTick {
    public OnServerTick() {
        ServerTickEvents.START_SERVER_TICK.register((server) -> {
            OnPlayerTick.onPlayerTick(server);
        });
    }
}
