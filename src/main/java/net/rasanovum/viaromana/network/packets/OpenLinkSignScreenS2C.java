package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.network.AbstractPacket;

/**
 * Instruct the client to open the link sign screen.
 */
public record OpenLinkSignScreenS2C() implements AbstractPacket {

    public OpenLinkSignScreenS2C(FriendlyByteBuf buf) {
        this();
    }

    public void write(FriendlyByteBuf buf) {
    }

    public void handle(Level level, Player player) {
        // Currently no implementation needed
    }
}
