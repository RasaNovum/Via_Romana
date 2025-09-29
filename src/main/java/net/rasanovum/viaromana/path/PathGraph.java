package net.rasanovum.viaromana.path;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.map.ServerMapUtils;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C;
import net.rasanovum.viaromana.storage.IPathStorage;
import net.rasanovum.viaromana.surveyor.ViaRomanaLandmark;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.WorldLandmarks;

public final class PathGraph {
    private final ObjectArrayList<Node> nodes = new ObjectArrayList<>();
    private final Long2IntOpenHashMap posToIndex = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap signPosToIndex = new Long2IntOpenHashMap();
    private final ConcurrentMap<UUID, NetworkCache> networkCacheById = new ConcurrentHashMap<>();
    private final Long2ObjectOpenHashMap<UUID> nodeToNetworkId = new Long2ObjectOpenHashMap<>();
    private final ConcurrentMap<UUID, FoWCache> fowCacheById = new ConcurrentHashMap<>();

    private static final DyeColor[] NETWORK_COLORS = {
            DyeColor.BLUE, DyeColor.RED, DyeColor.GREEN, DyeColor.PURPLE, DyeColor.CYAN,
            DyeColor.ORANGE, DyeColor.LIME, DyeColor.PINK, DyeColor.MAGENTA, DyeColor.YELLOW
    };

    /**
     * An immutable, cached snapshot of a connected network's properties.
     */
    public record NetworkCache(
            UUID id,
            Set<Long> nodePositions,
            BoundingBox bounds,
            List<Node> destinationNodes
    ) {
        public BlockPos getMin() {
            return new BlockPos(bounds.minX, bounds.minY, bounds.minZ);
        }

        public BlockPos getMax() {
            return new BlockPos(bounds.maxX, bounds.maxY, bounds.maxZ);
        }

        public List<DestinationResponseS2C.NodeNetworkInfo> getNodesAsInfo() {
            return nodePositions.stream()
                    .map(pos -> new DestinationResponseS2C.NodeNetworkInfo(BlockPos.of(pos), List.of()))
                    .collect(Collectors.toList());
        }
    }

    public record FoWCache(ChunkPos minChunk, ChunkPos maxChunk, Set<ChunkPos> allowedChunks) { }

