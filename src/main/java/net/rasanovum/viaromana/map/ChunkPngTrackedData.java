package net.rasanovum.viaromana.map;

import dev.corgitaco.dataanchor.data.registry.TrackedDataKey;
import dev.corgitaco.dataanchor.data.type.chunk.ServerLevelChunkTrackedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Server-side tracked data for chunk PNG bytes.
 */
public class ChunkPngTrackedData extends ServerLevelChunkTrackedData {
    private byte[] pngBytes = new byte[0];

    public ChunkPngTrackedData(TrackedDataKey<? extends ServerLevelChunkTrackedData> trackedDataKey, LevelChunk chunk) {
        super(trackedDataKey, chunk);
    }

    @Override
    public @Nullable CompoundTag save() {
        if (pngBytes.length == 0) return null;
        CompoundTag tag = new CompoundTag();
        tag.putByteArray("png_bytes", pngBytes);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains("png_bytes", net.minecraft.nbt.CompoundTag.TAG_BYTE_ARRAY)) {
            pngBytes = tag.getByteArray("png_bytes");
        }
    }

    public void setPngBytes(byte[] bytes) {
        this.pngBytes = bytes != null ? bytes.clone() : new byte[0];
        this.markDirty();
    }

    public Optional<byte[]> getPngBytes() {
        return pngBytes.length > 0 ? Optional.of(pngBytes.clone()) : Optional.empty();
    }
}