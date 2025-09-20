package net.rasanovum.viaromana.core;

import net.rasanovum.viaromana.storage.IPathStorage;
import net.rasanovum.viaromana.surveyor.ViaRomanaLandmarkManager;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.util.PathSyncUtils;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.UUID;

public class LinkHandler {
    public record LinkData(BlockPos signPos, BlockPos nodePos, Node.LinkType linkType, Node.Icon icon, String destinationName, UUID owner) {}

    /**
     * Checks if a given block position is a sign block based on the warp_block tag.
     */
    public static boolean isSignBlock(LevelAccessor world, BlockPos blockPos) {
        BlockState targetBlock = world.getBlockState(blockPos);
        if (targetBlock.is(TagKey.create(Registries.BLOCK, new ResourceLocation("via_romana", "warp_block")))) return true;
        return false;
    }

    /**
     * Gets the appropriate PathGraph for the given level.
     * On client side, uses ClientPathData. On server side, uses IPathStorage.
     */
    private static PathGraph getPathGraph(LevelAccessor level) {
        if (level instanceof Level levelInstance) {
            if (levelInstance.isClientSide()) {
                ClientPathData clientData = ClientPathData.getInstance();
                return clientData.hasValidData() ? clientData.getGraph() : null;
            } else {
                if (levelInstance instanceof ServerLevel serverLevel) {
                    IPathStorage storage = IPathStorage.get(serverLevel);
                    return storage != null ? storage.graph() : null;
                }
            }
        }
        return null;
    }

    /**
     * Links a sign to a specific node in the path graph
     * @return true if successful, false if the node already has a sign or doesn't exist
     */
    @SuppressWarnings("deprecation")
    public static boolean linkSignToNode(ServerLevel level, LinkData linkData) {
        if (!level.hasChunkAt(linkData.signPos())) {
            level.getChunk(linkData.signPos());
        }

        if (!isSignBlock(level, linkData.signPos())) {
            ViaRomana.LOGGER.warn("linkSignToNode: Sign block entity not found at {} or chunk not loaded.", linkData.signPos());
            return false;
        }

        IPathStorage storage = IPathStorage.get(level);
        Optional<Node> nodeOpt = storage.graph().getNodeAt(linkData.nodePos());

        if (nodeOpt.isEmpty()) {
            ViaRomana.LOGGER.warn("linkSignToNode: Node not found at {}", linkData.nodePos());
            return false;
        }

        Node node = nodeOpt.get();

        storage.graph().linkSignToNode(linkData.nodePos(), linkData.signPos(), linkData.linkType(), linkData.owner());
        node.setDestinationName(linkData.destinationName());
        node.setDestinationIcon(linkData.icon());

        storage.setDirty();
        PathSyncUtils.syncPathGraphToAllPlayers(level);

        ViaRomanaLandmarkManager.addDestinationLandmark(level, node);
        return true;
    }
    
    /**
     * Unlinks a sign from its associated node
     */
    public static boolean unlinkSignFromNode(ServerLevel level, BlockPos signPos) {
        if (!isSignBlock(level, signPos)) return false;
        
        IPathStorage storage = IPathStorage.get(level);
        
        Optional<Node> nodeOpt = storage.graph().getNodeBySignPos(signPos);
        if (nodeOpt.isPresent()) {
            Node node = nodeOpt.get();
            storage.graph().removeSignLink(signPos);
            storage.setDirty();
            
            PathSyncUtils.syncPathGraphToAllPlayers(level);
            ViaRomanaLandmarkManager.removeDestinationLandmark(level, node);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Handles sign destruction - removes link from node
     */
    public static void handleSignDestruction(ServerLevel level, BlockPos signPos) {
        IPathStorage storage = IPathStorage.get(level);
        Optional<Node> node = getLinkedNode(level, signPos);
        if (node.isPresent()) {
            Node linkedNode = node.get();
            if (linkedNode.getSignPos().isPresent() && linkedNode.getSignPos().get() == signPos.asLong()) {
                storage.graph().removeSignLink(signPos);
                storage.setDirty();
                
                PathSyncUtils.syncPathGraphToAllPlayers(level);
                ViaRomanaLandmarkManager.removeDestinationLandmark(level, linkedNode);
            }
        }
    }

    /**
     * Checks if a sign is linked to a path node (works on both client and server).
     */
    public static boolean isSignLinked(LevelAccessor world, BlockPos signPos) {
        return getLinkedNode(world, signPos).isPresent();
    }

    /**
     * Gets the node linked to a sign at the given position (works on both client and server).
     */
    public static Optional<Node> getLinkedNode(LevelAccessor level, BlockPos signPos) {
        if (!isSignBlock(level, signPos)) return Optional.empty();

        ViaRomana.LOGGER.debug("getLinkedNode: Checking sign at {}", signPos);

        PathGraph graph = getPathGraph(level);
        if (graph == null) return Optional.empty();
        
        return graph.getNodeBySignPos(signPos);
    }

    /**
     * Checks if a player has access to a node.
     */
    public static boolean hasAccess(Entity player, Node node) {
        UUID playerUuid = player.getUUID();
        if (node == null) return false;
        if (node.getPrivateOwner().isEmpty()) return true;
        return playerUuid.equals(node.getPrivateOwner().get());
    }

    /**
     * Gets the LinkData for a sign at the given position (works on both client and server).
     */
    public static Optional<LinkData> getLinkData(LevelAccessor level, BlockPos signPos) {
        Optional<Node> nodeOpt = getLinkedNode(level, signPos);
        if (nodeOpt.isPresent()) {
            Node node = nodeOpt.get();
            return Optional.of(new LinkData(
                signPos,
                node.getBlockPos(),
                node.getLinkType(),
                node.getDestinationIcon().orElse(Node.Icon.SIGNPOST),
                node.getDestinationName().orElse("Travel Destination"),
                node.getPrivateOwner().orElse(null)
            ));
        }

        // If no permanent link and on client, check temporary data
        if (level instanceof Level levelInstance && levelInstance.isClientSide()) {
            ClientPathData clientData = ClientPathData.getInstance();
            return clientData.getTemporarySignLink(signPos);
        }

        return Optional.empty();
    }
}
