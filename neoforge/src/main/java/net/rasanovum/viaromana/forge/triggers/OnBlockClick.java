package net.rasanovum.viaromana.forge.triggers;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.rasanovum.viaromana.core.SignInteract;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

@EventBusSubscriber(modid = ViaRomanaMod.MODID)
public class OnBlockClick {
    private static boolean cancelEvent = false;
    
    public OnBlockClick() {
        NeoForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelEvent = false;
        
        SignInteract.clicked(event.getLevel(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity());
        
        if (cancelEvent) {
            event.setCanceled(true);
            event.setUseBlock(TriState.FALSE);
        }
    }
    
    public static void setCancelEvent(boolean cancel) {
        cancelEvent = cancel;
    }
}