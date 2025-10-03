package net.rasanovum.viaromana.init;

import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import dev.corgitaco.dataanchor.data.registry.TrackedDataKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.rasanovum.viaromana.map.ChunkPngTrackedData;
import net.rasanovum.viaromana.util.VersionUtils;

public class MapInit {
        public static final TrackedDataKey<ChunkPngTrackedData> CHUNK_PNG_KEY =
                TrackedDataRegistries.CHUNK.register(
                        ResourceLocation.fromNamespaceAndPath("viaromana", "chunk_png"),
                        ChunkPngTrackedData.class,
                        (key, chunkAccess) -> {
                                if (chunkAccess instanceof LevelChunk lc) {
                                        return new ChunkPngTrackedData(key, lc);
                                }
                                return null;
                        }
                );
}