    public record BoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public static final BoundingBox ZERO = new BoundingBox(0, 0, 0, 0, 0, 0);
    }

    public static PathGraph getInstance(ServerLevel level) {
        return IPathStorage.get(level).graph();
    }

    // region Network & Cache Management

    /**
     * Finds the network for a given node, computing and caching it if not already present.
     * This is the primary entry point for accessing network-wide data.
     */
    @NotNull
    private NetworkCache getNetworkCacheForNode(Node startNode) {
        UUID networkId = nodeToNetworkId.get(startNode.getPos());
        if (networkId != null) {
            NetworkCache cached = networkCacheById.get(networkId);
            if (cached != null) {
                return cached;
            }
        }
        
        return discoverAndCacheNetwork(startNode);
    }

    /**
     * Traverses the graph from a starting node to discover a connected component (a "network"),
     * calculates its properties, and caches the result for all nodes within that network.
     */
    @SuppressWarnings("deprecation")
    private NetworkCache discoverAndCacheNetwork(Node startNode) {
        List<Node> networkNodes = getNetwork(startNode);
        Set<Long> positions = networkNodes.stream().map(Node::getPos).collect(Collectors.toSet());
        UUID networkId = generateDeterministicUUID(positions);

        // Check if another thread cached this network while we were working
        NetworkCache existingCache = networkCacheById.get(networkId);
        if (existingCache != null) {
            // Ensure this node's mapping is up-to-date
            nodeToNetworkId.put(startNode.getPos(), networkId);
            return existingCache;
        }

        BoundingBox bounds = calculateBoundsFor(networkNodes);
        List<Node> destinations = networkNodes.stream()
                .filter(n -> n.getLinkType() == Node.LinkType.DESTINATION || n.getLinkType() == Node.LinkType.PRIVATE)
                .toList();

        NetworkCache newCache = new NetworkCache(networkId, positions, bounds, destinations);

        // Update cache for all nodes in this newly discovered network
        networkCacheById.put(networkId, newCache);
        for (Long pos : positions) {
            nodeToNetworkId.put(pos, networkId);
        }

        // Drop any stale FoW cache for this network id
        fowCacheById.remove(networkId);
        return newCache;
    }

    /**
     * Invalidates all caches for the networks containing a given nodes.
     *
     * @param nodesToInvalidate One or more nodes that are about to be modified or removed.
     * @return The set of NetworkCache objects that were invalidated, representing the state before the change.
     */
    private Set<NetworkCache> invalidateNetworksContaining(Node... nodesToInvalidate) {
        Set<NetworkCache> invalidatedCaches = new HashSet<>();
        for (Node node : nodesToInvalidate) {
            // Traverse the graph to find the full component and its deterministic ID.
            // This avoids relying on a potentially stale cache.
            List<Node> component = getNetwork(node);
            if (component.isEmpty()) continue;

            Set<Long> positions = component.stream().map(Node::getPos).collect(Collectors.toSet());
            UUID componentId = generateDeterministicUUID(positions);

            // Remove the primary network cache
            NetworkCache removedCache = networkCacheById.remove(componentId);
            if (removedCache != null) {
                invalidatedCaches.add(removedCache);
            }

            // Clean up all related cache entries
            fowCacheById.remove(componentId);
            for (Node n : component) {
                nodeToNetworkId.remove(n.getPos());
            }

            try {
                ServerMapCache.invalidate(componentId);
            } catch (Exception e) {
                ViaRomana.LOGGER.warn("Failed to invalidate ServerMapCache for network {}: {}", componentId, e.getMessage());
            }
        }
        return invalidatedCaches;
    }

    /**
     * Performs a light cache refresh for a network after non-structural changes
     */
    private void refreshNetworkDestinations(Node startNode) {
        NetworkCache existing = getNetworkCacheForNode(startNode);

        List<Node> destinations = getNetwork(startNode).stream()
                .filter(n -> n.getLinkType() == Node.LinkType.DESTINATION || n.getLinkType() == Node.LinkType.PRIVATE)
                .toList();

        NetworkCache updated = new NetworkCache(existing.id(), existing.nodePositions(), existing.bounds(), destinations);
        networkCacheById.put(updated.id(), updated);
    }

    private UUID generateDeterministicUUID(Set<Long> nodePositions) {
        if (nodePositions.isEmpty()) return UUID.randomUUID();

        long[] sortedPositions = nodePositions.stream().mapToLong(l -> l).sorted().toArray();
        ByteBuffer bb = ByteBuffer.allocate(sortedPositions.length * Long.BYTES);

        for (long pos : sortedPositions) {
            bb.putLong(pos);
        }

        return UUID.nameUUIDFromBytes(bb.array());
    }

    public DyeColor getNetworkColor(Node node) {
        NetworkCache cache = getNetworkCacheForNode(node);
        int colorIndex = Math.abs(cache.id().hashCode() % NETWORK_COLORS.length);
        return NETWORK_COLORS[colorIndex];
    }
    
    public void updateAllNetworkColors(ServerLevel level) {
        WorldLandmarks worldLandmarks;
        try {
            worldLandmarks = WorldSummary.of(level).landmarks();
        } catch (Exception e) {
            ViaRomana.LOGGER.warn("Failed to access WorldLandmarks, skipping network color update.", e);
            return;
        }

        Set<UUID> processedNetworks = new HashSet<>();
        for (Node node : nodes) {
            NetworkCache cache = getNetworkCacheForNode(node);
            if (processedNetworks.contains(cache.id())) {
                continue;
            }

            DyeColor requiredColor = getNetworkColor(node);
            for (Node destinationNode : cache.destinationNodes()) {
                try {
                    Landmark<?> landmark = worldLandmarks.get(ViaRomanaLandmark.TYPE, destinationNode.getBlockPos());
                    if (landmark != null && landmark.color() != requiredColor) {
                        ViaRomanaLandmark updatedLandmark = ViaRomanaLandmark.createDestination(level, destinationNode, destinationNode.getBlockPos());
                        worldLandmarks.put(level, updatedLandmark);
                    }
                } catch (Exception e) {
                    ViaRomana.LOGGER.warn("Failed to update landmark color for node at {}: {}", destinationNode.getBlockPos(), e.getMessage());
                }
            }
            processedNetworks.add(cache.id());
        }
    }

    //endregion

    // region Basic Accessors & Mutators
    
    public List<Node> nodesView() {
        return Collections.unmodifiableList(nodes);
    }

    public int size() {
        return nodes.size();
    }
    
    public boolean contains(BlockPos pos) {
        return posToIndex.containsKey(pos.asLong());
    }

    public Optional<Node> getNodeAt(BlockPos pos) {
        int idx = posToIndex.getOrDefault(pos.asLong(), -1);
        return idx != -1 ? Optional.of(nodes.get(idx)) : Optional.empty();
    }

    public int getOrCreateNode(BlockPos pos, float quality) {
        return posToIndex.computeIfAbsent(pos.asLong(), p -> {
            int newIndex = nodes.size();
            nodes.add(new Node(p, quality));
            return newIndex;
        });
    }

    public void createConnectedPath(List<Node.NodeData> pathData) {
        if (pathData == null || pathData.size() < 2) return;

        Node previousNode = null;
        for (Node.NodeData currentData : pathData) {
            Node currentNode = nodes.get(getOrCreateNode(currentData.pos(), currentData.quality()));
            if (previousNode != null) {
                invalidateNetworksContaining(previousNode, currentNode);
                previousNode.connect(currentNode);
            }
            previousNode = currentNode;
        }
    }
    
    //endregion

    // region Node Removal

    public void removeNode(Node node) {
        removeNode(node.getBlockPos());
    }
    
    public boolean removeNode(BlockPos pos) {
        long packedPos = pos.asLong();
        int idx = posToIndex.getOrDefault(packedPos, -1);
        if (idx == -1) return false;

        Node removedNode = nodes.get(idx);

        // Invalidate the network this node belongs to BEFORE modifying connections.
        invalidateNetworksContaining(removedNode);

        // Disconnect from neighbors
        for (long neighborPos : removedNode.getConnectedNodes()) {
            getNodeAt(BlockPos.of(neighborPos)).ifPresent(neighbor -> neighbor.removeConnection(packedPos));
        }

        // Efficiently remove from the list by swapping with the last element
        int lastIdx = nodes.size() - 1;
        Node lastNode = nodes.get(lastIdx);
        nodes.set(idx, lastNode);
        nodes.remove(lastIdx);

        posToIndex.remove(packedPos);
        removedNode.getSignPos().ifPresent(signPos -> signPosToIndex.remove(signPos.longValue()));

        // If we moved a node, update its index in the map
        if (idx < lastIdx) {
            posToIndex.put(lastNode.getPos(), idx);
            lastNode.getSignPos().ifPresent(signPos -> signPosToIndex.put(signPos.longValue(), idx));
        }

        return true;
    }

    public Optional<NetworkCache> removeBranch(Node startNode) {
        Set<Node> branchNodes = findBranchNodes(startNode);
        if (branchNodes.isEmpty()) {
            return Optional.empty();
        }

        Set<NetworkCache> invalidatedCaches = invalidateNetworksContaining(startNode);

        Set<Long> branchPositions = branchNodes.stream().map(Node::getPos).collect(Collectors.toSet());

        for (Node node : branchNodes) {
            for (long neighborPos : node.getConnectedNodes()) {
                if (!branchPositions.contains(neighborPos)) {
                    getNodeAt(BlockPos.of(neighborPos)).ifPresent(neighbor -> neighbor.removeConnection(node.getPos()));
                }
            }

            removeNodeWithoutNeighborUpdates(node);
        }

        return invalidatedCaches.stream().findFirst();
    }
    
    /**
     * A simplified removal method for use when connections are already handled.
     */
    private void removeNodeWithoutNeighborUpdates(Node node) {
        long packedPos = node.getPos();
        int idx = posToIndex.getOrDefault(packedPos, -1);
        if (idx == -1) return;

        int lastIdx = nodes.size() - 1;
        Node lastNode = nodes.get(lastIdx);
        nodes.set(idx, lastNode);
        nodes.remove(lastIdx);

        posToIndex.remove(packedPos);
        node.getSignPos().ifPresent(signPos -> signPosToIndex.remove(signPos.longValue()));

        if (idx < lastIdx) {
            posToIndex.put(lastNode.getPos(), idx);
            lastNode.getSignPos().ifPresent(signPos -> signPosToIndex.put(signPos.longValue(), idx));
        }
    }

    /**
     * Removes a node but connects its direct neighbors to maintain path continuity.
     */
    public void removeAndHealConnections(Node node) {
        long[] neighbors = node.getConnectedNodes().toLongArray();

        invalidateNetworksContaining(node);

        for (long neighborPos : neighbors) {
            getNodeAt(BlockPos.of(neighborPos)).ifPresent(neighbor -> neighbor.removeConnection(node.getPos()));
        }

        if (neighbors.length == 2) {
            Optional<Node> nodeA = getNodeAt(BlockPos.of(neighbors[0]));
            Optional<Node> nodeB = getNodeAt(BlockPos.of(neighbors[1]));
            if (nodeA.isPresent() && nodeB.isPresent()) {
                nodeA.get().connect(nodeB.get());
            }
        }
        
        removeNodeWithoutNeighborUpdates(node);
    }
    
    public void removeAllNodes() {
        for (NetworkCache cache : networkCacheById.values()) {
            try {
                ServerMapCache.invalidate(cache.id());
            } catch (Exception e) {
                ViaRomana.LOGGER.warn("Failed to invalidate ServerMapCache during clear for {}: {}", cache.id(), e.getMessage());
            }
        }
        nodes.clear();
        posToIndex.clear();
        signPosToIndex.clear();
        networkCacheById.clear();
        nodeToNetworkId.clear();
        fowCacheById.clear();
    }

    // region Sign Linking

    public boolean linkSignToNode(BlockPos nodePos, BlockPos signPos, Node.LinkType linkType, UUID owner) {
        return getNodeAt(nodePos).map(node -> {
            node.setSignPos(signPos.asLong());
            node.setLinkType(linkType);
            if (linkType == Node.LinkType.PRIVATE && owner != null) {
                node.setPrivateOwner(owner);
            }

            signPosToIndex.put(signPos.asLong(), posToIndex.get(nodePos.asLong()));

            refreshNetworkDestinations(node);
            return true;
        }).orElse(false);
    }

    public boolean removeSignLink(BlockPos signPos) {
        return getNodeBySignPos(signPos).map(node -> {
            node.unlink();
            signPosToIndex.remove(signPos.asLong());

            refreshNetworkDestinations(node);
            return true;
        }).orElse(false);
    }

    public Optional<Node> getNodeBySignPos(BlockPos signPos) {
        int idx = signPosToIndex.getOrDefault(signPos.asLong(), -1);
        return idx != -1 ? Optional.of(nodes.get(idx)) : Optional.empty();
    }

    //endregion

    // region Queries & Traversals

    /*
     * Finds the nearest node to a given position within a specified maximum distance that satisfies the provided filter.
     */
    public Optional<Node> getNearestNode(BlockPos origin, double maxDistance, Predicate<Node> filter) {
        return getNearestNode(origin, maxDistance, maxDistance, filter);
    }

    /*
     * Finds the nearest node to a given position within specified horizontal and vertical distance limits that satisfies the provided filter.
     */
    public Optional<Node> getNearestNode(BlockPos origin, double maxDistance, double maxYDistance, Predicate<Node> filter) {
        return nodes.stream()
                .filter(filter)
                .filter(node -> ClientPathData.calculateDistance(node.getBlockPos(), origin, false) <= maxDistance * maxDistance && Math.abs(node.getBlockPos().getY() - origin.getY()) <= maxYDistance)
                .min(Comparator.comparingDouble(node -> ClientPathData.calculateDistance(node.getBlockPos(), origin, true)));
    }

    public List<Node> getNetwork(Node start) {
        List<Node> network = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Queue<Node> queue = new ArrayDeque<>();

        visited.add(start.getPos());
        queue.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            network.add(current);
            for (long neighborPos : current.getConnectedNodes()) {
                if (visited.add(neighborPos)) {
                    getNodeAt(BlockPos.of(neighborPos)).ifPresent(queue::add);
                }
            }
        }
        return network;
    }

    private Set<Node> findBranchNodes(Node start) {
        Set<Node> branch = new HashSet<>();
        if (start.getConnectedNodes().size() >= 3) {
            branch.add(start);
            return branch;
        }

        Queue<Node> queue = new ArrayDeque<>();
        Set<Node> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            branch.add(current);

            for (long neighborPos : current.getConnectedNodes()) {
                getNodeAt(BlockPos.of(neighborPos)).ifPresent(neighbor -> {
                    if (visited.add(neighbor) && neighbor.getConnectedNodes().size() < 3) {
                        queue.add(neighbor);
                    }
                });
            }
        }
        return branch;
    }

    public BoundingBox getNetworkBounds(Node sourceNode) {
        return getNetworkCacheForNode(sourceNode).bounds();
    }

    public List<Node> getCachedTeleportDestinationsFor(UUID playerId, Node sourceNode) {
        NetworkCache cache = getNetworkCacheForNode(sourceNode);
        return cache.destinationNodes().stream()
                .filter(node -> node.getPos() != sourceNode.getPos()) // Exclude source
                .filter(node -> node.isAccessibleBy(playerId))
                .toList();
    }

    private BoundingBox calculateBoundsFor(List<Node> networkNodes) {
        if (networkNodes.isEmpty()) return BoundingBox.ZERO;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (Node node : networkNodes) {
            BlockPos pos = node.getBlockPos();
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    @Nullable
    public NetworkCache getNetworkCache(UUID id) {
        return networkCacheById.get(id);
    }

    public NetworkCache getNetworkCache(Node startNode) {
        return getNetworkCacheForNode(startNode);
    }

    /**
     * Finds all networks whose Fog-of-War allowed chunks include the given chunk.
     * Intended for cases where a chunk may belong to multiple network maps.
     */
    public List<NetworkCache> findNetworksForChunk(ChunkPos chunkPos) {
        Set<UUID> seen = new HashSet<>();
        List<NetworkCache> result = new ArrayList<>();
        for (Node node : nodes) {
            NetworkCache cache = getNetworkCacheForNode(node);
            if (!seen.add(cache.id())) continue;

            FoWCache fow = getOrComputeFoWCache(cache);

            if (chunkPos.x >= fow.minChunk().x && chunkPos.x <= fow.maxChunk().x &&
                chunkPos.z >= fow.minChunk().z && chunkPos.z <= fow.maxChunk().z) {
                if (fow.allowedChunks().contains(chunkPos)) {
                    result.add(cache);
                }
            }
        }
        return result;
    }

    public FoWCache getOrComputeFoWCache(NetworkCache cache) {
        return fowCacheById.computeIfAbsent(cache.id(), id -> {
            int widthW = cache.bounds.maxX - cache.bounds.minX;
            int heightW = cache.bounds.maxZ - cache.bounds.minZ;
            int padX = Math.max(ServerMapUtils.MAP_BOUNDS_MIN_PADDING, (int) (widthW * ServerMapUtils.MAP_BOUNDS_PADDING_PERCENTAGE));
            int padZ = Math.max(ServerMapUtils.MAP_BOUNDS_MIN_PADDING, (int) (heightW * ServerMapUtils.MAP_BOUNDS_PADDING_PERCENTAGE));
            BlockPos paddedMin = new BlockPos(cache.bounds.minX - padX, cache.bounds.minY, cache.bounds.minZ - padZ);
            BlockPos paddedMax = new BlockPos(cache.bounds.maxX + padX, cache.bounds.maxY, cache.bounds.maxZ + padZ);

            ChunkPos minChunk = new ChunkPos(paddedMin);
            ChunkPos maxChunk = new ChunkPos(paddedMax);

            Set<ChunkPos> allowed = ServerMapUtils.calculateFogOfWarChunks(cache.getNodesAsInfo(), minChunk, maxChunk);
            return new FoWCache(minChunk, maxChunk, allowed);
        });
    }

    public List<Node> queryNearby(BlockPos center, double radius) {
        double radiusSquared = radius * radius;
        return nodes.stream()
                .filter(node -> node.getBlockPos().distSqr(center) <= radiusSquared)
                .collect(Collectors.toList());
    }

    //endregion

    // region Serialization
    
    public CompoundTag serialize(CompoundTag root) {
        ListTag list = new ListTag();
        for (Node node : nodes) {
            list.add(node.serialize(new CompoundTag()));
        }
        root.put("nodes", list);
        return root;
    }

    public void deserialize(CompoundTag root) {
        removeAllNodes();
        ListTag list = root.getList("nodes", Tag.TAG_COMPOUND);
        for (Tag raw : list) {
            nodes.add(new Node((CompoundTag) raw));
        }
        rebuildIndices();
    }

    public void rebuildIndices() {
        posToIndex.clear();
        signPosToIndex.clear();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            posToIndex.put(node.getPos(), i);
            final int finalI = i;
            node.getSignPos().ifPresent(sp -> signPosToIndex.put(sp.longValue(), finalI));
        }
    }
    //endregion
}