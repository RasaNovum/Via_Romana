package net.rasanovum.viaromana.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * C2S action packet
 */
public class RoutedActionC2S {

    public enum Operation {
        SEVER_NEAREST_NODE,
        REMOVE_BRANCH
    }

    private final Operation op;

    public RoutedActionC2S(Operation op) {
        this.op = op;
    }

    public RoutedActionC2S(FriendlyByteBuf buf) {
        this.op = buf.readEnum(Operation.class);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(op);
    }

    public Operation op() {
        return op;
    }
}
