package net.rasanovum.viaromana.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.ViaRomana;

/**
 * Network packet for synchronizing PathGraph data from server to client.
 * This allows clients to render node beams for all paths while charting.
 */
public class PathGraphSyncPacket {
    private final CompoundTag pathGraphData;
    
    public PathGraphSyncPacket(PathGraph graph) {
        this.pathGraphData = graph.serialize(new CompoundTag());
    }
    
    public PathGraphSyncPacket(FriendlyByteBuf buffer) {
        this.pathGraphData = buffer.readNbt();
    }
    
    public void write(FriendlyByteBuf buffer) {
        buffer.writeNbt(pathGraphData);
    }
    
    public static void handleClient(PathGraphSyncPacket packet) {
        try {
            PathGraph clientGraph = new PathGraph();
            clientGraph.deserialize(packet.pathGraphData);
            
            ClientPathData.getInstance().updatePathData(clientGraph);
            
            ViaRomana.LOGGER.debug("Client received PathGraph sync with {} nodes", clientGraph.size());
                
        } catch (Exception e) {
            ViaRomana.LOGGER.error("Failed to process PathGraph sync packet on client", e);
        }
    }
    
    public CompoundTag getPathGraphData() {
        return pathGraphData;
    }
}
