package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.network.AbstractPacket;

/**
 * Instruct the client to open the warp block screen for the block at the given position.
 */
public record OpenWarpBlockScreenS2C(BlockPos blockPos) implements AbstractPacket {

    public OpenWarpBlockScreenS2C(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.blockPos);
    }

    public void handle(Level level, Player player) {
        if (level != null && level.isClientSide) {
            OpenWarpBlockScreenS2C.ClientHandler.handleClient(this.blockPos);
        }
    }

    private static class ClientHandler {
        private static void handleClient(BlockPos blockPos) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new net.rasanovum.viaromana.client.gui.WarpBlockScreen(blockPos));
        }
    }
}
