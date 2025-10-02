package net.rasanovum.viaromana.mixins;

//? if fabric {
import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.util.RegistryPalette;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RegistryPalette.class, remap = false)
public class RegistryPaletteMixin {

    @Shadow private int[] inverse;

    /**
     * Overwrites .find(int value) with bounds checking to prevent ArrayIndexOutOfBoundsException on invalid registry IDs during chunk loading.
     */
    @Overwrite
    public int find(int value) {
        if (value < 0 || value >= inverse.length) {
            Surveyor.LOGGER.warn("[Via Romana RegistryPaletteMixin] Invalid registry ID {} in RegistryPalette.find (array length: {}); returning -1.", value, inverse.length);
            return -1;
        }
        return inverse[value];
    }
}
//?}