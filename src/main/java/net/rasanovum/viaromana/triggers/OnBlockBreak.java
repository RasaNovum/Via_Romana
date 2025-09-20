package net.rasanovum.viaromana.triggers;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.rasanovum.viaromana.core.SignInteract;
import net.rasanovum.viaromana.core.LinkHandler;

public class OnBlockBreak {
    public OnBlockBreak() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockentity) -> {
            if (LinkHandler.isSignBlock(world, pos) && LinkHandler.isSignLinked(world, pos)) {
                if (!player.isShiftKeyDown()) {
                    return false;
                }
            }
            
            SignInteract.broken(world, pos, player);
            return true;
        });
    }
}
