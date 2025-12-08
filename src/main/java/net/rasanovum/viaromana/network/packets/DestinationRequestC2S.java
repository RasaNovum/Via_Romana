package net.rasanovum.viaromana.network.packets;

import dev.corgitaco.dataanchor.network.broadcast.PacketBroadcaster;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.AbstractPacket;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.teleport.ServerTeleportHandler;

import java.util.List;

/**
 * Request the server to provide a list of available destinations for the network containing the sign at the given position.
 */
public record DestinationRequestC2S(BlockPos sourceSignPos) implements AbstractPacket {

    public DestinationRequestC2S(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.sourceSignPos);
    }

    public void handle(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            PathGraph graph = PathGraph.getInstance(serverLevel);

            var sourceNodeOpt = graph.getNodeBySignPos(this.sourceSignPos);

            if (sourceNodeOpt.isEmpty()) {
                ViaRomana.LOGGER.warn("Received destination request for unknown sign at {}", this.sourceSignPos);
                return;
            }

            var sourceNode = sourceNodeOpt.get();
            List<net.rasanovum.viaromana.path.Node> destinations = graph.getCachedTeleportDestinationsFor(serverPlayer.getUUID(), sourceNode);
            PathGraph.NetworkCache cache = graph.getNetworkCache(sourceNode);

            List<DestinationResponseS2C.DestinationInfo> destInfos = graph.getNodesAsDestinationInfo(destinations, sourceNode.getBlockPos());
            List<DestinationResponseS2C.NodeNetworkInfo> networkInfos = graph.getNodesAsInfo(cache);

            DestinationResponseS2C response = new DestinationResponseS2C(
                destInfos,
                this.sourceSignPos,
                sourceNode.getBlockPos(),
                networkInfos,
                cache.id()
            );

            if (!CommonConfig.direct_warp || destInfos.size() > 1) {
                PacketBroadcaster.S2C.sendToPlayer(response, serverPlayer);
            }
            else if (destInfos.size() == 1) {
                TeleportRequestC2S packet = new TeleportRequestC2S(this.sourceSignPos, destInfos.get(0).position);

                ServerTeleportHandler.handleTeleportRequest(packet, (ServerPlayer) player);
            }
        }
    }
}
