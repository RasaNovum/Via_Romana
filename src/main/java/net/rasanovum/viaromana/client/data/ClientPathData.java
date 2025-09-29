package net.rasanovum.viaromana.client.data;

import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.core.LinkHandler.LinkData;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.Node.NodeData;
import net.minecraft.core.BlockPos;

import java.util.function.Predicate;

import java.util.*;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side storage for path data synchronized from the server.
 */
public class ClientPathData {
    private static ClientPathData INSTANCE = new ClientPathData();
    
    // Server-synced persistent data
    private PathGraph clientGraph = new PathGraph();
    private boolean hasData = false;
    
    // Client-side temporary nodes for charting visualization
    private final List<NodeData> temporaryNodes = new ArrayList<>();
    private final List<LinkData> temporaryLinks = new ArrayList<>();
    
    public static ClientPathData getInstance() {
        return INSTANCE;
    }

    // region Graph Data
    
    public void updatePathData(PathGraph serverGraph) {
        this.clientGraph = serverGraph;
        this.hasData = true;
    }
    
    public PathGraph getGraph() {
        return hasData ? clientGraph : null;
    }

    public boolean hasValidData() {
        return hasData && clientGraph != null && !clientGraph.nodesView().isEmpty();
    }
    
    /**
     * Clears both persistent and temporary client-side path data.
     */
    public void clearData() {
        this.clientGraph = new PathGraph();
        this.hasData = false;
        this.temporaryNodes.clear();
        this.temporaryLinks.clear();
    }
    
    // region Temp Node Util

    public void addTemporaryNode(BlockPos pos, float quality) {
        temporaryNodes.add(new NodeData(pos, quality));
    }
    
    public void removeTemporaryNode(BlockPos pos) {
        temporaryNodes.removeIf(node -> node.pos().equals(pos));
    }
    
    public void clearTemporaryNodes() {
        temporaryNodes.clear();
        temporaryLinks.clear();
    }

    public List<NodeData> getTemporaryNodes() {
        return Collections.unmodifiableList(temporaryNodes);
    }
    
    public float getTemporaryNodeQuality(BlockPos pos) {
        return temporaryNodes.stream()
            .filter(node -> node.pos().equals(pos))
            .findFirst()
            .map(NodeData::quality)
            .orElse(1.0f);
    }

    public boolean isTemporaryNode(BlockPos pos) {
        return temporaryNodes.stream().anyMatch(node -> node.pos().equals(pos));
    }

    // region Temp Link Util

    public void addTemporaryLink(LinkData linkData) {
        temporaryLinks.removeIf(link -> link.signPos().equals(linkData.signPos()));
        temporaryLinks.add(linkData);
    }

    public void removeTemporaryLink(LinkData linkData) {
        temporaryLinks.removeIf(link -> link.equals(linkData));
    }

    public void clearTemporaryLinks() {
        temporaryLinks.clear();
    }

    public List<LinkData> getTemporaryLinks() {
        return Collections.unmodifiableList(temporaryLinks);
    }

    /*
     * Checks if a node at the given position has a temporary link
     */
    public Optional<LinkData> getTemporaryNodeLink(BlockPos nodePos) {
        return temporaryLinks.stream().filter(link -> link.nodePos().equals(nodePos)).findFirst();
    }

    /*
     * Checks if a sign at the given position has a temporary link
     */
    public Optional<LinkData> getTemporarySignLink(BlockPos signPos) {
        return temporaryLinks.stream().filter(link -> link.signPos().equals(signPos)).findFirst();
    }

    /*
     * Checks if either a node or sign at the given positions have temporary links
     */
    public boolean isNodeSignLinked(@Nullable BlockPos nodePos, @Nullable BlockPos signPos) {
        if (nodePos == null && signPos == null) return false;
        if (nodePos != null && signPos != null) return getTemporaryNodeLink(nodePos).isPresent() == getTemporarySignLink(signPos).isPresent();
        if (nodePos != null) return getTemporaryNodeLink(nodePos).isPresent();
        return getTemporarySignLink(signPos).isPresent();
    }

    // region Node Lookup

