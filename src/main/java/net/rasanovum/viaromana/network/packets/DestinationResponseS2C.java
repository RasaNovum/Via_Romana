package net.rasanovum.viaromana.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.client.gui.TeleportMapScreen;
import net.rasanovum.viaromana.network.AbstractPacket;
import net.rasanovum.viaromana.path.Node;

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
        this(
                buf.readList(DestinationInfo::new),
                buf.readNullable(b -> b.readBlockPos()),
                buf.readNullable(b -> b.readBlockPos()),
                buf.readList(NodeNetworkInfo::new),
                buf.readUUID()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(this.destinations, (b, dest) -> dest.write(b));
        buf.writeNullable(this.signPos, (b, pos) -> b.writeBlockPos(pos));
        buf.writeNullable(this.sourceNodePos, (b, pos) -> b.writeBlockPos(pos));
        buf.writeCollection(this.networkNodes, (b, node) -> node.write(b));
        buf.writeUUID(this.networkId);
    }

    @Override
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

        public DestinationInfo(FriendlyByteBuf buf) {
            this(
                    buf.readBlockPos(),
                    buf.readUtf(),
                    buf.readDouble(),
                    Node.Icon.valueOf(buf.readUtf())
            );
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(position);
            buf.writeUtf(name);
            buf.writeDouble(distance);
            buf.writeUtf(icon.name());
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

        public NodeNetworkInfo(FriendlyByteBuf buf) {
            this.position = buf.readBlockPos();
            this.clearance = buf.readFloat();
            this.connections = buf.readList(b -> b.readBlockPos());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(position);
            buf.writeFloat(clearance);
            buf.writeCollection(connections, (b, pos) -> b.writeBlockPos(pos));
        }
    }
}