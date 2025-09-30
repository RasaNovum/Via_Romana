package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.CommonConfig;

/**
 * C2S action packet for path operations.
 */
public record RoutedActionC2S(Operation op) implements CustomPacketPayload {
    public enum Operation { SEVER_NEAREST_NODE, REMOVE_BRANCH }

    public static final CustomPacketPayload.Type<RoutedActionC2S> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("viaromana:action_request_c2s"));

    public static final StreamCodec<FriendlyByteBuf, RoutedActionC2S> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(i -> Operation.values()[i], Operation::ordinal), RoutedActionC2S::op,
        RoutedActionC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(commonnetwork.networking.data.PacketContext<RoutedActionC2S> ctx) {
        if (commonnetwork.networking.data.Side.SERVER.equals(ctx.side())) {
            ctx.sender().server.execute(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.sender();
                net.minecraft.server.level.ServerLevel level = player.serverLevel();
                var storage = net.rasanovum.viaromana.storage.IPathStorage.get(level);
                var graph = storage.graph();

                java.util.Optional<net.rasanovum.viaromana.path.Node> nearestOpt = graph.getNearestNode(player.blockPosition(), CommonConfig.node_utility_distance, node -> true);

                if (nearestOpt.isEmpty()) {
                    net.rasanovum.viaromana.ViaRomana.LOGGER.warn("No nearby node found for action {} by player {}", ctx.message().op(), player.getName().getString());
                    return;
                }

                net.rasanovum.viaromana.path.Node nearestNode = nearestOpt.get();

                switch (ctx.message().op()) {
                    case SEVER_NEAREST_NODE -> {
                        graph.removeNode(nearestNode);
                    }
                    case REMOVE_BRANCH -> {
                        graph.removeBranch(nearestNode);
                    }
                }

                storage.setDirty();
                net.rasanovum.viaromana.util.PathSyncUtils.syncPathGraphToAllPlayers(level);
            });
        }
    }
}
