package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.core.LinkHandler.LinkData;
import net.rasanovum.viaromana.path.Node;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;

import java.util.UUID;

/*
 * Request the server to link a sign to a node with the provided link data.
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

    public static void handle(PacketContext<LinkSignRequestC2S> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            net.minecraft.server.level.ServerLevel level = ctx.sender().serverLevel();
            LinkData linkData = ctx.message().linkData();

            boolean success = net.rasanovum.viaromana.core.LinkHandler.linkSignToNode(level, linkData);

            if (!success) {
                net.rasanovum.viaromana.ViaRomana.LOGGER.warn("Failed to link sign at {} to node at {} for player {}",
                    linkData.signPos(), linkData.nodePos(), ctx.sender().getName().getString());
            } else {
                net.rasanovum.viaromana.ViaRomana.LOGGER.debug("Successfully linked sign at {} to node at {} for player {}",
                    linkData.signPos(), linkData.nodePos(), ctx.sender().getName().getString());
            }
        }
    }
}
