package net.rasanovum.viaromana.fabric.triggers;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.rasanovum.viaromana.core.ResetVariables;
// import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class OnJoinWorld {
    public OnJoinWorld() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			execute(handler.getPlayer().getLevel(), handler.getPlayer());
		});
		// ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
		// 	execute(client.player.getLevel(), client.player);
		// });
	}

    private static void execute(LevelAccessor world, Entity entity) {
        ResetVariables.execute(world, entity);
    }
}
