package net.rasanovum.viaromana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class DestinationRequestPacket {
    private final BlockPos sourceSignPos;
    
    public DestinationRequestPacket(BlockPos sourceSignPos) {
        this.sourceSignPos = sourceSignPos;
    }
    
    public DestinationRequestPacket(FriendlyByteBuf buffer) {
        this.sourceSignPos = buffer.readBlockPos();
    }
    
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(sourceSignPos);
    }
    
    public BlockPos getSourceSignPos() {
        return sourceSignPos;
    }
}
