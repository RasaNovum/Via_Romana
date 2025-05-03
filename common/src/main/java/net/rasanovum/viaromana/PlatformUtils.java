package net.rasanovum.viaromana;

import dev.architectury.injectables.annotations.ExpectPlatform;
import org.slf4j.Logger;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

import java.nio.file.Path;

public class PlatformUtils {

    @ExpectPlatform
    public static Logger getLogger() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static String getModId() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Path getConfigDirectory() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean hasEffect(Entity entity, String effectId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void applyEffect(Entity entity, String effectId, LevelAccessor world) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setString(LevelAccessor world, BlockPos pos, String tagName, String tagValue) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static String getString(LevelAccessor world, BlockPos pos, String tagName) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isModLoaded(String modId) {
		throw new AssertionError();
	}

    @ExpectPlatform
    public static void cancelClickEvent(Boolean cancel) {
        throw new AssertionError();
    }
}
