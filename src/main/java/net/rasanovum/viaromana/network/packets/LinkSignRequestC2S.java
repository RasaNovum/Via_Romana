package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.core.LinkHandler.LinkData;
import net.rasanovum.viaromana.path.Node;

import java.util.UUID;

/**
 * Packet sent from client to server to request linking a sign to a node.
 */
public record LinkSignRequestC2S(LinkData linkData, boolean isTempNode) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LinkSignRequestC2S> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("via_romana:link_sign_request"));

    public static final StreamCodec<FriendlyByteBuf, LinkSignRequestC2S> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public LinkSignRequestC2S decode(FriendlyByteBuf buffer) {
            BlockPos nodePos = buffer.readBlockPos();
            BlockPos signPos = buffer.readBlockPos();
            Node.LinkType linkType = buffer.readEnum(Node.LinkType.class);
            UUID owner = buffer.readBoolean() ? buffer.readUUID() : null;
            String destinationName = buffer.readUtf();
            Node.Icon icon = buffer.readEnum(Node.Icon.class);
            LinkData linkData = new LinkData(signPos, nodePos, linkType, icon, destinationName, owner);
            boolean isTempNode = buffer.readBoolean();
            return new LinkSignRequestC2S(linkData, isTempNode);
        }

        @Override
        public void encode(FriendlyByteBuf buffer, LinkSignRequestC2S packet) {
            buffer.writeBlockPos(packet.linkData.nodePos());
            buffer.writeBlockPos(packet.linkData.signPos());
            buffer.writeEnum(packet.linkData.linkType());
            buffer.writeBoolean(packet.linkData.owner() != null);
            if (packet.linkData.owner() != null) {
                buffer.writeUUID(packet.linkData.owner());
            }
            buffer.writeUtf(packet.linkData.destinationName());
            buffer.writeEnum(packet.linkData.icon());
            buffer.writeBoolean(packet.isTempNode);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
