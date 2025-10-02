package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.util.VersionUtils;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
 * Response from the server containing the list of available destinations, sign position, source node position, network nodes, and network ID.
 */
//? if <1.21 {
/*public record DestinationResponseS2C(
    List<DestinationInfo> destinations,
    BlockPos signPos,
    BlockPos sourceNodePos,
    List<NodeNetworkInfo> networkNodes,
    UUID networkId
) {
*///?} else {
public record DestinationResponseS2C(
    List<DestinationInfo> destinations,
    BlockPos signPos,
    BlockPos sourceNodePos,
    List<NodeNetworkInfo> networkNodes,
    UUID networkId
) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("via_romana:destination_response");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<DestinationResponseS2C> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("via_romana:destination_response"));

    public static final StreamCodec<FriendlyByteBuf, DestinationResponseS2C> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DestinationResponseS2C decode(FriendlyByteBuf buffer) {
            BlockPos signPos = buffer.readBlockPos();
            BlockPos sourceNodePos = buffer.readBlockPos();
            UUID networkId = buffer.readUUID();

            int size = buffer.readInt();
            List<DestinationInfo> destinations = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                destinations.add(new DestinationInfo(
                    buffer.readBlockPos(),
                    buffer.readUtf(),
                    buffer.readDouble(),
                    Node.Icon.valueOf(buffer.readUtf())
                ));
            }

            int networkSize = buffer.readInt();
            List<NodeNetworkInfo> networkNodes = new ArrayList<>(networkSize);
            for (int i = 0; i < networkSize; i++) {
                BlockPos pos = buffer.readBlockPos();
                float clearance = buffer.readFloat();

                int connectionCount = buffer.readInt();
                List<BlockPos> connections = new ArrayList<>(connectionCount);
                for (int j = 0; j < connectionCount; j++) {
                    connections.add(buffer.readBlockPos());
                }

                networkNodes.add(new NodeNetworkInfo(pos, clearance, connections));
            }

            return new DestinationResponseS2C(destinations, signPos, sourceNodePos, networkNodes, networkId);
        }

        @Override
        public void encode(FriendlyByteBuf buffer, DestinationResponseS2C packet) {
            buffer.writeBlockPos(packet.signPos);
            buffer.writeBlockPos(packet.sourceNodePos);
            buffer.writeUUID(packet.networkId);

            buffer.writeInt(packet.destinations.size());
            for (DestinationInfo dest : packet.destinations) {
                buffer.writeBlockPos(dest.position);
                buffer.writeUtf(dest.name);
                buffer.writeDouble(dest.distance);
                buffer.writeUtf(dest.icon.name());
            }

            buffer.writeInt(packet.networkNodes.size());
            for (NodeNetworkInfo node : packet.networkNodes) {
                buffer.writeBlockPos(node.position);
                buffer.writeFloat(node.clearance);
                buffer.writeInt(node.connections.size());
                for (BlockPos connection : node.connections) {
                    buffer.writeBlockPos(connection);
                }
            }
        }
    };
    //?}

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buf, DestinationResponseS2C packet) {
        buf.writeBlockPos(packet.signPos);
        buf.writeBlockPos(packet.sourceNodePos);
        buf.writeUUID(packet.networkId);

        buf.writeInt(packet.destinations.size());
        for (DestinationInfo dest : packet.destinations) {
            buf.writeBlockPos(dest.position);
            buf.writeUtf(dest.name);
            buf.writeDouble(dest.distance);
            buf.writeUtf(dest.icon.name());
        }

        buf.writeInt(packet.networkNodes.size());
        for (NodeNetworkInfo node : packet.networkNodes) {
            buf.writeBlockPos(node.position);
            buf.writeFloat(node.clearance);
            buf.writeInt(node.connections.size());
            for (BlockPos connection : node.connections) {
                buf.writeBlockPos(connection);
            }
        }
    }

    public static DestinationResponseS2C decode(FriendlyByteBuf buf) {
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

        return new DestinationResponseS2C(destinations, signPos, sourceNodePos, networkNodes, networkId);
    }

    public static void handle(PacketContext<DestinationResponseS2C> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            mc.execute(() -> {
                net.rasanovum.viaromana.client.gui.TeleportMapScreen screen = new net.rasanovum.viaromana.client.gui.TeleportMapScreen(ctx.message());
                mc.setScreen(screen);
            });
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
