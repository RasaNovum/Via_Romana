package net.rasanovum.viaromana.util;

import net.minecraft.resources.ResourceLocation;

public class VersionUtils {
    public static ResourceLocation getLocation(String location) {
        //? if <1.21 {
        /*return new ResourceLocation(location);
        *///?} else {
        return ResourceLocation.parse(location);
        //?}
    }

    public static ResourceLocation getLocation(String base, String path) {
        //? if <1.21 {
        /*return new ResourceLocation(base, path);
        *///?} else {
        return ResourceLocation.parse(base + ":" + path);
         //?}
    }
}