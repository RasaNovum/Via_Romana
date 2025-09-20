package net.rasanovum.viaromana.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

public class SignValidationS2C {
    private final BlockPos nodePos;
    private final boolean isValid;
    
    public SignValidationS2C(BlockPos nodePos, boolean isValid) {
        this.nodePos = nodePos;
        this.isValid = isValid;
    }
    
    public SignValidationS2C(FriendlyByteBuf buf) {
        this.nodePos = buf.readBlockPos();
        this.isValid = buf.readBoolean();
    }
    
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.nodePos);
        buf.writeBoolean(this.isValid);
    }
    
    public BlockPos getNodePos() {
        return nodePos;
    }
    
    public boolean isValid() {
        return isValid;
    }
}
