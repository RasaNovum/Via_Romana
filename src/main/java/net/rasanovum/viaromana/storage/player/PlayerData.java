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
    private static <T> T getValue(Player player, Function<PlayerTrackedData, T> getter, T defaultValue) {
        TrackedDataContainer<Entity, EntityTrackedData> container = TrackedDataRegistries.ENTITY.getContainer(player);
        if (container == null) return defaultValue;

        return container.dataAnchor$getTrackedData(DataInit.PLAYER_DATA_KEY)
                .filter(data -> data instanceof PlayerTrackedData)
                .map(getter)
                .orElse(defaultValue);
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
    public static boolean isChartingPath(Player player) {
        return getValue(player, PlayerTrackedData::isChartingPath, false);
    }

    public static void setChartingPath(Player player, boolean value) {
        setValue(player, data -> data.setChartingPath(value));
    }

    public static BlockPos getLastNodePos(Player player) {
        return getValue(player, PlayerTrackedData::getLastNodePos, null);
    }

    public static void setLastNodePos(Player player, BlockPos value, boolean sync) {
        setValue(player, data -> data.setLastNodePos(value, sync));
    }

    public static void setReceivedTutorial(Player player, boolean value) {
        setValue(player, data -> data.setReceivedTutorial(value));
    }

    public static boolean hasSeenLinkedSignToast(Player player) {
        return getValue(player, PlayerTrackedData::hasSeenLinkedSignToast, false);
    }

    public static void setSeenLinkedSignToast(Player player, boolean value) {
        setValue(player, data -> data.setSeenLinkedSignToast(value));
    }

    public static void resetVariables(Player player) {
        setValue(player, PlayerTrackedData::resetVariables);
    }

    public static double getDistanceWalkedOnPath(Player player) {
        return getValue(player, PlayerTrackedData::getDistanceWalkedOnPath, 0.0);
    }

    public static void addDistanceWalkedOnPath(Player player, double distance) {
        setValue(player, data -> data.addDistanceWalkedOnPath(distance));
    }

    public static BlockPos getLastPathWalkPosition(Player player) {
        return getValue(player, PlayerTrackedData::getLastPathWalkPosition, null);
    }

    public static void setLastPathWalkPosition(Player player, BlockPos pos) {
        setValue(player, data -> data.setLastPathWalkPosition(pos));
    }

    public static void syncPlayerData(Player player) {
        TrackedDataContainer<Entity, EntityTrackedData> container = TrackedDataRegistries.ENTITY.getContainer(player);
        if (container == null) return;

        container.dataAnchor$getTrackedData(DataInit.PLAYER_DATA_KEY)
                .filter(data -> data instanceof PlayerTrackedData)
                .ifPresent(PlayerTrackedData::sync);
    }
}