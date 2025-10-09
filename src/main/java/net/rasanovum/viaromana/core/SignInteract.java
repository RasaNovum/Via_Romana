package net.rasanovum.viaromana.core;

import commonnetwork.api.Dispatcher;
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
            if (LinkHandler.isSignLinked(world, blockPos)) {
                if (!entity.isShiftKeyDown()) {
                    long currentTime = System.currentTimeMillis();
                    if (blockPos.equals(lastClickedPos) && (currentTime - lastClickTime) < CLICK_DEBOUNCE_MS) {
                        return true;
                    }
                    
                    lastClickedPos = blockPos.immutable();
                    lastClickTime = currentTime;
                    
                    if (world.isClientSide() && entity instanceof Player) {
                        DestinationRequestC2S req = new DestinationRequestC2S(blockPos);
                        Dispatcher.sendToServer(req);
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
