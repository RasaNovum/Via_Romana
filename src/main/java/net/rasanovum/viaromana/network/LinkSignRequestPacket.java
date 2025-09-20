package net.rasanovum.viaromana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import net.rasanovum.viaromana.core.LinkHandler.LinkData;
import net.rasanovum.viaromana.path.Node;

import java.util.UUID;

/**
 * Packet sent from client to server to request linking a sign to a node.
 */
public class LinkSignRequestPacket {
    private final LinkData linkData;
    private final boolean isTempNode;

    public LinkSignRequestPacket(LinkData linkData, Boolean isTempNode) {
        this.linkData = linkData;
        this.isTempNode = isTempNode != null ? isTempNode : false;
    }

    public LinkSignRequestPacket(FriendlyByteBuf buffer) {
        BlockPos nodePos = buffer.readBlockPos();
        BlockPos signPos = buffer.readBlockPos();
        Node.LinkType linkType = buffer.readEnum(Node.LinkType.class);
        UUID owner = buffer.readBoolean() ? buffer.readUUID() : null;
        String destinationName = buffer.readUtf();
        Node.Icon icon = buffer.readEnum(Node.Icon.class);
        this.linkData = new LinkData(signPos, nodePos, linkType, icon, destinationName, owner);
        this.isTempNode = buffer.readBoolean();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(linkData.nodePos());
        buffer.writeBlockPos(linkData.signPos());
        buffer.writeEnum(linkData.linkType());
        buffer.writeBoolean(linkData.owner() != null);
        if (linkData.owner() != null) {
            buffer.writeUUID(linkData.owner());
        }
        buffer.writeUtf(linkData.destinationName());
        buffer.writeEnum(linkData.icon());
        buffer.writeBoolean(isTempNode);
    }

    public LinkData getLinkData() {
        return linkData;
    }

    public boolean isTempNode() {
        return isTempNode;
    }
}
