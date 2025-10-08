package net.rasanovum.viaromana.teleport;

import net.rasanovum.viaromana.util.EffectUtils;
import net.rasanovum.viaromana.util.VersionUtils;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.storage.player.PlayerData;
import net.rasanovum.viaromana.path.Node;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public class TeleportHelper {
    
    /**
     * Find the destination position when teleporting from source to destination signs via the node graph
     */
    public static Optional<BlockPos> findDestination(BlockEntity source, BlockEntity dest, Level level) {
        BlockPos sourcePos = source.getBlockPos();
        BlockPos destPos = dest.getBlockPos();
        
        PathGraph graph = PathGraph.getInstance((ServerLevel) level);
        
        Optional<Node> sourceNode = graph.getNodeBySignPos(sourcePos);
        Optional<Node> destNode = graph.getNodeBySignPos(destPos);
        
        if (sourceNode.isEmpty() || destNode.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(destNode.get().getBlockPos());
    }

    /**
     * Cycle the teleport effect
     */
    public static void cycle(LevelAccessor world, Entity entity) {
        if (!(entity instanceof Player player)) return;
        double fadeAmount = PlayerData.getFadeAmount(player);
        boolean isIncreasing = PlayerData.isFadeIncrease(player);
        
        if (fadeAmount > 0 || (fadeAmount == 0 && isIncreasing)) {
            handleFadeEffect(world, entity);
            
            fadeAmount = PlayerData.getFadeAmount(player);
            if (fadeAmount >= 15) {
                PlayerData.setFadeIncrease(player, false);
            }
            
            if (fadeAmount == 0 && !PlayerData.isFadeIncrease(player)) {
                PlayerData.setFadeAmount(player, 0);
                PlayerData.setFadeIncrease(player, false);
                PlayerData.setLastNodePos(player, BlockPos.ZERO);
            }
        }
    }

    /**
     * Apply teleport effect
     */
    public static void effect(LevelAccessor world, Entity entity) {
        if (!(entity instanceof Player player)) return;
        
        double fadeAmount = PlayerData.getFadeAmount(player);
        
        if (fadeAmount > 0) {
            double particleRadius = 4;
            if (world instanceof ServerLevel serverLevel) { // TODO: Allow disabling on client-side
                serverLevel.sendParticles(
                    ParticleTypes.ENCHANT, 
                    (player.getX() + Mth.nextDouble(RandomSource.create(), -0.1, 0.1) * particleRadius), 
                    (player.getY() + fadeAmount * 0.15),
                    (player.getZ() + Mth.nextDouble(RandomSource.create(), -0.1, 0.1) * particleRadius), 
                    32, 
                    (Mth.nextDouble(RandomSource.create(), -0.1, 0.1) * particleRadius), 
                    (fadeAmount * 0.02),
                    (Mth.nextDouble(RandomSource.create(), -0.1, 0.1) * particleRadius), 
                    0.5
                );
            }
        }
    }

    private static void playFootstepSound(LevelAccessor world, BlockPos soundSource) {
        if (!(world instanceof Level level)) return;
        
        BlockState blockState = world.getBlockState(soundSource);
        ResourceLocation soundId = BuiltInRegistries.SOUND_EVENT.getKey(blockState.getSoundType().getStepSound());

        if (soundId == null) soundId = VersionUtils.getLocation("minecraft:block.grass.step");

        if (!level.isClientSide()) {
            level.playSound(null, soundSource, BuiltInRegistries.SOUND_EVENT.get(soundId), SoundSource.BLOCKS, 0.2f, 1f);
        } else {
            level.playLocalSound(soundSource.getX(), soundSource.getY(), soundSource.getZ(), BuiltInRegistries.SOUND_EVENT.get(soundId), SoundSource.BLOCKS, 0.2f, 1f, false);
        }
    }
    
    private static void handleFadeEffect(LevelAccessor world, Entity entity) {
        if (!(entity instanceof Player player)) return;

        double fadeAmount = PlayerData.getFadeAmount(player);

        if (fadeAmount >= 0 && fadeAmount <= 15) {
            if (fadeAmount % 7 == 0) playFootstepSound(world, player.getOnPos());
            
            boolean isIncreasing = PlayerData.isFadeIncrease(player);
            double newFadeAmount = isIncreasing ? fadeAmount + 1 : fadeAmount - 1;
            
            newFadeAmount = Math.max(0, Math.min(15, newFadeAmount));
            
            PlayerData.setFadeAmount(player, newFadeAmount); // TODO: Prevent network spam

            if (fadeAmount == 1) EffectUtils.applyEffect(entity, "travellers_fatigue", world);
        }
    }
    
    /**
     * Teleport destination with metadata.
     */
    public static class TeleportDestination {
        public final BlockPos position;
        public final String name;
        public final double distance;
        public final Node.Icon icon;

        public TeleportDestination(BlockPos position, String name, double distance, Node.Icon icon) {
            this.position = position;
            this.name = name;
            this.distance = distance;
            this.icon = icon;
        }
    }
}
