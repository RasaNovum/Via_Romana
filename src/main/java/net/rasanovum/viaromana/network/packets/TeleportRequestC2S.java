package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.network.AbstractPacket;
import net.rasanovum.viaromana.teleport.ServerTeleportHandler;

/**
 * Request the server to teleport the player from the origin sign position to the destination position.
 */
public record TeleportRequestC2S(BlockPos originSignPos, BlockPos destinationPos) implements AbstractPacket {

    public TeleportRequestC2S(FriendlyByteBuf buf) {
        this(
            buf.readBlockPos(),
            buf.readBlockPos()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.originSignPos);
        buf.writeBlockPos(this.destinationPos);
    }

    public void handle(Level level, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ServerTeleportHandler.handleTeleportRequest(this, serverPlayer);
        }
    }
}
