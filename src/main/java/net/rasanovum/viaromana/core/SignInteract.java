package net.rasanovum.viaromana.core;

import net.rasanovum.viaromana.variables.VariableAccess;
import net.rasanovum.viaromana.network.packets.DestinationRequestC2S;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

/**
 * Handles sign interactions
 */
public class SignInteract {
    private static BlockPos lastClickedPos = null;
    private static long lastClickTime = 0;
    private static final long CLICK_DEBOUNCE_MS = 100;

    public static void broken(LevelAccessor world, BlockPos signPos, Entity entity) {
        if (entity == null) return;

        if (world instanceof ServerLevel serverLevel && LinkHandler.isSignBlock(world, signPos)) {
            LinkHandler.handleSignDestruction(serverLevel, signPos);
        }
    }

    public static boolean clicked(LevelAccessor world, BlockPos blockPos, Entity entity) {        
        if (entity == null) return false;

        if (LinkHandler.isSignBlock(world, blockPos)) {  
            BlockPos signPos = blockPos;
            if (LinkHandler.isSignLinked(world, signPos)) {
                if (!entity.isShiftKeyDown()) {
                    if (VariableAccess.playerVariables.getFadeAmount(entity) > 0) return false;
                    
                    long currentTime = System.currentTimeMillis();
                    if (blockPos.equals(lastClickedPos) && (currentTime - lastClickTime) < CLICK_DEBOUNCE_MS) {
                        return true;
                    }
                    
                    lastClickedPos = blockPos.immutable();
                    lastClickTime = currentTime;
                    
                    if (world.isClientSide() && entity instanceof Player) {
                        DestinationRequestC2S req = new DestinationRequestC2S(signPos);
                        commonnetwork.api.Dispatcher.sendToServer(req);
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
