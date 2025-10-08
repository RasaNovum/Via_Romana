package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.util.VersionUtils;

/**
 * Network packet for synchronizing PathGraph data from server to client.
 * Includes dimension information to support per-dimension path graphs.
 */
//? if <1.21 {
/*public record PathGraphSyncPacket(CompoundTag pathGraphData, ResourceKey<Level> dimension) {
*///?} else {
public record PathGraphSyncPacket(CompoundTag pathGraphData, ResourceKey<Level> dimension) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("via_romana:path_graph_sync_s2c");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<PathGraphSyncPacket> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("via_romana:path_graph_sync_s2c"));

    public static final StreamCodec<FriendlyByteBuf, PathGraphSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PathGraphSyncPacket decode(FriendlyByteBuf buffer) { return PathGraphSyncPacket.decode(buffer); }

        @Override
        public void encode(FriendlyByteBuf buffer, PathGraphSyncPacket packet) { PathGraphSyncPacket.encode(buffer, packet); }
    };
    //?}

    public PathGraphSyncPacket(PathGraph graph, ResourceKey<Level> dimension) {
        this(graph.serialize(new CompoundTag()), dimension);
    }

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buf, PathGraphSyncPacket packet) {
        buf.writeNbt(packet.pathGraphData);
        buf.writeUtf(packet.dimension.location().toString());
    }

    public static PathGraphSyncPacket decode(FriendlyByteBuf buf) {
        CompoundTag data = buf.readNbt();
        String dimensionStr = buf.readUtf();
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimensionStr));
        return new PathGraphSyncPacket(data, dimension);
    }

    public static void handle(commonnetwork.networking.data.PacketContext<PathGraphSyncPacket> ctx) {
        if (commonnetwork.networking.data.Side.CLIENT.equals(ctx.side())) {
            try {
                PathGraph clientGraph = new PathGraph();
                clientGraph.deserialize(ctx.message().pathGraphData());
                ResourceKey<Level> dimension = ctx.message().dimension();

                ClientPathData.getInstance().updatePathData(clientGraph, dimension);

                ViaRomana.LOGGER.debug("Client received PathGraph sync for dimension {} with {} nodes", 
                    dimension.location(), clientGraph.size());
            } catch (Exception e) {
                ViaRomana.LOGGER.error("Failed to process PathGraph sync packet on client", e);
            }
        }
    }
}
