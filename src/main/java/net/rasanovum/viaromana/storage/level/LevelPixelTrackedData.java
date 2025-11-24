package net.rasanovum.viaromana.storage.level;

import dev.corgitaco.dataanchor.data.registry.TrackedDataKey;
import dev.corgitaco.dataanchor.data.type.level.ServerLevelTrackedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Server-side tracked data for chunk raw pixels.
 * Stores MapColor packed IDs (color & brightness).
 */
public class LevelPixelTrackedData extends ServerLevelTrackedData {
    private final Map<String, byte[]> pixelMap = new HashMap<>();

    public LevelPixelTrackedData(TrackedDataKey<? extends ServerLevelTrackedData> trackedDataKey, ServerLevel level) {
        super((TrackedDataKey<ServerLevelTrackedData>) trackedDataKey, level);
    }

    @Override
    public CompoundTag save() {
        if (pixelMap.isEmpty()) return null;
        CompoundTag tag = new CompoundTag();
        tag.put("pixel_map", toNBT(pixelMap));
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains("pixel_map")) {
            pixelMap.clear();
            pixelMap.putAll(fromNBT(tag.getCompound("pixel_map")));
        }
    }

    public void setPixelBytes(ChunkPos pos, byte[] bytes) {
        String key = pos.x + "_" + pos.z;
        if (bytes != null && bytes.length == 256) {
            pixelMap.put(key, bytes.clone());
        } else {
            pixelMap.remove(key);
        }
        markDirty();
    }

    public boolean isChunkTracked(ChunkPos pos) {
        return pixelMap.containsKey(pos.x + "_" + pos.z);
    }

    public Optional<byte[]> getPixelBytes(ChunkPos pos) {
        String key = pos.x + "_" + pos.z;
        byte[] bytes = pixelMap.get(key);
        return bytes != null && bytes.length == 256 ? Optional.of(bytes.clone()) : Optional.empty();
    }

    private static CompoundTag toNBT(Map<String, byte[]> map) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            tag.putByteArray(entry.getKey(), entry.getValue());
        }
        return tag;
    }

    private static Map<String, byte[]> fromNBT(CompoundTag tag) {
        Map<String, byte[]> map = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            map.put(key, tag.getByteArray(key));
        }
        return map;
    }

    public void clearAll() {
        pixelMap.clear();
        markDirty();
    }
}