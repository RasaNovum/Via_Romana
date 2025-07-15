package net.rasanovum.viaromana.util;

import net.rasanovum.viaromana.variables.VariableAccess;

import net.minecraft.world.entity.Entity;

// TODO: Check if sync is needed for cancelling click events
public class TimerUtils {
    private static final int LEFT_CLICK_TIMEOUT = 400;
    private static final int MESSAGE_TIMEOUT = 1000;

    // public static void checkTimers(Entity player) {
    //     if (player == null)
    //         return;
        
    //     checkMessageTimer(player);
    //     checkLeftClickTimer(player);
    // }
    
    public static void startLeftClickTimer(Entity entity) {
        if (entity == null)
            return;

        VariableAccess.playerVariables.setLeftClickTimer(entity, System.currentTimeMillis());
        VariableAccess.playerVariables.syncAndSave(entity);
    }
    
    public static boolean checkLeftClickTimer(Entity entity) {
        if (entity == null)
            return false;

        return System.currentTimeMillis() - VariableAccess.playerVariables.getLeftClickTimer(entity) >= LEFT_CLICK_TIMEOUT;
    }
    
    public static void startMessageTimer(Entity entity) {
        if (entity == null)
            return;
        
        VariableAccess.playerVariables.setMessageCooldown(entity, System.currentTimeMillis());
        VariableAccess.playerVariables.syncAndSave(entity);
    }
    
    public static boolean checkMessageTimer(Entity entity) {
        if (entity == null)
            return false;
        
        return System.currentTimeMillis() - VariableAccess.playerVariables.getMessageCooldown(entity) >= MESSAGE_TIMEOUT;
    }
}
