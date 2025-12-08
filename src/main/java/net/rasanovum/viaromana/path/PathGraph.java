package net.rasanovum.viaromana.path;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.map.ServerMapUtils;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Predicate;

public final class PathGraph {
    // Core Data
    private final ObjectArrayList<Node> nodes = new ObjectArrayList<>();
    private final Long2IntOpenHashMap posToIndex = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap signPosToIndex = new Long2IntOpenHashMap();

    // Spatial Index
    private final Long2ObjectOpenHashMap<List<Node>> nodesByChunk = new Long2ObjectOpenHashMap<>();

    // Network Caching
    private final Map<UUID, NetworkCache> networkCacheById = new HashMap<>();
    private final Long2ObjectOpenHashMap<UUID> nodeToNetworkId = new Long2ObjectOpenHashMap<>();
    private final Map<UUID, FoWCache> fowCacheById = new HashMap<>();

    // Key - Packed ChunkPos
    private final Long2ObjectOpenHashMap<Set<UUID>> chunkToNetworkIds = new Long2ObjectOpenHashMap<>();

    // Primitive Set for dirty nodes
    private final LongOpenHashSet dirtyNodePositions = new LongOpenHashSet();

    private static final DyeColor[] NETWORK_COLORS = {
            DyeColor.BLUE, DyeColor.RED, DyeColor.GREEN, DyeColor.PURPLE, DyeColor.CYAN,
            DyeColor.ORANGE, DyeColor.LIME, DyeColor.PINK, DyeColor.MAGENTA, DyeColor.YELLOW
    };

