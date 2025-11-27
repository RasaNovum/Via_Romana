package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.AbstractPacket;
import net.rasanovum.viaromana.util.VersionUtils;

/**
 * Network packet for synchronizing PathGraph data from server to client.
 * Includes dimension information to support per-dimension path graphs.
 */
public record PathGraphSyncPacket(CompoundTag pathGraphData, ResourceKey<Level> dimension) implements AbstractPacket {

    public PathGraphSyncPacket(PathGraph graph, ResourceKey<Level> dimension) {
        this(graph.serialize(new CompoundTag()), dimension);
    }

    public PathGraphSyncPacket(FriendlyByteBuf buf) {
        this(
            buf.readNbt(),
            ResourceKey.create(Registries.DIMENSION, VersionUtils.getLocation(buf.readUtf()))
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(this.pathGraphData);
        buf.writeUtf(this.dimension.location().toString());
    }

    public void handle(Level level, Player player) {
        if (level.isClientSide) {
            try {
                PathGraph clientGraph = new PathGraph();
                clientGraph.deserialize(this.pathGraphData);

                ClientPathData.getInstance().updatePathData(clientGraph, this.dimension);

                ViaRomana.LOGGER.debug("Client received PathGraph sync for dimension {} with {} nodes", 
                    this.dimension.location(), clientGraph.size());
            } catch (Exception e) {
                ViaRomana.LOGGER.error("Failed to process PathGraph sync packet on client", e);
            }
        }
    }
}
