package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.util.VersionUtils;

/**
 * Network packet for synchronizing PathGraph data from server to client.
 */
//? if <1.21 {
/*public record PathGraphSyncPacket(CompoundTag pathGraphData) {
*///?} else {
public record PathGraphSyncPacket(CompoundTag pathGraphData) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("via_romana:path_graph_sync_s2c");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<PathGraphSyncPacket> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("via_romana:path_graph_sync_s2c"));

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
    //?}

    public PathGraphSyncPacket(PathGraph graph) {
        this(graph.serialize(new CompoundTag()));
    }

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buf, PathGraphSyncPacket packet) {
        buf.writeNbt(packet.pathGraphData);
    }

    public static PathGraphSyncPacket decode(FriendlyByteBuf buf) {
        return new PathGraphSyncPacket(buf.readNbt());
    }

    public static void handle(commonnetwork.networking.data.PacketContext<PathGraphSyncPacket> ctx) {
        if (commonnetwork.networking.data.Side.CLIENT.equals(ctx.side())) {
            try {
                PathGraph clientGraph = new PathGraph();
                clientGraph.deserialize(ctx.message().pathGraphData());

                ClientPathData.getInstance().updatePathData(clientGraph);

                ViaRomana.LOGGER.debug("Client received PathGraph sync with {} nodes", clientGraph.size());
            } catch (Exception e) {
                ViaRomana.LOGGER.error("Failed to process PathGraph sync packet on client", e);
            }
        }
    }
}
