package net.rasanovum.viaromana.terrain;

import net.minecraft.world.level.ChunkPos;

//? if neoforge {
public interface TerrainSummary {
    ChunkSummary get(ChunkPos pos);
    SimpleBlockPalette getBlockPalette(ChunkPos pos);
    void put(ChunkPos pos, ChunkSummary summary);
}
//?}