package net.rasanovum.viaromana.forge.triggers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rasanovum.viaromana.core.DimensionHandler;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

@Mod.EventBusSubscriber(modid = ViaRomanaMod.MODID)
public class OnDimensionChange {

    public OnDimensionChange() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DimensionHandler.preventHopping(player.level(), player);
        }
    }
}