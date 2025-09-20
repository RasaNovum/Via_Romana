package net.rasanovum.viaromana.fabric.triggers;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.rasanovum.viaromana.core.SignInteract;

public class OnBlockBreak {
    public OnBlockBreak() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockentity) -> {
            SignInteract.broken(world, pos.getX(), pos.getY(), pos.getZ(), player);
			return true;
		});
    }
}
