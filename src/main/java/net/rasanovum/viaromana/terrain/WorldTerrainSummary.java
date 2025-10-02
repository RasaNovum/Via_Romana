package net.rasanovum.viaromana.terrain;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;

//? if neoforge {
/*public class WorldTerrainSummary {
    private final ConcurrentHashMap<ChunkPos, ChunkSummary> cache = new ConcurrentHashMap<>();
    private final ServerLevel level;
    private final int maxCacheSize; // Stub for config; default 10000

    public WorldTerrainSummary(ServerLevel level, int maxCacheSize) {
        this.level = level;
        this.maxCacheSize = maxCacheSize;
    }

    /^*
     * Mirrors terrain.get(chunkPos): Get or compute summary.
     ^/
    public ChunkSummary get(ChunkPos pos) {
        return cache.computeIfAbsent(pos, p -> createSummary(p));
    }

    /^*
     * Stub: In Phase 2, this will compute from chunk sections.
     * For now, returns empty (air-only) summary to avoid NPEs in rendering.
     ^/
    private ChunkSummary createSummary(ChunkPos pos) {
        // Empty raw: All exists=false, depths=0, blocks=0 (air), water=0
        BitSet exists = new BitSet(256);
        int[] depths = new int[256];
        int[] blocks = new int[256]; // 0 = air ID
        int[] waterDepths = new int[256];
        LayerSummary.Raw raw = new LayerSummary.Raw(exists, depths, blocks, waterDepths);

        RegistryPalette<Block> palette = new RegistryPalette<>(BuiltInRegistries.BLOCK);
        palette.findOrAdd(0); // Air

        return new ChunkSummary(pos, raw, palette);
    }

    /^*
     * Put/update a summary (e.g., after refresh).
     ^/
    public void put(ChunkPos pos, ChunkSummary summary) {
        cache.put(pos, summary);
        pruneIfNeeded();
    }

    /^*
     * Mirrors terrain.getBlockPalette(pos): Return palette for the chunk.
     ^/
    public RegistryPalette<Block> getBlockPalette(ChunkPos pos) {
        ChunkSummary summary = get(pos); // Triggers lazy if needed
        // Return the palette from the summary
        return summary.palette();
    }

    private void pruneIfNeeded() {
        if (cache.size() > maxCacheSize) {
            // Simple LRU stub: Remove oldest (iterate and remove first half if over)
            // Full LRU in Phase 4 if needed
            cache.entrySet().iterator().remove(); // Placeholder: Remove one
        }
    }

    // For debugging
    public int cacheSize() {
        return cache.size();
    }
}
*///?}