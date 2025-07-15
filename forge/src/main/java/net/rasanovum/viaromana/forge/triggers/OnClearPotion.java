package net.rasanovum.viaromana.forge.triggers;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.common.Mod;
import net.rasanovum.viaromana.core.SignInteract;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

@Mod.EventBusSubscriber(modid = ViaRomanaMod.MODID)
public class OnClearPotion {
    private static boolean cancelEvent = false;
    
    public OnClearPotion() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelEvent = false;
        
        SignInteract.clicked(event.getLevel(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity());
        
        if (cancelEvent) {
            event.setCanceled(true);
            event.setResult(Event.Result.DENY);
        }
    }
    
    public static void setCancelEvent(boolean cancel) {
        cancelEvent = cancel;
    }
}
