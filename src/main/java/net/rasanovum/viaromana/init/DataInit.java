package net.rasanovum.viaromana.init;

import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import dev.corgitaco.dataanchor.data.registry.TrackedDataKey;
import net.minecraft.server.level.ServerLevel;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.storage.level.LevelCornerTrackedData;
import net.rasanovum.viaromana.storage.level.LevelPixelTrackedData;
import net.rasanovum.viaromana.storage.path.LevelPathTrackedData;
import net.rasanovum.viaromana.storage.player.PlayerTrackedData;
import net.rasanovum.viaromana.util.VersionUtils;

public class DataInit {
    public static void load() {
        ViaRomana.LOGGER.info("Via Romana Data Anchored");
    }

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

    public static final TrackedDataKey<LevelCornerTrackedData> CHUNK_CORNER_KEY =
        TrackedDataRegistries.LEVEL.register(
            VersionUtils.getLocation("viaromana:chunk_corners"),
            LevelCornerTrackedData.class,
            (key, level) -> {
                if (level instanceof ServerLevel serverLevel) {
                    return new LevelCornerTrackedData(key, serverLevel);
                }
                return null;
            }
        );

    public static final TrackedDataKey<LevelPathTrackedData> PATH_GRAPH_KEY =
        TrackedDataRegistries.LEVEL.register(
            VersionUtils.getLocation("viaromana:path_graph"),
            LevelPathTrackedData.class,
            (key, level) -> {
                if (level instanceof ServerLevel serverLevel) {
                    return new LevelPathTrackedData(key, serverLevel);
                }
                return null;
            }
        );

    public static final TrackedDataKey<PlayerTrackedData> PLAYER_DATA_KEY =
        TrackedDataRegistries.ENTITY.register(
            VersionUtils.getLocation("viaromana:player_data"),
            PlayerTrackedData.class,
            (key, entity) -> {
                if (entity instanceof net.minecraft.world.entity.player.Player player) {
                    return new PlayerTrackedData(key, player);
                }
                return null;
            }
        );
}