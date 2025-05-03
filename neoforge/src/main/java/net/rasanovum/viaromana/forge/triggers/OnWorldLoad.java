package net.rasanovum.viaromana.forge.triggers;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.rasanovum.viaromana.forge.ViaRomanaMod;
import net.rasanovum.viaromana.forge.configuration.ConfigVariables;

@EventBusSubscriber(modid = ViaRomanaMod.MODID)
public class OnWorldLoad {

    public OnWorldLoad() {
        NeoForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ConfigVariables.load(serverLevel);
        }
    }
}
