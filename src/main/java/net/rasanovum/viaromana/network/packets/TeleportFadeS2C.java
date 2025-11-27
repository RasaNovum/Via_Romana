package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.client.FadeManager;
import net.rasanovum.viaromana.network.AbstractPacket;

/**
 * Server-to-client packet to initiate the teleport fade effect on the client.
 */
public record TeleportFadeS2C(int fadeUpTicks, int holdTicks, int fadeDownTicks, int footstepInterval) implements AbstractPacket {

    public TeleportFadeS2C(FriendlyByteBuf buf) {
        this(
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.fadeUpTicks);
        buf.writeInt(this.holdTicks);
        buf.writeInt(this.fadeDownTicks);
        buf.writeInt(this.footstepInterval);
    }

    public void handle(Level level, Player player) {
        if (level.isClientSide) {
            FadeManager.startFade(
                this.fadeUpTicks,
                this.holdTicks,
                this.fadeDownTicks,
                this.footstepInterval
            );
        }
    }
}
