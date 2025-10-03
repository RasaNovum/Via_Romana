package net.rasanovum.viaromana.map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelResource;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.path.PathGraph;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Server-side map cache. Marks networks with modified chunks as "dirty"
 * and periodically re-processes them to update map images. Also handles
 * saving and lazy-loading map data to/from disk.
 */
public final class ServerMapCache {
    private static final String MAP_DIR_NAME = "via_romana/network_images";
    private static final Map<UUID, MapInfo> cache = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<ChunkPos>> dirtyNetworks = new ConcurrentHashMap<>();
    private static final Set<UUID> modifiedForSaving = ConcurrentHashMap.newKeySet();

    private static ScheduledExecutorService scheduler;
    private static ExecutorService mapBakingExecutor;
    private static MinecraftServer minecraftServer;

    private ServerMapCache() {}

    public static void init(MinecraftServer server) {
        minecraftServer = server;
        shutdown();

        scheduler = Executors.newScheduledThreadPool(2);
        mapBakingExecutor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 4));
        ViaRomana.LOGGER.info("Starting Via Romana schedulers...");

        scheduler.scheduleAtFixedRate(
                () -> runSafe(ServerMapCache::processAllDirtyNetworks, "Error during scheduled map reprocessing"),
                CommonConfig.map_refresh_interval,
                CommonConfig.map_refresh_interval,
                TimeUnit.SECONDS
        );
        ViaRomana.LOGGER.debug("Scheduled map reprocessing every {} seconds.", CommonConfig.map_refresh_interval);

        scheduler.scheduleAtFixedRate(
                () -> runSafe(() -> saveAllToDisk(false), "Error during scheduled map cache saving"),
                CommonConfig.map_save_interval,
                CommonConfig.map_save_interval,
                TimeUnit.MINUTES
        );
        ViaRomana.LOGGER.debug("Scheduled map saving every {} minutes.", CommonConfig.map_save_interval);
    }

    public static void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (mapBakingExecutor != null && !mapBakingExecutor.isShutdown()) {
            mapBakingExecutor.shutdown();
            try {
                if (!mapBakingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    mapBakingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                mapBakingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if ((scheduler != null && scheduler.isShutdown()) || (mapBakingExecutor != null && mapBakingExecutor.isShutdown())) {
            ViaRomana.LOGGER.info("Via Romana schedulers shut down.");
        }
    }

    public static void markChunkDirty(ServerLevel level, ChunkPos pos) {
        PathGraph graph = PathGraph.getInstance(level);
        if (graph == null) return;

        graph.findNetworksForChunk(pos).forEach(network -> {
            UUID networkId = network.id();
            dirtyNetworks.computeIfAbsent(networkId, k -> ConcurrentHashMap.newKeySet()).add(pos);
        });
    }

    public static void processAllDirtyNetworks() {
        if (dirtyNetworks.isEmpty()) {
            return;
        }

        long startTime = System.nanoTime();
        Map<UUID, Set<ChunkPos>> toProcess = new ConcurrentHashMap<>(dirtyNetworks);
        dirtyNetworks.clear();

        int totalDirtyChunks = toProcess.values().stream().mapToInt(Set::size).sum();
        ViaRomana.LOGGER.info("[PERF] Processing {} dirty networks ({} chunks) in scheduled update batch", 
            toProcess.size(), totalDirtyChunks);

        toProcess.forEach((networkId, chunks) -> {
            if (chunks != null && !chunks.isEmpty()) {
                minecraftServer.execute(() -> updateOrGenerateMapAsync(networkId, chunks));
            }
        });
    }

    private static CompletableFuture<MapInfo> updateOrGenerateMapAsync(UUID networkId, Collection<ChunkPos> chunksToUpdate) {
        return findLevelForNetwork(networkId)
                .map(level -> {
                    PathGraph graph = PathGraph.getInstance(level);
                    PathGraph.NetworkCache network = (graph != null) ? graph.getNetworkCache(networkId) : null;

                    if (network == null) {
                        ViaRomana.LOGGER.warn("Could not find network cache for network {}, aborting update.", networkId);
                        return CompletableFuture.<MapInfo>completedFuture(null);
                    }

                    return CompletableFuture.supplyAsync(() -> {
                        MapBakeWorker worker = new MapBakeWorker();
                        MapInfo previousResult = cache.get(networkId);
                        MapInfo newResult;

                        if (previousResult != null && previousResult.hasImageData()) {
                            ViaRomana.LOGGER.debug("Performing incremental update for network {}.", networkId);
                            newResult = worker.updateMap(previousResult, new HashSet<>(chunksToUpdate), level, network);
                        } else {
                            ViaRomana.LOGGER.debug("Performing full bake for network {}.", networkId);
                            newResult = worker.bake(networkId, level, network.getMin(), network.getMax(), graph.getNodesAsInfo(network));
                        }

                        cache.put(networkId, newResult);
                        modifiedForSaving.add(networkId);
                        ViaRomana.LOGGER.debug("Map update completed for network {}.", networkId);
                        return newResult;
                    }, mapBakingExecutor).exceptionally(ex -> {
                        ViaRomana.LOGGER.error("Failed during map update for network {}", networkId, ex);
                        return null;
                    });
                })
                .orElseGet(() -> {
                    ViaRomana.LOGGER.warn("Could not find level for network {}, aborting update.", networkId);
                    return CompletableFuture.completedFuture(null);
                });
    }

    private static Optional<ServerLevel> findLevelForNetwork(UUID networkId) {
        for (ServerLevel level : minecraftServer.getAllLevels()) {
            PathGraph graph = PathGraph.getInstance(level);
            if (graph != null && graph.getNetworkCache(networkId) != null) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }

    public static Optional<MapInfo> getMapData(UUID networkId) {
        MapInfo result = cache.get(networkId);
        if (result != null) {
            return Optional.of(result);
        }
        return loadFromDisk(networkId);
    }

    public static CompletableFuture<MapInfo> generateMapIfNeeded(UUID networkId, ServerLevel level) {
        return getMapData(networkId)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> {
                    ViaRomana.LOGGER.debug("Generating initial map for network {} (client request).", networkId);
                    PathGraph graph = PathGraph.getInstance(level);
                    if (graph != null) {
                        PathGraph.NetworkCache network = graph.getNetworkCache(networkId);
                        if (network != null) {
                            Set<ChunkPos> allChunks = graph.getNodesAsInfo(network).stream()
                                    .map(node -> new ChunkPos(node.position))
                                    .collect(Collectors.toSet());
                            return updateOrGenerateMapAsync(networkId, allChunks);
                        }
                    }
                    ViaRomana.LOGGER.warn("Could not find network to generate map for networkId {}", networkId);
                    return CompletableFuture.completedFuture(null);
                });
    }

    private static Optional<MapInfo> loadFromDisk(UUID networkId) {
        long startTime = System.nanoTime();
        try {
            Path mapDir = getMapDirectory();
            String base = "network-" + networkId;
            Path pngPath = mapDir.resolve(base + ".png");
            Path metaPath = mapDir.resolve(base + ".nbt");
            byte[] png;
            CompoundTag tag;

            try (InputStream pngStream = Files.newInputStream(pngPath);
                 InputStream metaStream = Files.newInputStream(metaPath)) {
                png = pngStream.readAllBytes();
                //? if <1.21 {
                /*tag = NbtIo.readCompressed(metaStream);
                 *///?} else {
                tag = NbtIo.readCompressed(metaStream, new NbtAccounter(Long.MAX_VALUE, Integer.MAX_VALUE));
                //?}
            } catch (NoSuchFileException e) {
                return Optional.empty();
            }

            BlockPos min = BlockPos.of(tag.getLong("min"));
            BlockPos max = BlockPos.of(tag.getLong("max"));
            int scale = tag.getInt("scale");

            MapInfo info = MapInfo.fromServerCache(networkId, min, max, List.of(), png, scale, List.of());
            cache.put(networkId, info);
            long loadTime = System.nanoTime() - startTime;
            ViaRomana.LOGGER.info("[PERF] Loaded map {} from disk: {}ms, size={}KB", 
                networkId, loadTime / 1_000_000.0, png.length / 1024.0);
            return Optional.of(info);

        } catch (IOException e) {
            ViaRomana.LOGGER.error("Failed to load map {} from disk", networkId, e);
            return Optional.empty();
        }
    }

    public static void invalidate(UUID networkId) {
        dirtyNetworks.remove(networkId);
        cache.remove(networkId);
        modifiedForSaving.remove(networkId);

        try {
            Path mapDir = getMapDirectory();
            String base = "network-" + networkId;
            boolean pngDeleted = Files.deleteIfExists(mapDir.resolve(base + ".png"));
            boolean nbtDeleted = Files.deleteIfExists(mapDir.resolve(base + ".nbt"));
            if (pngDeleted || nbtDeleted) {
                ViaRomana.LOGGER.debug("Deleted map files from disk for network {}", networkId);
            }
        } catch (IOException e) {
            ViaRomana.LOGGER.error("Failed to delete map files from disk for network {}", networkId, e);
        }
    }

    public static void clear() {
        cache.clear();
        dirtyNetworks.clear();
        modifiedForSaving.clear();
    }

    public static void saveAllToDisk(boolean forceSave) {
        if (modifiedForSaving.isEmpty() && !forceSave) {
            return;
        }

        Set<UUID> networksToSave = forceSave ? new HashSet<>(cache.keySet()) : new HashSet<>(modifiedForSaving);
        if (networksToSave.isEmpty()) return;

        long startTime = System.nanoTime();
        ViaRomana.LOGGER.debug("Saving {} maps to disk...", networksToSave.size());

        try {
            Path mapDir = getMapDirectory();
            Files.createDirectories(mapDir);
            int savedCount = 0;
            long totalBytes = 0;

            for (UUID id : networksToSave) {
                MapInfo info = cache.get(id);

                if (info == null || !info.hasImageData()) {
                    invalidate(id);
                    continue;
                }

                String base = "network-" + id;
                Path pngPath = mapDir.resolve(base + ".png");
                Path nbtPath = mapDir.resolve(base + ".nbt");

                CompoundTag tag = new CompoundTag();
                tag.putLong("min", info.minBounds().asLong());
                tag.putLong("max", info.maxBounds().asLong());
                tag.putInt("scale", info.bakeScaleFactor());
                if (info.createdAtMs() != null) {
                    tag.putLong("createdAt", info.createdAtMs());
                }

                try (OutputStream pngOut = Files.newOutputStream(pngPath);
                     OutputStream nbtOut = Files.newOutputStream(nbtPath)) {
                    pngOut.write(info.pngData());
                    NbtIo.writeCompressed(tag, nbtOut);
                    savedCount++;
                    totalBytes += info.pngData().length;
                } catch (IOException e) {
                    ViaRomana.LOGGER.error("Failed to write map files for network {}", id, e);
                }
            }

            long saveTime = System.nanoTime() - startTime;
            if (savedCount > 0) {
                ViaRomana.LOGGER.info("[PERF] Saved {} maps to disk: {}ms, total={}KB, avg={}KB/map", 
                    savedCount, saveTime / 1_000_000.0, totalBytes / 1024.0, 
                    savedCount > 0 ? (totalBytes / savedCount) / 1024.0 : 0);
            }
            modifiedForSaving.removeAll(networksToSave);

        } catch (IOException e) {
            ViaRomana.LOGGER.error("Failed to save map cache to disk.", e);
        }
    }

    public static void deleteAllMapsFromDisk() {
        clear();
        try {
            Path mapDir = getMapDirectory();
            if (Files.exists(mapDir)) {
                try (var stream = Files.walk(mapDir)) {
                    stream.sorted(java.util.Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(java.io.File::delete);
                }
                ViaRomana.LOGGER.debug("Deleted all map files from disk.");
            }
        } catch (IOException e) {
            ViaRomana.LOGGER.error("Failed to delete map directory from disk.", e);
        }
    }

    /**
     * Clears chunk PNG data for all networks in the cache.
     */
    public static void clearAllChunkPngData() {
        long startTime = System.nanoTime();
        int totalChunks = 0;
        
        for (ServerLevel level : minecraftServer.getAllLevels()) {
            PathGraph graph = PathGraph.getInstance(level);
            if (graph == null) continue;

            Set<ChunkPos> allChunks = new HashSet<>();
            for (UUID networkId : cache.keySet()) {
                PathGraph.NetworkCache network = graph.getNetworkCache(networkId);
                if (network != null) {
                    PathGraph.FoWCache fowCache = graph.getOrComputeFoWCache(network);
                    if (fowCache != null) {
                        allChunks.addAll(fowCache.allowedChunks());
                    }
                }
            }
            
            if (!allChunks.isEmpty()) {
                ChunkPngUtil.clearPngBytesForChunks(level, allChunks);
                totalChunks += allChunks.size();
            }
        }
        
        long totalTime = System.nanoTime() - startTime;
        ViaRomana.LOGGER.info("[PERF] Cleared all chunk PNG data: {} chunks in {}ms", 
            totalChunks, totalTime / 1_000_000.0);
    }

    /**
     * Regenerates chunk PNG data for all dirty networks.
     */
    public static void regenerateAllChunkPngData() {
        long startTime = System.nanoTime();
        int totalChunks = 0;
        
        for (ServerLevel level : minecraftServer.getAllLevels()) {
            PathGraph graph = PathGraph.getInstance(level);
            if (graph == null) continue;

            Set<ChunkPos> allChunks = new HashSet<>();
            for (Map.Entry<UUID, Set<ChunkPos>> entry : dirtyNetworks.entrySet()) {
                UUID networkId = entry.getKey();
                PathGraph.NetworkCache network = graph.getNetworkCache(networkId);
                if (network != null) {
                    PathGraph.FoWCache fowCache = graph.getOrComputeFoWCache(network);
                    if (fowCache != null) {
                        allChunks.addAll(fowCache.allowedChunks());
                    }
                }
            }
            
            if (!allChunks.isEmpty()) {
                ChunkPngUtil.regeneratePngBytesForChunks(level, allChunks);
                totalChunks += allChunks.size();
            }
        }
        
        long totalTime = System.nanoTime() - startTime;
        ViaRomana.LOGGER.info("[PERF] Regenerated all chunk PNG data: {} chunks in {}ms", 
            totalChunks, totalTime / 1_000_000.0);
    }

    private static Path getMapDirectory() {
        return minecraftServer.getWorldPath(LevelResource.ROOT).resolve(MAP_DIR_NAME);
    }

    private static void runSafe(Runnable task, String errorMessage) {
        try {
            task.run();
        } catch (Exception e) {
            ViaRomana.LOGGER.error(errorMessage, e);
        }
    }
}