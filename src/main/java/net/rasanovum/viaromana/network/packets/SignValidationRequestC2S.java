package net.rasanovum.viaromana.network.packets;

import dev.corgitaco.dataanchor.network.broadcast.PacketBroadcaster;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.AbstractPacket;
import net.rasanovum.viaromana.path.PathGraph;

/**
 * Request the server to validate the sign at the given position.
 * The server will respond with a SignValidationResponseS2C packet.
 */
public record SignValidationRequestC2S(BlockPos nodePos) implements AbstractPacket {

    public SignValidationRequestC2S(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.nodePos);
    }
    
    public void handle(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            PathGraph graph = PathGraph.getInstance(serverLevel);
            boolean isValid = graph.getNodeAt(this.nodePos).isPresent();

            if (CommonConfig.logging_enum.ordinal() > 0 && !isValid) ViaRomana.LOGGER.info("Validation failed for destination at {}", this.nodePos);
            
            SignValidationResponseS2C response = new SignValidationResponseS2C(this.nodePos, isValid);
            PacketBroadcaster.S2C.sendToPlayer(response, serverPlayer);
        }
    }
}
