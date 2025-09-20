package net.rasanovum.viaromana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class TeleportRequestPacket {
    private final BlockPos originSignPos;
    private final BlockPos destinationPos;

    public TeleportRequestPacket(BlockPos originSignPos, BlockPos destinationPos) {
        this.originSignPos = originSignPos;
        this.destinationPos = destinationPos;
    }

    public TeleportRequestPacket(FriendlyByteBuf buffer) {
        this.originSignPos = buffer.readBlockPos();
        this.destinationPos = buffer.readBlockPos();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(originSignPos);
        buffer.writeBlockPos(destinationPos);
    }

    public BlockPos getOriginSignPos() {
        return originSignPos;
    }

    public BlockPos getDestinationPos() {
        return destinationPos;
    }
}
