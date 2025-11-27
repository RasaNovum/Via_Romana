package net.rasanovum.viaromana.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.network.AbstractPacket;

/**
 * Instruct the client to open the charting screen.
 */
public record OpenChartingScreenS2C() implements AbstractPacket {

    public OpenChartingScreenS2C(FriendlyByteBuf buf) {
        this();
    }

    public void write(FriendlyByteBuf buf) {
    }

    public void handle(Level level, Player player) {
        if (level != null && level.isClientSide) {
            ClientHandler.handleClient();
        }
    }

    private static class ClientHandler {
        private static void handleClient() {
            net.minecraft.network.chat.Component title = net.minecraft.network.chat.Component.translatable("gui.viaromana.charting_screen.title");
            net.rasanovum.viaromana.client.gui.ChartingScreen screen = new net.rasanovum.viaromana.client.gui.ChartingScreen(title);
            Minecraft.getInstance().setScreen(screen);
        }
    }
}