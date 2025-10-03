package net.rasanovum.viaromana.init;

import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import dev.corgitaco.dataanchor.data.registry.TrackedDataKey;
import net.minecraft.server.level.ServerLevel;
import net.rasanovum.viaromana.map.LevelPngTrackedData;
import net.rasanovum.viaromana.util.VersionUtils;

public class MapInit {
        public static final TrackedDataKey<LevelPngTrackedData> CHUNK_PNG_KEY =
                TrackedDataRegistries.LEVEL.register(
                        VersionUtils.getLocation("viaromana:chunk_png"),
                        LevelPngTrackedData.class,
                        (key, level) -> {
                                if (level instanceof ServerLevel serverLevel) {
                                        return new LevelPngTrackedData(key, serverLevel);
                                }
                                return null;
                        }
                );
}