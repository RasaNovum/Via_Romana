package net.rasanovum.viaromana.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.rasanovum.viaromana.client.gui.WarpBlockScreen;

/**
 * Packet sent from server to client to request opening the WarpBlockScreen
 */
public class OpenWarpBlockScreenS2CPacket {
    private final BlockPos blockPos;

    public OpenWarpBlockScreenS2CPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }
    
    public OpenWarpBlockScreenS2CPacket(FriendlyByteBuf buffer) {
        this.blockPos = buffer.readBlockPos();
    }
    
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(blockPos);
    }
    
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    public static void handleClient(OpenWarpBlockScreenS2CPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            Player player = minecraft.player;
            if (player != null) {
                WarpBlockScreen screen = new WarpBlockScreen(packet.getBlockPos());
                minecraft.setScreen(screen);
            }
        });
    }
}
