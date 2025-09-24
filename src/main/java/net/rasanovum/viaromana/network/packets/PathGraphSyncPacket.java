package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.ViaRomana;

/**
 * Network packet for synchronizing PathGraph data from server to client.
 */
public record PathGraphSyncPacket(CompoundTag pathGraphData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PathGraphSyncPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("via_romana:path_graph_sync_s2c"));

    public static final StreamCodec<FriendlyByteBuf, PathGraphSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PathGraphSyncPacket decode(FriendlyByteBuf buffer) {
            return new PathGraphSyncPacket(buffer.readNbt());
        }

        @Override
        public void encode(FriendlyByteBuf buffer, PathGraphSyncPacket packet) {
            buffer.writeNbt(packet.pathGraphData);
        }
    };

    public PathGraphSyncPacket(PathGraph graph) {
        this(graph.serialize(new CompoundTag()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(PathGraphSyncPacket packet) {
        try {
            PathGraph clientGraph = new PathGraph();
            clientGraph.deserialize(packet.pathGraphData());

            ClientPathData.getInstance().updatePathData(clientGraph);

            ViaRomana.LOGGER.debug("Client received PathGraph sync with {} nodes", clientGraph.size());

        } catch (Exception e) {
            ViaRomana.LOGGER.error("Failed to process PathGraph sync packet on client", e);
        }
    }
}
