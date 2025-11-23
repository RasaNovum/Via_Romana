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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class PathGraph {
    private final ObjectArrayList<Node> nodes = new ObjectArrayList<>();
    private final Long2IntOpenHashMap posToIndex = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap signPosToIndex = new Long2IntOpenHashMap();
    private final ConcurrentMap<UUID, NetworkCache> networkCacheById = new ConcurrentHashMap<>();
    private final Long2ObjectOpenHashMap<UUID> nodeToNetworkId = new Long2ObjectOpenHashMap<>();
    private final ConcurrentMap<UUID, FoWCache> fowCacheById = new ConcurrentHashMap<>();
    private final ConcurrentMap<ChunkPos, Set<UUID>> chunkToNetworkIds = new ConcurrentHashMap<>();
    private final Set<Long> dirtyNodePositions = ConcurrentHashMap.newKeySet();

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

        public List<DestinationResponseS2C.NodeNetworkInfo> getNodesAsInfo(Long2IntOpenHashMap posToIndex, ObjectArrayList<Node> allNodes) {
            List<DestinationResponseS2C.NodeNetworkInfo> list = new ArrayList<>(nodePositions.size());
            for (long pos : nodePositions) {
                int index = posToIndex.getOrDefault(pos, -1);
                if (index != -1) {
                    Node node = allNodes.get(index);
                    List<BlockPos> connections = new ArrayList<>(node.getConnectedNodes().size());
                    for (long conn : node.getConnectedNodes()) {
                        connections.add(BlockPos.of(conn));
                    }
                    list.add(new DestinationResponseS2C.NodeNetworkInfo(BlockPos.of(pos), node.getClearance(), connections));
                } else {
                    list.add(new DestinationResponseS2C.NodeNetworkInfo(BlockPos.of(pos), 0, Collections.emptyList()));
                }
            }
            return list;
        }
    }

    /**
     * A cached snapshot of a network's Fog-of-War data, including bounding box and allowed chunks.
     */
    public record FoWCache(ChunkPos minChunk, ChunkPos maxChunk, BlockPos minBlock, BlockPos maxBlock, Set<ChunkPos> allowedChunks) { }

    public record BoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public static final BoundingBox ZERO = new BoundingBox(0, 0, 0, 0, 0, 0);
    }

    public static PathGraph getInstance(ServerLevel level) {
        return net.rasanovum.viaromana.storage.path.PathDataManager.getOrCreatePathGraph(level);
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
            dirtyNodePositions.remove(startNode.getPos());
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
            dirtyNodePositions.remove(pos);
        }

        // Force computation of FoW to populate the spatial index immediately
        fowCacheById.remove(networkId);
        getOrComputeFoWCache(newCache);

        return newCache;
    }

    /**
     * Invalidates all caches for the networks containing a given nodes.
     */
    private Set<NetworkCache> invalidateNetworksContaining(Node... nodesToInvalidate) {
        Set<NetworkCache> invalidatedCaches = new HashSet<>();
        for (Node node : nodesToInvalidate) {
            // Traverse the graph to find the full component and its deterministic ID.
            List<Node> component = getNetwork(node);
            if (component.isEmpty()) continue;

            Set<Long> positions = component.stream().map(Node::getPos).collect(Collectors.toSet());
            UUID componentId = generateDeterministicUUID(positions);

            // Clean up Spatial Index before removing the cache
            FoWCache fow = fowCacheById.get(componentId);
            if (fow != null) {
                removeNetworkFromChunkMap(componentId, fow);
            }

            // Remove the primary network cache
            NetworkCache removedCache = networkCacheById.remove(componentId);
            if (removedCache != null) {
                invalidatedCaches.add(removedCache);
                dirtyNodePositions.addAll(removedCache.nodePositions());
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

    public int getOrCreateNode(BlockPos pos, float quality, float clearance) {
        return posToIndex.computeIfAbsent(pos.asLong(), nodePos -> {
            int newIndex = nodes.size();
            nodes.add(new Node(nodePos, quality, clearance));
            dirtyNodePositions.add(nodePos);
            return newIndex;
        });
    }

    public void createConnectedPath(List<Node.NodeData> pathData) {
        if (pathData == null || pathData.size() < 2) return;

        // Create all nodes and collect them
        List<Node> pathNodes = new ArrayList<>();
        for (Node.NodeData currentData : pathData) {
            Node currentNode = nodes.get(getOrCreateNode(currentData.pos(), currentData.quality(), currentData.clearance()));
            pathNodes.add(currentNode);
        }

        // Collect unique networks affected by new
        Set<UUID> networksToInvalidate = new HashSet<>();
        for (Node node : pathNodes) {
            UUID networkId = nodeToNetworkId.get(node.getPos());
            if (networkId != null) {
                networksToInvalidate.add(networkId);
            }
        }

        // Invalidate all affected networks once
        for (UUID networkId : networksToInvalidate) {
            FoWCache fow = fowCacheById.get(networkId);
            if (fow != null) {
                removeNetworkFromChunkMap(networkId, fow);
            }

            NetworkCache cache = networkCacheById.remove(networkId);
            if (cache != null) {
                fowCacheById.remove(networkId);
                dirtyNodePositions.addAll(cache.nodePositions());
                for (Long pos : cache.nodePositions()) {
                    nodeToNetworkId.remove(pos.longValue());
                }
                try {
                    ServerMapCache.invalidate(networkId);
                } catch (Exception e) {
                    ViaRomana.LOGGER.warn("Failed to invalidate ServerMapCache for network {}: {}", networkId, e.getMessage());
                }
            }
        }

        for (int i = 1; i < pathNodes.size(); i++) {
            pathNodes.get(i - 1).connect(pathNodes.get(i));
        }

        for (Node node : pathNodes) {
            dirtyNodePositions.add(node.getPos());
        }
    }

    /**
     * Creates or updates a pseudonetwork for temporary charting paths.
     */
    public void createOrUpdatePseudoNetwork(UUID pseudoNetworkId, List<Node.NodeData> tempNodes) {
        if (tempNodes == null || tempNodes.size() < 2) return;

        NetworkCache existing = networkCacheById.remove(pseudoNetworkId);
        if (existing != null) {
            FoWCache fow = fowCacheById.get(pseudoNetworkId);
            if (fow != null) removeNetworkFromChunkMap(pseudoNetworkId, fow);

            for (long pos : existing.nodePositions) {
                nodeToNetworkId.remove(pos);
            }
            fowCacheById.remove(pseudoNetworkId);
        }

        Set<Long> nodePositions = tempNodes.stream()
                .map(nodeData -> nodeData.pos().asLong())
                .collect(Collectors.toSet());

        List<BlockPos> posList = tempNodes.stream()
                .map(Node.NodeData::pos)
                .toList();

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : posList) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        BoundingBox bounds = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

        NetworkCache pseudoCache = new NetworkCache(
                pseudoNetworkId,
                nodePositions,
                bounds,
                List.of()
        );

        networkCacheById.put(pseudoNetworkId, pseudoCache);

        for (long pos : nodePositions) {
            nodeToNetworkId.put(pos, pseudoNetworkId);
        }

        getOrComputeFoWCache(pseudoCache);

        ViaRomana.LOGGER.debug("Created/updated pseudonetwork {} with {} nodes", pseudoNetworkId, tempNodes.size());
    }

    //endregion

    // region Node Removal

    public void removeNode(Node node) {
        removeNode(node.getBlockPos());
    }

    public void removeNode(BlockPos pos) {
        long packedPos = pos.asLong();
        int idx = posToIndex.getOrDefault(packedPos, -1);
        if (idx == -1) return;

        Node removedNode = nodes.get(idx);

        invalidateNetworksContaining(removedNode);

        for (long neighborPos : removedNode.getConnectedNodes()) {
            getNodeAt(BlockPos.of(neighborPos)).ifPresent(neighbor -> neighbor.removeConnection(packedPos));
        }

        int lastIdx = nodes.size() - 1;
        Node lastNode = nodes.get(lastIdx);
        nodes.set(idx, lastNode);
        nodes.remove(lastIdx);

        posToIndex.remove(packedPos);
        dirtyNodePositions.remove(packedPos);
        removedNode.getSignPos().ifPresent(signPos -> signPosToIndex.remove(signPos.longValue()));

        if (idx < lastIdx) {
            posToIndex.put(lastNode.getPos(), idx);
            lastNode.getSignPos().ifPresent(signPos -> signPosToIndex.put(signPos.longValue(), idx));
        }
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
     * A removal method for use when connections are already handled.
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
        dirtyNodePositions.remove(packedPos);
        node.getSignPos().ifPresent(signPos -> signPosToIndex.remove(signPos.longValue()));

        if (idx < lastIdx) {
            posToIndex.put(lastNode.getPos(), idx);
            lastNode.getSignPos().ifPresent(signPos -> signPosToIndex.put(signPos.longValue(), idx));
        }
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
        chunkToNetworkIds.clear();
        dirtyNodePositions.clear();
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

    public List<Node> getCachedTeleportDestinationsFor(UUID playerId, Node sourceNode) {
        NetworkCache cache = getNetworkCacheForNode(sourceNode);
        return cache.destinationNodes().stream()
                .filter(node -> node.getPos() != sourceNode.getPos())
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
        processDirtyNodes();

        Set<UUID> networkIds = chunkToNetworkIds.get(chunkPos);
        if (networkIds == null || networkIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<NetworkCache> result = new ArrayList<>(networkIds.size());
        for (UUID uuid : networkIds) {
            NetworkCache cache = networkCacheById.get(uuid);
            if (cache != null) {
                result.add(cache);
            }
        }
        return result;
    }

    private void processDirtyNodes() {
        if (dirtyNodePositions.isEmpty()) return;

        Iterator<Long> it = dirtyNodePositions.iterator();
        while (it.hasNext()) {
            Long pos = it.next();
            if (!dirtyNodePositions.contains(pos)) continue;

            if (nodeToNetworkId.containsKey(pos.longValue())) {
                it.remove();
                continue;
            }

            int idx = posToIndex.getOrDefault(pos.longValue(), -1);
            if (idx != -1) {
                Node node = nodes.get(idx);
                discoverAndCacheNetwork(node);
            } else {
                it.remove();
            }
        }
    }

    /**
     * Gets or computes the Fog-of-War cache for a given network, calculating it if not already present.
     */
    public FoWCache getOrComputeFoWCache(NetworkCache network) {
        boolean isPseudo = ServerMapCache.isPseudoNetwork(network.id());
        return fowCacheById.computeIfAbsent(network.id(), id -> {
            FoWCache fow = calculateFoWData(network.nodePositions(), isPseudo);
            // Update spatial index when FoW is computed
            if (fow != null) {
                addNetworkToChunkMap(id, fow);
            }
            return fow;
        });
    }

    private void addNetworkToChunkMap(UUID networkId, FoWCache fow) {
        for (ChunkPos chunk : fow.allowedChunks()) {
            chunkToNetworkIds.computeIfAbsent(chunk, c -> ConcurrentHashMap.newKeySet()).add(networkId);
        }
    }

    private void removeNetworkFromChunkMap(UUID networkId, FoWCache fow) {
        for (ChunkPos chunk : fow.allowedChunks()) {
            Set<UUID> ids = chunkToNetworkIds.get(chunk);
            if (ids != null) {
                ids.remove(networkId);
            }
        }
    }

    public static FoWCache calculateFoWData(Set<Long> nodeLongs, boolean isPseudo) {
        if (nodeLongs.isEmpty()) return null;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (Long nodeLong : nodeLongs) {
            int x = BlockPos.getX(nodeLong);
            int y = BlockPos.getY(nodeLong);
            int z = BlockPos.getZ(nodeLong);

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        int cacheWidth = maxX - minX;
        int cacheHeight = maxZ - minZ;
        int padding = ServerMapUtils.calculateUniformPadding(cacheWidth, cacheHeight);

        BlockPos paddedMin = new BlockPos(minX - padding, minY, minZ - padding);
        BlockPos paddedMax = new BlockPos(maxX + padding, maxY, maxZ + padding);

        ChunkPos minChunk = new ChunkPos(paddedMin);
        ChunkPos maxChunk = new ChunkPos(paddedMax);

        Set<ChunkPos> allowedChunks = ServerMapUtils.calculateFogOfWarChunks(nodeLongs, minChunk, maxChunk, isPseudo);

        return new FoWCache(minChunk, maxChunk, paddedMin, paddedMax, allowedChunks);
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
            CompoundTag nodeTag = (CompoundTag) raw;
            long pos = nodeTag.getLong("pos");

            if (pos == BlockPos.ZERO.asLong()) continue; // Skip phantom nodes

            nodes.add(new Node(nodeTag));
        }
        rebuildIndices();
    }

    public void rebuildIndices() {
        posToIndex.clear();
        signPosToIndex.clear();
        dirtyNodePositions.clear();
        chunkToNetworkIds.clear();

        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            posToIndex.put(node.getPos(), i);
            final int finalI = i;
            node.getSignPos().ifPresent(sp -> signPosToIndex.put(sp.longValue(), finalI));
            dirtyNodePositions.add(node.getPos());
        }
    }
    //endregion

    public List<DestinationResponseS2C.NodeNetworkInfo> getNodesAsInfo(NetworkCache cache) {
        return cache.getNodesAsInfo(this.posToIndex, this.nodes);
    }
}