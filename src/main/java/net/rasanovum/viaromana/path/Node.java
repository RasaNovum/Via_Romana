package net.rasanovum.viaromana.path;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongPredicate;

public class Node {

    //region Enums & Records
    public record NodeData(BlockPos pos, float quality, float clearance) {}

    public enum LinkType {
        NONE,
        ACCESS,      // GUI access only, not a teleport target
        DESTINATION, // Public teleport destination
        PRIVATE      // Private teleport destination for one owner
    }

    public enum Icon {
        SIGNPOST, HOUSE, SHOP, TOWER, CAVE, CROP, PORTAL, BOOK
    }
    //endregion

    //region Core Data
    private final long pos;
    private final float quality;
    private final int clearance;
    private final LongSet connectedNodes = new LongOpenHashSet();
    private DestinationInfo destinationInfo = null; // Null if not a destination
    //endregion

    /**
     * A container for all data related to a node being a destination.
     */
    private static class DestinationInfo {
        LinkType linkType = LinkType.NONE;
        long signPos;
        long teleportPos; // TODO: Cancelled for now
        UUID privateOwner = null;
        String name = null;
        Icon icon = null;

        DestinationInfo(long nodePos) {
            this.signPos = nodePos;
            this.teleportPos = nodePos;
        }
    }

    //region Construction & Serialization
    public Node(long pos, float quality, float clearance) {
        this.pos = pos;
        this.quality = quality;
        if (!Float.isFinite(clearance)) clearance = 0.0f;
        int rounded = Math.round(clearance);
        if (rounded < 0) rounded = 0;
        else if (rounded > 255) rounded = 255;
        this.clearance = rounded;
    }

    public Node(CompoundTag tag) {
        this(
            tag.getLong("pos"),
            tag.contains("quality", Tag.TAG_FLOAT) ? tag.getFloat("quality") : 1.0f,
            tag.contains("clearance", Tag.TAG_FLOAT)
                    ? tag.getFloat("clearance")
                    : (tag.contains("clearance", Tag.TAG_BYTE)
                    ? Byte.toUnsignedInt(tag.getByte("clearance"))
                    : 0.0f)
        );

        long[] connections = tag.getLongArray("connections");
        for (long c : connections) {
            addConnection(c);
        }

        if (tag.contains("destination", Tag.TAG_COMPOUND)) {
            CompoundTag destTag = tag.getCompound("destination");
            DestinationInfo dest = getOrCreateDestinationInfo();
            dest.linkType = safeEnum(LinkType.class, destTag.getString("linkType"), LinkType.NONE);
            dest.signPos = destTag.getLong("signPos");
            dest.teleportPos = destTag.getLong("teleportPos");
            if (destTag.hasUUID("owner")) {
                dest.privateOwner = destTag.getUUID("owner");
            }
            if (destTag.contains("name", Tag.TAG_STRING)) {
                dest.name = destTag.getString("name");
            }
            if (destTag.contains("icon", Tag.TAG_STRING)) {
                dest.icon = safeEnum(Icon.class, destTag.getString("icon"), null);
            }
        }
    }

    public CompoundTag serialize(CompoundTag tag) {
        tag.putLong("pos", this.pos);
        tag.putByte("clearance", (byte) (this.clearance & 0xFF));
        tag.putLongArray("connections", this.connectedNodes.toLongArray());

        if (this.destinationInfo != null) {
            CompoundTag destTag = new CompoundTag();
            destTag.putString("linkType", destinationInfo.linkType.name());
            destTag.putLong("signPos", destinationInfo.signPos);
            destTag.putLong("teleportPos", destinationInfo.teleportPos);
            if (destinationInfo.privateOwner != null) {
                destTag.putUUID("owner", destinationInfo.privateOwner);
            }
            if (destinationInfo.name != null) {
                destTag.putString("name", destinationInfo.name);
            }
            if (destinationInfo.icon != null) {
                destTag.putString("icon", destinationInfo.icon.name());
            }
            tag.put("destination", destTag);
        }
        return tag;
    }
    //endregion

    //region Core Getters
    public long getPos() {
        return pos;
    }

    public BlockPos getBlockPos() {
        return BlockPos.of(pos);
    }

