package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.AbstractPacket;
import org.jetbrains.annotations.Nullable;

public record MeowC2S(int meow) implements AbstractPacket {

    public MeowC2S(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public void write(FriendlyByteBuf sendingBuffer) {
        sendingBuffer.writeInt(meow);
    }

    public void handle(@Nullable Level level, @Nullable Player player) {
        for (int x = 0; x < meow; x++) {
            assert level != null;
            if (level.isClientSide) {
                ViaRomana.LOGGER.info("Client - MEOWWWWWWWWWWWWWWWWWW");
            }
            else {
                ViaRomana.LOGGER.info("Server - MEOWWWWWWWWWWWWWWWWWW");
            }
        }
    }
}