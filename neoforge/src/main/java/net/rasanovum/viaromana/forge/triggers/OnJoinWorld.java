package net.rasanovum.viaromana.forge.triggers;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.rasanovum.viaromana.core.ResetVariables;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

@EventBusSubscriber(modid = ViaRomanaMod.MODID)
public class OnJoinWorld {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        execute(event.getEntity().level(), event.getEntity());
    }

    private static void execute(LevelAccessor world, Entity entity) {
        if (entity != null) {
            ResetVariables.execute(world, entity);
        }
    }
}