    public float getQuality() {
        return quality;
    }

    public float getClearance() {
        return clearance;
    }
    //endregion

    //region Destination & Link Getters/Setters
    @NotNull
    private DestinationInfo getOrCreateDestinationInfo() {
        if (this.destinationInfo == null) {
            this.destinationInfo = new DestinationInfo(this.pos);
        }
        return this.destinationInfo;
    }

    public boolean isLinked() {
        return this.destinationInfo != null;
    }

    /**
     * Removes all destination-related data from this node.
     */
    public void unlink() {
        this.destinationInfo = null;
    }
    
    public LinkType getLinkType() {
        return Optional.ofNullable(destinationInfo).map(d -> d.linkType).orElse(LinkType.NONE);
    }

    public void setLinkType(LinkType linkType) {
        if (linkType == LinkType.NONE) {
            unlink();
            return;
        }
        getOrCreateDestinationInfo().linkType = linkType;
        if (linkType != LinkType.PRIVATE) {
            getOrCreateDestinationInfo().privateOwner = null;
        }
    }

    public Optional<Long> getSignPos() {
        return Optional.ofNullable(destinationInfo).map(d -> d.signPos);
    }
    
    public void setSignPos(long signPos) {
        getOrCreateDestinationInfo().signPos = signPos;
    }

    public long getTeleportPos() {
        return Optional.ofNullable(destinationInfo).map(d -> d.teleportPos).orElse(pos);
    }
    
    public void setTeleportPos(long teleportPos) {
        getOrCreateDestinationInfo().teleportPos = teleportPos;
    }

    public Optional<UUID> getPrivateOwner() {
        return Optional.ofNullable(destinationInfo).map(d -> d.privateOwner);
    }
    
    public void setPrivateOwner(UUID owner) {
        DestinationInfo dest = getOrCreateDestinationInfo();
        dest.privateOwner = owner;
        dest.linkType = owner != null ? LinkType.PRIVATE : LinkType.DESTINATION;
    }

    public Optional<String> getDestinationName() {
        return Optional.ofNullable(destinationInfo).map(d -> d.name);
    }
    
    public void setDestinationName(String name) {
        getOrCreateDestinationInfo().name = name;
    }

    public Optional<Icon> getDestinationIcon() {
        return Optional.ofNullable(destinationInfo).map(d -> d.icon);
    }
    
    public void setDestinationIcon(Icon icon) {
        getOrCreateDestinationInfo().icon = icon;
    }

    public boolean isAccessibleBy(UUID playerId) {
        if (!isLinked()) return false;
        
        return switch (destinationInfo.linkType) {
            case NONE, ACCESS -> false;
            case DESTINATION -> true;
            case PRIVATE -> Objects.equals(playerId, destinationInfo.privateOwner);
        };
    }
    //endregion

    //region Connection Management
    public LongSet getConnectedNodes() {
        return connectedNodes;
    }

    public void connect(Node other) {
        if (other == null || other == this) return;
        this.connectedNodes.add(other.pos);
        other.connectedNodes.add(this.pos);
    }

    public void disconnect(Node other) {
        if (other == null) return;
        this.connectedNodes.remove(other.pos);
        other.connectedNodes.remove(this.pos);
    }

    public void addConnection(long otherPos) {
        if (otherPos != pos) connectedNodes.add(otherPos);
    }

    public void removeConnection(long otherPos) {
        connectedNodes.remove(otherPos);
    }

    void removeConnectionIf(LongPredicate predicate) {
        connectedNodes.removeIf(predicate);
    }
    //endregion

    //region Overrides & Utilities
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Node{pos=").append(BlockPos.of(pos));
        sb.append(", quality=").append(quality);
        sb.append(", neighbors=").append(connectedNodes.size());
        if (isLinked()) {
            sb.append(", linkType=").append(destinationInfo.linkType);
            if (destinationInfo.name != null) {
                sb.append(", name=").append(destinationInfo.name);
            }
        }
        sb.append("}");
        return sb.toString();
    }
    
    private static <E extends Enum<E>> E safeEnum(Class<E> type, String name, E fallback) {
        if (name == null || name.isEmpty()) return fallback;
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
    //endregion
}