package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.path.Node.NodeData;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from client to server when a player finishes charting a path.
 * Contains all the temporary nodes/links that should be made permanent
 * and connected as a path on the server-side PathGraph.
 */
public record ChartedPathC2S(List<NodeData> chartedNodes) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChartedPathC2S> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("via_romana:charted_path_c2s"));

    public static final StreamCodec<FriendlyByteBuf, ChartedPathC2S> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ChartedPathC2S decode(FriendlyByteBuf buffer) {
            int nodeCount = buffer.readInt();
            List<NodeData> chartedNodes = new ArrayList<>(nodeCount);

            for (int i = 0; i < nodeCount; i++) {
                BlockPos pos = buffer.readBlockPos();
                float quality = buffer.readFloat();
                float clearance = buffer.readFloat();
                chartedNodes.add(new NodeData(pos, quality, clearance));
            }

            return new ChartedPathC2S(chartedNodes);
        }

        @Override
        public void encode(FriendlyByteBuf buffer, ChartedPathC2S packet) {
            buffer.writeInt(packet.chartedNodes.size());

            for (NodeData nodeData : packet.chartedNodes) {
                buffer.writeBlockPos(nodeData.pos());
                buffer.writeFloat(nodeData.quality());
                buffer.writeFloat(nodeData.clearance());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public ChartedPathC2S(List<NodeData> chartedNodes) {
        this.chartedNodes = chartedNodes != null ? List.copyOf(chartedNodes) : List.of();
    }

    public List<NodeData> getChartedNodes() {
        return chartedNodes;
    }

    public static void handle(commonnetwork.networking.data.PacketContext<ChartedPathC2S> ctx) {
        if (commonnetwork.networking.data.Side.SERVER.equals(ctx.side())) {
            net.minecraft.server.level.ServerLevel level = ctx.sender().serverLevel();
            net.rasanovum.viaromana.storage.IPathStorage storage = net.rasanovum.viaromana.storage.IPathStorage.get(level);

            java.util.List<NodeData> chartingNodes = ctx.message().getChartedNodes();

            if (chartingNodes.isEmpty()) {
                net.rasanovum.viaromana.ViaRomana.LOGGER.warn("Received empty charted path from player {}", ctx.sender().getName().getString());
                return;
            }

            try {
                storage.graph().createConnectedPath(chartingNodes);
                storage.setDirty();

                net.rasanovum.viaromana.util.PathSyncUtils.syncPathGraphToAllPlayers(level);

                // awardAdvancementIfNeeded(ctx.sender(), "via_romana:story/a_strand_type_game");

                net.rasanovum.viaromana.ViaRomana.LOGGER.debug("Created charted path with {} nodes for player {}", chartingNodes.size(), ctx.sender().getName().getString());
            } catch (Exception e) {
                net.rasanovum.viaromana.ViaRomana.LOGGER.error("Failed to create charted path for player {}: {}", ctx.sender().getName().getString(), e.getMessage());
            }
        }
    }
}
