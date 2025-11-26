package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.AbstractPacket;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.storage.path.PathDataManager;
import net.rasanovum.viaromana.util.PathSyncUtils;

import java.util.Optional;

/**
 * C2S action packet for path operations.
 */
public record RoutedActionC2S(Operation op) implements AbstractPacket {
    public enum Operation { SEVER_NEAREST_NODE, REMOVE_BRANCH }

    public RoutedActionC2S(FriendlyByteBuf buf) {
        this(Operation.values()[buf.readVarInt()]);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(op.ordinal());
    }

    public void handle(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            PathGraph graph = PathGraph.getInstance(serverLevel);

            Optional<Node> nearestOpt = graph.getNearestNode(
                    serverPlayer.blockPosition(),
                    CommonConfig.node_utility_distance,
                    node -> true
            );

            if (nearestOpt.isEmpty()) {
                ViaRomana.LOGGER.warn("No nearby node found for action {} by player {}", this.op, serverPlayer.getName().getString());
                return;
            }

            Node nearestNode = nearestOpt.get();

            switch (this.op) {
                case SEVER_NEAREST_NODE -> graph.removeNode(nearestNode);
                case REMOVE_BRANCH -> graph.removeBranch(nearestNode);
            }

            PathDataManager.markDirty(serverLevel);
            PathSyncUtils.syncPathGraphToAllPlayers(serverLevel);
        }
    }
}