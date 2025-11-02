package net.rasanovum.viaromana.storage.level;

import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import dev.corgitaco.dataanchor.data.TrackedDataContainer;
import dev.corgitaco.dataanchor.data.type.level.LevelTrackedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.DataInit;

import java.util.Optional;
import java.util.Set;

import static net.rasanovum.viaromana.map.ChunkPixelRenderer.renderChunkPixels;

public class LevelDataManager {
    public static Optional<byte[]> getPixelBytes(ServerLevel level, ChunkPos pos) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return Optional.empty();

        return container.dataAnchor$getTrackedData(DataInit.CHUNK_PIXEL_KEY)
                .filter(data -> data instanceof LevelPixelTrackedData)
                .flatMap(data -> data.getPixelBytes(pos));
    }

    public static void setPixelBytes(ServerLevel level, ChunkPos pos, byte[] bytes) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        container.dataAnchor$createTrackedData();

        container.dataAnchor$getTrackedData(DataInit.CHUNK_PIXEL_KEY)
                .filter(data -> data instanceof LevelPixelTrackedData)
                .ifPresent(data -> data.setPixelBytes(pos, bytes));
    }

    public static Optional<byte[]> getCornerBytes(ServerLevel level, ChunkPos pos) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return Optional.empty();

        return container.dataAnchor$getTrackedData(DataInit.CHUNK_CORNER_KEY)
                .filter(data -> data instanceof LevelCornerTrackedData)
                .flatMap(data -> data.getCornerBytes(pos));
    }

    public static void setCornerBytes(ServerLevel level, ChunkPos pos, byte[] bytes) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        container.dataAnchor$createTrackedData();

        container.dataAnchor$getTrackedData(DataInit.CHUNK_CORNER_KEY)
                .filter(data -> data instanceof LevelCornerTrackedData)
                .ifPresent(data -> data.setCornerBytes(pos, bytes));
    }

    public static void clearPixelBytes(ServerLevel level, ChunkPos pos) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        container.dataAnchor$getTrackedData(DataInit.CHUNK_PIXEL_KEY)
                .filter(data -> data instanceof LevelPixelTrackedData)
                .ifPresent(data -> data.setPixelBytes(pos, null));
    }

    public static void clearPixelBytesForChunks(ServerLevel level, Set<ChunkPos> chunks) {
        long startTime = System.nanoTime();
        for (ChunkPos pos : chunks) {
            clearPixelBytes(level, pos);
        }
        long totalTime = System.nanoTime() - startTime;
        ViaRomana.LOGGER.debug("[PERF] Cleared pixel data for {} chunks in {}ms", chunks.size(), totalTime / 1_000_000.0);
    }

    public static void clearAllPixelBytes(ServerLevel level) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        container.dataAnchor$getTrackedData(DataInit.CHUNK_PIXEL_KEY)
                .filter(data -> data instanceof LevelPixelTrackedData)
                .ifPresent(LevelPixelTrackedData::clearAll);
    }

    public static void clearAllCornerBytes(ServerLevel level) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        container.dataAnchor$getTrackedData(DataInit.CHUNK_CORNER_KEY)
                .filter(data -> data instanceof LevelCornerTrackedData)
                .ifPresent(LevelCornerTrackedData::clearAll);
    }

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
        ViaRomana.LOGGER.debug("[PERF] Regenerated pixel data for {} chunks in {}ms, total size={}KB (256B/chunk)",
                regenerated, totalTime / 1_000_000.0, totalBytes / 1024.0);
    }
}
