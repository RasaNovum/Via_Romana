package net.rasanovum.viaromana.forge.triggers;

import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rasanovum.viaromana.core.SignInteract;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

@Mod.EventBusSubscriber(modid = ViaRomanaMod.MODID)
public class OnBlockBreak {
    public OnBlockBreak() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        SignInteract.broken(event.getLevel(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getPlayer());
    }
}
