package net.rasanovum.viaromana.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.client.gui.TeleportMapScreen;
import net.rasanovum.viaromana.network.AbstractPacket;

/**
 * Response from the server indicating whether the sign at the given position is valid.
 */
public record SignValidationResponseS2C(BlockPos nodePos, boolean isValid) implements AbstractPacket {

    public SignValidationResponseS2C(FriendlyByteBuf buf) {
        this(
            buf.readBlockPos(),
            buf.readBoolean()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.nodePos);
        buf.writeBoolean(this.isValid);
    }

    public void handle(Level level, Player player) {
        if (level.isClientSide) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof TeleportMapScreen screen) {
                screen.handleSignValidation(this.nodePos, this.isValid);
            }
        }
    }
}
