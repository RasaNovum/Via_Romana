package net.rasanovum.viaromana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.rasanovum.viaromana.path.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DestinationResponsePacket {
    private final List<DestinationInfo> destinations;
    private final BlockPos signPos;
    private final BlockPos sourceNodePos;
    private final List<NodeNetworkInfo> networkNodes;
    private final UUID networkId;
    
    public DestinationResponsePacket(List<DestinationInfo> destinations, BlockPos signPos, BlockPos sourceNodePos, List<NodeNetworkInfo> networkNodes, UUID networkId) {
        this.destinations = destinations;
        this.signPos = signPos;
        this.sourceNodePos = sourceNodePos;
        this.networkNodes = networkNodes;
        this.networkId = networkId;
    }
    
    public DestinationResponsePacket(FriendlyByteBuf buffer) {
        this.signPos = buffer.readBlockPos();
        this.sourceNodePos = buffer.readBlockPos();
        this.networkId = buffer.readUUID();
        
        int size = buffer.readInt();
        this.destinations = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            destinations.add(new DestinationInfo(
                buffer.readBlockPos(),
                buffer.readUtf(),
                buffer.readDouble(),
                Node.Icon.valueOf(buffer.readUtf())
            ));
        }
        
        int networkSize = buffer.readInt();
        this.networkNodes = new ArrayList<>(networkSize);
        for (int i = 0; i < networkSize; i++) {
            BlockPos pos = buffer.readBlockPos();
            
            int connectionCount = buffer.readInt();
            List<BlockPos> connections = new ArrayList<>(connectionCount);
            for (int j = 0; j < connectionCount; j++) {
                connections.add(buffer.readBlockPos());
            }
            
            networkNodes.add(new NodeNetworkInfo(pos, connections));
        }
    }
    
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(signPos);
        buffer.writeBlockPos(sourceNodePos);
        buffer.writeUUID(networkId);

        buffer.writeInt(destinations.size());
        for (DestinationInfo dest : destinations) {
            buffer.writeBlockPos(dest.position);
            buffer.writeUtf(dest.name);
            buffer.writeDouble(dest.distance);
            buffer.writeUtf(dest.icon.name());
        }
        
        buffer.writeInt(networkNodes.size());
        for (NodeNetworkInfo node : networkNodes) {
            buffer.writeBlockPos(node.position);
            
            buffer.writeInt(node.connections.size());
            for (BlockPos connection : node.connections) {
                buffer.writeBlockPos(connection);
            }
        }
    }
    
    public List<DestinationInfo> getDestinations() {
        return destinations;
    }
    
    public List<NodeNetworkInfo> getNetworkNodes() {
        return networkNodes;
    }
    
    public BlockPos getSignPos() {
        return signPos;
    }

    public BlockPos getSourceNodePos() {
        return sourceNodePos;
    }
    
    public UUID getNetworkId() {
        return networkId;
    }
    
    public static class DestinationInfo {
        public final BlockPos position;
        public final String name;
        public final double distance;
        public final Node.Icon icon;
        
        public DestinationInfo(BlockPos position, String name, double distance, Node.Icon icon) {
            this.position = position;
            this.name = name;
            this.distance = distance;
            this.icon = icon;
        }
    }
    
    public static class NodeNetworkInfo {
        public final BlockPos position;
        public final List<BlockPos> connections;
        
        public NodeNetworkInfo(BlockPos position, List<BlockPos> connections) {
            this.position = position;
            this.connections = connections;
        }
    }
}
