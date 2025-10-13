package net.rasanovum.viaromana.client;

//? if fabric
@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
//? if neoforge
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)*/
public class ColorUtil {
    /**
     * Converts RGB float values to hexadecimal color code.
     * 
     * @param r Red component (0.0f to 1.0f)
     * @param g Green component (0.0f to 1.0f)
     * @param b Blue component (0.0f to 1.0f)
     * @return Hexadecimal color code as integer (e.g., 0xFFFFFF for white)
     */
    public static int rgbToHex(float r, float g, float b) { // I hate hex colors so I made this to not deal with them
        r = Math.max(0.0f, Math.min(1.0f, r));
        g = Math.max(0.0f, Math.min(1.0f, g));
        b = Math.max(0.0f, Math.min(1.0f, b));
        
        int red = (int) (r * 255.0f + 0.5f);
        int green = (int) (g * 255.0f + 0.5f);
        int blue = (int) (b * 255.0f + 0.5f);
        
        return (red << 16) | (green << 8) | blue;
    }
}