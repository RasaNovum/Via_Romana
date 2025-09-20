package net.rasanovum.viaromana.forge;

import net.rasanovum.viaromana.forge.util.BlockTagHelper;
import net.rasanovum.viaromana.forge.util.MobEffectHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.rasanovum.viaromana.forge.triggers.OnBlockClick;

import org.slf4j.Logger;

import java.nio.file.Path;

public class PlatformUtilsImpl {
    public static Logger getLogger() {
        return org.slf4j.LoggerFactory.getLogger(ViaRomanaMod.MODID);
    }

    public static String getModId() {
        return ViaRomanaMod.MODID;
    }

    public static Path getConfigDirectory() {
        throw new AssertionError();
    }

    public static boolean hasEffect(Entity entity, String effectId) {
        return MobEffectHelper.hasEffect(entity, effectId);
    }

    public static void applyEffect(Entity entity, String effectId, LevelAccessor world) {
        MobEffectHelper.applyEffect(entity, effectId, world);
    }

    public static void setString(LevelAccessor world, BlockPos pos, String tagName, String tagValue) {
		BlockTagHelper.setString(world, pos, tagName, tagValue);
	}

    public static String getString(LevelAccessor world, BlockPos pos, String tagName) {
		return BlockTagHelper.getString(world, pos, tagName);
	}

    public static boolean isModLoaded(String modId) {
        return net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }

    public static void cancelClickEvent(Boolean cancel) {
        OnBlockClick.setCancelEvent(cancel);
    }
}
