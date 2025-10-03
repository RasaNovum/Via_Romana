package net.rasanovum.viaromana.init;

import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import dev.corgitaco.dataanchor.data.registry.TrackedDataKey;
import net.minecraft.server.level.ServerLevel;
import net.rasanovum.viaromana.map.LevelPixelTrackedData;
import net.rasanovum.viaromana.util.VersionUtils;

public class MapInit {
        public static final TrackedDataKey<LevelPixelTrackedData> CHUNK_PIXEL_KEY =
                TrackedDataRegistries.LEVEL.register(
                        VersionUtils.getLocation("viaromana:chunk_pixels"),
                        LevelPixelTrackedData.class,
                        (key, level) -> {
                                if (level instanceof ServerLevel serverLevel) {
                                        return new LevelPixelTrackedData(key, serverLevel);
                                }
                                return null;
                        }
                );
}