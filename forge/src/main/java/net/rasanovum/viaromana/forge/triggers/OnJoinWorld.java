package net.rasanovum.viaromana.forge.triggers;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rasanovum.viaromana.core.ResetVariables;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

@Mod.EventBusSubscriber(modid = ViaRomanaMod.MODID)
public class OnJoinWorld {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        execute(event.getEntity().getLevel(), event.getEntity());
    }

    private static void execute(LevelAccessor world, Entity entity) {
        if (entity != null) {
            ResetVariables.execute(world, entity);
        }
    }
}
