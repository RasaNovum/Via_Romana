package net.rasanovum.viaromana.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rasanovum.viaromana.util.VersionUtils;

/**
 * Client-side manager for teleport fade effects.
 */
@Environment(EnvType.CLIENT)
public class FadeManager {
    private static int fadeUpTicks = 0;
    private static int holdTicks = 0;
    private static int fadeDownTicks = 0;
    private static int footstepInterval = 0;
    private static int currentTick = 0;
    private static boolean active = false;
    
    private FadeManager() {}

    /**
     * Returns if the given client is mid-warp
     */
    public static boolean isActive() {
        return active;
    }
    
    /**
     * Start a new fade effect with the given timing parameters
     */
    public static void startFade(int fadeUp, int hold, int fadeDown, int footstep) {
        fadeUpTicks = fadeUp;
        holdTicks = hold;
        fadeDownTicks = fadeDown;
        footstepInterval = footstep;
        currentTick = 0;
        active = true;
    }
    
    /**
     * Called every client tick to advance the fade effect
     */
    public static void onClientTick() {
        if (!active) return;
        
        currentTick++;
        
        // Play footstep sounds at intervals
        if (footstepInterval > 0 && currentTick % footstepInterval == 0) {
            playFootstep();
        }
        
        int totalTicks = fadeUpTicks + holdTicks + fadeDownTicks;
        if (currentTick >= totalTicks) {
            active = false;
            currentTick = 0;
        }
    }
    
    /**
     * Get the current fade alpha value (0.0 to 1.0)
     */
    public static float getCurrentFadeAlpha() {
        if (!active) return 0f;
        
        int tick = currentTick;

        if (tick < fadeUpTicks) {
            return (float) tick / fadeUpTicks;
        }

        tick -= fadeUpTicks;
        if (tick < holdTicks) {
            return 1.0f;
        }

        tick -= holdTicks;
        if (tick < fadeDownTicks) {
            return 1.0f - ((float) tick / fadeDownTicks);
        }
        
        return 0f;
    }
    
    /**
     * Play a footstep sound based on the block the player is standing on
     */
    private static void playFootstep() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        
        if (player == null || level == null) return;
        
        BlockPos soundSource = player.getOnPos();
        BlockState blockState = level.getBlockState(soundSource);
        ResourceLocation soundId = BuiltInRegistries.SOUND_EVENT.getKey(blockState.getSoundType().getStepSound());
        
        if (soundId == null) {
            soundId = VersionUtils.getLocation("minecraft:block.grass.step");
        }
        
        level.playLocalSound(
            soundSource.getX(),
            soundSource.getY(),
            soundSource.getZ(),
            BuiltInRegistries.SOUND_EVENT.get(soundId),
            SoundSource.BLOCKS,
            0.2f,
            1.0f,
            false
        );
    }
}
