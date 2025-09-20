package net.rasanovum.viaromana.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import net.rasanovum.viaromana.client.gui.LinkSignScreen;
import net.rasanovum.viaromana.core.LinkHandler.LinkData;
import net.rasanovum.viaromana.path.Node;

import java.util.UUID;

/**
 * Packet sent from server to client to request opening the LinkSignScreen
 */
public class OpenLinkScreenPacket {
    private final LinkData linkData;
    private final UUID playerUuid;
    private final boolean isTempNode;
    private final boolean isSignLinked;

    public OpenLinkScreenPacket(LinkData linkData, UUID playerUuid, boolean isTempNode, boolean isSignLinked) {
        this.linkData = linkData;
        this.playerUuid = playerUuid;
        this.isTempNode = isTempNode;
        this.isSignLinked = isSignLinked;
    }
    
    public OpenLinkScreenPacket(FriendlyByteBuf buffer) {
        BlockPos signPos = buffer.readBlockPos();
        BlockPos nodePos = buffer.readBlockPos();
        Node.LinkType linkType = Node.LinkType.values()[buffer.readInt()];
        Node.Icon icon = Node.Icon.values()[buffer.readInt()];
        String destinationName = buffer.readUtf(32767);
        UUID owner = buffer.readUUID();
        this.linkData = new LinkData(signPos, nodePos, linkType, icon, destinationName, owner);
        this.playerUuid = buffer.readUUID();
        this.isTempNode = buffer.readBoolean();
        this.isSignLinked = buffer.readBoolean();
    }
    
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(linkData.signPos());
        buffer.writeBlockPos(linkData.nodePos());
        buffer.writeInt(linkData.linkType().ordinal());
        buffer.writeInt(linkData.icon().ordinal());
        buffer.writeUtf(linkData.destinationName());
        buffer.writeUUID(linkData.owner());
        buffer.writeUUID(playerUuid);
        buffer.writeBoolean(isTempNode);
        buffer.writeBoolean(isSignLinked);
    }
    
    public LinkData getLinkData() {
        return linkData;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public Boolean isTempNode() {
        return isTempNode;
    }

    public Boolean isSignLinked() {
        return isSignLinked;
    }
    
    public static void handleClient(OpenLinkScreenPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            Player player = minecraft.player;
            if (player != null) {
                LinkSignScreen screen = new LinkSignScreen(player, packet.getLinkData(), packet.isTempNode(), packet.isSignLinked());
                minecraft.setScreen(screen);
            }
        });
    }
}
