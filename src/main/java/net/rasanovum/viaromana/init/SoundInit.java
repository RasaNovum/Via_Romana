package net.rasanovum.viaromana.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.util.VersionUtils;

/**
 * Initialization for custom sound events.
 */
public class SoundInit {
    
    public static final SoundEvent NODE_PULSE = register("ambient.node_pulse");
    
    private static SoundEvent register(String name) {
        ResourceLocation id = VersionUtils.getLocation("via_romana:" + name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
    
    public static void load() {
        ViaRomana.LOGGER.info("Loaded custom sound events");
    }
}
