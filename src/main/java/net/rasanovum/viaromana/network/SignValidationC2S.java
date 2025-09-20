package net.rasanovum.viaromana.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

public class SignValidationC2S {
    private final BlockPos nodePos;
    
    public SignValidationC2S(BlockPos nodePos) {
        this.nodePos = nodePos;
    }
    
    public SignValidationC2S(FriendlyByteBuf buf) {
        this.nodePos = buf.readBlockPos();
    }
    
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.nodePos);
    }
    
    public BlockPos getNodePos() {
        return nodePos;
    }
}
