package net.rasanovum.viaromana.map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.path.PathGraph.FoWCache;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class MapBaker {
    /**
     * Asynchronously bakes a full map image for a network ID.
     * 
     * @param networkId The network UUID
     * @param level The server level
     * @param executor The executor to run the task on
     * @return CompletableFuture that completes with the baked MapInfo
     */
    public static CompletableFuture<MapInfo> bakeAsync(
            UUID networkId, 
            ServerLevel level, 
            ExecutorService executor) {
        return CompletableFuture.supplyAsync(
            () -> bake(networkId, level),
            executor
        );
    }

    /**
     * Bakes a full map image for the given network ID using its FoW data and node information.
     * 
     * @param networkId The network UUID
     * @param level The server level
     * @return MapInfo object containing the baked map data.
     */
    public static MapInfo bake(UUID networkId, ServerLevel level) {
        ViaRomana.LOGGER.debug("[PERF] Starting full map bake for network {}", networkId);

        long bakeStartTime = System.nanoTime();

        PathGraph graph = PathGraph.getInstance(level);
        if (graph == null) throw new IllegalStateException("PathGraph is null");
        
        PathGraph.NetworkCache network = graph.getNetworkCache(networkId);
        if (network == null) throw new IllegalStateException("Network cache not found for " + networkId);
        
        PathGraph.FoWCache fowCache = graph.getOrComputeFoWCache(network);
        if (fowCache == null) throw new IllegalStateException("FoW cache is null");

        boolean isPseudo = ServerMapCache.isPseudoNetwork(networkId);
        Set<ChunkPos> bakeChunks = fowCache.allowedChunks();
        ChunkPos bakeMinChunk = fowCache.minChunk();
        ChunkPos bakeMaxChunk = fowCache.maxChunk();
        BlockPos desiredMinBlock = fowCache.minBlock();
        BlockPos desiredMaxBlock = fowCache.maxBlock();

        ViaRomana.LOGGER.debug("Chunks Allowed: {}", bakeChunks.size());

        Set<ChunkPos> mapChunks = new HashSet<>();
        for (int cx = bakeMinChunk.x; cx <= bakeMaxChunk.x; cx++) {
            for (int cz = bakeMinChunk.z; cz <= bakeMaxChunk.z; cz++) {
                mapChunks.add(new ChunkPos(cx, cz));
            }
        }

        int fullChunkWidth = (bakeMaxChunk.x - bakeMinChunk.x + 1) * 16;
        int fullChunkHeight = (bakeMaxChunk.z - bakeMinChunk.z + 1) * 16;
        int scaleFactor = MapPixelAssembler.calculateScaleFactor(fullChunkWidth, fullChunkHeight);

        long renderStartTime = System.nanoTime();
        int fullPixelWidth = fullChunkWidth / scaleFactor;
        int fullPixelHeight = fullChunkHeight / scaleFactor;
        byte[] biomePixels = new byte[fullPixelWidth * fullPixelHeight];
        byte[] chunkPixels = new byte[fullPixelWidth * fullPixelHeight];
        
        int chunksWithData = MapPixelAssembler.processChunkPixels(biomePixels, chunkPixels, level, mapChunks, bakeChunks, scaleFactor, fullPixelWidth, fullPixelHeight, bakeMinChunk, isPseudo);
        long renderTime = System.nanoTime() - renderStartTime;

        if (isPseudo) {
            long totalBakeTime = System.nanoTime() - bakeStartTime;
            ViaRomana.LOGGER.debug("[PERF] Pseudonetwork {} pre-processing completed: total={}ms, render={}ms, " +
                "dimensions={}x{} (full FoW), scale={}, chunks={}",
                networkId, totalBakeTime / 1_000_000.0, renderTime / 1_000_000.0,
                fullPixelWidth, fullPixelHeight, scaleFactor, chunksWithData);
            
            return MapInfo.fromServer(networkId, new byte[0], new byte[0], 0, 0, 1, 0, 0, 0, 0, new ArrayList<>(bakeChunks), new ArrayList<>());
        }

        int desiredMinX = desiredMinBlock.getX();
        int desiredMinZ = desiredMinBlock.getZ();
        int desiredMaxX = desiredMaxBlock.getX();
        int desiredMaxZ = desiredMaxBlock.getZ();
        
        int bakedMinX = bakeMinChunk.x * 16;
        int bakedMinZ = bakeMinChunk.z * 16;
        
        int startPixelX = (desiredMinX - bakedMinX) / scaleFactor;
        int startPixelZ = (desiredMinZ - bakedMinZ) / scaleFactor;
        int croppedPixelWidth = ((desiredMaxX - desiredMinX + 1) / scaleFactor);
        int croppedPixelHeight = ((desiredMaxZ - desiredMinZ + 1) / scaleFactor);
        
        // Clamp to valid ranges
        int effectiveStartX = Math.max(0, startPixelX);
        int effectiveStartZ = Math.max(0, startPixelZ);
        int effectiveWidth = Math.min(croppedPixelWidth, fullPixelWidth - effectiveStartX);
        int effectiveHeight = Math.min(croppedPixelHeight, fullPixelHeight - effectiveStartZ);
        
        byte[] croppedBiomePixels = new byte[effectiveWidth * effectiveHeight];
        byte[] croppedChunkPixels = new byte[effectiveWidth * effectiveHeight];

        // Copy relevant region to cropped arrays
        for (int z = 0; z < effectiveHeight; z++) {
            int srcZ = effectiveStartZ + z;
            if (srcZ >= fullPixelHeight) break;
            int srcIdx = srcZ * fullPixelWidth + effectiveStartX;
            int dstIdx = z * effectiveWidth;
            int copyWidth = Math.min(effectiveWidth, fullPixelWidth - effectiveStartX);
            System.arraycopy(biomePixels, srcIdx, croppedBiomePixels, dstIdx, copyWidth);
            System.arraycopy(chunkPixels, srcIdx, croppedChunkPixels, dstIdx, copyWidth);
        }

        // Return with chunk-aligned world coordinates from FoW
        long totalBakeTime = System.nanoTime() - bakeStartTime;
        
        ViaRomana.LOGGER.debug("[PERF] Map bake completed for network {}: total={}ms, render={}ms, " +
            "dimensions={}x{}, scale={}, rawSize={}KB, chunks={}",
            networkId, totalBakeTime / 1_000_000.0, renderTime / 1_000_000.0,
            effectiveWidth, effectiveHeight, scaleFactor, (croppedBiomePixels.length + croppedChunkPixels.length) / 1024.0,
            chunksWithData);

        return MapInfo.fromServer(networkId, croppedBiomePixels, croppedChunkPixels, effectiveWidth, effectiveHeight, scaleFactor, desiredMinX, desiredMinZ, desiredMaxX, desiredMaxZ, new ArrayList<>(bakeChunks), graph.getNodesAsInfo(network));
    }


    /**
     * Rebake network to update dirty chunks
     * TODO: Re-implement image array splicing
     */
    public MapInfo updateMap(MapInfo previousResult, Set<ChunkPos> dirtyChunks, ServerLevel level) {
        ViaRomana.LOGGER.debug("Incremental update requested for {} dirty chunks, performing rebake.", dirtyChunks.size());
        return bake(previousResult.networkId(), level);
    }
}