package net.rasanovum.viaromana.storage.level;

import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import dev.corgitaco.dataanchor.data.TrackedDataContainer;
import dev.corgitaco.dataanchor.data.type.level.LevelTrackedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.DataInit;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

import static net.rasanovum.viaromana.map.ChunkPixelRenderer.renderChunkPixels;

public class LevelDataManager {
    public static boolean isPixelChunkTracked(ServerLevel level, ChunkPos pos) {
        TrackedDataContainer<Level, LevelTrackedData> container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return false;

        var dataOpt = container.dataAnchor$getTrackedData(DataInit.CHUNK_PIXEL_KEY);

        if (dataOpt.isPresent()) {
            var data = dataOpt.get();
            if (data instanceof LevelPixelTrackedData) {
                return data.isChunkTracked(pos);
            }
        }

        return false;
    }

    public static byte @Nullable [] getPixelBytes(ServerLevel level, ChunkPos pos) {
        var container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return null;

        var dataOpt = container.dataAnchor$getTrackedData(DataInit.CHUNK_PIXEL_KEY);

        if (dataOpt.isPresent()) {
            Object dataObj = dataOpt.get();
            if (dataObj instanceof LevelPixelTrackedData data) {
                var bytesOpt = data.getPixelBytes(pos);
                return bytesOpt.orElse(null);
            }
        }

        return null;
    }

    public static void setPixelBytes(ServerLevel level, ChunkPos pos, byte[] bytes) {
        var container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        var dataOpt = container.dataAnchor$getTrackedData(DataInit.CHUNK_PIXEL_KEY);

        if (dataOpt.isPresent() && dataOpt.get() instanceof LevelPixelTrackedData) {
            LevelPixelTrackedData pixelData = dataOpt.get();
            pixelData.setPixelBytes(pos, bytes);
            return;
        }

        // Slow path
        container.dataAnchor$createTrackedData();
        var newDataOpt = container.dataAnchor$getTrackedData(DataInit.CHUNK_PIXEL_KEY);

        if (newDataOpt.isPresent() && newDataOpt.get() instanceof LevelPixelTrackedData) {
            LevelPixelTrackedData pixelData = newDataOpt.get();
            pixelData.setPixelBytes(pos, bytes);
        }
    }

    public static byte @Nullable [] getCornerBytes(ServerLevel level, ChunkPos pos) {
        var container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return null;

        var dataOpt = container.dataAnchor$getTrackedData(DataInit.CHUNK_CORNER_KEY);

        if (dataOpt.isPresent()) {
            var data = dataOpt.get();
            if (data instanceof LevelCornerTrackedData) {
                return data.getCornerBytes(pos).orElse(null);
            }
        }

        return null;
    }

    public static void setCornerBytes(ServerLevel level, ChunkPos pos, byte[] bytes) {
        var container = TrackedDataRegistries.LEVEL.getContainer(level);
        if (container == null) return;

        var dataOpt = container.dataAnchor$getTrackedData(DataInit.CHUNK_CORNER_KEY);

        if (dataOpt.isPresent() && dataOpt.get() instanceof LevelCornerTrackedData) {
            LevelCornerTrackedData pixelData = dataOpt.get();
            pixelData.setCornerBytes(pos, bytes);
            return;
        }

        // Slow path
        container.dataAnchor$createTrackedData();
        var newDataOpt = container.dataAnchor$getTrackedData(DataInit.CHUNK_CORNER_KEY);

        if (newDataOpt.isPresent() && newDataOpt.get() instanceof LevelCornerTrackedData) {
            LevelCornerTrackedData pixelData = newDataOpt.get();
            pixelData.setCornerBytes(pos, bytes);
        }
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
