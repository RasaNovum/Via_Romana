package net.rasanovum.viaromana.fabric.triggers;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.rasanovum.viaromana.fabric.configuration.ConfigVariables;

public class OnWorldLoad {
    public OnWorldLoad() {
        ServerWorldEvents.LOAD.register((server, level) -> {
            ConfigVariables.load(level);
        });
    }
}