    /**
     * An immutable, cached snapshot of a connected network's properties.
     */
    public record NetworkCache(
            UUID id,
            LongSet nodePositions,
            BoundingBox bounds,
            List<Node> destinationNodes
    ) {
        public NetworkCache {
            if (nodePositions != null) {
                nodePositions = LongSets.unmodifiable(nodePositions);
            }
        }

        public List<DestinationResponseS2C.NodeNetworkInfo> getNodesAsInfo(Long2IntOpenHashMap posToIndex, ObjectArrayList<Node> allNodes) {
            List<DestinationResponseS2C.NodeNetworkInfo> list = new ArrayList<>(nodePositions.size());
            LongIterator it = nodePositions.iterator();
            while (it.hasNext()) {
                long pos = it.nextLong();
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

    public record FoWCache(ChunkPos minChunk, ChunkPos maxChunk, BlockPos minBlock, BlockPos maxBlock, Set<ChunkPos> allowedChunks) { }

    public record BoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public static final BoundingBox ZERO = new BoundingBox(0, 0, 0, 0, 0, 0);
    }

    public static PathGraph getInstance(ServerLevel level) {
        return net.rasanovum.viaromana.storage.path.PathDataManager.getOrCreatePathGraph(level);
    }

    // region Network & Cache Management
    public NetworkCache getNetworkCacheForNode(Node startNode) {
        UUID networkId = nodeToNetworkId.get(startNode.getPos());
        if (networkId != null) {
            NetworkCache cached = networkCacheById.get(networkId);
            if (cached != null) {
                return cached;
            }
        }

        return discoverAndCacheNetwork(startNode);
    }

    private NetworkCache discoverAndCacheNetwork(Node startNode) {
        List<Node> networkNodes = getNetwork(startNode);

        long[] positionsArr = new long[networkNodes.size()];
        for(int i = 0; i < networkNodes.size(); i++) {
            positionsArr[i] = networkNodes.get(i).getPos();
        }

        UUID networkId = generateDeterministicUUID(positionsArr);

        NetworkCache existingCache = networkCacheById.get(networkId);
        if (existingCache != null) {
            nodeToNetworkId.put(startNode.getPos(), networkId);
            dirtyNodePositions.remove(startNode.getPos());
            return existingCache;
        }

        BoundingBox bounds = calculateBoundsFor(networkNodes);
        List<Node> destinations = networkNodes.stream()
                .filter(n -> n.getLinkType() == Node.LinkType.DESTINATION || n.getLinkType() == Node.LinkType.PRIVATE)
                .toList();

        LongOpenHashSet positionsSet = new LongOpenHashSet(positionsArr);
        NetworkCache newCache = new NetworkCache(networkId, positionsSet, bounds, destinations);

        networkCacheById.put(networkId, newCache);
        for (long pos : positionsArr) {
            nodeToNetworkId.put(pos, networkId);
            dirtyNodePositions.remove(pos);
        }

        fowCacheById.remove(networkId);
        getOrComputeFoWCache(newCache);

        return newCache;
    }

    private void invalidateNetworksContaining(Node... nodesToInvalidate) {
        for (Node node : nodesToInvalidate) {
            List<Node> component = getNetwork(node);
            if (component.isEmpty()) continue;

            long[] posArr = new long[component.size()];
            for(int i = 0; i < component.size(); i++) posArr[i] = component.get(i).getPos();

            UUID componentId = generateDeterministicUUID(posArr);

            FoWCache fow = fowCacheById.get(componentId);
            if (fow != null) {
                removeNetworkFromChunkMap(componentId, fow);
            }

            NetworkCache removedCache = networkCacheById.remove(componentId);
            if (removedCache != null) {
                dirtyNodePositions.addAll(removedCache.nodePositions());
            }

            fowCacheById.remove(componentId);
            for (Node n : component) {
                nodeToNetworkId.remove(n.getPos());
            }

            ServerMapCache.invalidate(componentId);
        }
    }

    private void refreshNetworkDestinations(Node startNode) {
        NetworkCache existing = getNetworkCacheForNode(startNode);

        List<Node> destinations = getNetwork(startNode).stream()
                .filter(n -> n.getLinkType() == Node.LinkType.DESTINATION || n.getLinkType() == Node.LinkType.PRIVATE)
                .toList();

        NetworkCache updated = new NetworkCache(existing.id(), existing.nodePositions(), existing.bounds(), destinations);
        networkCacheById.put(updated.id(), updated);
    }

    private UUID generateDeterministicUUID(long[] sortedPositions) {
        if (sortedPositions.length == 0) return UUID.randomUUID();

        LongArrays.quickSort(sortedPositions);

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

    public Optional<Node> getNodeAt(BlockPos pos) {
        int idx = posToIndex.getOrDefault(pos.asLong(), -1);
        return idx != -1 ? Optional.of(nodes.get(idx)) : Optional.empty();
    }

    public int getOrCreateNode(BlockPos pos, float quality, float clearance) {
        long packedPos = pos.asLong();
        return posToIndex.computeIfAbsent(packedPos, k -> {
            int newIndex = nodes.size();
            Node newNode = new Node(packedPos, quality, clearance);
            nodes.add(newNode);

            long chunkPos = ChunkPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
            nodesByChunk.computeIfAbsent(chunkPos, c -> new ArrayList<>()).add(newNode);

            dirtyNodePositions.add(packedPos);
            return newIndex;
        });
    }

    public void createConnectedPath(List<Node.NodeData> pathData) {
        if (pathData == null || pathData.size() < 2) return;

        List<Node> pathNodes = new ArrayList<>(pathData.size());
        for (Node.NodeData currentData : pathData) {
            Node currentNode = nodes.get(getOrCreateNode(currentData.pos(), currentData.quality(), currentData.clearance()));
            pathNodes.add(currentNode);
        }

        Set<UUID> networksToInvalidate = new HashSet<>();
        for (Node node : pathNodes) {
            UUID networkId = nodeToNetworkId.get(node.getPos());
            if (networkId != null) {
                networksToInvalidate.add(networkId);
            }
        }

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
                ServerMapCache.invalidate(networkId);
            }
        }

        for (int i = 1; i < pathNodes.size(); i++) {
            pathNodes.get(i - 1).connect(pathNodes.get(i));
        }

        for (Node node : pathNodes) {
            dirtyNodePositions.add(node.getPos());
        }
    }

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

        LongOpenHashSet nodePositions = new LongOpenHashSet();
        List<BlockPos> posList = new ArrayList<>(tempNodes.size());

        for (Node.NodeData data : tempNodes) {
            nodePositions.add(data.pos().asLong());
            posList.add(data.pos());
        }

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

        removeNodeFromSpatialIndex(removedNode);

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

    public void removeBranch(Node startNode) {
        Set<Node> branchNodes = findBranchNodes(startNode);
        if (branchNodes.isEmpty()) return;

        invalidateNetworksContaining(startNode);
        LongOpenHashSet branchPositions = new LongOpenHashSet();
        for(Node n : branchNodes) branchPositions.add(n.getPos());

        for (Node node : branchNodes) {
            for (long neighborPos : node.getConnectedNodes()) {
                if (!branchPositions.contains(neighborPos)) {
                    getNodeAt(BlockPos.of(neighborPos)).ifPresent(neighbor -> neighbor.removeConnection(node.getPos()));
                }
            }
            removeNodeWithoutNeighborUpdates(node);
        }
    }

    private void removeNodeWithoutNeighborUpdates(Node node) {
        long packedPos = node.getPos();
        int idx = posToIndex.getOrDefault(packedPos, -1);
        if (idx == -1) return;

        removeNodeFromSpatialIndex(node);

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

    private void removeNodeFromSpatialIndex(Node node) {
        BlockPos p = node.getBlockPos();
        long chunkPos = ChunkPos.asLong(SectionPos.blockToSectionCoord(p.getX()), SectionPos.blockToSectionCoord(p.getZ()));
        List<Node> chunkNodes = nodesByChunk.get(chunkPos);
        if (chunkNodes != null) {
            chunkNodes.remove(node);
            if (chunkNodes.isEmpty()) {
                nodesByChunk.remove(chunkPos);
            }
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
        nodesByChunk.clear();
        networkCacheById.clear();
        nodeToNetworkId.clear();
        fowCacheById.clear();
        chunkToNetworkIds.clear();
        dirtyNodePositions.clear();
    }

    // region Sign Linking

    public void linkSignToNode(BlockPos nodePos, BlockPos signPos, Node.LinkType linkType, UUID owner) {
        getNodeAt(nodePos).map(node -> {
            node.setSignPos(signPos.asLong());
            node.setLinkType(linkType);
            if (linkType == Node.LinkType.PRIVATE && owner != null) {
                node.setPrivateOwner(owner);
            }

            signPosToIndex.put(signPos.asLong(), posToIndex.get(nodePos.asLong()));

            refreshNetworkDestinations(node);
            return true;
        });
    }

    public void removeSignLink(BlockPos signPos) {
        getNodeBySignPos(signPos).map(node -> {
            node.unlink();
            signPosToIndex.remove(signPos.asLong());

            if (CommonConfig.logging_enum.ordinal() > 0) ViaRomana.LOGGER.info("Successfully unlinked sign at {}", signPos);

            refreshNetworkDestinations(node);
            return true;
        });
    }

    public Optional<Node> getNodeBySignPos(BlockPos signPos) {
        int idx = signPosToIndex.getOrDefault(signPos.asLong(), -1);
        return idx != -1 ? Optional.of(nodes.get(idx)) : Optional.empty();
    }

    //endregion

    // region Queries & Traversals

    public Optional<Node> getNearestNode(BlockPos origin, double maxDistance) {
        return getNearestNode(origin, maxDistance, maxDistance, node -> true);
    }

    public Optional<Node> getNearestNode(BlockPos origin, double maxDistance, Predicate<Node> filter) {
        return getNearestNode(origin, maxDistance, maxDistance, filter);
    }

    public Optional<Node> getNearestNode(BlockPos origin, double maxDistance, double maxYDistance, Predicate<Node> filter) {
        int originX = origin.getX();
        int originY = origin.getY();
        int originZ = origin.getZ();

        double minDistanceSq = maxDistance * maxDistance;

        int chunkRadius = (int) Math.ceil(maxDistance / 16.0);
        int centerChunkX = SectionPos.blockToSectionCoord(originX);
        int centerChunkZ = SectionPos.blockToSectionCoord(originZ);

        Node bestNode = null;

        for (int x = -chunkRadius; x <= chunkRadius; x++) { //TODO: This is kinda messy
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                int absChunkX = centerChunkX + x;
                int absChunkZ = centerChunkZ + z;
                long chunkKey = ChunkPos.asLong(absChunkX, absChunkZ);
                List<Node> chunkNodes = nodesByChunk.get(chunkKey);

                if (chunkNodes == null || chunkNodes.isEmpty()) continue;

                int chunkMinX = absChunkX << 4;
                int chunkMinZ = absChunkZ << 4;
                int chunkMaxX = chunkMinX + 15;
                int chunkMaxZ = chunkMinZ + 15;

                double dX = 0;
                if (originX < chunkMinX) dX = chunkMinX - originX;
                else if (originX > chunkMaxX) dX = originX - chunkMaxX;

                double dZ = 0;
                if (originZ < chunkMinZ) dZ = chunkMinZ - originZ;
                else if (originZ > chunkMaxZ) dZ = originZ - chunkMaxZ;

                if (dX * dX + dZ * dZ > minDistanceSq) continue;

                for (Node node : chunkNodes) {
                    if (!filter.test(node)) continue;

                    BlockPos nodePos = node.getBlockPos();
                    int dy = Math.abs(nodePos.getY() - originY);
                    if (dy > maxYDistance) continue;

                    double dXCentered = (nodePos.getX() + 0.5) - (originX + 0.5);
                    double dZCentered = (nodePos.getZ() + 0.5) - (originZ + 0.5);
                    double dYCentered = (nodePos.getY() + 0.5) - (originY + 0.5);

                    double distSq = dXCentered * dXCentered + dYCentered * dYCentered + dZCentered * dZCentered;

                    if (distSq <= minDistanceSq) {
                        minDistanceSq = distSq;
                        bestNode = node;
                    }
                }
            }
        }

        return Optional.ofNullable(bestNode);
    }

    public List<Node> getNetwork(Node start) {
        List<Node> network = new ArrayList<>();
        LongOpenHashSet visited = new LongOpenHashSet();
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

    public List<NetworkCache> findNetworksForChunk(ChunkPos chunkPos) {
        processDirtyNodes();

        Set<UUID> networkIds = chunkToNetworkIds.get(chunkPos.toLong());
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

        long[] toProcess = dirtyNodePositions.toLongArray();
        dirtyNodePositions.clear();

        for (long pos : toProcess) {
            if (nodeToNetworkId.containsKey(pos)) {
                continue;
            }

            int idx = posToIndex.getOrDefault(pos, -1);
            if (idx != -1) {
                Node node = nodes.get(idx);
                discoverAndCacheNetwork(node);
            }
        }
    }

    public FoWCache getOrComputeFoWCache(NetworkCache network) {
        boolean isPseudo = ServerMapCache.isPseudoNetwork(network.id());
        return fowCacheById.computeIfAbsent(network.id(), id -> {
            FoWCache fow = calculateFoWData(network.nodePositions(), isPseudo);
            if (fow != null) {
                addNetworkToChunkMap(id, fow);
            }
            return fow;
        });
    }

    private void addNetworkToChunkMap(UUID networkId, FoWCache fow) {
        for (ChunkPos chunk : fow.allowedChunks()) {
            chunkToNetworkIds.computeIfAbsent(chunk.toLong(), c -> new HashSet<>()).add(networkId);
        }
    }

    private void removeNetworkFromChunkMap(UUID networkId, FoWCache fow) {
        for (ChunkPos chunk : fow.allowedChunks()) {
            Set<UUID> ids = chunkToNetworkIds.get(chunk.toLong());
            if (ids != null) {
                ids.remove(networkId);
                if (ids.isEmpty()) {
                    chunkToNetworkIds.remove(chunk.toLong());
                }
            }
        }
    }

    public static FoWCache calculateFoWData(LongSet nodeLongs, boolean isPseudo) {
        if (nodeLongs.isEmpty()) return null;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        LongIterator it = nodeLongs.iterator();

        while (it.hasNext()) {
            long nodeLong = it.nextLong();
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
        List<Node> results = new ArrayList<>();

        int chunkRadius = (int) Math.ceil(radius / 16.0);
        int centerChunkX = SectionPos.blockToSectionCoord(center.getX());
        int centerChunkZ = SectionPos.blockToSectionCoord(center.getZ());

        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                long chunkKey = ChunkPos.asLong(centerChunkX + x, centerChunkZ + z);
                List<Node> chunkNodes = nodesByChunk.get(chunkKey);

                if (chunkNodes == null) continue;

                for (Node node : chunkNodes) {
                    if (node.getBlockPos().distSqr(center) <= radiusSquared) {
                        results.add(node);
                    }
                }
            }
        }
        return results;
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

            if (pos == BlockPos.ZERO.asLong()) continue;

            nodes.add(new Node(nodeTag));
        }
        rebuildIndices();
    }

    public void rebuildIndices() {
        posToIndex.clear();
        signPosToIndex.clear();
        dirtyNodePositions.clear();
        chunkToNetworkIds.clear();
        nodesByChunk.clear();

        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            posToIndex.put(node.getPos(), i);
            final int finalI = i;
            node.getSignPos().ifPresent(sp -> signPosToIndex.put(sp.longValue(), finalI));
            dirtyNodePositions.add(node.getPos());

            long chunkPos = ChunkPos.asLong(SectionPos.blockToSectionCoord(node.getBlockPos().getX()), SectionPos.blockToSectionCoord(node.getBlockPos().getZ()));
            nodesByChunk.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(node);
        }
    }
    //endregion

    public List<DestinationResponseS2C.NodeNetworkInfo> getNodesAsInfo(NetworkCache cache) {
        return cache.getNodesAsInfo(this.posToIndex, this.nodes);
    }

    public List<DestinationResponseS2C.DestinationInfo> getNodesAsDestinationInfo(List<net.rasanovum.viaromana.path.Node> nodes, BlockPos origin) {
        return nodes.stream()
                .map(dest -> {
                    double distance = Math.sqrt(origin.distSqr(dest.getBlockPos()));
                    return new DestinationResponseS2C.DestinationInfo(
                            dest.getBlockPos(),
                            dest.getDestinationName().orElse("Unknown"),
                            distance,
                            dest.getDestinationIcon().orElse(net.rasanovum.viaromana.path.Node.Icon.SIGNPOST)
                    );
                })
                .toList();

    }
}