package net.rasanovum.viaromana.client.triggers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.rasanovum.viaromana.client.core.ChartingHandler;

/**
 * Client-side player tick handler for operations that need to run on the client.
 */
public class OnClientPlayerTick {
    public static void onClientTick() {
        Minecraft minecraft = Minecraft.getInstance();

        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        
        if (player == null || level == null || !player.isAlive()) return;

        ChartingHandler.chartPath(level, player);
    }
}