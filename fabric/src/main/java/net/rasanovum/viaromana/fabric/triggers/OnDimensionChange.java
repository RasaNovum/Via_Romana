package net.rasanovum.viaromana.fabric.triggers;

import net.minecraft.world.level.LevelAccessor;
import net.rasanovum.viaromana.core.DimensionHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;

public class OnDimensionChange {
    public OnDimensionChange() {
        ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register((originalEntity, newEntity, origin, destination) -> {
            if (!(newEntity instanceof Player))
                execute(destination, newEntity);
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            execute(destination, player);
        });
    }

    private static void execute(LevelAccessor world, Entity entity) {
        if (entity == null)
            return;

        DimensionHandler.preventHopping(world, entity);        
    }
}
