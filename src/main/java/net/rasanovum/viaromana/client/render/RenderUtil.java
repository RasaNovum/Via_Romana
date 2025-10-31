package net.rasanovum.viaromana.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rasanovum.viaromana.tags.TagGenerator;
import net.rasanovum.viaromana.util.PathUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RenderUtil {
    @net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    public static double findSuitableYPosition(ClientLevel level, BlockPos pos, float offsetY) {
        return findSuitableYPosition(level, pos, offsetY, 5, 5);
    }

    @net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    public static double findSuitableYPosition(ClientLevel level, BlockPos pos, float offsetY, int maxUp, int maxDown) {
        if (level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false) == null) {
            return pos.getY() + offsetY;
        }

        BlockPos belowSource = pos.below();
        BlockState belowSourceState = level.getBlockState(belowSource);
        VoxelShape belowSourceShape = belowSourceState.getCollisionShape(level, belowSource, CollisionContext.empty());
        double sourceSurfaceY = belowSourceShape.isEmpty() ? pos.getY() : belowSource.getY() + belowSourceShape.max(Direction.Axis.Y);
        
        List<Double> validPositions = new ArrayList<>();
        
        for (int yOffset = -maxDown; yOffset <= maxUp; yOffset++) {
            BlockPos checkPos = pos.offset(0, yOffset, 0);
            BlockState checkState = level.getBlockState(checkPos);
            VoxelShape checkShape = checkState.getCollisionShape(level, checkPos, CollisionContext.empty());
            
            if (!checkShape.isEmpty() || PathUtils.isBlockValidPath(level, checkPos)) {
                BlockPos abovePos = checkPos.above();
                BlockState aboveState = level.getBlockState(abovePos);
                VoxelShape aboveShape = aboveState.getCollisionShape(level, abovePos, CollisionContext.empty());
                
                if (aboveShape.isEmpty() && !PathUtils.isBlockValidPath(level, abovePos)) {
                    double surfaceY = checkShape.isEmpty() ? checkPos.getY() + 1.0 : checkPos.getY() + checkShape.max(Direction.Axis.Y);
                    validPositions.add(surfaceY + offsetY);
                }
            }
        }
        
        if (validPositions.isEmpty()) {
            return sourceSurfaceY + offsetY;
        }
        
        double biasedTarget = sourceSurfaceY + offsetY + 1.0;
        return validPositions.stream()
                .min(Comparator.comparingDouble(a -> Math.abs(a - biasedTarget)))
                .orElse(sourceSurfaceY + offsetY);
    }
}
