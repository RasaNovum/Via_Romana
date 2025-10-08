package net.rasanovum.viaromana.storage.player;

import dev.corgitaco.dataanchor.data.TrackedDataContainer;
import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import dev.corgitaco.dataanchor.data.type.entity.EntityTrackedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.rasanovum.viaromana.init.DataInit;

import java.util.Optional;

public class PlayerDataManager {
    /**
     * Gets charting path from Data Anchor for a player entity.
     */
    public static Optional<Boolean> isChartingPath(Player player) {
        TrackedDataContainer<Entity, EntityTrackedData> container = TrackedDataRegistries.ENTITY.getContainer(player);
        if (container == null) return Optional.empty();

        return container.dataAnchor$getTrackedData(DataInit.PLAYER_DATA_KEY)
                .filter(data -> data instanceof PlayerTrackedData)
                .map(PlayerTrackedData::isChartingPath);
    }

    /**
     * Sets charting path to Data Anchor for a player entity.
     */
    public static void setChartingPath(Player player, boolean value) {
        TrackedDataContainer<Entity, EntityTrackedData> container = TrackedDataRegistries.ENTITY.getContainer(player);
        if (container == null) return;

        Optional<PlayerTrackedData> existing = container.dataAnchor$getTrackedData(DataInit.PLAYER_DATA_KEY)
                .filter(data -> data instanceof PlayerTrackedData)
                .map(data -> (PlayerTrackedData) data);
        
        if (existing.isEmpty()) {
            container.dataAnchor$createTrackedData();
        }

        container.dataAnchor$getTrackedData(DataInit.PLAYER_DATA_KEY)
                .filter(data -> data instanceof PlayerTrackedData)
                .ifPresent(data -> ((PlayerTrackedData) data).setChartingPath(value));
    }
}
