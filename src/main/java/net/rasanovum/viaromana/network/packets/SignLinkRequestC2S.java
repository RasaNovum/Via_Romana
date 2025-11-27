package net.rasanovum.viaromana.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.core.LinkHandler.LinkData;
import net.rasanovum.viaromana.network.AbstractPacket;
import net.rasanovum.viaromana.path.Node;

import java.util.UUID;

/**
 * Request the server to link a sign to a node with the provided link data.
 */
public record SignLinkRequestC2S(LinkData linkData, boolean isTempNode) implements AbstractPacket {

    public SignLinkRequestC2S(FriendlyByteBuf buf) {
        this(readLinkData(buf), buf.readBoolean());
    }

    private static LinkData readLinkData(FriendlyByteBuf buf) {
        BlockPos nodePos = buf.readBlockPos();
        BlockPos signPos = buf.readBlockPos();
        Node.LinkType linkType = buf.readEnum(Node.LinkType.class);
        UUID owner = buf.readBoolean() ? buf.readUUID() : null;
        String destinationName = buf.readUtf();
        Node.Icon icon = buf.readEnum(Node.Icon.class);
        return new LinkData(signPos, nodePos, linkType, icon, destinationName, owner);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.linkData.nodePos());
        buf.writeBlockPos(this.linkData.signPos());
        buf.writeEnum(this.linkData.linkType());
        buf.writeBoolean(this.linkData.owner() != null);
        if (this.linkData.owner() != null) {
            buf.writeUUID(this.linkData.owner());
        }
        buf.writeUtf(this.linkData.destinationName());
        buf.writeEnum(this.linkData.icon());
        buf.writeBoolean(this.isTempNode);
    }

    public void handle(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            boolean success = net.rasanovum.viaromana.core.LinkHandler.linkSignToNode(serverLevel, this.linkData);

            if (!success) {
                ViaRomana.LOGGER.warn("Failed to link sign at {} to node at {} for player {}",
                    this.linkData.signPos(), this.linkData.nodePos(), serverPlayer.getName().getString());
            } else {
                if (CommonConfig.logging_enum.ordinal() > 0) ViaRomana.LOGGER.info("Successfully linked sign at {} to node at {} for player {}",
                    this.linkData.signPos(), this.linkData.nodePos(), serverPlayer.getName().getString());
            }
        }
    }
}
