package net.rasanovum.viaromana.forge.triggers;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rasanovum.viaromana.forge.ViaRomanaMod;
import net.rasanovum.viaromana.forge.configuration.ConfigVariables;

@Mod.EventBusSubscriber(modid = ViaRomanaMod.MODID)
public class OnWorldLoad {

    public OnWorldLoad() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ConfigVariables.load(serverLevel);
        }
    }
}
