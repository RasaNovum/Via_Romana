package net.rasanovum.viaromana.forge.triggers;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.rasanovum.viaromana.core.SignInteract;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

@EventBusSubscriber(modid = ViaRomanaMod.MODID)
public class OnBlockBreak {
    public OnBlockBreak() {
        NeoForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        SignInteract.broken(event.getLevel(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getPlayer());
    }
}
