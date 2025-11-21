package net.rasanovum.viaromana.teleport;

import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.path.Node;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
     * Apply teleport effect (server-side particles)
     */
    public static void effect(LevelAccessor world, Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (!(world instanceof ServerLevel serverLevel)) return;
        
        double fadeAmount = 10;
        double particleRadius = 4;
        
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
