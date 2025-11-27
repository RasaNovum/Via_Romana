package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.core.LinkHandler;
import net.rasanovum.viaromana.network.AbstractPacket;

/**
 * Request the server to unlink the sign at the given position.
 */
public record SignUnlinkRequestC2S(BlockPos signPos) implements AbstractPacket {

    public SignUnlinkRequestC2S(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.signPos);
    }

    public void handle(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            if (CommonConfig.logging_enum.ordinal() > 0) ViaRomana.LOGGER.info("{} requested sign unlink at {}", serverPlayer, this.signPos);
            
            LinkHandler.unlinkSignFromNode(serverLevel, this.signPos);
        }
    }
}
