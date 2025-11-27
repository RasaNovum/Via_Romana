package net.rasanovum.viaromana.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.client.gui.TeleportMapScreen;
import net.rasanovum.viaromana.network.AbstractPacket;
import net.rasanovum.viaromana.path.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Response from the server containing the list of available destinations, sign position, source node position, network nodes, and network ID.
 */
public record DestinationResponseS2C(
    List<DestinationInfo> destinations,
    BlockPos signPos,
    BlockPos sourceNodePos,
    List<NodeNetworkInfo> networkNodes,
    UUID networkId
) implements AbstractPacket {

    public DestinationResponseS2C(FriendlyByteBuf buf) {
        this(readFromBuffer(buf));
    }

    private DestinationResponseS2C(Data data) {
        this(data.destinations, data.signPos, data.sourceNodePos, data.networkNodes, data.networkId);
    }

    private static Data readFromBuffer(FriendlyByteBuf buf) {
        BlockPos signPos = buf.readBlockPos();
        BlockPos sourceNodePos = buf.readBlockPos();
        UUID networkId = buf.readUUID();
        
        int size = buf.readInt();
        List<DestinationInfo> destinations = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            destinations.add(new DestinationInfo(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readDouble(),
                Node.Icon.valueOf(buf.readUtf())
            ));
        }

        int networkSize = buf.readInt();
        List<NodeNetworkInfo> networkNodes = new ArrayList<>(networkSize);
        for (int i = 0; i < networkSize; i++) {
            BlockPos pos = buf.readBlockPos();
            float clearance = buf.readFloat();

            int connectionCount = buf.readInt();
            List<BlockPos> connections = new ArrayList<>(connectionCount);
            for (int j = 0; j < connectionCount; j++) {
                connections.add(buf.readBlockPos());
            }

            networkNodes.add(new NodeNetworkInfo(pos, clearance, connections));
        }
        
        return new Data(destinations, signPos, sourceNodePos, networkNodes, networkId);
    }

    private record Data(List<DestinationInfo> destinations, BlockPos signPos, BlockPos sourceNodePos, List<NodeNetworkInfo> networkNodes, UUID networkId) {}

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.signPos);
        buf.writeBlockPos(this.sourceNodePos);
        buf.writeUUID(this.networkId);

        buf.writeInt(this.destinations.size());
        for (DestinationInfo dest : this.destinations) {
            buf.writeBlockPos(dest.position);
            buf.writeUtf(dest.name);
            buf.writeDouble(dest.distance);
            buf.writeUtf(dest.icon.name());
        }

        buf.writeInt(this.networkNodes.size());
        for (NodeNetworkInfo node : this.networkNodes) {
            buf.writeBlockPos(node.position);
            buf.writeFloat(node.clearance);
            buf.writeInt(node.connections.size());
            for (BlockPos connection : node.connections) {
                buf.writeBlockPos(connection);
            }
        }
    }

    public void handle(Level level, Player player) {
        if (level != null && level.isClientSide) {
            DestinationResponseS2C.ClientHandler.handleClient(this);
        }
    }

    private static class ClientHandler {
        private static void handleClient(DestinationResponseS2C response) {
            Minecraft.getInstance().setScreen(new TeleportMapScreen(response));
        }
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
        public final float clearance;
        public final List<BlockPos> connections;

        public NodeNetworkInfo(BlockPos position, float clearance, List<BlockPos> connections) {
            this.position = position;
            this.clearance = clearance;
            this.connections = connections;
        }
    }
}
