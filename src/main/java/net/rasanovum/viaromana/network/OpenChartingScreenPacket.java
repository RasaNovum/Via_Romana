package net.rasanovum.viaromana.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Packet sent from server to client to request opening the ChartingScreen
 */
public class OpenChartingScreenPacket {

    private final boolean isCharting;

    public OpenChartingScreenPacket(boolean isCharting) {
        this.isCharting = isCharting;
    }

    public OpenChartingScreenPacket(FriendlyByteBuf buffer) {
        this.isCharting = buffer.readBoolean();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(isCharting);
    }

    public boolean isCharting() {
        return isCharting;
    }
}
