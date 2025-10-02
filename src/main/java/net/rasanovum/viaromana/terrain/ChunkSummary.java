package net.rasanovum.viaromana.terrain;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
//? if neoforge
/*import net.rasanovum.viaromana.terrain.LayerSummary.Raw;*/

//? if neoforge {
/*public class ChunkSummary {
    private final Raw layer;
    private final RegistryPalette<Block> palette;
    private final ChunkPos pos; // For debugging/logging

    public ChunkSummary(ChunkPos pos, Raw layer, RegistryPalette<Block> palette) {
        this.pos = pos;
        this.layer = layer;
        this.palette = palette;
    }

    /^*
     * Mirrors Surveyor's toSingleLayer(null, null, maxHeight): Returns the raw single layer.
     ^/
    public Raw toSingleLayer(String structure, String fluid, int maxHeight) {
        // Ignore params as per usage; always return the single layer
        return layer;
    }

    public RegistryPalette<Block> palette() {
        return palette;
    }

    // Getter for pos if needed in logs
    public ChunkPos pos() {
        return pos;
    }
}
*///?}