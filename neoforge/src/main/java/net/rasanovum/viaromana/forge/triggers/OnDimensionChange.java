package net.rasanovum.viaromana.forge.triggers;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.rasanovum.viaromana.core.DimensionHandler;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

@EventBusSubscriber(modid = ViaRomanaMod.MODID)
public class OnDimensionChange {

    public OnDimensionChange() {
        NeoForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DimensionHandler.preventHopping(player.level(), player);
        }
    }
}