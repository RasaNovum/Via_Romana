package net.rasanovum.viaromana.storage.level;

import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import dev.corgitaco.dataanchor.data.TrackedDataContainer;
import dev.corgitaco.dataanchor.data.type.level.LevelTrackedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.MapInit;

import java.util.Optional;
import java.util.Set;

import static net.rasanovum.viaromana.map.ChunkPixelUtil.renderChunkPixels;

public class LevelDataManager {
    /**
     * Gets corner bytes from Data Anchor.
     */
    public static Optional<byte[]> getPixelBytes(ServerLevel level, ChunkPos pos) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return Optional.empty();

        return container.dataAnchor$getTrackedData(MapInit.CHUNK_PIXEL_KEY)
                .filter(data -> data instanceof LevelPixelTrackedData)
                .flatMap(data -> data.getPixelBytes(pos));
    }

    /**
     * Sets pixel bytes to Data Anchor.
     */
    public static void setPixelBytes(ServerLevel level, ChunkPos pos, byte[] bytes) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        container.dataAnchor$createTrackedData();

        container.dataAnchor$getTrackedData(MapInit.CHUNK_PIXEL_KEY)
                .filter(data -> data instanceof LevelPixelTrackedData)
                .ifPresent(data -> data.setPixelBytes(pos, bytes));
    }

    /**
     * Gets corner bytes from Data Anchor.
     */
    public static Optional<byte[]> getCornerBytes(ServerLevel level, ChunkPos pos) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return Optional.empty();

        return container.dataAnchor$getTrackedData(MapInit.CHUNK_CORNER_KEY)
                .filter(data -> data instanceof LevelCornerTrackedData)
                .flatMap(data -> data.getCornerBytes(pos));
    }

    /**
     * Sets corner bytes to Data Anchor.
     */
    public static void setCornerBytes(ServerLevel level, ChunkPos pos, byte[] bytes) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        container.dataAnchor$createTrackedData();

        container.dataAnchor$getTrackedData(MapInit.CHUNK_CORNER_KEY)
                .filter(data -> data instanceof LevelCornerTrackedData)
                .ifPresent(data -> data.setCornerBytes(pos, bytes));
    }

    /**
     * Clears pixel bytes from Data Anchor.
     */
    public static void clearPixelBytes(ServerLevel level, ChunkPos pos) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        container.dataAnchor$getTrackedData(MapInit.CHUNK_PIXEL_KEY)
                .filter(data -> data instanceof LevelPixelTrackedData)
                .ifPresent(data -> data.setPixelBytes(pos, null));
    }

    /**
     * Clears pixel bytes for all chunks in the given set.
     */
    public static void clearPixelBytesForChunks(ServerLevel level, Set<ChunkPos> chunks) {
        long startTime = System.nanoTime();
        for (ChunkPos pos : chunks) {
            clearPixelBytes(level, pos);
        }
        long totalTime = System.nanoTime() - startTime;
        ViaRomana.LOGGER.info("[PERF] Cleared pixel data for {} chunks in {}ms", chunks.size(), totalTime / 1_000_000.0);
    }

    /**
     * Regenerates pixel bytes for all chunks in the given set.
     */
    public static void regeneratePixelBytesForChunks(ServerLevel level, Set<ChunkPos> chunks) {
        long startTime = System.nanoTime();
        int regenerated = 0;

        for (ChunkPos pos : chunks) {
            byte[] newBytes = renderChunkPixels(level, pos);
            if (newBytes.length == 256) {
                setPixelBytes(level, pos, newBytes);
                regenerated++;
            }
        }

        long totalTime = System.nanoTime() - startTime;
        long totalBytes = regenerated * 256L;
        ViaRomana.LOGGER.info("[PERF] Regenerated pixel data for {} chunks in {}ms, total size={}KB (256B/chunk)",
                regenerated, totalTime / 1_000_000.0, totalBytes / 1024.0);
    }
}
