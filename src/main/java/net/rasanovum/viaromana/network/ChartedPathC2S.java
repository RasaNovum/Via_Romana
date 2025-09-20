package net.rasanovum.viaromana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.rasanovum.viaromana.path.Node.NodeData;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from client to server when a player finishes charting a path.
 * Contains all the temporary nodes that should be converted to permanent nodes
 * and connected as a path on the server-side PathGraph.
 */
public class ChartedPathC2S {
    private final List<NodeData> chartedNodes;

    public ChartedPathC2S(List<NodeData> chartedNodes) {
        this.chartedNodes = chartedNodes != null ? new ArrayList<>(chartedNodes) : new ArrayList<>();
    }

    public ChartedPathC2S(FriendlyByteBuf buffer) {
        int nodeCount = buffer.readInt();
        this.chartedNodes = new ArrayList<>(nodeCount);
        
        for (int i = 0; i < nodeCount; i++) {
            BlockPos pos = buffer.readBlockPos();
            float quality = buffer.readFloat();
            this.chartedNodes.add(new NodeData(pos, quality));
        }
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeInt(chartedNodes.size());
        
        for (NodeData nodeData : chartedNodes) {
            buffer.writeBlockPos(nodeData.pos());
            buffer.writeFloat(nodeData.quality());
        }
    }

    public List<NodeData> getChartedNodes() {
        return chartedNodes;
    }
}
