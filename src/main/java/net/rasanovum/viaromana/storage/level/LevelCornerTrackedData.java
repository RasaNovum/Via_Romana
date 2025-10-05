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
 * Server-side tracked data for chunk corner biome data.
 * Stores packed IDs for the 4 corners (byte[4]).
 */
public class LevelCornerTrackedData extends ServerLevelTrackedData {
    private final Map<String, byte[]> cornerMap = new HashMap<>();

    public LevelCornerTrackedData(TrackedDataKey<? extends ServerLevelTrackedData> trackedDataKey, ServerLevel level) {
        super((TrackedDataKey<ServerLevelTrackedData>) trackedDataKey, level);
    }

    @Override
    public CompoundTag save() {
        if (cornerMap.isEmpty()) return null;
        CompoundTag tag = new CompoundTag();
        tag.put("corner_map", toNBT(cornerMap));
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains("corner_map")) {
            cornerMap.clear();
            cornerMap.putAll(fromNBT(tag.getCompound("corner_map")));
        }
    }

    public void setCornerBytes(ChunkPos pos, byte[] bytes) {
        String key = pos.x + "_" + pos.z;
        if (bytes != null && bytes.length == 4) {
            cornerMap.put(key, bytes.clone());
        } else {
            cornerMap.remove(key);
        }
        markDirty();
    }

    public Optional<byte[]> getCornerBytes(ChunkPos pos) {
        String key = pos.x + "_" + pos.z;
        byte[] bytes = cornerMap.get(key);
        return bytes != null && bytes.length == 4 ? Optional.of(bytes.clone()) : Optional.empty();
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
}