    /**
     * Finds the nearest node to the given position, optionally including temporary nodes.
     */
    public Optional<Node> getNearestNode(BlockPos origin, double maxDistance, boolean includeTemp) {
        return getNearestNode(origin, maxDistance, maxDistance, includeTemp, node -> true, node -> true);
    }
    
    /**
     * Finds the nearest node to the given position, optionally including temporary nodes, with separate Y-axis range.
     */
    public Optional<Node> getNearestNode(BlockPos origin, double maxDistance, double maxYDistance, boolean includeTemp) {
        return getNearestNode(origin, maxDistance, maxYDistance, includeTemp, node -> true, node -> true);
    }

    /**
     * Finds the nearest node to the given position, optionally including temporary nodes, with filtering.
     */
    public Optional<Node> getNearestNode(BlockPos origin, double maxDistance, boolean includeTemp, Predicate<Node> graphFilter, Predicate<Node> clientFilter) {
        return getNearestNode(origin, maxDistance, maxDistance, includeTemp, graphFilter, clientFilter);
    }
    
    /**
     * Finds the nearest node to the given position, optionally including temporary nodes.
     */
    public Optional<Node> getNearestNode(BlockPos origin, double maxDistance, double maxYDistance, boolean includeTemp, Predicate<Node> graphFilter, Predicate<Node> clientFilter) {
        PathGraph graph = getGraph();
        if (graph == null) return Optional.empty();
    
        Optional<Node> bestPersistent = graph.getNearestNode(origin, maxDistance, graphFilter);
    
        if (!includeTemp || temporaryNodes.isEmpty()) return bestPersistent;
    
        double maxDistSq = maxDistance * maxDistance;
        Optional<Node> bestTemporary = temporaryNodes.stream()
                .map(data -> new Node(data.pos().asLong(), data.quality()))
                .filter(clientFilter)
                .filter(node -> calculateDistance(node.getBlockPos(), origin, false) <= maxDistSq && Math.abs(node.getBlockPos().getY() - origin.getY()) <= maxYDistance)
                .min(Comparator.comparingDouble(node -> calculateDistance(node.getBlockPos(), origin, true)));
    
        if (bestPersistent.isEmpty()) return bestTemporary;
        if (bestTemporary.isEmpty()) return bestPersistent;
    
        Node persistentNode = bestPersistent.get();
        Node temporaryNode = bestTemporary.get();
    
        double persistentNodeDistance = calculateDistance(persistentNode.getBlockPos(), origin);
        double tempNodeDistance = calculateDistance(temporaryNode.getBlockPos(), origin);
    
        return persistentNodeDistance <= tempNodeDistance ? bestPersistent : bestTemporary;
    }
    
    /**
     * Gets persistent and temporary nodes within a radius.
     */
    public List<Node> getNearbyNodes(BlockPos center, double radius, boolean includeTemp) {
        List<Node> result = new ArrayList<>();
        
        PathGraph graph = getGraph();
        if (graph != null) {
            result.addAll(graph.queryNearby(center, radius));
        }
        
        if (includeTemp) {
            double r2 = radius * radius;
            double cx = center.getX() + 0.5;
            double cy = center.getY() + 0.5;
            double cz = center.getZ() + 0.5;
            
            for (NodeData packed : temporaryNodes) {
                double dx = (packed.pos().getX() + 0.5) - cx;
                double dy = (packed.pos().getY() + 0.5) - cy;
                double dz = (packed.pos().getZ() + 0.5) - cz;
                
                if (dx*dx + dy*dy + dz*dz <= r2) {
                    result.add(new Node(packed.pos().asLong(), packed.quality()));
                }
            }
        }
        
        return result;
    }

    public static double calculateDistance(BlockPos a, BlockPos b) {
        return calculateDistance(a, b, false);
    }

    public static double calculateDistance(BlockPos a, BlockPos b, boolean includeY) {
        double dx = (a.getX() + 0.5) - (b.getX() + 0.5);
        double dy = includeY ? (a.getY() + 0.5) - (b.getY() + 0.5) : 0;
        double dz = (a.getZ() + 0.5) - (b.getZ() + 0.5);
        return dx*dx + dy*dy + dz*dz;
    }
}
