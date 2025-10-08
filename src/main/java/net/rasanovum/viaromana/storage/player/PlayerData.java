package net.rasanovum.viaromana.storage.player;

import dev.corgitaco.dataanchor.data.TrackedDataContainer;
import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import dev.corgitaco.dataanchor.data.type.entity.EntityTrackedData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.rasanovum.viaromana.init.DataInit;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class PlayerData {
    /**
     * Generic method to get any value from PlayerTrackedData
     */
    private static <T> Optional<T> getValue(Player player, Function<PlayerTrackedData, T> getter) {
        TrackedDataContainer<Entity, EntityTrackedData> container = TrackedDataRegistries.ENTITY.getContainer(player);
        if (container == null) return Optional.empty();

        return container.dataAnchor$getTrackedData(DataInit.PLAYER_DATA_KEY)
                .filter(data -> data instanceof PlayerTrackedData)
                .map(getter);
    }

    /**
     * Generic method to set any value in PlayerTrackedData
     */
    private static void setValue(Player player, Consumer<PlayerTrackedData> setter) {
        TrackedDataContainer<Entity, EntityTrackedData> container = TrackedDataRegistries.ENTITY.getContainer(player);
        if (container == null) return;

        Optional<PlayerTrackedData> existing = container.dataAnchor$getTrackedData(DataInit.PLAYER_DATA_KEY)
                .filter(data -> data instanceof PlayerTrackedData);
        
        if (existing.isEmpty()) {
            container.dataAnchor$createTrackedData();
        }

        container.dataAnchor$getTrackedData(DataInit.PLAYER_DATA_KEY)
                .filter(data -> data instanceof PlayerTrackedData)
                .ifPresent(setter);
    }

    // Setters & Getters
    public static Optional<Boolean> isChartingPath(Player player) {
        return getValue(player, PlayerTrackedData::isChartingPath);
    }

    public static void setChartingPath(Player player, boolean value) {
        setValue(player, data -> data.setChartingPath(value));
    }

    public static Optional<Double> getFadeAmount(Player player) {
        return getValue(player, PlayerTrackedData::getFadeAmount);
    }

    public static void setFadeAmount(Player player, double value) {
        setValue(player, data -> data.setFadeAmount(value));
    }

    public static Optional<Boolean> isFadeIncrease(Player player) {
        return getValue(player, PlayerTrackedData::isFadeIncrease);
    }

    public static void setFadeIncrease(Player player, boolean value) {
        setValue(player, data -> data.setFadeIncrease(value));
    }

    public static Optional<BlockPos> getLastNodePos(Player player) {
        return getValue(player, PlayerTrackedData::getLastNodePos);
    }

    public static void setLastNodePos(Player player, BlockPos value) {
        setValue(player, data -> data.setLastNodePos(value));
    }

    public static Optional<Boolean> hasReceivedTutorial(Player player) {
        return getValue(player, PlayerTrackedData::hasReceivedTutorial);
    }

    public static void setReceivedTutorial(Player player, boolean value) {
        setValue(player, data -> data.setReceivedTutorial(value));
    }

    public static void resetVariables(Player player) {
        setValue(player, PlayerTrackedData::resetVariables);
    }
}