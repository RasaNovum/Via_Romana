package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import commonnetwork.api.Dispatcher;

/*
 * Request the server to provide a list of available destinations for the network containing the sign at the given position.
 */
public record DestinationRequestC2S(BlockPos sourceSignPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DestinationRequestC2S> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("viaromana:destination_request_c2s"));

    public static final StreamCodec<FriendlyByteBuf, DestinationRequestC2S> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, DestinationRequestC2S::sourceSignPos,
        DestinationRequestC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketContext<DestinationRequestC2S> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            net.minecraft.server.level.ServerLevel level = ctx.sender().serverLevel();
            net.rasanovum.viaromana.storage.IPathStorage storage = net.rasanovum.viaromana.storage.IPathStorage.get(level);
            net.rasanovum.viaromana.path.PathGraph graph = storage.graph();

            BlockPos signPos = ctx.message().sourceSignPos();
            java.util.Optional<net.rasanovum.viaromana.path.Node> sourceNodeOpt = graph.getNodeBySignPos(signPos);

            if (sourceNodeOpt.isEmpty()) {
                net.rasanovum.viaromana.ViaRomana.LOGGER.warn("Received destination request for unknown sign at {}", signPos);
                return;
            }

            net.rasanovum.viaromana.path.Node sourceNode = sourceNodeOpt.get();
            java.util.List<net.rasanovum.viaromana.path.Node> destinations = graph.getCachedTeleportDestinationsFor(ctx.sender().getUUID(), sourceNode);
            net.rasanovum.viaromana.path.PathGraph.NetworkCache cache = graph.getNetworkCache(sourceNode);

            // Create destination info
            java.util.List<DestinationResponseS2C.DestinationInfo> destInfos = destinations.stream()
                .map(dest -> {
                    double distance = Math.sqrt(sourceNode.getBlockPos().distSqr(dest.getBlockPos()));
                    return new DestinationResponseS2C.DestinationInfo(
                        dest.getBlockPos(),
                        dest.getDestinationName().orElse("Unknown"),
                        distance,
                        dest.getDestinationIcon().orElse(net.rasanovum.viaromana.path.Node.Icon.SIGNPOST)
                    );
                })
                .toList();

            // Create network node info
            java.util.List<DestinationResponseS2C.NodeNetworkInfo> networkInfos = cache.getNodesAsInfo();

            DestinationResponseS2C response = new DestinationResponseS2C(
                destInfos,
                signPos,
                sourceNode.getBlockPos(),
                networkInfos,
                cache.id()
            );

            Dispatcher.sendToClient(response, ctx.sender());
        }
    }
}
