package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.core.LinkHandler.LinkData;
import net.rasanovum.viaromana.path.Node;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.rasanovum.viaromana.util.VersionUtils;

import java.util.UUID;

/*
 * Request the server to link a sign to a node with the provided link data.
 */
//? if <1.21 {
/*public record SignLinkRequestC2S(LinkData linkData, boolean isTempNode) {
*///?} else {
public record SignLinkRequestC2S(LinkData linkData, boolean isTempNode) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("via_romana:link_sign_request");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<SignLinkRequestC2S> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("via_romana:link_sign_request"));

    public static final StreamCodec<FriendlyByteBuf, SignLinkRequestC2S> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SignLinkRequestC2S decode(FriendlyByteBuf buffer) {
            BlockPos nodePos = buffer.readBlockPos();
            BlockPos signPos = buffer.readBlockPos();
            Node.LinkType linkType = buffer.readEnum(Node.LinkType.class);
            UUID owner = buffer.readBoolean() ? buffer.readUUID() : null;
            String destinationName = buffer.readUtf();
            Node.Icon icon = buffer.readEnum(Node.Icon.class);
            LinkData linkData = new LinkData(signPos, nodePos, linkType, icon, destinationName, owner);
            boolean isTempNode = buffer.readBoolean();
            return new SignLinkRequestC2S(linkData, isTempNode);
        }

        @Override
        public void encode(FriendlyByteBuf buffer, SignLinkRequestC2S packet) {
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
    //?}

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buf, SignLinkRequestC2S packet) {
        buf.writeBlockPos(packet.linkData.nodePos());
        buf.writeBlockPos(packet.linkData.signPos());
        buf.writeEnum(packet.linkData.linkType());
        buf.writeBoolean(packet.linkData.owner() != null);
        if (packet.linkData.owner() != null) {
            buf.writeUUID(packet.linkData.owner());
        }
        buf.writeUtf(packet.linkData.destinationName());
        buf.writeEnum(packet.linkData.icon());
        buf.writeBoolean(packet.isTempNode);
    }

    public static SignLinkRequestC2S decode(FriendlyByteBuf buf) {
        BlockPos nodePos = buf.readBlockPos();
        BlockPos signPos = buf.readBlockPos();
        Node.LinkType linkType = buf.readEnum(Node.LinkType.class);
        UUID owner = buf.readBoolean() ? buf.readUUID() : null;
        String destinationName = buf.readUtf();
        Node.Icon icon = buf.readEnum(Node.Icon.class);
        LinkData linkData = new LinkData(signPos, nodePos, linkType, icon, destinationName, owner);
        boolean isTempNode = buf.readBoolean();
        return new SignLinkRequestC2S(linkData, isTempNode);
    }

    public static void handle(PacketContext<SignLinkRequestC2S> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            net.minecraft.server.level.ServerLevel level = ctx.sender().serverLevel();
            LinkData linkData = ctx.message().linkData();

            boolean success = net.rasanovum.viaromana.core.LinkHandler.linkSignToNode(level, linkData);

            if (!success) {
                net.rasanovum.viaromana.ViaRomana.LOGGER.warn("Failed to link sign at {} to node at {} for player {}",
                    linkData.signPos(), linkData.nodePos(), ctx.sender().getName().getString());
            } else {
                if (CommonConfig.logging_enum.ordinal() > 0) ViaRomana.LOGGER.info("Successfully linked sign at {} to node at {} for player {}",
                    linkData.signPos(), linkData.nodePos(), ctx.sender().getName().getString());
            }
        }
    }
}
