package net.rasanovum.viaromana.fabric.triggers;

import net.minecraft.world.InteractionResult;
import net.rasanovum.viaromana.core.SignInteract;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;

public class OnBlockClick {
    private static boolean cancelEvent = false;
    
    public OnBlockClick() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            cancelEvent = false;
            SignInteract.clicked(level, pos.getX(), pos.getY(), pos.getZ(), player);
            return cancelEvent ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
    }
    
    public static void setCancelEvent(boolean cancel) {
        cancelEvent = cancel;
    }
}